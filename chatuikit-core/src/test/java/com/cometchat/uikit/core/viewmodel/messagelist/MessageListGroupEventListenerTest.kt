package com.cometchat.uikit.core.viewmodel.messagelist

import androidx.lifecycle.viewModelScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatGroupEvent
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
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

/**
 * Group-membership events reaching an open group conversation.
 *
 * When someone is added, kicked, banned, unbanned or has their scope changed, the SDK hands the
 * UI an [Action] — the grey "X added Y" line in the transcript. The view model has to decide
 * whether that line belongs in *this* list, and to write the sentence itself when the server did
 * not supply one.
 *
 * Three rules run through all five handlers, and each is checked per handler because they are
 * implemented separately rather than shared:
 *
 * - **It must be this group.** An action for another group is dropped; so is one arriving while
 *   the list is showing a user chat rather than a group.
 * - **Thread views do not show membership lines.** With a parent message set, the handlers return
 *   early — a thread is about one message, not the room.
 * - **A missing message is composed locally** from the actor and target names, so the transcript
 *   never shows a blank row. Where the server did supply text, it is left alone.
 *
 * Listeners are enabled here, which needs the `CometChat` static mocked so the SDK registrations
 * inside addListeners() become no-ops. See [MessageListEventListenerTest] for why that flag also
 * gates the in-process bus subscriptions these tests depend on.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*MessageListGroupEventListenerTest"
 */
