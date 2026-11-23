package com.cometchat.uikit.core

import android.content.Context
import android.os.Looper
import com.cometchat.chat.core.AppSettings
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.OfflineConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

/**
 * [UIKitSettings] offline-config pass-through (G14): [CometChatUIKit.init] hands the configured
 * [OfflineConfig] to the SDK's `AppSettings.AppSettingsBuilder.setOfflineConfig`, and makes no such
 * call when none is configured (offline stays off). The builder is intercepted with
 * `mockConstruction`, so the assertion is on the exact object `init` builds.
 */
class UIKitSettingsOfflineConfigTest : FunSpec({

    /** Runs `init` on a simulated main thread with the SDK statics mocked; returns the built builder. */
    fun initAndCaptureBuilder(settings: UIKitSettings): AppSettings.AppSettingsBuilder {
        Mockito.mockStatic(Looper::class.java).use { looper ->
            val main = Mockito.mock(Looper::class.java)
            looper.`when`<Looper> { Looper.getMainLooper() }.thenReturn(main)
            looper.`when`<Looper?> { Looper.myLooper() }.thenReturn(main)
            Mockito.mockStatic(CometChat::class.java).use {
                Mockito.mockConstruction(AppSettings.AppSettingsBuilder::class.java).use { constructed ->
                    CometChatUIKit.init(mock<Context>(), settings, null)
                    return constructed.constructed().single()
                }
            }
        }
    }

    test("setOfflineConfig is carried onto the built settings") {
        val config = mock<OfflineConfig>()
        val settings = UIKitSettings.UIKitSettingsBuilder().setAppId("app").setRegion("us").setOfflineConfig(config).build()
        settings.offlineConfig shouldBe config
    }

    test("init passes the configured OfflineConfig to AppSettingsBuilder.setOfflineConfig") {
        val config = mock<OfflineConfig>()
        val settings = UIKitSettings.UIKitSettingsBuilder().setAppId("app").setRegion("us").setOfflineConfig(config).build()
        val builder = initAndCaptureBuilder(settings)
        verify(builder).setOfflineConfig(config)
    }

    test("init makes no setOfflineConfig call when none is configured") {
        val settings = UIKitSettings.UIKitSettingsBuilder().setAppId("app").setRegion("us").build()
        settings.offlineConfig shouldBe null
        val builder = initAndCaptureBuilder(settings)
        verify(builder, never()).setOfflineConfig(any())
    }
})
