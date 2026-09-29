package com.cometchat.uikit.core

import android.os.Looper
import com.cometchat.chat.core.CometChat
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import org.mockito.Mockito

/**
 * ENG-38658 (X5): the threading contract on CometChatUIKit's entry points.
 *
 * init/initFromSettings/login/loginWithAuthToken/logout are main-thread-only
 * and must fail fast with a clear IllegalStateException when called from any
 * other thread. Looper is static-mocked so the JVM test can simulate both a
 * main-thread and an off-main-thread caller.
 */
class CometChatUIKitThreadingContractTest : FunSpec({

    fun offMainThread(block: () -> Unit) {
        Mockito.mockStatic(Looper::class.java).use { looper ->
            val main = Mockito.mock(Looper::class.java)
            looper.`when`<Looper> { Looper.getMainLooper() }.thenReturn(main)
            looper.`when`<Looper?> { Looper.myLooper() }.thenReturn(null)
            block()
        }
    }

    test("login off the main thread fails fast with a clear message") {
        offMainThread {
            val ex = shouldThrow<IllegalStateException> {
                CometChatUIKit.login("some-uid", null)
            }
            ex.message shouldContain "CometChatUIKit.login must be called from the main thread"
        }
    }

    test("logout off the main thread fails fast") {
        offMainThread {
            shouldThrow<IllegalStateException> {
                CometChatUIKit.logout(null)
            }
        }
    }

    test("loginWithAuthToken off the main thread fails fast") {
        offMainThread {
            shouldThrow<IllegalStateException> {
                CometChatUIKit.loginWithAuthToken("token", null)
            }
        }
    }

    test("a main-thread caller passes the guard") {
        Mockito.mockStatic(Looper::class.java).use { looper ->
            val main = Mockito.mock(Looper::class.java)
            looper.`when`<Looper> { Looper.getMainLooper() }.thenReturn(main)
            looper.`when`<Looper?> { Looper.myLooper() }.thenReturn(main)
            Mockito.mockStatic(CometChat::class.java).use {
                // must not throw: the guard passes and logout proceeds into the SDK
                CometChatUIKit.logout(null)
            }
        }
    }
})
