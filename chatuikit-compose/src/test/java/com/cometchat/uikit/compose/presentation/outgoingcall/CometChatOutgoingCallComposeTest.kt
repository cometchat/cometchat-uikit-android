package com.cometchat.uikit.compose.presentation.outgoingcall

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.outgoingcall.style.CometChatOutgoingCallStyle
import com.cometchat.uikit.compose.presentation.outgoingcall.ui.CometChatOutgoingCall
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
 * ENG-38680 — Compose CometChatOutgoingCall matrix (style) + functional. Uses a
 * mock Call; no live calls runtime. avatarStyle nested waived.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatOutgoingCallComposeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun mockCall(): Call {
        val caller = mock<User>().also { whenever(it.uid).thenReturn("c"); whenever(it.name).thenReturn("PM Caller"); whenever(it.avatar).thenReturn(null) }
        return mock<Call>().also {
            whenever(it.sessionId).thenReturn("s1"); whenever(it.type).thenReturn(CometChatConstants.CALL_TYPE_AUDIO)
            whenever(it.callInitiator).thenReturn(caller); whenever(it.callReceiver).thenReturn(caller); whenever(it.sender).thenReturn(caller)
        }
    }

    @Test
    fun everyScalarStyleField_isOverridable_andRestyledComponentRenders() {
        lateinit var base: CometChatOutgoingCallStyle
        lateinit var custom: CometChatOutgoingCallStyle
        rule.setContent {
            CometChatTheme {
                base = CometChatOutgoingCallStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp; val ts = TextStyle(fontSize = 33.sp); val p = ColorPainter(Color(0xFF00C0FF))
                custom = base.copy(
                    backgroundColor = c, strokeColor = c, titleTextColor = c, subtitleTextColor = c,
                    endCallIconTint = c, endCallButtonBackgroundColor = c,
                    cornerRadius = d, strokeWidth = d,
                    titleTextStyle = ts, subtitleTextStyle = ts,
                    endCallIcon = p,
                )
                CometChatOutgoingCall(call = mockCall(), style = custom)
            }
        }
        rule.waitForIdle()
        val scalarFields = CometChatOutgoingCallStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name != "avatarStyle"
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals("every scalar style field overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 11 scalar style fields", 11, scalarFields.size)
    }

    @Test
    fun outgoingCall_rendersEndCallButton() {
        rule.setContent { CometChatTheme { CometChatOutgoingCall(call = mockCall()) } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("End call", substring = true).assertExists()
    }

    @Test
    fun onEndCallClick_firesOnEndButtonClick() {
        var ended = false
        rule.setContent { CometChatTheme { CometChatOutgoingCall(call = mockCall(), onEndCallClick = { ended = true }) } }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("End call", substring = true).performClick()
        assertTrue("onEndCallClick should fire", ended)
    }
}
