package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for [CometChatCollaborativeBubble].
 *
 * Driven through the data overload (title / subtitle / type / url), which renders
 * without the SDK. The `CustomMessage` overload re-extracts the same fields from
 * message metadata.
 *
 * This bubble renders plain text, so Compose semantics see all of it.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCollaborativeBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        title: String = "Collaborative Document",
        subtitle: String = "Open to edit together",
        type: CollaborativeType = CollaborativeType.DOCUMENT,
        url: String = "https://cometchat.com/doc/1",
        buttonText: String = "Join",
        onJoinClick: ((String) -> Unit)? = null,
    ) {
        composeRule.setContent {
            CometChatTheme {
                CometChatCollaborativeBubble(
                    title = title,
                    subtitle = subtitle,
                    type = type,
                    url = url,
                    buttonText = buttonText,
                    onJoinClick = onJoinClick,
                )
            }
        }
    }

    @Test
    fun rendersTitleSubtitleAndButton() {
        render()

        composeRule.onNodeWithText("Collaborative Document").assertIsDisplayed()
        composeRule.onNodeWithText("Open to edit together").assertIsDisplayed()
        composeRule.onNodeWithText("Join").assertIsDisplayed()
    }

    @Test
    fun tappingJoin_handsBackTheUrl() {
        var joined: String? = null
        render(onJoinClick = { joined = it })

        composeRule.onNodeWithText("Join").performClick()
        composeRule.waitForIdle()

        assertEquals("https://cometchat.com/doc/1", joined)
    }

    @Test
    fun whiteboardType_rendersItsOwnContent() {
        render(title = "Collaborative Whiteboard", type = CollaborativeType.WHITEBOARD)

        composeRule.onNodeWithText("Collaborative Whiteboard").assertIsDisplayed()
    }

    @Test
    fun customButtonText_isUsed() {
        render(buttonText = "Open")

        composeRule.onNodeWithText("Open").assertIsDisplayed()
    }

    @Test
    fun emptyStrings_renderWithoutCrashing() {
        render(title = "", subtitle = "")

        composeRule.onNodeWithText("Join").assertIsDisplayed()
    }
}
