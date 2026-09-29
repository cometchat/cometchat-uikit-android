package com.cometchat.uikit.compose.presentation.incomingcall

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.incomingcall.style.CometChatIncomingCallStyle
import com.cometchat.uikit.compose.presentation.incomingcall.ui.CometChatIncomingCall
import com.cometchat.uikit.compose.theme.CometChatTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38680 — Compose CometChatIncomingCall matrix (style) + functional (semantics).
 * Uses a mock Call; no live calls runtime. avatarStyle nested waived.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatIncomingCallComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun mockCall(name: String = "PM Caller"): Call {
        val caller = mock<User>().also { whenever(it.uid).thenReturn("c"); whenever(it.name).thenReturn(name); whenever(it.avatar).thenReturn(null) }
        return mock<Call>().also {
            whenever(it.sessionId).thenReturn("s1"); whenever(it.type).thenReturn(CometChatConstants.CALL_TYPE_AUDIO)
            whenever(it.callInitiator).thenReturn(caller); whenever(it.sender).thenReturn(caller)
        }
    }

    @Test
    fun everyScalarStyleField_isOverridable_andRestyledComponentRenders() {
        lateinit var base: CometChatIncomingCallStyle
        lateinit var custom: CometChatIncomingCallStyle
        rule.setContent {
            CometChatTheme {
                base = CometChatIncomingCallStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp; val ts = TextStyle(fontSize = 33.sp); val p = ColorPainter(Color(0xFF00C0FF))
                custom = base.copy(
                    backgroundColor = c, strokeColor = c, titleTextColor = c, subtitleTextColor = c, iconTint = c,
                    acceptButtonBackgroundColor = c, acceptButtonTextColor = c, rejectButtonBackgroundColor = c, rejectButtonTextColor = c,
                    cornerRadius = d, strokeWidth = d,
                    titleTextStyle = ts, subtitleTextStyle = ts, acceptButtonTextStyle = ts, rejectButtonTextStyle = ts,
                    voiceCallIcon = p, videoCallIcon = p,
                )
                CometChatIncomingCall(call = mockCall(), style = custom)
            }
        }
        rule.waitForIdle()
        val scalarFields = CometChatIncomingCallStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name != "avatarStyle"
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals("every scalar style field overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 17 scalar style fields", 17, scalarFields.size)
    }

    @Test
    fun callerName_rendersFromCall() {
        rule.setContent { CometChatTheme { CometChatIncomingCall(call = mockCall("Iron Man")) } }
        rule.waitForIdle()
        rule.onNodeWithText("Iron Man").assertIsDisplayed()
    }

    @Test
    fun acceptButton_isRenderedAndClickable() {
        // Accept always routes through viewModel.acceptCall() (Calls-SDK boundary),
        // so the callback isn't firable without the calls runtime; assert the
        // UI-Kit surface — the Accept button is rendered and carries a click action.
        rule.setContent { CometChatTheme { CometChatIncomingCall(call = mockCall(), onAcceptClick = {}) } }
        rule.waitForIdle()
        rule.onNodeWithText("Accept").assertIsDisplayed().assertHasClickAction()
    }

    @Test
    fun onRejectClick_firesOnDeclineButtonClick() {
        var rejected = false
        rule.setContent { CometChatTheme { CometChatIncomingCall(call = mockCall(), onRejectClick = { rejected = true }) } }
        rule.waitForIdle()
        rule.onNodeWithText("Decline").performClick()
        assertTrue("onRejectClick should fire", rejected)
    }
}
