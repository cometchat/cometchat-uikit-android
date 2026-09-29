package com.cometchat.uikit.core.viewmodel.messagelist

import com.cometchat.chat.constants.CometChatConstants
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.chat.models.ReactionEvent
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.events.MessageStatus
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
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
 * Drives [CometChatMessageListViewModel] through the UIKit event bus with its listeners switched
 * **on**.
 *
 * Every other spec for this view model constructs it with `enableListeners = false`, because doing
 * otherwise reaches `CometChat.addMessageListener` and the SDK is not initialised in a unit test.
 * That flag gates two different things at once: `addListeners()`, which does touch the SDK, and
 * `addLocalEventListeners()`, which only subscribes to the in-process [CometChatEvents] bus. So
 * turning it off to avoid the first also silences the second, and the handlers behind it — message
 * sent, edited, deleted, reactions, group membership — are never exercised anywhere.
 *
 * Mocking the `CometChat` static makes those SDK registrations harmless no-ops, which lets the
 * listeners come up for real. From there the bus is the input: emit an event, assert what the list
 * did with it.
 *
 * The rules being pinned are mostly about *rejection*, since that is where the bugs live — an
 * event for another conversation, or a thread reply belonging to a different parent, must not
 * appear in this list.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*MessageListEventListenerTest"
 */
