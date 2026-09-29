package com.cometchat.uikit.compose.presentation.search

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.search.style.CometChatSearchStyle
import com.cometchat.uikit.compose.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.factory.CometChatSearchViewModelFactory
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.reflect.Modifier

/**
 * ENG-38678 — Compose CometChatSearchStyle matrix, Phase 2 Slice 2c.
 *
 * LAYER: unit / Robolectric — construction + render + a reflection completeness
 * guard. The style is a value-class data class, so (like the Avatar compose
 * pilot) each scalar field is overridden via compile-time `.copy()`; a reflection
 * READ then proves every scalar field actually differs from the default, and the
 * fully-restyled composable renders. The per-pixel VISUAL effect of each colour /
 * dimension / typography token is verified in the snapshot layer (Roborazzi
 * themed baselines), folded into Phase 3.
 *
 * The 6 nested style objects (conversationItemStyle, messageItemStyle, the four
 * state styles) are waived here — they are matrixed by their own components.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatSearchComposeStyleTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val nestedStyleFields = setOf(
        "conversationItemStyle", "messageItemStyle",
        "emptyStateStyle", "errorStateStyle", "loadingStateStyle", "initialStateStyle",
    )

    @Test
    fun everyScalarStyleField_isOverridable_andRestyledSearchRenders() {
        val vm: CometChatSearchViewModel =
            CometChatSearchViewModelFactory().create(CometChatSearchViewModel::class.java)

        lateinit var base: CometChatSearchStyle
        lateinit var custom: CometChatSearchStyle

        rule.setContent {
            CometChatTheme {
                base = CometChatSearchStyle.default()
                val c = Color(0xFFAB12CD)
                val d = 77.dp
                val ts = TextStyle(fontSize = 33.sp)
                val p = ColorPainter(Color(0xFF00C0FF))
                custom = base.copy(
                    backgroundColor = c,
                    searchBarBackgroundColor = c,
                    searchBarStrokeColor = c,
                    searchBarStrokeWidth = d,
                    searchBarCornerRadius = d,
                    searchBarTextColor = c,
                    searchBarTextStyle = ts,
                    searchBarHintTextColor = c,
                    searchBarHintTextStyle = ts,
                    backIcon = p,
                    backIconTint = c,
                    clearIcon = p,
                    clearIconTint = c,
                    searchIcon = p,
                    searchIconTint = c,
                    filterChipBackgroundColor = c,
                    filterChipSelectedBackgroundColor = c,
                    filterChipTextColor = c,
                    filterChipSelectedTextColor = c,
                    filterChipTextStyle = ts,
                    filterChipStrokeColor = c,
                    filterChipSelectedStrokeColor = c,
                    filterChipStrokeWidth = d,
                    filterChipCornerRadius = d,
                    sectionHeaderTextColor = c,
                    sectionHeaderTextStyle = ts,
                    sectionHeaderBackgroundColor = c,
                    dateSeparatorBackgroundColor = c,
                    dateSeparatorTextColor = c,
                    dateSeparatorTextStyle = ts,
                    seeMoreTextColor = c,
                    seeMoreTextStyle = ts,
                )
                CometChatSearch(viewModel = vm, style = custom)
            }
        }
        rule.waitForIdle()

        // completeness: every scalar (non-nested) field differs from the default.
        val scalarFields = CometChatSearchStyle::class.java.declaredFields.filter {
            !it.isSynthetic && !Modifier.isStatic(it.modifiers) && it.name !in nestedStyleFields
        }
        scalarFields.forEach { it.isAccessible = true }
        val unchanged = scalarFields.filter { it.get(base) == it.get(custom) }
        assertEquals(
            "every scalar style field must be overridden to a non-default; missed=${unchanged.map { it.name }}",
            emptyList<String>(),
            unchanged.map { it.name },
        )
        // sanity: the surface we assert is the 32 scalar tokens
        assertEquals("expected 32 scalar style fields", 32, scalarFields.size)

        // the fully-restyled search still renders
        rule.onNodeWithContentDescription("Back").assertExists()
    }
}
