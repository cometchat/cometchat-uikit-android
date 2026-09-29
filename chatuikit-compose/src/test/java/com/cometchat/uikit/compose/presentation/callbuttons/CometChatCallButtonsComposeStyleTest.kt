package com.cometchat.uikit.compose.presentation.callbuttons

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.callbuttons.style.CallButtonStyle
import com.cometchat.uikit.compose.presentation.callbuttons.style.CometChatCallButtonsStyle
import com.cometchat.uikit.compose.presentation.callbuttons.ui.CometChatCallButtons
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
 * ENG-38680 — Compose CometChatCallButtonsStyle matrix (.copy() + reflection
 * completeness guard). 11 scalar fields overridden; 2 nested CallButtonStyle waived.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCallButtonsComposeStyleTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun fieldsOf(cls: Class<*>) = cls.declaredFields
        .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
        .onEach { it.isAccessible = true }

    @Test
    fun everyStyleField_isOverridable_andRestyledComponentRenders() {
        lateinit var baseBtn: CallButtonStyle
        lateinit var customBtn: CallButtonStyle
        lateinit var base: CometChatCallButtonsStyle
        lateinit var custom: CometChatCallButtonsStyle
        rule.setContent {
            CometChatTheme {
                val c = Color(0xFFAB12CD); val d = 77.dp; val ts = TextStyle(fontSize = 33.sp); val p = ColorPainter(Color(0xFF00C0FF))
                base = CometChatCallButtonsStyle.default()
                baseBtn = base.voiceCallButtonStyle
                customBtn = baseBtn.copy(
                    icon = p, iconTint = c, iconSize = d, textColor = c, textStyle = ts,
                    backgroundColor = c, cornerRadius = d, strokeWidth = d, strokeColor = c, buttonPadding = d,
                )
                custom = base.copy(voiceCallButtonStyle = customBtn, videoCallButtonStyle = customBtn, marginBetweenButtons = 55.dp)
                CometChatCallButtons(style = custom)
            }
        }
        rule.waitForIdle()

        // CallButtonStyle: all 10 scalar styling fields differ
        val btnUnchanged = fieldsOf(CallButtonStyle::class.java).filter { it.get(baseBtn) == it.get(customBtn) }
        assertEquals("every CallButtonStyle field overridden; missed=${btnUnchanged.map { it.name }}", emptyList<String>(), btnUnchanged.map { it.name })
        assertEquals("expected 10 CallButtonStyle fields", 10, fieldsOf(CallButtonStyle::class.java).size)

        // Outer CometChatCallButtonsStyle: both nested styles + marginBetweenButtons differ
        val outerUnchanged = fieldsOf(CometChatCallButtonsStyle::class.java).filter { it.get(base) == it.get(custom) }
        assertEquals("every outer style field overridden; missed=${outerUnchanged.map { it.name }}", emptyList<String>(), outerUnchanged.map { it.name })
        assertEquals("expected 3 outer style fields", 3, fieldsOf(CometChatCallButtonsStyle::class.java).size)

        rule.onRoot().assertExists()
    }
}
