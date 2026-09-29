package com.cometchat.uikit.core.viewmodel.conversations

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.helpers.CometChatHelper
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.ConversationUpdateSettings
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatGroupEvent
import com.cometchat.uikit.core.events.CometChatUserEvent
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever

/**
 * How the conversation list reacts to activity arriving in real time.
 *
 * Everything a user sees move in that list — a chat jumping to the top, an unread badge ticking
 * up, a row vanishing when they leave a group — runs through one private function,
 * `updateConversation`. It is reached only from the SDK's `MessageListener` and the in-process
 * event bus, so with `enableListeners = false` (what every earlier spec used) none of it runs.
 * These tests turn the listeners on, mock the SDK statics so registration is inert, and then drive
 * the captured listener directly, exactly as a socket delivery would.
 *
 * The rules being pinned, and why each one matters if it breaks:
 *
 * - **Pinned conversations do not move.** A pinned row keeps its slot when it updates, and new
 *   activity enters *below* the pinned block rather than at index 0 — otherwise a pinned chat gets
 *   shoved down the list by whoever messaged most recently, which is the opposite of pinning.
 * - **Pin state survives an update.** `CometChatHelper.getConversationFromMessage()` builds a
 *   conversation from a message and has no idea the viewer pinned it, so the update has to carry
 *   `pinnedAt`/`pinnedBy` across from the copy already loaded or the pin silently disappears.
 * - **The unread badge counts only what should be counted.** Not your own messages, not group
 *   action messages, not a message you have already read, and not the same message twice.
 * - **The type filter applies to arrivals too.** A list built with a group-only filter must not
 *   sprout a one-to-one conversation because someone sent a direct message.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*ConversationRealtimeUpdateTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationRealtimeUpdateTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val me = "logged_in_user"
    val testDispatcher = UnconfinedTestDispatcher()

    lateinit var getConversationListUseCase: GetConversationListUseCase
    lateinit var deleteConversationUseCase: DeleteConversationUseCase
    lateinit var refreshConversationListUseCase: RefreshConversationListUseCase
    lateinit var cometChatMock: MockedStatic<CometChat>
    lateinit var helperMock: MockedStatic<CometChatHelper>
    lateinit var updateSettings: ConversationUpdateSettings

    // Listener-enabled VMs launch bus collectors on viewModelScope. Hosting each in a
    // ViewModelStore and clearing it in afterTest keeps those from outliving the spec and
    // getting their leaked exceptions pinned on whichever spec runs next.
    val stores = mutableListOf<androidx.lifecycle.ViewModelStore>()

    fun user(uid: String) = User().apply { this.uid = uid; name = uid }
    fun group(guid: String) = Group().apply { this.guid = guid; name = guid }

    /** A real Conversation -- the VM clones it and mutates the copy, which a mock cannot model. */
    fun conversation(
        id: String,
        type: String = UIKitConstants.ConversationType.USERS,
        with: String = "partner",
        unread: Int = 0,
        pinnedAt: Long = 0L,
        last: BaseMessage? = null
    ): Conversation = Conversation(id, type).apply {
        conversationWith = if (type == UIKitConstants.ConversationType.GROUPS) group(with) else user(with)
        unreadMessageCount = unread
        this.pinnedAt = pinnedAt
        if (pinnedAt > 0L) pinnedBy = me
        lastMessage = last
    }

    fun text(id: Long, from: String, to: String = me, readAt: Long = 0L): TextMessage =
        TextMessage(to, "message $id", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            this.readAt = readAt
            sender = user(from)
        }

    fun viewModel(loaded: List<Conversation>): CometChatConversationsViewModel {
        val store = androidx.lifecycle.ViewModelStore()
        stores += store
        return androidx.lifecycle.ViewModelProvider(
            store,
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                    CometChatConversationsViewModel(
                        getConversationListUseCase,
                        deleteConversationUseCase,
                        refreshConversationListUseCase,
                        enableListeners = true
                    ).also { vm ->
                        // The constructor already fetched; a second call appends nothing new.
                        vm.conversations
                    } as T
            }
        )[CometChatConversationsViewModel::class.java].also {
            // Seeding happens through the use case the constructor's fetch consumes.
            loaded.size
        }
    }

    /** The MessageListener the VM handed the SDK -- the object a socket delivery would call. */
    fun listenerOf(): CometChat.MessageListener {
        val captor = argumentCaptor<CometChat.MessageListener>()
        cometChatMock.verify({ CometChat.addMessageListener(any(), captor.capture()) }, atLeastOnce())
        return captor.lastValue
    }

    /** VM pre-loaded with [loaded], plus its captured MessageListener. */
    suspend fun listening(
        loaded: List<Conversation>
    ): Pair<CometChatConversationsViewModel, CometChat.MessageListener> {
        whenever(getConversationListUseCase.invoke(any())).thenReturn(Result.success(loaded))
        whenever(getConversationListUseCase.hasMore()).thenReturn(false)
        val vm = viewModel(loaded)
        return vm to listenerOf()
    }

    /** What `CometChatHelper` would hand back for a message arriving in [conversation]. */
    fun helperReturns(conversation: Conversation?) {
        helperMock.`when`<Conversation?> { CometChatHelper.getConversationFromMessage(any()) }
            .thenReturn(conversation)
    }

    beforeTest {
        Dispatchers.setMain(testDispatcher)

        cometChatMock = Mockito.mockStatic(CometChat::class.java)
        cometChatMock.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(user(me))

        updateSettings = mock()
        whenever(updateSettings.shouldUpdateOnMessageReplies()).thenReturn(true)
        whenever(updateSettings.shouldUpdateOnCustomMessages()).thenReturn(true)
        cometChatMock.`when`<ConversationUpdateSettings> { CometChat.getConversationUpdateSettings() }
            .thenReturn(updateSettings)

        helperMock = Mockito.mockStatic(CometChatHelper::class.java)

        getConversationListUseCase = mock()
        deleteConversationUseCase = mock()
        refreshConversationListUseCase = mock()
    }

    afterTest {
        // Clear while the statics are still mocked: onCleared() removes the SDK listeners.
        stores.forEach { it.clear() }
        stores.clear()
        helperMock.close()
        cometChatMock.close()
        Dispatchers.resetMain()
    }

    // ==================== an update to a conversation already in the list ====================

    test("an edit to a loaded conversation replaces it rather than duplicating it") {
        val loaded = conversation("c1", last = text(1, "partner"))
        val (vm, listener) = listening(listOf(loaded))

        helperReturns(conversation("c1", last = text(2, "partner")))
        listener.onMessageEdited(text(2, "partner"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c1")
    }

    test("an update carries the conversationWith already loaded, which is the richer copy") {
        val loaded = conversation("c1", with = "partner", last = text(1, "partner"))
        val (vm, listener) = listening(listOf(loaded))

        // The helper-built conversation knows only the uid, not the display name.
        helperReturns(Conversation("c1", UIKitConstants.ConversationType.USERS).apply {
            conversationWith = User().apply { uid = "partner" }
            lastMessage = text(2, "partner")
        })
        listener.onMessageEdited(text(2, "partner"))

        (vm.conversations.value.single().conversationWith as User).name shouldBe "partner"
    }

    test("a conversation with no last message is ignored") {
        val loaded = conversation("c1", last = text(1, "partner"))
        val (vm, listener) = listening(listOf(loaded))

        helperReturns(conversation("c1", last = null))
        listener.onMessageEdited(text(2, "partner"))

        vm.conversations.value.single().lastMessage?.id shouldBe 1L
    }

    test("a null conversation from the helper changes nothing") {
        val loaded = conversation("c1", last = text(1, "partner"))
        val (vm, listener) = listening(listOf(loaded))

        helperReturns(null)
        listener.onMessageDeleted(text(2, "partner"))

        vm.conversations.value.single().lastMessage?.id shouldBe 1L
    }

    // ==================== the unread badge ====================

    test("a new unread message from someone else bumps the count by one") {
        val loaded = conversation("c1", unread = 3, last = text(1, "partner"))
        val (vm, listener) = listening(listOf(loaded))

        helperReturns(conversation("c1", unread = 0, last = text(2, "partner")))
        listener.onMessageEdited(text(2, "partner"))

        vm.conversations.value.single().unreadMessageCount shouldBe 4
    }

    test("the same message arriving twice bumps the count only once") {
        val loaded = conversation("c1", unread = 3, last = text(1, "partner"))
        val (vm, listener) = listening(listOf(loaded))

        // Same id as what is already the last message: nothing new to be unread about.
        helperReturns(conversation("c1", unread = 0, last = text(1, "partner")))
        listener.onMessageEdited(text(1, "partner"))

        vm.conversations.value.single().unreadMessageCount shouldBe 3
    }

    test("a message already read does not bump the count") {
        val loaded = conversation("c1", unread = 3, last = text(1, "partner"))
        val (vm, listener) = listening(listOf(loaded))

        helperReturns(conversation("c1", unread = 0, last = text(2, "partner", readAt = 5_000L)))
        listener.onMessageEdited(text(2, "partner"))

        vm.conversations.value.single().unreadMessageCount shouldBe 3
    }

    test("your own message never bumps your own unread count") {
        val loaded = conversation("c1", unread = 3, last = text(1, "partner"))
        val (vm, listener) = listening(listOf(loaded))

        helperReturns(conversation("c1", unread = 0, last = text(2, me)))
        listener.onMessageEdited(text(2, me))

        vm.conversations.value.single().unreadMessageCount shouldBe 3
    }

    test("the sender match ignores case, so a differently-cased uid is still you") {
        val loaded = conversation("c1", unread = 3, last = text(1, "partner"))
        val (vm, listener) = listening(listOf(loaded))

        helperReturns(conversation("c1", unread = 0, last = text(2, me.uppercase())))
        listener.onMessageEdited(text(2, me.uppercase()))

        vm.conversations.value.single().unreadMessageCount shouldBe 3
    }

    // ==================== conversations arriving for the first time ====================

    test("a message from a conversation not yet loaded adds it with one unread") {
        val (vm, listener) = listening(listOf(conversation("c1", last = text(1, "partner"))))

        helperReturns(conversation("c2", with = "stranger", last = text(9, "stranger")))
        listener.onMessageEdited(text(9, "stranger"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c2", "c1")
        vm.conversations.value.first().unreadMessageCount shouldBe 1
    }

    test("a conversation you started yourself arrives with nothing unread") {
        val (vm, listener) = listening(listOf(conversation("c1", last = text(1, "partner"))))

        helperReturns(conversation("c2", with = "stranger", last = text(9, me)))
        listener.onMessageEdited(text(9, me))

        vm.conversations.value.first().unreadMessageCount shouldBe 0
    }

    test("a new conversation whose last message is a group action arrives with nothing unread") {
        val (vm, listener) = listening(listOf(conversation("c1", last = text(1, "partner"))))

        val action = Action().apply { id = 9L; sender = user("someone") }
        helperReturns(
            conversation("c2", type = UIKitConstants.ConversationType.GROUPS, with = "g1", last = action)
        )
        listener.onMessageEdited(action)

        vm.conversations.value.first().unreadMessageCount shouldBe 0
    }

    // ==================== the type filter ====================

    test("a group-only list does not sprout a one-to-one conversation") {
        val (vm, listener) = listening(
            listOf(conversation("g1", type = UIKitConstants.ConversationType.GROUPS, with = "alpha",
                last = text(1, "partner")))
        )
        val request = mock<ConversationsRequest>()
        whenever(request.conversationType).thenReturn(UIKitConstants.ConversationType.GROUPS)
        val builder = mock<ConversationsRequest.ConversationsRequestBuilder>()
        whenever(builder.build()).thenReturn(request)
        vm.setConversationsRequestBuilder(builder)

        helperReturns(conversation("c2", with = "stranger", last = text(9, "stranger")))
        listener.onMessageEdited(text(9, "stranger"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("g1")
    }

    test("a group-only list still accepts a group conversation") {
        val (vm, listener) = listening(
            listOf(conversation("g1", type = UIKitConstants.ConversationType.GROUPS, with = "alpha",
                last = text(1, "partner")))
        )
        val request = mock<ConversationsRequest>()
        whenever(request.conversationType).thenReturn(UIKitConstants.ConversationType.GROUPS)
        val builder = mock<ConversationsRequest.ConversationsRequestBuilder>()
        whenever(builder.build()).thenReturn(request)
        vm.setConversationsRequestBuilder(builder)

        helperReturns(conversation("g2", type = UIKitConstants.ConversationType.GROUPS, with = "beta",
            last = text(9, "stranger")))
        listener.onMessageEdited(text(9, "stranger"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("g2", "g1")
    }

    test("a filter of BOTH lets everything through") {
        val (vm, listener) = listening(
            listOf(conversation("g1", type = UIKitConstants.ConversationType.GROUPS, with = "alpha",
                last = text(1, "partner")))
        )
        val request = mock<ConversationsRequest>()
        whenever(request.conversationType).thenReturn(UIKitConstants.ConversationType.BOTH)
        val builder = mock<ConversationsRequest.ConversationsRequestBuilder>()
        whenever(builder.build()).thenReturn(request)
        vm.setConversationsRequestBuilder(builder)

        helperReturns(conversation("c2", with = "stranger", last = text(9, "stranger")))
        listener.onMessageEdited(text(9, "stranger"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c2", "g1")
    }

    test("a request carrying no type filter lets everything through") {
        val (vm, listener) = listening(listOf(conversation("c1", last = text(1, "partner"))))
        val request = mock<ConversationsRequest>()
        whenever(request.conversationType).thenReturn(null)
        val builder = mock<ConversationsRequest.ConversationsRequestBuilder>()
        whenever(builder.build()).thenReturn(request)
        vm.setConversationsRequestBuilder(builder)

        helperReturns(conversation("c2", with = "stranger", last = text(9, "stranger")))
        listener.onMessageEdited(text(9, "stranger"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c2", "c1")
    }

    // ==================== pinning ====================

    test("an update to a pinned conversation leaves it where it is") {
        val pinned = conversation("p1", pinnedAt = 1_000L, last = text(1, "partner"))
        val plain = conversation("c1", with = "other", last = text(2, "other"))
        val (vm, listener) = listening(listOf(pinned, plain))

        helperReturns(conversation("p1", last = text(3, "partner")))
        listener.onMessageEdited(text(3, "partner"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("p1", "c1")
    }

    test("pin state survives an update that does not carry it") {
        val pinned = conversation("p1", pinnedAt = 1_000L, last = text(1, "partner"))
        val (vm, listener) = listening(listOf(pinned))

        // What the helper builds from a message never carries pinnedAt/pinnedBy.
        helperReturns(conversation("p1", pinnedAt = 0L, last = text(3, "partner")))
        listener.onMessageEdited(text(3, "partner"))

        vm.conversations.value.single().isPinned shouldBe true
        vm.conversations.value.single().pinnedBy shouldBe me
    }

    test("an unpinned conversation surfacing stops below the pinned block") {
        val pinned = conversation("p1", pinnedAt = 1_000L, last = text(1, "partner"))
        val a = conversation("c1", with = "a", last = text(2, "a"))
        val b = conversation("c2", with = "b", last = text(3, "b"))
        val (vm, listener) = listening(listOf(pinned, a, b))

        helperReturns(conversation("c2", with = "b", last = text(4, "b")))
        listener.onMessageEdited(text(4, "b"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("p1", "c2", "c1")
    }

    test("a brand-new conversation also enters below the pinned block") {
        val pinned = conversation("p1", pinnedAt = 1_000L, last = text(1, "partner"))
        val a = conversation("c1", with = "a", last = text(2, "a"))
        val (vm, listener) = listening(listOf(pinned, a))

        helperReturns(conversation("c9", with = "new", last = text(9, "new")))
        listener.onMessageEdited(text(9, "new"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("p1", "c9", "c1")
    }

    test("with every conversation pinned a new one lands at the end") {
        val p1 = conversation("p1", pinnedAt = 1_000L, last = text(1, "partner"))
        val p2 = conversation("p2", with = "b", pinnedAt = 900L, last = text(2, "b"))
        val (vm, listener) = listening(listOf(p1, p2))

        helperReturns(conversation("c9", with = "new", last = text(9, "new")))
        listener.onMessageEdited(text(9, "new"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("p1", "p2", "c9")
    }

    // ==================== incoming deliveries ====================

    test("an incoming text message marks itself delivered and surfaces the conversation") {
        val (vm, listener) = listening(listOf(conversation("c1", with = "a", last = text(1, "a"))))

        helperReturns(conversation("c2", with = "partner", last = text(9, "partner")))
        listener.onTextMessageReceived(text(9, "partner"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c2", "c1")
        cometChatMock.verify({ CometChat.markAsDelivered(any<BaseMessage>()) }, atLeastOnce())
    }

    test("your own message coming back over the socket is not marked delivered") {
        listening(listOf(conversation("c1", with = "a", last = text(1, "a")))).let { (_, listener) ->
            helperReturns(conversation("c1", with = "a", last = text(9, me)))
            listener.onTextMessageReceived(text(9, me))
        }

        cometChatMock.verify({ CometChat.markAsDelivered(any<BaseMessage>()) }, never())
    }

    test("with receipts disabled nothing is marked delivered") {
        val (vm, listener) = listening(listOf(conversation("c1", with = "a", last = text(1, "a"))))
        vm.setDisableReceipt(true)

        helperReturns(conversation("c2", with = "partner", last = text(9, "partner")))
        listener.onTextMessageReceived(text(9, "partner"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c2", "c1")
        cometChatMock.verify({ CometChat.markAsDelivered(any<BaseMessage>()) }, never())
    }

    test("a threaded reply is ignored when replies are set not to update the list") {
        whenever(updateSettings.shouldUpdateOnMessageReplies()).thenReturn(false)
        val (vm, listener) = listening(listOf(conversation("c1", with = "a", last = text(1, "a"))))

        helperReturns(conversation("c2", with = "partner", last = text(9, "partner")))
        listener.onTextMessageReceived(text(9, "partner").apply { parentMessageId = 42L })

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c1")
    }

    test("a threaded reply updates the list when replies are set to") {
        val (vm, listener) = listening(listOf(conversation("c1", with = "a", last = text(1, "a"))))

        helperReturns(conversation("c2", with = "partner", last = text(9, "partner")))
        listener.onTextMessageReceived(text(9, "partner").apply { parentMessageId = 42L })

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c2", "c1")
    }

    // ==================== custom messages ====================

    test("a custom message is ignored when custom messages are set not to update the list") {
        whenever(updateSettings.shouldUpdateOnCustomMessages()).thenReturn(false)
        val (vm, listener) = listening(listOf(conversation("c1", with = "a", last = text(1, "a"))))

        val custom = CustomMessage(me, CometChatConstants.RECEIVER_TYPE_USER, "sticker", org.json.JSONObject())
            .apply { id = 9L; sender = user("partner") }
        helperReturns(conversation("c2", with = "partner", last = custom))
        listener.onCustomMessageReceived(custom)

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c1")
    }

    test("a custom message updates the list when custom messages are set to") {
        val (vm, listener) = listening(listOf(conversation("c1", with = "a", last = text(1, "a"))))

        val custom = CustomMessage(me, CometChatConstants.RECEIVER_TYPE_USER, "sticker", org.json.JSONObject())
            .apply { id = 9L; sender = user("partner") }
        helperReturns(conversation("c2", with = "partner", last = custom))
        listener.onCustomMessageReceived(custom)

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c2", "c1")
    }

    test("a plain text message is unaffected by the custom-message setting") {
        whenever(updateSettings.shouldUpdateOnCustomMessages()).thenReturn(false)
        val (vm, listener) = listening(listOf(conversation("c1", with = "a", last = text(1, "a"))))

        helperReturns(conversation("c2", with = "partner", last = text(9, "partner")))
        listener.onTextMessageReceived(text(9, "partner"))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c2", "c1")
    }

    // ==================== rows leaving the list ====================
    // The bus's fire-and-forget emitters dispatch on CometChatEvents' own scope, so an
    // assertion right after one can outrun the collector. The module-internal *Sync variants
    // suspend until every subscriber has taken the event, which is what these need.

    test("leaving a group removes its conversation and leaves the others") {
        val (vm, _) = listening(
            listOf(
                conversation("g1", type = UIKitConstants.ConversationType.GROUPS, with = "alpha", last = text(1, "a")),
                conversation("c1", with = "partner", last = text(2, "partner"))
            )
        )

        CometChatEvents.emitGroupEventSync(CometChatGroupEvent.GroupDeleted(group("alpha")))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c1")
    }

    test("a group event for a group not in the list removes nothing") {
        val (vm, _) = listening(
            listOf(conversation("g1", type = UIKitConstants.ConversationType.GROUPS, with = "alpha",
                last = text(1, "a")))
        )

        CometChatEvents.emitGroupEventSync(CometChatGroupEvent.GroupDeleted(group("beta")))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("g1")
    }

    test("blocking a user removes their conversation and leaves group ones alone") {
        val (vm, _) = listening(
            listOf(
                conversation("c1", with = "partner", last = text(1, "partner")),
                conversation("g1", type = UIKitConstants.ConversationType.GROUPS, with = "alpha", last = text(2, "a"))
            )
        )

        CometChatEvents.emitUserEventSync(CometChatUserEvent.UserBlocked(user("partner")))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("g1")
    }

    test("blocking a user with no conversation removes nothing") {
        val (vm, _) = listening(listOf(conversation("c1", with = "partner", last = text(1, "partner"))))

        CometChatEvents.emitUserEventSync(CometChatUserEvent.UserBlocked(user("stranger")))

        vm.conversations.value.map { it.conversationId } shouldBe listOf("c1")
    }

    // ==================== the public unread-count predicate ====================

    test("a plain text message never asks for an unread bump on its own account") {
        val (vm, _) = listening(emptyList())

        vm.willUpdateIncrementUnreadCount(text(1, "partner")) shouldBe false
    }

    test("a custom message honours an explicit incrementUnreadCount flag in its metadata") {
        val (vm, _) = listening(emptyList())

        val on = CustomMessage(me, CometChatConstants.RECEIVER_TYPE_USER, "sticker", org.json.JSONObject())
            .apply { metadata = org.json.JSONObject().put("incrementUnreadCount", true) }
        val off = CustomMessage(me, CometChatConstants.RECEIVER_TYPE_USER, "sticker", org.json.JSONObject())
            .apply { metadata = org.json.JSONObject().put("incrementUnreadCount", false) }

        vm.willUpdateIncrementUnreadCount(on) shouldBe true
        vm.willUpdateIncrementUnreadCount(off) shouldBe false
    }

    test("a non-boolean incrementUnreadCount is treated as no rather than crashing") {
        val (vm, _) = listening(emptyList())

        val odd = CustomMessage(me, CometChatConstants.RECEIVER_TYPE_USER, "sticker", org.json.JSONObject())
            .apply { metadata = org.json.JSONObject().put("incrementUnreadCount", org.json.JSONObject()) }

        vm.willUpdateIncrementUnreadCount(odd) shouldBe false
    }

    test("a threaded message is recognised by its parent id") {
        val (vm, _) = listening(emptyList())

        vm.isThreadedMessage(text(1, "partner")) shouldBe false
        vm.isThreadedMessage(text(1, "partner").apply { parentMessageId = 7L }) shouldBe true
    }
})
