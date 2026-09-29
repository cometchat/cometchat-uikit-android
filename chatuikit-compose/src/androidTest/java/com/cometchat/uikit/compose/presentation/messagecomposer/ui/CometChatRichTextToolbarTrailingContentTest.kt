package com.cometchat.uikit.compose.presentation.messagecomposer.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Instrumented tests for the trailing custom-content slot on [CometChatRichTextToolbar].
 *
 * Verifies:
 * - Content passed via `trailingToolbarContent` is rendered.
 * - When the slot is absent, no trailing content is present (guard keeps the zero-content path
 *   identical to the built-in toolbar).
 *
 * Run with:
 *   ./gradlew :chatuikit-compose:connectedDebugAndroidTest \
 *     --tests "com.cometchat.uikit.compose.presentation.messagecomposer.ui.CometChatRichTextToolbarTrailingContentTest"
 */
@RunWith(AndroidJUnit4::class)
class CometChatRichTextToolbarTrailingContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun trailingContent_isRendered_whenProvided() {
        composeTestRule.setContent {
            CometChatTheme {
                CometChatRichTextToolbar(
                    trailingToolbarContent = { Text("TRAILING_MARKER") }
                )
            }
        }

        // The toolbar is a Row with .horizontalScroll(), and the trailing slot renders last —
        // after the divider and all ten FormatButtons, since enabledFormats defaults to every
        // RichTextFormat. That is wider than the emulator viewport, so the marker is composed
        // and in the semantics tree but sits off the right edge, and assertIsDisplayed() asks
        // for visibility rather than existence. Scroll to it first: that still proves the slot
        // renders real, reachable content, where assertExists() alone would not.
        composeTestRule.onNodeWithText("TRAILING_MARKER").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun trailingContent_isAbsent_whenNotProvided() {
        composeTestRule.setContent {
            CometChatTheme {
                CometChatRichTextToolbar()
            }
        }

        composeTestRule.onNodeWithText("TRAILING_MARKER").assertDoesNotExist()
    }
}
