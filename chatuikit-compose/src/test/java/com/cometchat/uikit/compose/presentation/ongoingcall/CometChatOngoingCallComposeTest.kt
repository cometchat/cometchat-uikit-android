package com.cometchat.uikit.compose.presentation.ongoingcall

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.compose.presentation.ongoingcall.style.CometChatOngoingCallStyle
import com.cometchat.uikit.compose.presentation.ongoingcall.ui.CometChatOngoingCall
import com.cometchat.uikit.compose.theme.CometChatTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38680 — Compose CometChatOngoingCall, Phase 4 (Calls-SDK boundary).
 *
 * OngoingCall wraps the Calls SDK. Without a live calls runtime the component must
 * render its own "connecting" surface gracefully (the documented
 * ClassNotFoundException contract, Track 2/A5) — never crash. These assert exactly
 * that + the style matrix. Goldens are deferred (statusBarColor bug, Track 1/T5).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatOngoingCallComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun everyScalarStyleField_isOverridable_andRestyledComponentRenders() {
        lateinit var base: CometChatOngoingCallStyle
        lateinit var custom: CometChatOngoingCallStyle
        rule.setContent {
            CometChatTheme {
                base = CometChatOngoingCallStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp
                custom = base.copy(
                    backgroundColor = c, progressIndicatorColor = c, cornerRadius = d, strokeWidth = d, strokeColor = c,
                )
                CometChatOngoingCall(sessionId = "s1", callType = CometChatConstants.CALL_TYPE_AUDIO, style = custom)
            }
        }
        rule.waitForIdle()
        val scalarFields = CometChatOngoingCallStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers)
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals("every style field overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 5 style fields", 5, scalarFields.size)
    }

    @Test
    fun rendersConnectingState_gracefullyWithoutCallsRuntime() {
        rule.setContent { CometChatTheme { CometChatOngoingCall(sessionId = "s1", callType = CometChatConstants.CALL_TYPE_AUDIO) } }
        rule.waitForIdle()
        // No crash at the Calls-SDK boundary; the UI-Kit renders its own connecting surface.
        rule.onNodeWithContentDescription("Ongoing call").assertExists()
        rule.onNodeWithContentDescription("Connecting to call").assertExists()
    }

    @Test
    fun videoCallType_alsoRendersGracefully() {
        rule.setContent { CometChatTheme { CometChatOngoingCall(sessionId = "s2", callType = CometChatConstants.CALL_TYPE_VIDEO) } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Ongoing call").assertExists()
    }
}
