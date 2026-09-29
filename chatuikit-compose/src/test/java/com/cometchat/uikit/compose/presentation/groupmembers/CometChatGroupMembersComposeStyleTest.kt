package com.cometchat.uikit.compose.presentation.groupmembers

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.models.Group
import com.cometchat.uikit.compose.presentation.groupmembers.style.CometChatGroupMembersStyle
import com.cometchat.uikit.compose.presentation.groupmembers.ui.CometChatGroupMembers
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38679 — Compose CometChatGroupMembersStyle matrix (.copy() + reflection
 * completeness guard). 49 scalar fields overridden; 5 nested component-styles waived.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatGroupMembersComposeStyleTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val nestedStyleFields = setOf(
        "avatarStyle", "statusIndicatorStyle", "emptyStateStyle", "errorStateStyle", "loadingStateStyle",
    )

    private fun group() = mock<Group>().also { whenever(it.guid).thenReturn("pm-guid") }

    @Test
    fun everyScalarStyleField_isOverridable_andRestyledComponentRenders() {
        lateinit var base: CometChatGroupMembersStyle
        lateinit var custom: CometChatGroupMembersStyle
        rule.setContent {
            CometChatTheme {
                base = CometChatGroupMembersStyle.default()
                val c = Color(0xFFAB12CD); val d = 77.dp; val ts = TextStyle(fontSize = 33.sp); val p = ColorPainter(Color(0xFF00C0FF))
                custom = base.copy(
                    backgroundColor = c, strokeColor = c, titleTextColor = c, backIconTint = c,
                    searchBackgroundColor = c, searchTextColor = c, searchPlaceholderColor = c, searchStartIconTint = c, searchEndIconTint = c, searchStrokeColor = c,
                    itemBackgroundColor = c, itemSelectedBackgroundColor = c, itemTitleTextColor = c, itemScopeTextColor = c,
                    scopeChipOwnerBackgroundColor = c, scopeChipOwnerTextColor = c, scopeChipBackgroundColor = c, scopeChipTextColor = c, scopeChipStrokeColor = c,
                    separatorColor = c, discardSelectionIconTint = c, submitSelectionIconTint = c, selectionCountTextColor = c,
                    checkBoxStrokeColor = c, checkBoxBackgroundColor = c, checkBoxCheckedBackgroundColor = c, checkBoxSelectIconTint = c,
                    cornerRadius = d, strokeWidth = d, searchCornerRadius = d, searchStrokeWidth = d,
                    scopeChipStrokeWidth = d, scopeChipCornerRadius = d, scopeChipPaddingHorizontal = d, scopeChipPaddingVertical = d,
                    separatorHeight = d, checkBoxStrokeWidth = d, checkBoxCornerRadius = d,
                    titleTextStyle = ts, searchTextStyle = ts, itemTitleTextStyle = ts, itemScopeTextStyle = ts, selectionCountTextStyle = ts,
                    backIcon = p, searchStartIcon = p, searchEndIcon = p, discardSelectionIcon = p, submitSelectionIcon = p, checkBoxSelectIcon = p,
                )
                CometChatGroupMembers(group = group(), viewModel = GroupMembersTestVm.build(), style = custom)
            }
        }
        rule.waitForIdle()

        val scalarFields = CometChatGroupMembersStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name !in nestedStyleFields
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals("every scalar style field must be overridden; missed=${unchanged.map { it.name }}", emptyList<String>(), unchanged.map { it.name })
        assertEquals("expected 49 scalar style fields", 49, scalarFields.size)
        rule.onRoot().assertExists()
    }
}
