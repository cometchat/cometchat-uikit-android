package com.cometchat.uikit.core.utils

import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [CallingState] is the process-wide record of the call the UIKit is in: the call buttons refuse
 * a second call while one is active, logout cancels the outgoing call still ringing, and the
 * outgoing screen times its ring from when the call was placed.
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*CallingStateTest"
 */
class CallingStateTest : FunSpec({

    // The state is a singleton: leave nothing behind for the next test.
    afterTest {
        CallingState.clear()
        listOf("s-1", "s-2").forEach { CallingState.forgetPlaced(it) }
    }

    test("starts with no active call, no meeting and no ringing outgoing call") {
        CallingState.clear()

        CallingState.getActiveCall() shouldBe null
        CallingState.isActiveMeeting() shouldBe false
        CallingState.ringingOutgoingCall shouldBe null
    }

    test("the active call is the last one set, and setting null clears it") {
        val first = MockFactory.createCall(sessionId = "s-1")
        val second = MockFactory.createCall(sessionId = "s-2")

        CallingState.setActiveCall(first)
        CallingState.getActiveCall() shouldBe first

        CallingState.setActiveCall(second)
        CallingState.getActiveCall() shouldBe second

        CallingState.setActiveCall(null)
        CallingState.getActiveCall() shouldBe null
    }

    test("the meeting flag is independent of the active call") {
        val call = MockFactory.createCall(sessionId = "s-1")
        CallingState.setActiveCall(call)

        CallingState.setIsActiveMeeting(true)
        CallingState.isActiveMeeting() shouldBe true
        CallingState.getActiveCall() shouldBe call

        CallingState.setIsActiveMeeting(false)
        CallingState.isActiveMeeting() shouldBe false
        CallingState.getActiveCall() shouldBe call
    }

    test("clear() ends the active call, the meeting and the ringing outgoing call together") {
        CallingState.setActiveCall(MockFactory.createCall(sessionId = "s-1"))
        CallingState.setIsActiveMeeting(true)
        CallingState.ringingOutgoingCall = MockFactory.createCall(sessionId = "s-2")

        CallingState.clear()

        CallingState.getActiveCall() shouldBe null
        CallingState.isActiveMeeting() shouldBe false
        CallingState.ringingOutgoingCall shouldBe null
    }

    test("the time a call was placed is kept per session until it is forgotten") {
        CallingState.markPlaced("s-1", at = 1_000L)
        CallingState.markPlaced("s-2", at = 2_000L)

        CallingState.placedAt("s-1") shouldBe 1_000L
        CallingState.placedAt("s-2") shouldBe 2_000L

        CallingState.forgetPlaced("s-1")

        CallingState.placedAt("s-1") shouldBe null
        CallingState.placedAt("s-2") shouldBe 2_000L
    }

    test("placing the same session again replaces its time") {
        CallingState.markPlaced("s-1", at = 1_000L)
        CallingState.markPlaced("s-1", at = 5_000L)

        CallingState.placedAt("s-1") shouldBe 5_000L
    }

    test("a session that was never placed has no time, and forgetting it is harmless") {
        CallingState.placedAt("never-placed") shouldBe null

        CallingState.forgetPlaced("never-placed")

        CallingState.placedAt("never-placed") shouldBe null
    }
})
