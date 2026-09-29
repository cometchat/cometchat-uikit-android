package com.cometchat.uikit.compose.screenshots

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.compose.presentation.utils.RoborazziConfig
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.cometchat.uikit.core.constants.SearchFilter
import com.cometchat.uikit.core.factory.CometChatSearchViewModelFactory
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * ENG-38678 — CometChatSearch themed golden, Phase 3 Slice 3b (snapshot layer).
 *
 * LAYER: snapshot / Roborazzi. Captures the real CometChatSearch surface (search
 * bar + filter chips + initial state) themed light and dark — the "one themed
 * baseline per major surface" that doubles as the dark-theme check.
 * Goldens: screenshot-gallery/compose/search/.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatSearchScreenshotTest {

    @get:Rule(order = 0)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 1)
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/search"
        )
    )

    private fun newViewModel(): CometChatSearchViewModel =
        CometChatSearchViewModelFactory().create(CometChatSearchViewModel::class.java)

    private fun capture() {
        composeTestRule.onRoot().captureRoboImage(roborazziOptions = RoborazziConfig.options())
    }

    @Test
    fun search_light() {
        composeTestRule.setContent {
            CometChatTheme(colorScheme = lightColorScheme()) {
                CometChatSearch(
                    viewModel = newViewModel(),
                    searchFilters = listOf(SearchFilter.PHOTOS, SearchFilter.VIDEOS),
                )
            }
        }
        capture()
    }

    @Test
    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    fun search_dark() {
        composeTestRule.setContent {
            CometChatTheme(colorScheme = darkColorScheme()) {
                CometChatSearch(
                    viewModel = newViewModel(),
                    searchFilters = listOf(SearchFilter.PHOTOS, SearchFilter.VIDEOS),
                )
            }
        }
        capture()
    }
}
