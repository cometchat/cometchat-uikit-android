package com.cometchat.uikit.compose.presentation.users

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.users.style.CometChatUsersStyle
import com.cometchat.uikit.compose.presentation.users.ui.CometChatUsers
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.viewmodel.CometChatUsersViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * ENG-38679 — Compose CometChatUsersStyle matrix (unit/Robolectric, .copy() +
 * reflection completeness guard). 44 scalar fields overridden; 5 nested
 * component-styles waived.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatUsersComposeStyleTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val nestedStyleFields = setOf(
        "itemStyle", "popupMenuStyle", "emptyStateStyle", "errorStateStyle", "loadingStateStyle",
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    /** VM built with a no-op fake repo + listeners off, so no real SDK fetch throws inside the compose TestScope. */
    private fun vm(): CometChatUsersViewModel = UsersTestVm.build()

    @Test
    fun everyScalarStyleField_isOverridable_andRestyledComponentRenders() {
        lateinit var base: CometChatUsersStyle
        lateinit var custom: CometChatUsersStyle
        rule.setContent {
            CometChatTheme {
                base = CometChatUsersStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp; val ts = TextStyle(fontSize = 33.sp); val p = ColorPainter(Color(0xFF00C0FF))
                custom = base.copy(
                    backgroundColor = c, strokeColor = c, titleTextColor = c, backIconTint = c,
                    toolbarSeparatorColor = c, emptyStateTitleTextColor = c, emptyStateSubtitleTextColor = c,
                    errorStateTitleTextColor = c, errorStateSubtitleTextColor = c, separatorColor = c,
                    searchBackgroundColor = c, searchTextColor = c, searchPlaceholderColor = c,
                    searchStartIconTint = c, searchEndIconTint = c, searchStrokeColor = c,
                    stickyHeaderTextColor = c, stickyHeaderBackgroundColor = c,
                    discardSelectionIconTint = c, submitSelectionIconTint = c, selectionCountTextColor = c,
                    strokeWidth = d, cornerRadius = d, toolbarSeparatorHeight = d, separatorHeight = d,
                    searchCornerRadius = d, searchStrokeWidth = d,
                    titleTextStyle = ts, emptyStateTitleTextStyle = ts, emptyStateSubtitleTextStyle = ts,
                    errorStateTitleTextStyle = ts, errorStateSubtitleTextStyle = ts, searchTextStyle = ts,
                    searchPlaceholderTextStyle = ts, stickyHeaderTextStyle = ts, selectionCountTextStyle = ts,
                    backIcon = p, emptyStateIcon = p, errorStateIcon = p, searchStartIcon = p, searchEndIcon = p,
                    discardSelectionIcon = p, submitSelectionIcon = p,
                    showToolbarSeparator = !base.showToolbarSeparator,
                )
                CometChatUsers(usersViewModel = vm(), style = custom)
            }
        }
        rule.waitForIdle()

        val scalarFields = CometChatUsersStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name !in nestedStyleFields
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals("every scalar style field must be overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 44 scalar style fields", 44, scalarFields.size)
        rule.onRoot().assertExists()
    }
}
