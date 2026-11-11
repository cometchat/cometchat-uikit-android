package com.cometchat.uikit.core.utils

import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.UIKitSettings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.cometchat.calls.core.CallSession
import com.cometchat.calls.core.CometChatCalls
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * [CallsUtils] decides whether the UIKit shows calling at all: the Calls SDK must be on the
 * classpath and the app must have turned calling on in [UIKitSettings]. It also tells the
 * deprecated ring screen whether the Push SDK's calls module is there to ring instead.
 *
 * Run: ./gradlew :chatuikit-core:testDebugUnitTest --tests "*CallsUtilsTest"
 */
class CallsUtilsTest : FunSpec({

    /**
     * The settings live on CometChatUIKit's private field; `mockStatic` cannot intercept a Kotlin
     * object's instance methods, so inject them directly.
     */
    fun setSettings(settings: UIKitSettings?) {
        val field = CometChatUIKit::class.java.getDeclaredField("authenticationSettings")
        field.isAccessible = true
        field.set(CometChatUIKit, settings)
    }

    fun settings(enableCalling: Boolean): UIKitSettings = UIKitSettings.UIKitSettingsBuilder()
        .setAppId("app").setRegion("us").setAuthKey("key")
        .setEnableCalling(enableCalling)
        .build()

    beforeTest { CallsUtils.clearCache() }

    afterTest {
        setSettings(null)
        CallsUtils.clearCache()
    }

    test("the Calls SDK is available: the kit depends on it") {
        CallsUtils.isCallsSDKAvailable() shouldBe true
    }

    test("the Push SDK calls module is not available: the kit does not depend on it") {
        CallsUtils.isPushCallsSDKAvailable() shouldBe false
    }

    test("the availability checks give the same answer after the cache is cleared") {
        val calls = CallsUtils.isCallsSDKAvailable()
        val pushCalls = CallsUtils.isPushCallsSDKAvailable()

        CallsUtils.clearCache()

        CallsUtils.isCallsSDKAvailable() shouldBe calls
        CallsUtils.isPushCallsSDKAvailable() shouldBe pushCalls
    }

    test("calling is off before the UIKit has settings") {
        setSettings(null)

        CallsUtils.isCallingEnabled() shouldBe false
    }

    test("calling is off when the app did not turn it on") {
        setSettings(settings(enableCalling = false))

        CallsUtils.isCallingEnabled() shouldBe false
    }

    test("calling is on when the app turned it on and the Calls SDK is there") {
        setSettings(settings(enableCalling = true))

        CallsUtils.isCallingEnabled() shouldBe true
    }

    test("calling follows the current settings: only SDK availability is cached") {
        setSettings(settings(enableCalling = true))
        CallsUtils.isCallingEnabled() shouldBe true

        setSettings(settings(enableCalling = false))
        CallsUtils.isCallingEnabled() shouldBe false

        setSettings(settings(enableCalling = true))
        CallsUtils.isCallingEnabled() shouldBe true
    }

    // ==================== ownsActiveSession ====================

    /** [block] runs while the Calls SDK reports [sessionId] (null: nothing) as its active session. */
    fun withActiveSession(sessionId: String?, block: () -> Unit) {
        val calls: MockedStatic<CometChatCalls> = Mockito.mockStatic(CometChatCalls::class.java)
        try {
            val session: CallSession? = sessionId?.let { id -> mock { on { this.sessionId } doReturn id } }
            calls.`when`<CallSession?> { CometChatCalls.getActiveSession() }.thenReturn(session)
            block()
        } finally {
            calls.close()
        }
    }

    test("with no active session any screen may tear down: the leave is a no-op") {
        withActiveSession(null) {
            CallsUtils.ownsActiveSession("s-A") shouldBe true
            CallsUtils.ownsActiveSession(null) shouldBe true
        }
    }

    test("a screen owns the active session only when it is its own") {
        withActiveSession("s-A") {
            CallsUtils.ownsActiveSession("s-A") shouldBe true
        }
    }

    test("an old call screen destroyed after the next call became active leaves it alone") {
        // "End & accept": screen A (s-A) is destroyed after screen B prepared s-B.
        withActiveSession("s-B") {
            CallsUtils.ownsActiveSession("s-A") shouldBe false
        }
    }

    test("a screen that never learned its session id owns nothing while a call is active") {
        withActiveSession("s-B") {
            CallsUtils.ownsActiveSession(null) shouldBe false
        }
    }
})