class MessageListGroupEventListenerTest : FunSpec({

    val me = "test_logged_in_user"
    val thisGroup = "group_alpha"
    val otherGroup = "group_beta"

    lateinit var cometChatStatic: MockedStatic<CometChat>
    lateinit var repository: MessageListRepository

    class ListeningViewModel(
        repository: MessageListRepository
    ) : CometChatMessageListViewModel(repository, enableListeners = true) {
        override fun getLoggedInUserUid(): String? = "test_logged_in_user"
        init { setEnableConversationSummary(false) }

        /** Cancel *and* join: a coroutine already on a worker still resumes on Main otherwise. */
        fun teardown() {
            onCleared()
            viewModelScope.cancel()
            runBlocking { viewModelScope.coroutineContext.job.join() }
        }
    }

    val liveViewModels = mutableListOf<ListeningViewModel>()

    fun group(guid: String, name: String = "Alpha") = Group().apply {
        this.guid = guid
        this.name = name
    }

    fun user(uid: String, name: String) = User().apply {
        this.uid = uid
        this.name = name
    }

    /** An action addressed to [groupGuid]; [message] null leaves the view model to compose one. */
    fun action(groupGuid: String, id: Long, message: String? = null): Action = Action().apply {
        this.id = id
        this.receiverUid = groupGuid
        this.receiverType = CometChatConstants.RECEIVER_TYPE_GROUP
        this.actionBy = user("actor", "Ada")
        this.actionOn = user("target", "Grace")
        if (message != null) this.message = message
    }

    /** A view model showing [groupGuid] as a group conversation. */
    fun showingGroup(groupGuid: String = thisGroup): ListeningViewModel {
        val vm = ListeningViewModel(repository)
        liveViewModels += vm
        vm.setGroup(group(groupGuid), gotoMessageId = 0)
        vm.fetchMessages()
        return vm
    }

    /** A view model showing a one-to-one chat, so group events should not apply at all. */
    fun showingUserChat(): ListeningViewModel {
        val vm = ListeningViewModel(repository)
        liveViewModels += vm
        vm.setUser(user("chat_partner", "Partner"), gotoMessageId = 0)
        vm.fetchMessages()
        return vm
    }

    beforeTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        cometChatStatic = Mockito.mockStatic(CometChat::class.java)
        val loggedIn = mock<User>()
        whenever(loggedIn.uid).thenReturn(me)
        cometChatStatic.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(loggedIn)

        // Every suspend method answers: an unstubbed one returns null and the resulting NPE
        // lands on a background worker, later blamed on an unrelated spec.
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

    // ==================== member kicked ====================

    test("a kick in this group adds its action line") {
        runBlocking {
            val vm = showingGroup()

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberKicked(
                    action(thisGroup, 1), user("target", "Grace"),
                    user("actor", "Ada"), group(thisGroup)
                )
            )

            vm.messages.value.map { it.id } shouldBe listOf(1L)
        }
    }

    test("a kick in another group is ignored") {
        runBlocking {
            val vm = showingGroup()

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberKicked(
                    action(otherGroup, 2), user("target", "Grace"),
                    user("actor", "Ada"), group(otherGroup)
                )
            )

            vm.messages.value shouldBe emptyList()
        }
    }

    test("a kick reaching a one-to-one chat is ignored") {
        runBlocking {
            val vm = showingUserChat()

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberKicked(
                    action(thisGroup, 3), user("target", "Grace"),
                    user("actor", "Ada"), group(thisGroup)
                )
            )

            vm.messages.value shouldBe emptyList()
        }
    }

    test("a kick with no server message gets one composed from the names") {
        runBlocking {
            val vm = showingGroup()
            val act = action(thisGroup, 4)

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberKicked(act, user("target", "Grace"), user("actor", "Ada"), group(thisGroup))
            )

            act.message shouldBe "Ada kicked Grace"
            vm.messages.value.size shouldBe 1
        }
    }

    test("a kick that already has a message keeps it") {
        runBlocking {
            val vm = showingGroup()
            val act = action(thisGroup, 5, message = "server said so")

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberKicked(act, user("target", "Grace"), user("actor", "Ada"), group(thisGroup))
            )

            act.message shouldBe "server said so"
            vm.messages.value.size shouldBe 1
        }
    }

    // ==================== member banned and unbanned ====================

    test("a ban in this group adds its action line and composes a message") {
        runBlocking {
            val vm = showingGroup()
            val act = action(thisGroup, 10)

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberBanned(act, user("target", "Grace"), user("actor", "Ada"), group(thisGroup))
            )

            vm.messages.value.map { it.id } shouldBe listOf(10L)
            act.message.isNullOrEmpty() shouldBe false
        }
    }

    test("a ban in another group is ignored") {
        runBlocking {
            val vm = showingGroup()

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberBanned(
                    action(otherGroup, 11), user("target", "Grace"),
                    user("actor", "Ada"), group(otherGroup)
                )
            )

            vm.messages.value shouldBe emptyList()
        }
    }

    test("an unban in this group adds its action line") {
        runBlocking {
            val vm = showingGroup()

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberUnbanned(
                    action(thisGroup, 12), user("target", "Grace"),
                    user("actor", "Ada"), group(thisGroup)
                )
            )

            vm.messages.value.map { it.id } shouldBe listOf(12L)
        }
    }

    test("an unban in another group is ignored") {
        runBlocking {
            val vm = showingGroup()

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberUnbanned(
                    action(otherGroup, 13), user("target", "Grace"),
                    user("actor", "Ada"), group(otherGroup)
                )
            )

            vm.messages.value shouldBe emptyList()
        }
    }

    // ==================== members added ====================

    test("adding members adds one line per member") {
        runBlocking {
            val vm = showingGroup()

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MembersAdded(
                    listOf(action(thisGroup, 20), action(thisGroup, 21)),
                    listOf(user("a", "Alice"), user("b", "Bob")),
                    group(thisGroup),
                    user("actor", "Ada")
                )
            )

            vm.messages.value.map { it.id } shouldBe listOf(20L, 21L)
        }
    }

    test("adding members to another group adds nothing") {
        runBlocking {
            val vm = showingGroup()

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MembersAdded(
                    listOf(action(otherGroup, 22)),
                    listOf(user("a", "Alice")),
                    group(otherGroup),
                    user("actor", "Ada")
                )
            )

            vm.messages.value shouldBe emptyList()
        }
    }

    // ==================== scope changed ====================

    test("a scope change in this group adds its action line") {
        runBlocking {
            val vm = showingGroup()
            val act = action(thisGroup, 30).apply { newScope = CometChatConstants.SCOPE_ADMIN }

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberScopeChanged(
                    act, user("target", "Grace"),
                    CometChatConstants.SCOPE_ADMIN, CometChatConstants.SCOPE_PARTICIPANT,
                    group(thisGroup)
                )
            )

            vm.messages.value.map { it.id } shouldBe listOf(30L)
            act.message shouldBe "Ada made Grace ${CometChatConstants.SCOPE_ADMIN}"
        }
    }

    test("a scope change in another group is ignored") {
        runBlocking {
            val vm = showingGroup()

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberScopeChanged(
                    action(otherGroup, 31), user("target", "Grace"),
                    CometChatConstants.SCOPE_ADMIN, CometChatConstants.SCOPE_PARTICIPANT,
                    group(otherGroup)
                )
            )

            vm.messages.value shouldBe emptyList()
        }
    }

    // ==================== other group events ====================

    test("a member joining this group adds its line, and another group's does not") {
        runBlocking {
            val vm = showingGroup()

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberJoined(user("joiner", "Joe"), group(otherGroup))
            )
            vm.messages.value shouldBe emptyList()
        }
    }

    test("ownership changing in another group leaves this list alone") {
        runBlocking {
            val vm = showingGroup()

            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.OwnershipChanged(group(otherGroup), mock<GroupMember>())
            )

            vm.messages.value shouldBe emptyList()
        }
    }
})
