package com.cometchat.uikit.core.events

import com.cometchat.chat.core.Call
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.mockito.kotlin.mock

/**
 * Tests for the [CometChatEvents] event bus (ENG-38677 / L — `core/events` was
 * at 20%; the old suite was deleted with no replacement).
 *
 * Each family's `emit*` publishes to its `SharedFlow`. We register a collector
 * with `UNDISPATCHED` start (so it subscribes before the emit), emit, and assert
 * the same event round-trips. Payloads are lightweight mocks — the bus is
 * payload-agnostic, so identity of the round-tripped event is what matters.
 */
class CometChatEventsTest : FunSpec({

    // Collects the first event of a flow while [emit] runs, with a safety timeout.
    suspend fun <T> awaitEmit(flow: kotlinx.coroutines.flow.SharedFlow<T>, emit: () -> Unit): T =
        runBlocking {
            val received = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(5000) { flow.first() }
            }
            emit()
            received.await()
        }

    test("emitMessageEvent delivers to a message subscriber") {
        val event = CometChatMessageEvent.MessageSent(mock<BaseMessage>(), MessageStatus.SUCCESS)
        awaitEmit(CometChatEvents.messageEvents) { CometChatEvents.emitMessageEvent(event) } shouldBe event
    }

    test("emitUserEvent delivers to a user subscriber") {
        val event = CometChatUserEvent.UserBlocked(mock<User>())
        awaitEmit(CometChatEvents.userEvents) { CometChatEvents.emitUserEvent(event) } shouldBe event
    }

    test("emitGroupEvent delivers to a group subscriber") {
        val event = CometChatGroupEvent.GroupCreated(mock<Group>())
        awaitEmit(CometChatEvents.groupEvents) { CometChatEvents.emitGroupEvent(event) } shouldBe event
    }

    test("emitCallEvent delivers to a call subscriber") {
        val event = CometChatCallEvent.OutgoingCall(mock<Call>())
        awaitEmit(CometChatEvents.callEvents) { CometChatEvents.emitCallEvent(event) } shouldBe event
    }

    test("emitConversationEvent delivers to a conversation subscriber") {
        val event = CometChatConversationEvent.ConversationDeleted(mock<Conversation>())
        awaitEmit(CometChatEvents.conversationEvents) { CometChatEvents.emitConversationEvent(event) } shouldBe event
    }

    test("emitUIEvent delivers to a UI subscriber") {
        val event = CometChatUIEvent.ComposeMessage(id = "compose-1", text = "hello")
        awaitEmit(CometChatEvents.uiEvents) { CometChatEvents.emitUIEvent(event) } shouldBe event
    }

    test("emitMessageEventSync delivers synchronously to an active subscriber") {
        runBlocking {
            val event = CometChatMessageEvent.MessageSent(mock<BaseMessage>(), MessageStatus.ERROR)
            val received = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(5000) { CometChatEvents.messageEvents.first() }
            }
            CometChatEvents.emitMessageEventSync(event) shouldBe true
            received.await() shouldBe event
        }
    }
})
