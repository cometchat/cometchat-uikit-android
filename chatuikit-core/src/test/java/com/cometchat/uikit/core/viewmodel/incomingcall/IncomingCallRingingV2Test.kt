package com.cometchat.uikit.core.viewmodel.incomingcall

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.utils.CallRingingUtils
import com.cometchat.uikit.core.viewmodel.CometChatIncomingCallViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Ringing v2 (ENG-39907): the incoming-call ViewModel answers and declines without the
 * Chat SDK's deprecated calling APIs and without the Push SDK, closes when the ring ends
 * elsewhere, and the fields of a Push SDK IncomingCall convert into the Chat SDK Call the
 * component renders through the kit's Push-free builder.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IncomingCallRingingV2Test : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    fun createViewModel() = CometChatIncomingCallViewModel(enableListeners = false)

    beforeTest { Dispatchers.setMain(testDispatcher) }
    afterTest { Dispatchers.resetMain() }

    // ==================== Accept / decline ====================

    test("acceptCall emits the current call as accepted without a Chat SDK round trip") {
        val viewModel = createViewModel()
        val call = MockFactory.createCall(sessionId = "s-1")
        viewModel.setCall(call)

        viewModel.acceptCall()

        viewModel.acceptedCall.value shouldBe call
        viewModel.rejectedCall.value shouldBe null
    }

    test("rejectCall emits the current call as rejected without a Chat SDK round trip") {
        val viewModel = createViewModel()
        val call = MockFactory.createCall(sessionId = "s-2")
        viewModel.setCall(call)

        viewModel.rejectCall()

        viewModel.rejectedCall.value shouldBe call
        viewModel.acceptedCall.value shouldBe null
    }

    // ==================== Ring ended elsewhere ====================

    test("onCallEnded for the current session closes the screen") {
        val viewModel = createViewModel()
        val call = MockFactory.createCall(sessionId = "s-3")
        viewModel.setCall(call)

        viewModel.onCallEnded("s-3")

        viewModel.rejectedCall.value shouldBe call
    }

    test("onCallEnded for another session is ignored") {
        val viewModel = createViewModel()
        viewModel.setCall(MockFactory.createCall(sessionId = "s-4"))

        viewModel.onCallEnded("other-session")

        viewModel.rejectedCall.value shouldBe null
    }

    test("onCallEnded after answering does not turn the answer into a rejection") {
        val viewModel = createViewModel()
        viewModel.setCall(MockFactory.createCall(sessionId = "s-5"))
        viewModel.acceptCall()

        // The Push SDK reports ring end "answered" right after the answer.
        viewModel.onCallEnded("s-5")

        viewModel.rejectedCall.value shouldBe null
    }

    // ==================== IncomingCall fields -> Call ====================

    test("buildIncomingCall maps a 1:1 push call into a Chat SDK Call") {
        val call = CallRingingUtils.buildIncomingCall(
            sessionId = "session-v2",
            callType = CometChatConstants.CALL_TYPE_VIDEO,
            receiverType = CometChatConstants.RECEIVER_TYPE_USER,
            receiverUid = "callee-1",
            callerUid = "caller-1",
            callerName = "Caller One",
            callerAvatarUrl = "https://example.com/a.png",
            initiatedAtMs = 1_760_000_000_000L,
            conversationId = "caller-1_user_callee-1"
        )

        call.sessionId shouldBe "session-v2"
        call.type shouldBe CometChatConstants.CALL_TYPE_VIDEO
        call.category shouldBe CometChatConstants.CATEGORY_CALL
        call.callStatus shouldBe CometChatConstants.CALL_STATUS_INITIATED
        call.receiverType shouldBe CometChatConstants.RECEIVER_TYPE_USER
        call.receiverUid shouldBe "callee-1"
        call.conversationId shouldBe "caller-1_user_callee-1"
        call.initiatedAt shouldBe 1_760_000_000L

        val initiator = call.callInitiator
        initiator.shouldBeInstanceOf<User>()
        initiator.uid shouldBe "caller-1"
        initiator.name shouldBe "Caller One"
        initiator.avatar shouldBe "https://example.com/a.png"

        val receiver = call.callReceiver
        receiver.shouldBeInstanceOf<User>()
        receiver.uid shouldBe "callee-1"
    }

    test("buildIncomingCall maps an audio group push call with the group as receiver") {
        val call = CallRingingUtils.buildIncomingCall(
            sessionId = "session-g",
            callType = CometChatConstants.CALL_TYPE_AUDIO,
            receiverType = CometChatConstants.RECEIVER_TYPE_GROUP,
            receiverUid = "group-1",
            callerUid = "caller-1",
            callerName = "Caller One",
            callerAvatarUrl = null,
            initiatedAtMs = 1_760_000_000_000L
        )

        call.type shouldBe CometChatConstants.CALL_TYPE_AUDIO
        call.receiverType shouldBe CometChatConstants.RECEIVER_TYPE_GROUP
        val receiver = call.callReceiver
        receiver.shouldBeInstanceOf<Group>()
        receiver.guid shouldBe "group-1"
        (call.callInitiator as User).avatar shouldBe null
    }

    test("buildIncomingCall falls back to the uid when the caller has no name") {
        val call = CallRingingUtils.buildIncomingCall(
            sessionId = "session-n",
            callType = "VIDEO",
            receiverUid = "callee-1",
            callerUid = "caller-1",
            callerName = null
        )

        call.type shouldBe CometChatConstants.CALL_TYPE_VIDEO
        (call.callInitiator as User).name shouldBe "caller-1"
    }
})
