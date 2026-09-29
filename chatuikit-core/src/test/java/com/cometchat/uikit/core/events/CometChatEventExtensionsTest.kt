package com.cometchat.uikit.core.events

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.cometchat.chat.core.Call
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.chat.models.ReactionEvent
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.TransientMessage
import com.cometchat.chat.models.TypingIndicator
import com.cometchat.chat.models.User
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.mock

/**
 * Tests the lifecycle-aware event subscription extensions in CometChatEventExtensions.kt.
 *
 * There are forty typed extensions across six event families, and each is the same shape: a
 * subscription to the family flow plus a type filter that either matches the event or ignores it.
 * Both sides of that filter matter — a filter that fired for everything would deliver a group
 * event to a typing-indicator callback — so every extension is checked twice here: once with the
 * event it is meant to catch, and once with an event of the same family it must ignore.
 *
 * Mechanics worth knowing before changing this file:
 *
 * - The subscription collects on `Dispatchers.Main.immediate`, so [Dispatchers.setMain] with an
 *   unconfined dispatcher is what makes delivery synchronous and the assertions deterministic.
 * - `emitMessageEvent` and friends launch on the bus's own scope, so this file uses the internal
 *   `emit*Sync` entry points instead. They return only once the event has been offered to
 *   subscribers, which is what lets a test assert immediately after emitting.
 * - Every subscription is cancelled and the lifecycle driven to DESTROYED before `resetMain`.
 *   Tearing Main down underneath a live collector is a known way to make this suite flake.
 * - [LifecycleRegistry.createUnsafe] skips the main-thread assertion that a JVM test cannot
 *   satisfy.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*CometChatEventExtensionsTest"
 */
