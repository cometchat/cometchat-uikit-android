package com.cometchat.uikit.core.utils

import android.content.Context
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.model.InitiateCallOptions
import com.cometchat.calls.model.InitiatedCall
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.core.constants.UIKitConstants
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.eq
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.times
import org.mockito.kotlin.never

/**
 * SM-07: two taps on a call button 120 ms apart both reached the server before either session
 * existed, so two calls were created and the second one's busy reply closed the first.
 * [CallInitiationUtils.initiateUserCall] now refuses a start while another is in flight.
 *
 * The Calls SDK is mocked so its callback is held: that is a request still in flight. A held
 * callback that is never released is the Review Buddy P1 on the v6.2.0 release PR: the guard
 * must clear on its own, or calling stays refused until the app restarts.
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*CallInitiationInFlightGuardTest"
 */
class CallInitiationInFlightGuardTest : FunSpec({

    lateinit var calls: MockedStatic<CometChatCalls>
    val pending = mutableListOf<CometChatCalls.CallbackListener<InitiatedCall>>()
    val context = mock<Context>()
    val audio = CometChatConstants.CALL_TYPE_AUDIO

    beforeTest {
        pending.clear()
        whenever(context.getString(any<Int>())).thenReturn("No internet connection")
        calls = Mockito.mockStatic(CometChatCalls::class.java)
        calls.`when`<Unit> {
            CometChatCalls.initiateCall(
                any<Context>(),
                any<String>(),
                any<InitiateCallOptions>(),
                any<CometChatCalls.CallbackListener<InitiatedCall>>()
            )
        }.thenAnswer { invocation ->
            pending += invocation.getArgument<CometChatCalls.CallbackListener<InitiatedCall>>(3)
            null
        }
    }

    afterTest {
        // Never leave a request open for the next test: the guard is process-wide.
        pending.forEach { it.onError(com.cometchat.calls.exceptions.CometChatException("TEST", "done")) }
        calls.close()
    }

    fun serverError() = com.cometchat.calls.exceptions.CometChatException("ERR_TEST", "server said no")

    fun created(sessionId: String): InitiatedCall = mock { on { this.sessionId } doReturn sessionId }

    fun endCalls(sessionId: String, n: Int) = calls.verify(
        { CometChatCalls.endCall(eq(sessionId), eq(UIKitConstants.CallStatusConstants.CANCELLED), any()) },
        times(n)
    )

    test("a second start while the first is in flight is refused and sends no request") {
        runTest {
            val first = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-2", audio) }
            runCurrent()
            pending.size shouldBe 1

            val second = CallInitiationUtils.initiateUserCall(context, "cometchat-uid-2", audio)

            pending.size shouldBe 1
            (second.exceptionOrNull() as CometChatException).code shouldBe
                UIKitConstants.CallErrorCodes.CALL_IN_PROGRESS
            CallInitiationUtils.isCallInProgress(second.exceptionOrNull()) shouldBe true

            pending.removeAt(0).onError(serverError())
            first.await().isFailure shouldBe true
        }
    }

    test("the first request's own result is not the in-progress refusal") {
        runTest {
            val first = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-2", audio) }
            runCurrent()
            pending.removeAt(0).onError(serverError())

            val error = first.await().exceptionOrNull() as CometChatException
            error.code shouldBe "ERR_TEST"
            CallInitiationUtils.isCallInProgress(error) shouldBe false
        }
    }

    test("once the first request is answered, a new call can start") {
        runTest {
            val first = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-2", audio) }
            runCurrent()
            pending.removeAt(0).onError(serverError())
            first.await()

            val next = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-4", audio) }
            runCurrent()

            pending.size shouldBe 1
            pending.removeAt(0).onError(serverError())
            CallInitiationUtils.isCallInProgress(next.await().exceptionOrNull()) shouldBe false
        }
    }

    test("a callback that never arrives times out, fails as NO_INTERNET and frees the guard") {
        runTest {
            val stuck = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-2", audio) }
            runCurrent()
            pending.size shouldBe 1

            advanceTimeBy(CallInitiationUtils.INITIATE_TIMEOUT_MS - 1)
            runCurrent()
            stuck.isCompleted shouldBe false
            CallInitiationUtils.isCallInProgress(
                CallInitiationUtils.initiateUserCall(context, "cometchat-uid-2", audio).exceptionOrNull()
            ) shouldBe true

            advanceTimeBy(2)
            runCurrent()
            val error = stuck.await().exceptionOrNull() as CometChatException
            error.code shouldBe UIKitConstants.CallErrorCodes.NO_INTERNET
            CallInitiationUtils.isCallInProgress(error) shouldBe false

            // The guard is free again: the next start reaches the Calls SDK.
            val next = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-4", audio) }
            runCurrent()
            pending.size shouldBe 2
            pending.removeAt(1).onError(serverError())
            (next.await().exceptionOrNull() as CometChatException).code shouldBe "ERR_TEST"

            // The stale request's late answer is ignored, not delivered to anyone.
            pending.removeAt(0).onError(serverError())
            stuck.await().isFailure shouldBe true
        }
    }

    test("a request abandoned by its caller frees the guard") {
        runTest {
            val abandoned = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-2", audio) }
            runCurrent()
            pending.size shouldBe 1
            abandoned.cancel()
            runCurrent()

            val next = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-4", audio) }
            runCurrent()
            pending.size shouldBe 2
            pending.removeAt(1).onError(serverError())
            CallInitiationUtils.isCallInProgress(next.await().exceptionOrNull()) shouldBe false
        }
    }

    test("a success the caller stopped waiting for is cancelled on the backend, once") {
        runTest {
            val job = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-2", audio) }
            runCurrent()
            pending.size shouldBe 1

            // The Calls SDK answers (the callee is now being pushed), but the caller's scope is
            // cancelled before the test dispatcher delivers the resumed value: prompt
            // cancellation drops the result, so nobody would ever own session s-1.
            pending.removeAt(0).onSuccess(created("s-1"))
            job.cancel()
            runCurrent()

            job.isCancelled shouldBe true
            endCalls("s-1", 1)
            CallingState.placedAt("s-1") shouldBe null

            // The guard is free and the next start reaches the Calls SDK.
            val next = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-4", audio) }
            runCurrent()
            pending.size shouldBe 1
            pending.removeAt(0).onError(serverError())
            CallInitiationUtils.isCallInProgress(next.await().exceptionOrNull()) shouldBe false
        }
    }

    test("a success that lands after the wait timed out is cancelled on the backend") {
        runTest {
            val stuck = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-2", audio) }
            runCurrent()
            advanceTimeBy(CallInitiationUtils.INITIATE_TIMEOUT_MS + 1)
            runCurrent()
            (stuck.await().exceptionOrNull() as CometChatException).code shouldBe
                UIKitConstants.CallErrorCodes.NO_INTERNET

            pending.removeAt(0).onSuccess(created("s-late"))

            endCalls("s-late", 1)
            CallingState.placedAt("s-late") shouldBe null
        }
    }

    test("a success that is delivered is not cancelled") {
        // Building the Chat SDK Call from the delivered InitiatedCall reads the logged-in user.
        val chat = Mockito.mockStatic(com.cometchat.chat.core.CometChat::class.java)
        try {
            chat.`when`<com.cometchat.chat.models.User?> { com.cometchat.chat.core.CometChat.getLoggedInUser() }
                .thenReturn(com.cometchat.chat.models.User("me", "Me"))
            runTest {
                val first = async { CallInitiationUtils.initiateUserCall(context, "cometchat-uid-2", audio) }
                runCurrent()
                pending.removeAt(0).onSuccess(created("s-ok"))
                runCurrent()

                first.await().isSuccess shouldBe true
                endCalls("s-ok", 0)
                CallingState.forgetPlaced("s-ok")
            }
        } finally {
            chat.close()
        }
    }

    test("isCallInProgress ignores other errors and null") {
        CallInitiationUtils.isCallInProgress(null) shouldBe false
        CallInitiationUtils.isCallInProgress(RuntimeException("x")) shouldBe false
        CallInitiationUtils.isCallInProgress(
            CometChatException(UIKitConstants.CallErrorCodes.ACTIVE_CALL, "busy")
        ) shouldBe false
    }
})
