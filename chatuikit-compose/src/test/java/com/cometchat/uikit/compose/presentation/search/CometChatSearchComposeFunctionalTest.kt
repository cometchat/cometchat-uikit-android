package com.cometchat.uikit.compose.presentation.search

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.SearchFilter
import com.cometchat.uikit.core.factory.CometChatSearchViewModelFactory
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38678 — Compose CometChatSearch functional matrix, Phase 2 Slice 2a.
 *
 * LAYER: unit / Robolectric, asserting via Compose semantics (src/test,
 * testDebugUnitTest — no emulator). The composable exposes no testTags, so these
 * hang off the real accessibility anchors the child composables emit (the "Back"
 * button content-description, filter-chip labels, the initial-state text).
 *
 * Covers VALUE params hideSearchBar / hideFilterChips / searchFilters /
 * hideInitialState and the CALLBACK param onBackPress.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatSearchComposeFunctionalTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun newViewModel(): CometChatSearchViewModel =
        CometChatSearchViewModelFactory().create(CometChatSearchViewModel::class.java)

    private fun render(content: @Composable (CometChatSearchViewModel) -> Unit) {
        rule.setContent { CometChatTheme { content(newViewModel()) } }
        rule.waitForIdle()
    }

    @Test
    fun hideSearchBar_false_showsBackButton() {
        render { vm -> CometChatSearch(viewModel = vm, hideSearchBar = false) }
        rule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun hideSearchBar_true_removesTheSearchBar() {
        render { vm -> CometChatSearch(viewModel = vm, hideSearchBar = true) }
        rule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun searchFilters_rendersOneChipPerFilter() {
        render { vm ->
            CometChatSearch(
                viewModel = vm,
                searchFilters = listOf(SearchFilter.PHOTOS, SearchFilter.VIDEOS),
            )
        }
        rule.onNodeWithText("Photos").assertIsDisplayed()
        rule.onNodeWithText("Videos").assertIsDisplayed()
    }

    @Test
    fun hideFilterChips_true_removesTheChips() {
        render { vm ->
            CometChatSearch(
                viewModel = vm,
                searchFilters = listOf(SearchFilter.PHOTOS, SearchFilter.VIDEOS),
                hideFilterChips = true,
            )
        }
        rule.onNodeWithText("Photos").assertDoesNotExist()
        rule.onNodeWithText("Videos").assertDoesNotExist()
    }

    @Test
    fun hideInitialState_false_showsInitialState() {
        render { vm -> CometChatSearch(viewModel = vm, hideInitialState = false) }
        rule.onNodeWithText("Start Your Search").assertIsDisplayed()
    }

    @Test
    fun hideInitialState_true_removesInitialState() {
        render { vm -> CometChatSearch(viewModel = vm, hideInitialState = true) }
        rule.onNodeWithText("Start Your Search").assertDoesNotExist()
    }

    @Test
    fun onBackPress_firesWhenBackButtonClicked() {
        var fired = false
        render { vm -> CometChatSearch(viewModel = vm, onBackPress = { fired = true }) }
        rule.onNodeWithContentDescription("Back").performClick()
        assertTrue("onBackPress should fire on back-button click", fired)
    }
}
