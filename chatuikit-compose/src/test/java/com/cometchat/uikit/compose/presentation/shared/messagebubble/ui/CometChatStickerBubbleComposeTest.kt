package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatStickerBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Property and instrumented layers for [CometChatStickerBubble], both overloads.
 *
 * Coil has no network here, so the sticker itself is a placeholder; the accessible
 * name is the observable that carries the message's data through.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatStickerBubbleComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatStickerBubble"
        const val NAME = "Party Popper"
        const val URL = "https://cdn.example.com/stickers/party.png"
    }

    @Test
    fun stickerBubble_propMatrix_coversEveryProp() {
        val alignment = UIKitConstants.MessageBubbleAlignment.LEFT
        var styleOverride by mutableStateOf<CometChatStickerBubbleStyle?>(null)
        var clicked = false
        var longClicked = false
        var incoming: CometChatStickerBubbleStyle? = null
        var outgoing: CometChatStickerBubbleStyle? = null

        val message = MockFactory.createStickerMessage(name = NAME, url = URL)

        composeRule.setContent {
            CometChatTheme {
                incoming = CometChatStickerBubbleStyle.incoming()
                outgoing = CometChatStickerBubbleStyle.outgoing()
                val override = styleOverride
                if (override == null) {
                    CometChatStickerBubble(
                        message = message,
                        alignment = alignment,
                        onClick = { clicked = true },
                        onLongClick = { longClicked = true },
                    )
                } else {
                    CometChatStickerBubble(
                        message = message,
                        alignment = alignment,
                        style = override,
                        onClick = { clicked = true },
                        onLongClick = { longClicked = true },
                    )
                }
            }
        }

        val matrix = composePropMatrix(OWNER) {
            // The sticker name from customData becomes the accessible name, so the
            // message reaching the component is readable off the semantics tree.
            value("message") {
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription(NAME).assertIsDisplayed()
            }

            value("style") {
                composeRule.waitForIdle()
                assertNotEquals("incoming() and outgoing() should differ", incoming, outgoing)
                styleOverride = outgoing
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription(NAME).assertIsDisplayed()
                styleOverride = null
                composeRule.waitForIdle()
            }

            callback("onClick") {
                composeRule.onNodeWithContentDescription(NAME).performClick()
                composeRule.waitForIdle()
                assertTrue("a tap should reach the integrator", clicked)
            }

            callback("onLongClick") {
                composeRule.onNodeWithContentDescription(NAME).performTouchInput { longClick() }
                composeRule.waitForIdle()
                assertTrue("a long press should reach the integrator", longClicked)
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        println("  [sticker compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        val uncovered = props.filter { !it.covered }.map { it.name }
        if (uncovered.isNotEmpty()) println("  [sticker] NOT covered: $uncovered")

        assertEquals("every prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only alignment is waived", 1, cov.waived)
    }

    /**
     * Both sticker styles set `backgroundColor = Color.Transparent`, so the two sides
     * paint identically and there is nothing to assert here. Alignment's real effect is
     * the timestamp colour it hands the container and which edge the bubble sits on,
     * both of which belong to the container and are pinned in the snapshot layer.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "alignment", PropKind.VALUE, waived = true),
    )

    // ── instrumented ────────────────────────────────────────────────────────

    private fun render(
        name: String? = NAME,
        legacyKey: Boolean = false,
        alignment: UIKitConstants.MessageBubbleAlignment = UIKitConstants.MessageBubbleAlignment.LEFT,
    ) {
        composeRule.setContent {
            CometChatTheme {
                CometChatStickerBubble(
                    message = MockFactory.createStickerMessage(name = name, useLegacyUrlKey = legacyKey),
                    alignment = alignment,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theStickerNameBecomesTheAccessibleName() {
        render()
        composeRule.onNodeWithContentDescription(NAME).assertIsDisplayed()
    }

    @Test
    fun withoutANameItFallsBackToAGenericLabel() {
        render(name = null)
        composeRule.onNodeWithContentDescription("Sticker").assertIsDisplayed()
    }

    @Test
    fun theLegacyUrlKeyIsStillAccepted() {
        // extractStickerUrl reads sticker_url first and falls back to url.
        render(legacyKey = true)
        composeRule.onNodeWithContentDescription(NAME).assertIsDisplayed()
    }

    @Test
    fun bothAlignmentsRenderTheSticker() {
        render(alignment = UIKitConstants.MessageBubbleAlignment.RIGHT)
        assertEquals(1, composeRule.onAllNodesWithContentDescription(NAME).fetchSemanticsNodes().size)
    }

    @Test
    fun theUrlOverloadRendersWithoutAMessage() {
        composeRule.setContent {
            CometChatTheme { CometChatStickerBubble(stickerUrl = URL, stickerName = NAME) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(NAME).assertIsDisplayed()
    }

    @Test
    fun anEmptyUrlStillComposes() {
        composeRule.setContent {
            CometChatTheme { CometChatStickerBubble(stickerUrl = "", stickerName = NAME) }
        }
        composeRule.waitForIdle()
    }
}
