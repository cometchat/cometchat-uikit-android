package com.cometchat.uikit.core.viewmodel.outgoingcall

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.utils.CallMessageJson
import com.cometchat.uikit.core.viewmodel.CometChatOutgoingCallViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Ringing v2 (ENG-39907), caller side: the outgoing-call ViewModel learns answered,
 * declined, busy and unanswered from the Calls SDK only. Answered is the callee joining the
 * prepared session; the rest arrive as `onIncomingCallEnded(call, reason)` from the
 * terminal push the backend sends the caller. No Chat SDK calling API is involved.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OutgoingCallRingingV2Test : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    /** Drives the private Calls SDK handlers the way the SDK listeners would. */
    class TestableViewModel : CometChatOutgoingCallViewModel(enableListeners = false) {
        fun callEnded(sessionId: String?, reason: String) {
            val m = CometChatOutgoingCallViewModel::class.java.getDeclaredMethod(
                "handleCallEnded", String::class.java, String::class.java
            )
            m.isAccessible = true
            m.invoke(this, sessionId, reason)
        }

        fun participantConnected(uid: String?) {
            val m = CometChatOutgoingCallViewModel::class.java.getDeclaredMethod(
                "handleParticipantConnected", String::class.java
            )
            m.isAccessible = true
            m.invoke(this, uid)
        }

        fun connectionClosed() {
            val m = CometChatOutgoingCallViewModel::class.java.getDeclaredMethod("handleConnectionClosed")
            m.isAccessible = true
            m.invoke(this)
        }

        fun userJoined(uid: String?) {
            val m = CometChatOutgoingCallViewModel::class.java.getDeclaredMethod(
                "handleUserJoined", String::class.java
            )
            m.isAccessible = true
            m.invoke(this, uid)
        }
    }

    beforeTest { Dispatchers.setMain(testDispatcher) }
    afterTest { Dispatchers.resetMain() }

    // ==================== Ended elsewhere ====================

    test("a terminal push for the current session closes the screen with its reason") {
        val vm = TestableViewModel()
        val call = MockFactory.createCall(sessionId = "s-out-1")
        vm.setCall(call)

        vm.callEnded("s-out-1", UIKitConstants.CallStatusConstants.REJECTED)

        vm.rejectedCall.value shouldBe call
        vm.endReason.value shouldBe UIKitConstants.CallStatusConstants.REJECTED
        vm.acceptedCall.value shouldBe null
    }

    test("busy and unanswered are reported through endReason") {
        listOf(
            UIKitConstants.CallStatusConstants.BUSY,
            UIKitConstants.CallStatusConstants.UNANSWERED,
            UIKitConstants.CallStatusConstants.ENDED
        ).forEach { reason ->
            val vm = TestableViewModel()
            val call = MockFactory.createCall(sessionId = "s-$reason")
            vm.setCall(call)

            vm.callEnded("s-$reason", reason)

            vm.rejectedCall.value shouldBe call
            vm.endReason.value shouldBe reason
        }
    }

    test("a terminal push for another session is ignored") {
        val vm = TestableViewModel()
        val call = MockFactory.createCall(sessionId = "s-mine")
        vm.setCall(call)

        vm.callEnded("s-other", UIKitConstants.CallStatusConstants.REJECTED)
        vm.callEnded(null, UIKitConstants.CallStatusConstants.REJECTED)

        vm.rejectedCall.value shouldBe null
        vm.endReason.value shouldBe null
    }

    test("a terminal push after the call was answered does not reject it") {
        val vm = TestableViewModel()
        val call = MockFactory.createCall(sessionId = "s-answered")
        vm.setCall(call)

        vm.userJoined("callee-1")
        vm.callEnded("s-answered", UIKitConstants.CallStatusConstants.ENDED)

        vm.acceptedCall.value shouldBe call
        vm.rejectedCall.value shouldBe null
    }

    // ==================== Parity with Flutter ====================

    test("end reasons are normalised to the UI Kit's vocabulary, as on Flutter") {
        listOf(
            "declined" to UIKitConstants.CallStatusConstants.REJECTED,
            "timed_out" to UIKitConstants.CallStatusConstants.UNANSWERED,
            "remoteEnded" to UIKitConstants.CallStatusConstants.ENDED
        ).forEach { (raw, expected) ->
            val vm = TestableViewModel()
            val call = MockFactory.createCall(sessionId = "s-$raw")
            vm.setCall(call)

            vm.callEnded("s-$raw", raw)

            vm.endReason.value shouldBe expected
            vm.rejectedCall.value shouldBe call
        }
    }

    test("an end reason the screen does not know is ignored") {
        val vm = TestableViewModel()
        vm.setCall(MockFactory.createCall(sessionId = "s-odd"))

        vm.callEnded("s-odd", "something-new")

        vm.rejectedCall.value shouldBe null
        vm.endReason.value shouldBe null
    }

    test("End wins: an answer that lands after this side ended the call is not joined") {
        val vm = TestableViewModel()
        val call = MockFactory.createCall(sessionId = "s-late")
        vm.setCall(call)
        CometChatOutgoingCallViewModel::class.java.getDeclaredField("endRequested").apply {
            isAccessible = true
            setBoolean(vm, true)
        }

        vm.userJoined("callee-1")

        vm.acceptedCall.value shouldBe null
    }

    test("a call with no session cancels at once and disables End, and a second tap does nothing") {
        val vm = TestableViewModel()
        val call = MockFactory.createCall(sessionId = "")
        vm.setCall(call)

        vm.cancelCall()
        vm.cancelCall()

        vm.endCallButtonEnabled.value shouldBe false
        vm.endReason.value shouldBe UIKitConstants.CallStatusConstants.CANCELLED
        vm.rejectedCall.value shouldBe call
    }

    // ==================== Answered ====================

    test("the callee joining the session marks the call accepted and locks the end button") {
        val vm = TestableViewModel()
        val call = MockFactory.createCall(sessionId = "s-join")
        vm.setCall(call)

        vm.userJoined("callee-1")

        vm.acceptedCall.value shouldBe call
        vm.endCallButtonEnabled.value shouldBe false
        vm.rejectedCall.value shouldBe null
    }

    test("a join without a current call is a no-op") {
        val vm = TestableViewModel()

        vm.userJoined("callee-1")

        vm.acceptedCall.value shouldBe null
        vm.endCallButtonEnabled.value shouldBe true
    }

    test("a second join after acceptance changes nothing") {
        val vm = TestableViewModel()
        val call = MockFactory.createCall(sessionId = "s-join-2")
        vm.setCall(call)

        vm.userJoined("callee-1")
        vm.userJoined("callee-2")

        vm.acceptedCall.value shouldBe call
    }

    // ==================== Prepared session ====================

    test("the callee's phone connecting to the prepared session shows ringing, not answered") {
        val vm = TestableViewModel()
        vm.setCall(MockFactory.createCall(sessionId = "s-ring"))

        vm.participantConnected("callee-1")

        vm.calleeRinging.value shouldBe true
        vm.acceptedCall.value shouldBe null
    }

    test("the prepared session closing before a join ends the call, keeping a reason already pushed") {
        val vm = TestableViewModel()
        val call = MockFactory.createCall(sessionId = "s-closed")
        vm.setCall(call)

        vm.callEnded("s-closed", UIKitConstants.CallStatusConstants.BUSY)
        vm.connectionClosed()

        vm.rejectedCall.value shouldBe call
        vm.endReason.value shouldBe UIKitConstants.CallStatusConstants.BUSY
    }

    test("the prepared session closing with no push reports ended once the grace is over") {
        val vm = TestableViewModel()
        vm.closeGraceMs = 0
        val call = MockFactory.createCall(sessionId = "s-closed-2")
        vm.setCall(call)

        vm.connectionClosed()

        vm.rejectedCall.value shouldBe call
        vm.endReason.value shouldBe UIKitConstants.CallStatusConstants.ENDED
    }

    test("a terminal push that lands after the close still names the reason") {
        val vm = TestableViewModel()
        vm.closeGraceMs = 0
        val call = MockFactory.createCall(sessionId = "s-late")
        vm.setCall(call)

        vm.connectionClosed()
        vm.callEnded("s-late", UIKitConstants.CallStatusConstants.BUSY)

        vm.rejectedCall.value shouldBe call
        vm.endReason.value shouldBe UIKitConstants.CallStatusConstants.BUSY
    }

    test("while the grace runs, the push closes the call with its reason at once") {
        val vm = TestableViewModel()
        val call = MockFactory.createCall(sessionId = "s-grace")
        vm.setCall(call)

        vm.connectionClosed()
        vm.rejectedCall.value shouldBe null
        vm.callEnded("s-grace", UIKitConstants.CallStatusConstants.REJECTED)

        vm.rejectedCall.value shouldBe call
        vm.endReason.value shouldBe UIKitConstants.CallStatusConstants.REJECTED
    }

    test("a close after the callee joined does not reject the call") {
        val vm = TestableViewModel()
        val call = MockFactory.createCall(sessionId = "s-joined-close")
        vm.setCall(call)

        vm.userJoined("callee-1")
        vm.connectionClosed()

        vm.acceptedCall.value shouldBe call
        vm.rejectedCall.value shouldBe null
    }

    // ==================== Ring timer ====================

    test("the ring timer is off while listeners are disabled, so setCall never ends the call by itself") {
        val vm = TestableViewModel()
        vm.ringTimeoutMs = 1
        val call = MockFactory.createCall(sessionId = "s-timer")
        vm.setCall(call)

        Thread.sleep(20)

        vm.rejectedCall.value shouldBe null
        vm.endReason.value shouldBe null
    }

    test("the default ring timeout matches the Push SDK's 45 second ring window") {
        CometChatOutgoingCallViewModel.DEFAULT_RING_TIMEOUT_MS shouldBe 45_000L
        TestableViewModel().ringTimeoutMs shouldBe 45_000L
    }

    // ==================== Call message built from Calls SDK fields ====================

    test("CallMessageJson builds a Chat Call with session, type, status, caller and receiver") {
        val call: Call = CallMessageJson.build(
            sessionId = "s-json",
            callType = CometChatConstants.CALL_TYPE_VIDEO,
            callStatus = CometChatConstants.CALL_STATUS_INITIATED,
            receiverType = CometChatConstants.RECEIVER_TYPE_USER,
            receiverId = "callee-1",
            callerUid = "caller-1",
            callerName = "Caller One",
            callerAvatar = null,
            initiatedAtSec = 1_760_000_000L,
            conversationId = "caller-1_user_callee-1",
            messageId = 4242L
        )

        call.sessionId shouldBe "s-json"
        call.type shouldBe CometChatConstants.CALL_TYPE_VIDEO
        call.callStatus shouldBe CometChatConstants.CALL_STATUS_INITIATED
        call.receiverType shouldBe CometChatConstants.RECEIVER_TYPE_USER
        call.receiverUid shouldBe "callee-1"
        call.category shouldBe CometChatConstants.CATEGORY_CALL
        call.id shouldBe 4242L
        call.conversationId shouldBe "caller-1_user_callee-1"
    }
})
