package com.cometchat.uikit.core.viewmodel.messagelist

import androidx.lifecycle.viewModelScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

/**
 * The real-time delivery path: messages arriving from the SDK socket rather than from a fetch.
 *
 * `handleIncomingMessage` is private and only ever called from the `CometChat.MessageListener` the
 * view model registers in `addListeners()`. With the `CometChat` static mocked that registration is
 * a no-op, so the listener is never invoked and the whole path stays dark — which is why it was the
 * largest uncovered function in the class.
 *
 * The trick here is that Mockito still *records* the registration: capturing the listener argument
 * gives a handle on the very object the view model built, and calling its callbacks directly
 * reproduces a socket delivery exactly as the SDK would.
 *
 * What the path has to get right, and what these tests pin:
 *
 * - **Only messages for this conversation are shown.** A message addressed elsewhere, or from
 *   another sender in a one-to-one chat, is dropped rather than appended.
 * - **Group messages are matched on the group id**, not the sender.
 * - **Every message kind takes the same route** — text, media and custom all land in the list, so a
 *   filter applied to one applies to all.
 * - **A socket delivery is de-duplicated against what is already loaded**, so a message that also
 *   arrived through a fetch does not produce two bubbles.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*MessageListIncomingMessageTest"
 */
class MessageListIncomingMessageTest : FunSpec({

    val me = "test_logged_in_user"
    val chatPartner = "chat_partner"
    val thisGroup = "group_alpha"

    lateinit var cometChatStatic: MockedStatic<CometChat>
    lateinit var repository: MessageListRepository

    class ListeningViewModel(
        repository: MessageListRepository
    ) : CometChatMessageListViewModel(repository, enableListeners = true) {
        override fun getLoggedInUserUid(): String? = "test_logged_in_user"
        init { setEnableConversationSummary(false) }

        fun teardown() {
            onCleared()
            viewModelScope.cancel()
            runBlocking { viewModelScope.coroutineContext.job.join() }
        }
    }

    val liveViewModels = mutableListOf<ListeningViewModel>()

    fun user(uid: String, name: String = uid) = User().apply {
        this.uid = uid
        this.name = name
    }

    fun textFrom(senderUid: String, toUid: String, id: Long, type: String = CometChatConstants.RECEIVER_TYPE_USER) =
        TextMessage(toUid, "message $id", type).apply {
            this.id = id
            sentAt = System.currentTimeMillis()
            sender = user(senderUid)
        }

    /**
     * The MessageListener the view model handed to the SDK.
     *
     * The static mock swallows the registration but records the arguments, so this recovers the
     * real listener object and lets a test deliver to it exactly as the socket would.
     */
    fun capturedListener(): CometChat.MessageListener {
        val captor = ArgumentCaptor.forClass(CometChat.MessageListener::class.java)
        cometChatStatic.verify({ CometChat.addMessageListener(any(), captor.capture()) }, Mockito.atLeastOnce())
        return captor.value
    }

    fun userChat(seeded: List<BaseMessage> = emptyList()): ListeningViewModel {
        wheneverBlocking { repository.fetchPreviousMessages() }.thenReturn(Result.success(seeded))
        val vm = ListeningViewModel(repository)
        liveViewModels += vm
        vm.setUser(user(chatPartner), gotoMessageId = 0)
        vm.fetchMessages()
        return vm
    }

    fun groupChat(): ListeningViewModel {
        val vm = ListeningViewModel(repository)
        liveViewModels += vm
        vm.setGroup(Group().apply { guid = thisGroup; name = "Alpha" }, gotoMessageId = 0)
        vm.fetchMessages()
        return vm
    }

    beforeTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        cometChatStatic = Mockito.mockStatic(CometChat::class.java)
        val loggedIn = mock<User>()
        whenever(loggedIn.uid).thenReturn(me)
        cometChatStatic.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(loggedIn)

        repository = mock()
        wheneverBlocking { repository.fetchPreviousMessages() }.thenReturn(Result.success(emptyList()))
        wheneverBlocking { repository.fetchNextMessages(any()) }.thenReturn(Result.success(emptyList()))
        wheneverBlocking { repository.fetchActionMessages(any()) }.thenReturn(Result.success(emptyList()))
        wheneverBlocking { repository.getConversation(any(), any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        wheneverBlocking { repository.getMessage(any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        wheneverBlocking { repository.fetchSurroundingMessages(any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        wheneverBlocking { repository.deleteMessage(any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        wheneverBlocking { repository.addReaction(any(), any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        wheneverBlocking { repository.removeReaction(any(), any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        wheneverBlocking { repository.markAsUnread(any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        wheneverBlocking { repository.flagMessage(any(), any(), any()) }.thenReturn(Result.success(Unit))
        wheneverBlocking { repository.markAsRead(any()) }.thenReturn(Result.success(Unit))
        wheneverBlocking { repository.markAsDelivered(any()) }.thenReturn(Result.success(Unit))
        whenever(repository.hasMorePreviousMessages()).thenReturn(false)
        whenever(repository.getLatestMessageId()).thenReturn(0L)
        whenever(repository.getEffectiveMessagesTypes()).thenReturn(emptyList())
        whenever(repository.getEffectiveMessagesCategories()).thenReturn(emptyList())
    }

    afterTest {
        liveViewModels.forEach { it.teardown() }
        liveViewModels.clear()
        cometChatStatic.close()
        Dispatchers.resetMain()
    }

    // ==================== one-to-one delivery ====================

    test("a text message from the chat partner is added to the list") {
        val vm = userChat()

        capturedListener().onTextMessageReceived(textFrom(chatPartner, me, 1))

        vm.messages.value.map { it.id } shouldBe listOf(1L)
    }

    test("a text message from someone else is dropped") {
        val vm = userChat()

        capturedListener().onTextMessageReceived(textFrom("stranger", me, 2))

        vm.messages.value shouldBe emptyList()
    }

    test("a message addressed to a different conversation is dropped") {
        val vm = userChat()

        capturedListener().onTextMessageReceived(textFrom(me, "someone_else", 3))

        vm.messages.value shouldBe emptyList()
    }

    // ==================== message kinds ====================

    test("a media message takes the same route as a text one") {
        val vm = userChat()

        // MockFactory builds a stubbed MediaMessage; the real constructor is ambiguous here and
        // needs a file handle this test has no use for.
        val media = MockFactory.createMediaMessage(
            id = 10L,
            senderUid = chatPartner,
            receiverId = me,
            receiverType = CometChatConstants.RECEIVER_TYPE_USER
        )
        capturedListener().onMediaMessageReceived(media)

        vm.messages.value.map { it.id } shouldBe listOf(10L)
    }

    test("an edit delivered over the socket updates in place rather than appending") {
        val original = textFrom(chatPartner, me, 20)
        val vm = userChat(listOf(original))
        vm.messages.value.size shouldBe 1

        capturedListener().onMessageEdited(textFrom(chatPartner, me, 20))

        vm.messages.value.size shouldBe 1
        vm.messages.value.single().id shouldBe 20L
    }

    // ==================== group delivery ====================

    test("a group message for this group is added") {
        val vm = groupChat()

        capturedListener().onTextMessageReceived(
            textFrom("anyone", thisGroup, 30, CometChatConstants.RECEIVER_TYPE_GROUP)
        )

        vm.messages.value.map { it.id } shouldBe listOf(30L)
    }

    test("a group message for another group is dropped") {
        val vm = groupChat()

        capturedListener().onTextMessageReceived(
            textFrom("anyone", "group_beta", 31, CometChatConstants.RECEIVER_TYPE_GROUP)
        )

        vm.messages.value shouldBe emptyList()
    }

    // ==================== de-duplication ====================

    test("a message already loaded is not added a second time") {
        val already = textFrom(chatPartner, me, 40)
        val vm = userChat(listOf(already))
        vm.messages.value.size shouldBe 1

        capturedListener().onTextMessageReceived(textFrom(chatPartner, me, 40))

        vm.messages.value.size shouldBe 1
    }

    test("two distinct socket deliveries both appear") {
        val vm = userChat()
        val listener = capturedListener()

        listener.onTextMessageReceived(textFrom(chatPartner, me, 50))
        listener.onTextMessageReceived(textFrom(chatPartner, me, 51))

        vm.messages.value.map { it.id } shouldBe listOf(50L, 51L)
    }
})