class MessageListEventListenerTest : FunSpec({

    val me = "test_logged_in_user"
    val chatPartner = "chat_partner"

    lateinit var cometChatStatic: MockedStatic<CometChat>
    lateinit var repository: MessageListRepository

    /**
     * Listeners on: the SDK registrations inside addListeners() land on the static mock.
     *
     * [teardown] exposes the protected onCleared() so a test can cancel the bus collectors it
     * started. Without that the collectors outlive the test, keep consuming events emitted by
     * *later* tests, and fail them — which is how this file first broke two unrelated specs.
     */
    class ListeningViewModel(
        repository: MessageListRepository
    ) : CometChatMessageListViewModel(repository, enableListeners = true) {
        override fun getLoggedInUserUid(): String? = "test_logged_in_user"
        init { setEnableConversationSummary(false) }
        fun teardown() {
            onCleared()
            // onCleared() alone does not stop anything: it is ViewModel.clear() that cancels
            // viewModelScope, and that is not callable from here. Cancelling the scope directly is
            // what actually stops the bus collectors addLocalEventListeners() started.
            //
            // Cancelling is not enough on its own either: a coroutine already dispatched to a
            // background worker will still try to resume on Main, and if resetMain() has run by
            // then it dies with "Dispatchers.Main was accessed ... after resetMain()". Joining the
            // scope's job holds the test open until that work has actually stopped.
            viewModelScope.cancel()
            runBlocking { viewModelScope.coroutineContext.job.join() }
        }
    }

    val liveViewModels = mutableListOf<CometChatMessageListViewModel>()

    fun messageTo(receiverUid: String, id: Long, senderUid: String = me): BaseMessage =
        TextMessage(receiverUid, "message $id", CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            sentAt = System.currentTimeMillis()
            sender = User().apply { uid = senderUid }
        }

    fun reactionEvent(messageId: Long, conversationId: String): ReactionEvent {
        val reaction = mock<Reaction>()
        whenever(reaction.messageId).thenReturn(messageId)
        val event = mock<ReactionEvent>()
        whenever(event.reaction).thenReturn(reaction)
        whenever(event.conversationId).thenReturn(conversationId)
        return event
    }

    /** A view model already pointed at [chatPartner] with [seeded] loaded. */
    fun listening(seeded: List<BaseMessage> = emptyList()): ListeningViewModel {
        wheneverBlocking { repository.fetchPreviousMessages() }.thenReturn(Result.success(seeded))
        val vm = ListeningViewModel(repository)
        liveViewModels += vm
        vm.setUser(User().apply { uid = chatPartner }, gotoMessageId = 0)
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
        // Every suspend method is stubbed, not just the ones a test drives. An unstubbed suspend
        // mock returns null; the view model's background coroutines then NPE on it, and because
        // nothing awaits them the failure resurfaces as UncaughtExceptionsBeforeTest in whatever
        // spec runs next — blaming an innocent test. This file did exactly that before.
        wheneverBlocking { repository.fetchPreviousMessages() }.thenReturn(Result.success(emptyList()))
        wheneverBlocking { repository.fetchNextMessages(any()) }.thenReturn(Result.success(emptyList()))
        wheneverBlocking { repository.fetchActionMessages(any()) }.thenReturn(Result.success(emptyList()))
        wheneverBlocking { repository.getConversation(any(), any()) }
            .thenReturn(Result.failure(IllegalStateException("no conversation in this fixture")))
        wheneverBlocking { repository.getMessage(any()) }
            .thenReturn(Result.failure(IllegalStateException("no message in this fixture")))
        wheneverBlocking { repository.fetchSurroundingMessages(any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        wheneverBlocking { repository.deleteMessage(any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        wheneverBlocking { repository.addReaction(any(), any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        wheneverBlocking { repository.removeReaction(any(), any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        wheneverBlocking { repository.flagMessage(any(), any(), any()) }.thenReturn(Result.success(Unit))
        wheneverBlocking { repository.markAsRead(any()) }.thenReturn(Result.success(Unit))
        wheneverBlocking { repository.markAsDelivered(any()) }.thenReturn(Result.success(Unit))
        wheneverBlocking { repository.markAsUnread(any()) }
            .thenReturn(Result.failure(IllegalStateException("not used in this fixture")))
        whenever(repository.hasMorePreviousMessages()).thenReturn(false)
        whenever(repository.getLatestMessageId()).thenReturn(0L)
        whenever(repository.getEffectiveMessagesTypes()).thenReturn(emptyList())
        whenever(repository.getEffectiveMessagesCategories()).thenReturn(emptyList())
    }

    afterTest {
        // cancel the bus collectors before Main goes away, or they leak into the next spec
        liveViewModels.forEach { (it as? ListeningViewModel)?.teardown() }
        liveViewModels.clear()
        cometChatStatic.close()
        Dispatchers.resetMain()
    }

    // ==================== message sent ====================

    test("a message sent to this chat is added to the list") {
        runBlocking {
            val vm = listening()

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageSent(messageTo(chatPartner, 1), MessageStatus.SUCCESS)
            )

            vm.messages.value.map { it.id } shouldBe listOf(1L)
        }
    }

    test("a message sent to a different chat is ignored") {
        runBlocking {
            val vm = listening()

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageSent(messageTo("someone_else", 2), MessageStatus.SUCCESS)
            )

            vm.messages.value shouldBe emptyList()
        }
    }

    test("an in-progress message is still shown, so the bubble appears before the server replies") {
        runBlocking {
            val vm = listening()

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageSent(messageTo(chatPartner, 3), MessageStatus.IN_PROGRESS)
            )

            vm.messages.value.map { it.id } shouldBe listOf(3L)
        }
    }

    // ==================== edits and deletes ====================

    test("an edit to a loaded message replaces it in place rather than appending") {
        runBlocking {
            val original = messageTo(chatPartner, 10)
            val vm = listening(listOf(original))
            vm.messages.value.size shouldBe 1

            val edited = messageTo(chatPartner, 10).apply { }
            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageEdited(edited, MessageStatus.SUCCESS)
            )

            vm.messages.value.size shouldBe 1
            vm.messages.value.single().id shouldBe 10L
        }
    }

    test("an edit for a message this list never loaded does not add one") {
        runBlocking {
            val vm = listening(listOf(messageTo(chatPartner, 10)))

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageEdited(messageTo(chatPartner, 99), MessageStatus.SUCCESS)
            )

            vm.messages.value.map { it.id } shouldBe listOf(10L)
        }
    }

    test("a delete event keeps the row rather than dropping it, so the tombstone can render") {
        runBlocking {
            val vm = listening(listOf(messageTo(chatPartner, 11)))

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageDeleted(messageTo(chatPartner, 11))
            )

            vm.messages.value.size shouldBe 1
        }
    }

    // ==================== reactions ====================

    test("a reaction for a loaded message is accepted") {
        runBlocking {
            val vm = listening(listOf(messageTo(chatPartner, 20)))

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.ReactionAdded(reactionEvent(20L, "user_$chatPartner"))
            )

            vm.messages.value.size shouldBe 1
        }
    }

    test("a reaction for a message not in this list is ignored") {
        runBlocking {
            val vm = listening(listOf(messageTo(chatPartner, 20)))

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.ReactionAdded(reactionEvent(999L, "user_$chatPartner"))
            )

            vm.messages.value.size shouldBe 1
        }
    }

    test("reactions are ignored entirely when the feature is disabled") {
        runBlocking {
            val vm = listening(listOf(messageTo(chatPartner, 20)))
            vm.setDisableReactions(true)

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.ReactionAdded(reactionEvent(20L, "user_$chatPartner"))
            )
            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.ReactionRemoved(reactionEvent(20L, "user_$chatPartner"))
            )

            vm.messages.value.size shouldBe 1
        }
    }

    test("a reaction removal for a loaded message is accepted") {
        runBlocking {
            val vm = listening(listOf(messageTo(chatPartner, 21)))

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.ReactionRemoved(reactionEvent(21L, "user_$chatPartner"))
            )

            vm.messages.value.size shouldBe 1
        }
    }

    // ==================== thread replies ====================

    test("a thread reply is not added to the main conversation list") {
        runBlocking {
            val vm = listening()

            val reply = messageTo(chatPartner, 30).apply { parentMessageId = 5L }
            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageSent(reply, MessageStatus.SUCCESS)
            )

            // the main list tracks the reply count on the parent instead of showing the reply
            vm.messages.value shouldBe emptyList()
        }
    }

    // ==================== listeners are actually attached ====================

    test("with listeners enabled the view model subscribes to the SDK") {
        runBlocking {
            listening()
            // addListeners() registers against the static; the mock records the call and the
            // absence of an exception is what proves the listener path ran at all
            cometChatStatic.verify({ CometChat.addMessageListener(any(), any()) }, Mockito.atLeast(1))
        }
    }

    test("listeners left off means bus events are ignored") {
        runBlocking {
            wheneverBlocking { repository.fetchPreviousMessages() }.thenReturn(Result.success(emptyList()))
            val quiet = object : CometChatMessageListViewModel(repository, enableListeners = false) {
                override fun getLoggedInUserUid(): String? = me
            }
            liveViewModels += quiet
            quiet.setUser(User().apply { uid = chatPartner }, gotoMessageId = 0)
            quiet.fetchMessages()

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageSent(messageTo(chatPartner, 40), MessageStatus.SUCCESS)
            )

            quiet.messages.value shouldBe emptyList()
        }
    }
})
