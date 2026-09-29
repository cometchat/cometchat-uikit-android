package com.cometchat.uikit.compose.presentation.search

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.factory.CometChatSearchViewModelFactory
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38678 — Compose CometChatSearch, Phase 2 de-risk.
 *
 * Confirms the screen-level composable renders under Robolectric with an
 * externally-supplied ViewModel (built from the factory's default repository, so
 * no live SDK), before wiring the full DSL param matrix + style + functional
 * assertions.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatSearchComposePropMatrixTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun newViewModel(): CometChatSearchViewModel =
        CometChatSearchViewModelFactory().create(CometChatSearchViewModel::class.java)

    @Test
    fun composeSearch_rendersWithExternalViewModel() {
        rule.setContent {
            CometChatTheme {
                CometChatSearch(viewModel = newViewModel())
            }
        }
        rule.waitForIdle()
        rule.onRoot().assertExists()
    }
}
