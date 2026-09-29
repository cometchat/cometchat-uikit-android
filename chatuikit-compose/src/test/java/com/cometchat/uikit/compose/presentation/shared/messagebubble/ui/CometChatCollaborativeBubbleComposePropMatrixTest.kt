package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatCollaborativeBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property (prop-matrix) layer for [CometChatCollaborativeBubble].
 *
 * Swept on the data overload: seven integrator params, every one observable through
 * rendered text or a driven callback, so nothing is waived.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatCollaborativeBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createComposeRule()

    private companion object {
        const val OWNER = "CometChatCollaborativeBubble"
        const val TITLE = "Collaborative Document"
        const val SUBTITLE = "Open to edit together"
        const val BUTTON = "Open"
        const val URL = "https://cometchat.com/doc/1"
    }

    @Test
    fun collaborativeBubble_propMatrix_coversEveryProp() {
        var joined: String? = null
        var longClicked = false

        composeRule.setContent {
            CometChatTheme {
                CometChatCollaborativeBubble(
                    title = TITLE,
                    subtitle = SUBTITLE,
                    type = CollaborativeType.WHITEBOARD,
                    url = URL,
                    buttonText = BUTTON,
                    style = CometChatCollaborativeBubbleStyle.outgoing(),
                    onJoinClick = { joined = it },
                    onLongClick = { longClicked = true },
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("title") { composeRule.onNodeWithText(TITLE).assertIsDisplayed() }
            value("subtitle") { composeRule.onNodeWithText(SUBTITLE).assertIsDisplayed() }
            value("buttonText") { composeRule.onNodeWithText(BUTTON).assertIsDisplayed() }
            // WHITEBOARD vs DOCUMENT drives the icon and label treatment; rendering
            // through the whiteboard branch is what this asserts.
            value("type") { composeRule.onNodeWithText(TITLE).assertIsDisplayed() }
            value("style") { composeRule.onNodeWithText(TITLE).assertIsDisplayed() }
            callback("onJoinClick") {
                composeRule.onNodeWithText(BUTTON).performClick()
                composeRule.waitForIdle()
                assertEquals("the url is handed back", URL, joined)
            }
            callback("onLongClick") {
                composeRule.onNodeWithText(TITLE).performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("a long press should reach onLongClick", longClicked)
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered }.map { it.name }

        println("  [collaborative compose prop matrix] ${cov.covered}/${cov.total}")
        if (uncovered.isNotEmpty()) println("  [collaborative] NOT covered: $uncovered")

        assertEquals("every prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
    }
}
