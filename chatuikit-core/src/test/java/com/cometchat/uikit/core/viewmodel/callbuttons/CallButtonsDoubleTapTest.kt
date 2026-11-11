package com.cometchat.uikit.core.viewmodel.callbuttons

import com.cometchat.chat.core.Call
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.data.datasource.CallButtonsDataSource
import com.cometchat.uikit.core.data.repository.CallButtonsRepositoryImpl
import com.cometchat.uikit.core.domain.usecase.InitiateUserCallUseCase
import com.cometchat.uikit.core.domain.usecase.StartGroupCallUseCase
import com.cometchat.uikit.core.state.CallButtonsEvent
import com.cometchat.uikit.core.state.CallButtonsUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatCallButtonsViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.mock

/**
 * SM-07: a double tap on the voice icon started two calls. While the first is being started, a
 * second tap does nothing; a refusal from another entry point already starting a call is not
 * shown as an error.
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*CallButtonsDoubleTapTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CallButtonsDoubleTapTest : FunSpec({

    afterTest { Dispatchers.resetMain() }

    /** A data source whose call stays in flight until [release] completes; counts requests. */
    class HeldDataSource(private val result: Result<Call>) : CallButtonsDataSource {
        val release = CompletableDeferred<Unit>()
        var requests = 0
        override suspend fun initiateUserCall(receiverId: String, callType: String, receiver: User?): Result<Call> {
            requests++
            release.await()
            return result
        }
        override suspend fun sendGroupCallMessage(groupId: String, callType: String): Result<CustomMessage> =
            throw IllegalStateException("not a group call")
        override fun getActiveCall(): Call? = null
        override fun getActiveCallingExtensionCall(): Call? = null
        override fun isActiveMeeting(): Boolean = false
    }

    fun viewModel(dataSource: CallButtonsDataSource): CometChatCallButtonsViewModel {
        val repository = CallButtonsRepositoryImpl(dataSource)
        return CometChatCallButtonsViewModel(
            initiateUserCallUseCase = InitiateUserCallUseCase(repository),
            startGroupCallUseCase = StartGroupCallUseCase(repository),
            enableListeners = false
        ).apply { setUser(MockFactory.createUser(uid = "cometchat-uid-2", name = "George Alan")) }
    }

    test("a second tap while the first call is being started sends no second request") {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        runTest(dispatcher) {
            val dataSource = HeldDataSource(Result.success(mock<Call>()))
            val vm = viewModel(dataSource)
            val events = mutableListOf<CallButtonsEvent>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(events) }

            vm.initiateCall("audio")
            vm.initiateCall("audio")

            dataSource.requests shouldBe 1
            vm.uiState.value shouldBe CallButtonsUIState.Initiating

            dataSource.release.complete(Unit)
            advanceUntilIdle()

            dataSource.requests shouldBe 1
            events.count { it is CallButtonsEvent.CallInitiated } shouldBe 1
            vm.uiState.value shouldBe CallButtonsUIState.Idle
            job.cancel()
        }
    }

    test("after the first call is started, the button works again") {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        runTest(dispatcher) {
            val dataSource = HeldDataSource(Result.success(mock<Call>()))
            val vm = viewModel(dataSource)

            vm.initiateCall("audio")
            dataSource.release.complete(Unit)
            advanceUntilIdle()
            vm.initiateCall("video")
            advanceUntilIdle()

            dataSource.requests shouldBe 2
        }
    }

    test("a refusal because another entry point is starting a call is not reported") {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        runTest(dispatcher) {
            val refusal = CometChatException(UIKitConstants.CallErrorCodes.CALL_IN_PROGRESS, "already starting")
            val dataSource = HeldDataSource(Result.failure(refusal)).apply { release.complete(Unit) }
            val vm = viewModel(dataSource)
            val errors = mutableListOf<CometChatException>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.errorEvent.toList(errors) }

            vm.initiateCall("audio")
            advanceUntilIdle()

            errors.size shouldBe 0
            vm.uiState.value shouldBe CallButtonsUIState.Idle

            // The refusal must not leave the button stuck: the next tap sends a request.
            vm.initiateCall("audio")
            advanceUntilIdle()
            dataSource.requests shouldBe 2
            job.cancel()
        }
    }

    test("a real failure is still reported") {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        runTest(dispatcher) {
            val failure = CometChatException("ERR_UID_NOT_FOUND", "no such user")
            val dataSource = HeldDataSource(Result.failure(failure)).apply { release.complete(Unit) }
            val vm = viewModel(dataSource)
            val errors = mutableListOf<CometChatException>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.errorEvent.toList(errors) }

            vm.initiateCall("audio")
            advanceUntilIdle()

            errors.size shouldBe 1
            vm.uiState.value.shouldBeInstanceOf<CallButtonsUIState.Error>()
            job.cancel()
        }
    }
})