class CometChatEventExtensionsTest : FunSpec({

    val dispatcher = UnconfinedTestDispatcher()

    lateinit var owner: LifecycleOwner
    lateinit var registry: LifecycleRegistry
    val openJobs = mutableListOf<Job>()

    beforeTest {
        Dispatchers.setMain(dispatcher)
        owner = object : LifecycleOwner {
            override val lifecycle: Lifecycle get() = registry
        }
        registry = LifecycleRegistry.createUnsafe(owner)
        registry.currentState = Lifecycle.State.STARTED
        openJobs.clear()
    }

    afterTest {
        openJobs.forEach { it.cancel() }
        openJobs.clear()
        registry.currentState = Lifecycle.State.DESTROYED
        Dispatchers.resetMain()
    }

    /**
     * Subscribes via [subscribe], emits [matching] then [ignored], and asserts the callback ran
     * for the first and not the second — covering both sides of the extension's type filter.
     */
    fun <E : Any> bothSides(
        subscribe: (onFired: () -> Unit) -> Job,
        matching: E,
        ignored: E,
        emit: suspend (E) -> Unit
    ) = runBlocking {
        var fired = 0
        openJobs += subscribe { fired++ }

        emit(matching)
        fired shouldBe 1

        emit(ignored)
        fired shouldBe 1
    }

    fun message(m: CometChatMessageEvent, ignored: CometChatMessageEvent, sub: (() -> Unit) -> Job) =
        bothSides(sub, m, ignored) { CometChatEvents.emitMessageEventSync(it) }

    // A message event that none of the typed message extensions below claim.
    val otherMessageEvent = CometChatMessageEvent.MessageModerated(mock<BaseMessage>())

    // ==================== message family ====================

    test("onMessageSent fires only for MessageSent") {
        message(
            CometChatMessageEvent.MessageSent(mock<BaseMessage>(), MessageStatus.SUCCESS),
            otherMessageEvent
        ) { fire -> owner.onMessageSent { _, _ -> fire() } }
    }

    test("onMessageEdited fires only for MessageEdited") {
        message(
            CometChatMessageEvent.MessageEdited(mock<BaseMessage>(), MessageStatus.SUCCESS),
            otherMessageEvent
        ) { fire -> owner.onMessageEdited { _, _ -> fire() } }
    }

    test("onMessageDeleted fires only for MessageDeleted") {
        message(
            CometChatMessageEvent.MessageDeleted(mock<BaseMessage>()),
            otherMessageEvent
        ) { fire -> owner.onMessageDeleted { fire() } }
    }

    test("onMessagePinned fires only for MessagePinned") {
        message(
            CometChatMessageEvent.MessagePinned(mock<BaseMessage>()),
            otherMessageEvent
        ) { fire -> owner.onMessagePinned { fire() } }
    }

    test("onMessageUnpinned fires only for MessageUnpinned") {
        message(
            CometChatMessageEvent.MessageUnpinned(mock<BaseMessage>()),
            otherMessageEvent
        ) { fire -> owner.onMessageUnpinned { fire() } }
    }

    test("onMessageSaved fires only for MessageSaved") {
        message(
            CometChatMessageEvent.MessageSaved(mock<BaseMessage>()),
            otherMessageEvent
        ) { fire -> owner.onMessageSaved { fire() } }
    }

    test("onMessageUnsaved fires only for MessageUnsaved") {
        message(
            CometChatMessageEvent.MessageUnsaved(mock<BaseMessage>()),
            otherMessageEvent
        ) { fire -> owner.onMessageUnsaved { fire() } }
    }

    test("onMessageRead fires only for MessageRead") {
        message(
            CometChatMessageEvent.MessageRead(mock<BaseMessage>()),
            otherMessageEvent
        ) { fire -> owner.onMessageRead { fire() } }
    }

    test("onTextMessageReceived fires only for TextMessageReceived") {
        message(
            CometChatMessageEvent.TextMessageReceived(mock<TextMessage>()),
            otherMessageEvent
        ) { fire -> owner.onTextMessageReceived { fire() } }
    }

    test("onMediaMessageReceived fires only for MediaMessageReceived") {
        message(
            CometChatMessageEvent.MediaMessageReceived(mock<MediaMessage>()),
            otherMessageEvent
        ) { fire -> owner.onMediaMessageReceived { fire() } }
    }

    test("onCustomMessageReceived fires only for CustomMessageReceived") {
        message(
            CometChatMessageEvent.CustomMessageReceived(mock<CustomMessage>()),
            otherMessageEvent
        ) { fire -> owner.onCustomMessageReceived { fire() } }
    }

    test("onTypingStarted fires only for TypingStarted") {
        message(
            CometChatMessageEvent.TypingStarted(mock<TypingIndicator>()),
            otherMessageEvent
        ) { fire -> owner.onTypingStarted { fire() } }
    }

    test("onTypingEnded fires only for TypingEnded") {
        message(
            CometChatMessageEvent.TypingEnded(mock<TypingIndicator>()),
            otherMessageEvent
        ) { fire -> owner.onTypingEnded { fire() } }
    }

    test("onMessagesDelivered fires only for MessagesDelivered") {
        message(
            CometChatMessageEvent.MessagesDelivered(mock<MessageReceipt>()),
            otherMessageEvent
        ) { fire -> owner.onMessagesDelivered { fire() } }
    }

    test("onMessagesRead fires only for MessagesRead") {
        message(
            CometChatMessageEvent.MessagesRead(mock<MessageReceipt>()),
            otherMessageEvent
        ) { fire -> owner.onMessagesRead { fire() } }
    }

    test("onReactionAdded fires only for ReactionAdded") {
        message(
            CometChatMessageEvent.ReactionAdded(mock<ReactionEvent>()),
            otherMessageEvent
        ) { fire -> owner.onReactionAdded { fire() } }
    }

    test("onReactionRemoved fires only for ReactionRemoved") {
        message(
            CometChatMessageEvent.ReactionRemoved(mock<ReactionEvent>()),
            otherMessageEvent
        ) { fire -> owner.onReactionRemoved { fire() } }
    }

    test("onTransientMessageReceived fires only for TransientMessageReceived") {
        message(
            CometChatMessageEvent.TransientMessageReceived(mock<TransientMessage>()),
            otherMessageEvent
        ) { fire -> owner.onTransientMessageReceived { fire() } }
    }

    test("onLiveReaction fires only for LiveReaction") {
        message(
            CometChatMessageEvent.LiveReaction(42),
            otherMessageEvent
        ) { fire -> owner.onLiveReaction { fire() } }
    }

    test("onMessageReceived fires for all three received kinds and not for others") {
        runBlocking {
            var fired = 0
            openJobs += owner.onMessageReceived { fired++ }

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.TextMessageReceived(mock<TextMessage>())
            )
            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MediaMessageReceived(mock<MediaMessage>())
            )
            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.CustomMessageReceived(mock<CustomMessage>())
            )
            fired shouldBe 3

            CometChatEvents.emitMessageEventSync(otherMessageEvent)
            fired shouldBe 3
        }
    }

    // ==================== user family ====================

    test("onUserBlocked and onUserUnblocked each fire only for their own event") {
        runBlocking {
            var blocked = 0
            var unblocked = 0
            openJobs += owner.onUserBlocked { blocked++ }
            openJobs += owner.onUserUnblocked { unblocked++ }

            CometChatEvents.emitUserEventSync(CometChatUserEvent.UserBlocked(mock<User>()))
            blocked shouldBe 1
            unblocked shouldBe 0

            CometChatEvents.emitUserEventSync(CometChatUserEvent.UserUnblocked(mock<User>()))
            blocked shouldBe 1
            unblocked shouldBe 1
        }
    }

    // ==================== group family ====================

    test("each group extension fires only for its own event") {
        runBlocking {
            val counts = mutableMapOf<String, Int>()
            fun bump(key: String) = counts.merge(key, 1, Int::plus)

            openJobs += owner.onGroupCreated { bump("created") }
            openJobs += owner.onGroupDeleted { bump("deleted") }
            openJobs += owner.onGroupLeft { _, _, _ -> bump("left") }
            openJobs += owner.onMemberJoined { _, _ -> bump("joined") }
            openJobs += owner.onMembersAdded { _, _, _, _ -> bump("added") }
            openJobs += owner.onMemberKicked { _, _, _, _ -> bump("kicked") }
            openJobs += owner.onMemberBanned { _, _, _, _ -> bump("banned") }
            openJobs += owner.onMemberUnbanned { _, _, _, _ -> bump("unbanned") }
            openJobs += owner.onMemberScopeChanged { _, _, _, _, _ -> bump("scope") }
            openJobs += owner.onOwnershipChanged { _, _ -> bump("ownership") }

            val group = mock<Group>()
            val user = mock<User>()
            val action = mock<Action>()

            CometChatEvents.emitGroupEventSync(CometChatGroupEvent.GroupCreated(group))
            CometChatEvents.emitGroupEventSync(CometChatGroupEvent.GroupDeleted(group))
            CometChatEvents.emitGroupEventSync(CometChatGroupEvent.GroupLeft(action, user, group))
            CometChatEvents.emitGroupEventSync(CometChatGroupEvent.MemberJoined(user, group))
            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MembersAdded(listOf(action), listOf(user), group, user)
            )
            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberKicked(action, user, user, group)
            )
            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberBanned(action, user, user, group)
            )
            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberUnbanned(action, user, user, group)
            )
            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.MemberScopeChanged(action, user, "admin", "participant", group)
            )
            CometChatEvents.emitGroupEventSync(
                CometChatGroupEvent.OwnershipChanged(group, mock<GroupMember>())
            )

            // each extension saw exactly its own event, and ignored the other nine
            listOf(
                "created", "deleted", "left", "joined", "added",
                "kicked", "banned", "unbanned", "scope", "ownership"
            ).forEach { key -> counts[key] shouldBe 1 }
        }
    }

    // ==================== call family ====================

    test("each call extension fires only for its own event") {
        runBlocking {
            val counts = mutableMapOf<String, Int>()
            openJobs += owner.onOutgoingCall { counts.merge("outgoing", 1, Int::plus) }
            openJobs += owner.onCallAccepted { counts.merge("accepted", 1, Int::plus) }
            openJobs += owner.onCallRejected { counts.merge("rejected", 1, Int::plus) }
            openJobs += owner.onCallEnded { counts.merge("ended", 1, Int::plus) }

            val call = mock<Call>()
            CometChatEvents.emitCallEventSync(CometChatCallEvent.OutgoingCall(call))
            CometChatEvents.emitCallEventSync(CometChatCallEvent.CallAccepted(call))
            CometChatEvents.emitCallEventSync(CometChatCallEvent.CallRejected(call))
            CometChatEvents.emitCallEventSync(CometChatCallEvent.CallEnded(call))

            listOf("outgoing", "accepted", "rejected", "ended").forEach { counts[it] shouldBe 1 }
        }
    }

    // ==================== conversation family ====================

    test("onConversationDeleted fires for a deletion and ignores an update") {
        runBlocking {
            var fired = 0
            openJobs += owner.onConversationDeleted { fired++ }

            CometChatEvents.emitConversationEventSync(
                CometChatConversationEvent.ConversationDeleted(mock<Conversation>())
            )
            fired shouldBe 1

            CometChatEvents.emitConversationEventSync(
                CometChatConversationEvent.ConversationUpdated(mock<Conversation>())
            )
            fired shouldBe 1
        }
    }

    // ==================== UI family ====================

    test("each UI extension fires only for its own event") {
        runBlocking {
            val counts = mutableMapOf<String, Int>()
            openJobs += owner.onActiveChatChanged { _, _, _, _, _ -> counts.merge("active", 1, Int::plus) }
            openJobs += owner.onComposeMessage { _, _ -> counts.merge("compose", 1, Int::plus) }
            openJobs += owner.onOpenChat { _, _ -> counts.merge("open", 1, Int::plus) }

            CometChatEvents.emitUIEventSync(
                CometChatUIEvent.ActiveChatChanged(emptyMap(), null, null, null)
            )
            CometChatEvents.emitUIEventSync(CometChatUIEvent.ComposeMessage("id", "hello"))
            CometChatEvents.emitUIEventSync(CometChatUIEvent.OpenChat(mock<User>(), null))

            listOf("active", "compose", "open").forEach { counts[it] shouldBe 1 }
        }
    }

    // ==================== lifecycle gating ====================

    test("a subscription created below the start state receives nothing until it starts") {
        runBlocking {
            registry.currentState = Lifecycle.State.CREATED

            var fired = 0
            openJobs += owner.onMessageDeleted { fired++ }

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageDeleted(mock<BaseMessage>())
            )
            fired shouldBe 0

            registry.currentState = Lifecycle.State.STARTED
            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageDeleted(mock<BaseMessage>())
            )
            fired shouldBe 1
        }
    }

    test("a stopped subscription stops receiving, and resumes when started again") {
        runBlocking {
            var fired = 0
            openJobs += owner.onMessageDeleted { fired++ }

            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageDeleted(mock<BaseMessage>())
            )
            fired shouldBe 1

            registry.currentState = Lifecycle.State.CREATED // ON_STOP
            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageDeleted(mock<BaseMessage>())
            )
            fired shouldBe 1

            registry.currentState = Lifecycle.State.STARTED
            CometChatEvents.emitMessageEventSync(
                CometChatMessageEvent.MessageDeleted(mock<BaseMessage>())
            )
            fired shouldBe 2
        }
    }
})
