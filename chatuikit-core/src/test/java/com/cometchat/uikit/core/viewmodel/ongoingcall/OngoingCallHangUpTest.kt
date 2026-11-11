package com.cometchat.uikit.core.viewmodel.ongoingcall

import androidx.lifecycle.LifecycleOwner
import com.cometchat.calls.core.CallSession
import com.cometchat.calls.listeners.ButtonClickListener
import com.cometchat.uikit.core.constants.UIKitConstants.CallWorkFlow
import com.cometchat.uikit.core.models.OngoingCallUIState
import com.cometchat.uikit.core.viewmodel.CometChatOngoingCallViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Ringing v2 (ENG-39907): the Calls SDK's own End button runs leave() before it calls
 * ButtonClickListener.onLeaveSessionButtonClicked, so the kit must only clean up there. A
 * hang-up started from code (endCall) still leaves the session itself.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OngoingCallHangUpTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()

    beforeTest { Dispatchers.setMain(testDispatcher) }
    afterTest { Dispatchers.resetMain() }

    /** A ViewModel bound to [session]. */
    fun viewModelOn(session: CallSession, workFlow: CallWorkFlow): CometChatOngoingCallViewModel {
        val vm = CometChatOngoingCallViewModel()
        vm.callSession = session
        vm.setCallWorkFlow(workFlow)
        return vm
    }

    fun endButtonOf(vm: CometChatOngoingCallViewModel, session: CallSession): ButtonClickListener {
        vm.addListeners(mock<LifecycleOwner>())
        val captor = argumentCaptor<ButtonClickListener>()
        verify(session).addButtonClickListener(any(), captor.capture())
        return captor.firstValue
    }

    CallWorkFlow.values().forEach { workFlow ->
        test("$workFlow: the SDK End button closes the call without a second leave()") {
            val session = mock<CallSession>()
            val vm = viewModelOn(session, workFlow)

            endButtonOf(vm, session).onLeaveSessionButtonClicked()

            verify(session, never()).leave()
            vm.uiState.value shouldBe OngoingCallUIState.Ended
        }

        test("$workFlow: endCall from code leaves the session once") {
            val session = mock<CallSession>()
            val vm = viewModelOn(session, workFlow)

            vm.endCall()

            verify(session, times(1)).leave()
            vm.uiState.value shouldBe OngoingCallUIState.Ended
        }
    }
})
