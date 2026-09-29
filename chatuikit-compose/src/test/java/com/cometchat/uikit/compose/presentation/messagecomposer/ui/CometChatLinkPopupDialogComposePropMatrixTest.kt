package com.cometchat.uikit.compose.presentation.messagecomposer.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.presentation.messagecomposer.style.CometChatMessageComposerStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/**
 * Property (prop-matrix) layer for [CometChatLinkPopupDialog] — what the composer shows when
 * the caret is already inside a link. It had no test of any kind.
 *
 * Five params and no `modifier`, so [Denominator] excludes nothing and the denominator is
 * five. Nothing is waived.
 *
 * `url` is asserted twice over, because the dialog does two different things with it: it
 * shows it, and it opens it. The second is only observable by handing the composition its own
 * [UriHandler] — the real one would launch a browser intent — which is also the only place
 * that "tapping the url opens *that* url" can be checked at all.
 *
 * `onDismiss` is reached through the platform dialog rather than through any affordance of
 * this component: unlike the edit dialog there is no close button, so back and outside-tap are
 * the only ways out, and a back press on the shown dialog is what the test performs.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatLinkPopupDialogComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatLinkPopupDialog"
        const val URL = "https://example.invalid/release-notes"
    }

    /** Records instead of launching a browser. */
    private class RecordingUriHandler : UriHandler {
        val opened = mutableListOf<String>()
        override fun openUri(uri: String) {
            opened += uri
        }
    }

    @Before
    fun stopTheClock() {
        composeRule.mainClock.autoAdvance = false
    }

    /**
     * Drives the frozen clock far enough for the dialog to compose, lay out and finish its
     * entry. The clock is frozen because a text field inside a dialog blinks its caret for as
     * long as it is focused, and an indefinite animation means the test clock is never idle —
     * left on auto-advance, every wait here times out after a minute rather than failing.
     */
    private fun settle() {
        repeat(3) {
            composeRule.mainClock.advanceTimeBy(300L)
            composeRule.waitForIdle()
        }
    }

    private fun string(id: Int): String = composeRule.activity.getString(id)

    private fun heightOf(text: String): Dp = composeRule.onNodeWithText(text)
        .getUnclippedBoundsInRoot()
        .let { it.bottom - it.top }

    private fun tapText(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    @Test
    fun linkPopupDialog_propMatrix_coversEveryProp() {
        val uriHandler = RecordingUriHandler()
        var url by mutableStateOf(URL)
        var styleOverride by mutableStateOf<CometChatMessageComposerStyle?>(null)

        var edited = 0
        var removed = 0
        var dismissed = 0

        var defaultStyle: CometChatMessageComposerStyle? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                CometChatTheme {
                    defaultStyle = CometChatMessageComposerStyle.default()
                    CometChatLinkPopupDialog(
                        url = url,
                        style = styleOverride ?: defaultStyle!!,
                        onEdit = { edited++ },
                        onRemove = { removed++ },
                        onDismiss = { dismissed++ },
                    )
                }
            }
        }
        settle()

        val matrix = composePropMatrix(OWNER) {
            value("url") {
                composeRule.onNodeWithContentDescription(string(R.string.cometchat_a11y_link_popup_dialog))
                    .assertIsDisplayed()
                composeRule.onNodeWithText(URL).assertIsDisplayed()

                // The url is not only shown, it is the thing a tap opens.
                tapText(URL)
                assertEquals("tapping the url should open that url", listOf(URL), uriHandler.opened)

                url = "https://example.invalid/changed"
                settle()
                composeRule.onNodeWithText("https://example.invalid/changed").assertIsDisplayed()
                url = URL
                settle()
            }

            value("style") {
                // The dialog has its own window, which the decor-view bitmap does not include,
                // so this asserts a measurable consequence rather than pixels.
                val title = string(R.string.cometchat_link)
                val plain = heightOf(title)
                styleOverride = defaultStyle!!.copy(
                    linkDialogTitleTextStyle = defaultStyle!!.linkDialogTitleTextStyle
                        .copy(fontSize = 40.sp),
                )
                settle()
                assertTrue(
                    "the supplied title style should reach the dialog: ${heightOf(title)} > $plain",
                    heightOf(title) > plain,
                )
                styleOverride = null
                settle()
            }

            callback("onEdit") {
                tapText(string(R.string.cometchat_edit))
                assertEquals("edit should hand back to the host", 1, edited)
            }

            callback("onRemove") {
                tapText(string(R.string.cometchat_remove))
                assertEquals("remove should hand back to the host", 1, removed)
            }

            callback("onDismiss") {
                // No close button here — back is the affordance, so back is what is pressed.
                val dialog = ShadowDialog.getLatestDialog()
                    ?: error("the popup should be showing in a dialog window")
                dialog.onBackPressed()
                settle()
                assertEquals("a back press should reach the host's dismiss", 1, dismissed)
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [link popup dialog prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [link popup dialog] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("the whole parameter list", 5, cov.total)
        assertEquals("the dialog has nothing worth waiving", 0, cov.waived)
    }
}
