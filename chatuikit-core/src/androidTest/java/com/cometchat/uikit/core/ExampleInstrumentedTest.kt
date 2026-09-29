package com.cometchat.uikit.core

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // A library module has no separate app under test, so the instrumentation
        // targets its own test APK: the target context carries the `.test` suffix,
        // not the library's namespace. The assertion AGP generates assumes an
        // application module and has always been wrong here.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.cometchat.uikit.core.test", appContext.packageName)
    }
}