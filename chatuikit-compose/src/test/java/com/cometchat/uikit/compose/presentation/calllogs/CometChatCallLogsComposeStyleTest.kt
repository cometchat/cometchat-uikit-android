package com.cometchat.uikit.compose.presentation.calllogs

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.calllogs.style.CometChatCallLogsStyle
import com.cometchat.uikit.compose.presentation.calllogs.ui.CometChatCallLogs
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
 * ENG-38680 — Compose CometChatCallLogsStyle matrix (.copy() + reflection
 * completeness guard). 21 scalar fields overridden; 4 nested component-styles waived.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCallLogsComposeStyleTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val nestedStyleFields = setOf("itemStyle", "emptyStateStyle", "errorStateStyle", "loadingStateStyle")

    @Test
    fun everyScalarStyleField_isOverridable_andRestyledComponentRenders() {
        lateinit var base: CometChatCallLogsStyle
        lateinit var custom: CometChatCallLogsStyle
        rule.setContent {
            CometChatTheme {
                base = CometChatCallLogsStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp; val ts = TextStyle(fontSize = 33.sp); val p = ColorPainter(Color(0xFF00C0FF))
                custom = base.copy(
                    backgroundColor = c, strokeColor = c, titleTextColor = c, backIconTint = c, toolbarSeparatorColor = c,
                    emptyStateTitleTextColor = c, emptyStateSubtitleTextColor = c, errorStateTitleTextColor = c, errorStateSubtitleTextColor = c,
                    strokeWidth = d, cornerRadius = d, toolbarSeparatorHeight = d,
                    titleTextStyle = ts, emptyStateTitleTextStyle = ts, emptyStateSubtitleTextStyle = ts, errorStateTitleTextStyle = ts, errorStateSubtitleTextStyle = ts,
                    backIcon = p, emptyStateIcon = p, errorStateIcon = p,
                    showToolbarSeparator = !base.showToolbarSeparator,
                )
                CometChatCallLogs(viewModel = CallLogsTestVm.build(), style = custom)
            }
        }
        rule.waitForIdle()

        val scalarFields = CometChatCallLogsStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name !in nestedStyleFields
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals("every scalar style field must be overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 21 scalar style fields", 21, scalarFields.size)
        rule.onRoot().assertExists()
    }
}
