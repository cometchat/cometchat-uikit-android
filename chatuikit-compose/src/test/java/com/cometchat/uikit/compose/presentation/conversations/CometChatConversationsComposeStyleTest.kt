package com.cometchat.uikit.compose.presentation.conversations

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.conversations.style.CometChatConversationsStyle
import com.cometchat.uikit.compose.presentation.conversations.ui.CometChatConversations
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.factory.CometChatConversationsViewModelFactory
import com.cometchat.uikit.core.viewmodel.CometChatConversationsViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38679 — Compose CometChatConversationsStyle matrix.
 *
 * LAYER: unit/Robolectric — construction + render + reflection completeness guard
 * (same technique as the Search compose style, Slice 2c). Value-class data class,
 * so every scalar field is overridden via compile-time .copy(); a reflection read
 * proves all differ from default and the fully-restyled component renders. The 7
 * nested component-style objects are waived (matrixed by their own components).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatConversationsComposeStyleTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val nestedStyleFields = setOf(
        "itemStyle", "popupMenuStyle", "emptyStateStyle", "errorStateStyle",
        "loadingStateStyle", "dialogStyle", "mentionStyle",
    )

    private fun vm(): CometChatConversationsViewModel =
        CometChatConversationsViewModelFactory().create(CometChatConversationsViewModel::class.java)

    @Test
    fun everyScalarStyleField_isOverridable_andRestyledComponentRenders() {
        lateinit var base: CometChatConversationsStyle
        lateinit var custom: CometChatConversationsStyle

        rule.setContent {
            CometChatTheme {
                base = CometChatConversationsStyle.default()
                val c = Color(0xFFAB12CD)
                val d = 77.dp
                val ts = TextStyle(fontSize = 33.sp)
                val p = ColorPainter(Color(0xFF00C0FF))
                custom = base.copy(
                    backgroundColor = c, strokeColor = c, strokeWidth = d, cornerRadius = d,
                    titleTextColor = c, titleTextStyle = ts, backIcon = p, backIconTint = c,
                    toolbarSeparatorColor = c, toolbarSeparatorHeight = d,
                    emptyStateTitleTextColor = c, emptyStateTitleTextStyle = ts,
                    emptyStateSubtitleTextColor = c, emptyStateSubtitleTextStyle = ts, emptyStateIcon = p,
                    errorStateTitleTextColor = c, errorStateTitleTextStyle = ts,
                    errorStateSubtitleTextColor = c, errorStateSubtitleTextStyle = ts, errorStateIcon = p,
                    separatorColor = c, separatorHeight = d,
                    searchBackgroundColor = c, searchTextColor = c, searchTextStyle = ts,
                    searchPlaceholderColor = c, searchPlaceholderTextStyle = ts,
                    searchStartIcon = p, searchStartIconTint = c, searchEndIcon = p, searchEndIconTint = c,
                    searchCornerRadius = d, searchStrokeWidth = d, searchStrokeColor = c,
                    discardSelectionIcon = p, discardSelectionIconTint = c,
                    submitSelectionIcon = p, submitSelectionIconTint = c,
                    selectionCountTextColor = c, selectionCountTextStyle = ts,
                    deleteOptionIcon = p, deleteOptionIconTint = c,
                    deleteOptionTextColor = c, deleteOptionTextStyle = ts,
                    showToolbarSeparator = !base.showToolbarSeparator,
                )
                CometChatConversations(conversationListViewModel = vm(), style = custom)
            }
        }
        rule.waitForIdle()

        val scalarFields = CometChatConversationsStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name !in nestedStyleFields
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals(
            "every scalar style field must be overridden; missed=${unchanged.map { it.name }}",
            emptyList<String>(), unchanged.map { it.name },
        )
        assertEquals("expected 45 scalar style fields", 45, scalarFields.size)

        rule.onRoot().assertExists()
    }
}
