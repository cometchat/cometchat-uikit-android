package com.cometchat.uikit.compose.presentation.groups

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.groups.style.CometChatGroupsStyle
import com.cometchat.uikit.compose.presentation.groups.ui.CometChatGroups
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
 * ENG-38679 — Compose CometChatGroupsStyle matrix (.copy() + reflection
 * completeness guard). 49 scalar fields overridden; 6 nested component-styles
 * waived.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatGroupsComposeStyleTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val nestedStyleFields = setOf(
        "itemStyle", "popupMenuStyle", "emptyStateStyle", "errorStateStyle", "loadingStateStyle", "selectedGroupsAvatarStyle",
    )

    @Test
    fun everyScalarStyleField_isOverridable_andRestyledComponentRenders() {
        lateinit var base: CometChatGroupsStyle
        lateinit var custom: CometChatGroupsStyle
        rule.setContent {
            CometChatTheme {
                base = CometChatGroupsStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp; val ts = TextStyle(fontSize = 33.sp); val p = ColorPainter(Color(0xFF00C0FF))
                custom = base.copy(
                    backgroundColor = c, strokeColor = c, titleTextColor = c, backIconTint = c, toolbarSeparatorColor = c,
                    emptyStateTitleTextColor = c, emptyStateSubtitleTextColor = c, errorStateTitleTextColor = c, errorStateSubtitleTextColor = c,
                    retryButtonTextColor = c, retryButtonBackgroundColor = c, retryButtonStrokeColor = c,
                    searchBackgroundColor = c, searchTextColor = c, searchPlaceholderColor = c, searchStartIconTint = c, searchEndIconTint = c, searchStrokeColor = c,
                    discardSelectionIconTint = c, submitSelectionIconTint = c, selectionCountTextColor = c, selectedGroupsItemTextColor = c, selectedGroupsRemoveIconTint = c,
                    strokeWidth = d, cornerRadius = d, toolbarSeparatorHeight = d, retryButtonStrokeWidth = d, retryButtonCornerRadius = d, searchCornerRadius = d, searchStrokeWidth = d,
                    titleTextStyle = ts, emptyStateTitleTextStyle = ts, emptyStateSubtitleTextStyle = ts, errorStateTitleTextStyle = ts, errorStateSubtitleTextStyle = ts,
                    retryButtonTextStyle = ts, searchTextStyle = ts, searchPlaceholderTextStyle = ts, selectionCountTextStyle = ts, selectedGroupsItemTextStyle = ts,
                    backIcon = p, emptyStateIcon = p, errorStateIcon = p, searchStartIcon = p, searchEndIcon = p, discardSelectionIcon = p, submitSelectionIcon = p, selectedGroupsRemoveIcon = p,
                    showToolbarSeparator = !base.showToolbarSeparator,
                )
                CometChatGroups(viewModel = GroupsTestVm.build(), style = custom)
            }
        }
        rule.waitForIdle()

        val scalarFields = CometChatGroupsStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name !in nestedStyleFields
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals("every scalar style field must be overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 49 scalar style fields", 49, scalarFields.size)
        rule.onRoot().assertExists()
    }
}
