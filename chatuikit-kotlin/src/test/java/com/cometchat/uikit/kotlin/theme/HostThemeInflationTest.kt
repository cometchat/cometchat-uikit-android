package com.cometchat.uikit.kotlin.theme

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.presentation.aiassistantchathistory.ui.CometChatAIAssistantChatHistory
import com.cometchat.uikit.kotlin.presentation.groupmembers.scopechange.CometChatScopeChange
import com.cometchat.uikit.kotlin.presentation.pinnedmessages.CometChatPinnedMessages
import com.cometchat.uikit.kotlin.presentation.savedmessages.CometChatSavedMessages
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-39309 regression: kit views must inflate without crashing even when the host app's theme
 * does NOT derive from `CometChatTheme.DayNight`.
 *
 * The views wrap their context in [cometchatThemedContext] so the layout's `?attr/cometchat*`
 * references resolve at inflate time. Wrapping only the super constructor is not enough — the
 * inflation happens inside `init {}`, where the bare `context` parameter (the host context) would
 * still be used unless the code explicitly inflates with `getContext()`. These tests inflate each
 * affected view (and the scope-change row) under a plain Material host theme and assert no
 * `InflateException` is thrown; they fail if the `init`-block inflation reverts to the raw context.
 *
 * Runs on the JVM via Robolectric — no device needed.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class HostThemeInflationTest {

    // Provides MaterialComponents attrs (MaterialCardView needs them) but NOT the kit's
    // ?attr/cometchat* attrs — i.e. the un-themed-host scenario ENG-39309 addresses.
    private val plainHostTheme = com.google.android.material.R.style.Theme_MaterialComponents_DayNight

    private fun withPlainThemedActivity(block: (ComponentActivity) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(plainHostTheme)
            block(activity)
        }
        scenario.close()
    }

    @Test
    fun `CometChatPinnedMessages inflates under a non-CometChat host theme`() {
        withPlainThemedActivity { activity -> CometChatPinnedMessages(activity) }
    }

    @Test
    fun `CometChatSavedMessages inflates under a non-CometChat host theme`() {
        withPlainThemedActivity { activity -> CometChatSavedMessages(activity) }
    }

    @Test
    fun `CometChatAIAssistantChatHistory inflates under a non-CometChat host theme`() {
        withPlainThemedActivity { activity -> CometChatAIAssistantChatHistory(activity) }
    }

    @Test
    fun `CometChatScopeChange and its row inflate under a non-CometChat host theme`() {
        withPlainThemedActivity { activity ->
            val view = CometChatScopeChange(activity)
            // The scope rows inflate lazily through the adapter; force one so the row layout
            // (?attr/cometchatTextColorPrimary) is actually inflated with the adapter's context.
            val parent: ViewGroup = FrameLayout(activity)
            view.getScopeAdapter().onCreateViewHolder(parent, 0)
        }
    }
}
