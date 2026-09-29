package com.cometchat.uikit.compose.presentation.createpoll

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.createpoll.style.CometChatCreatePollStyle
import com.cometchat.uikit.compose.presentation.createpoll.ui.CometChatCreatePoll
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Property (prop-matrix) layer for [CometChatCreatePoll] — the Compose half of the
 * satellite matrices, mirroring the View `CometChatCreatePollPropMatrixTest`.
 *
 * Eight integrator params, `modifier` excluded by [Denominator], leaving seven, and
 * nothing waived.
 *
 * The composer owns its question and option text internally — there is no way to hand it
 * a filled-in poll from outside — so `onSubmitClick` cannot be reached by tapping alone:
 * the button stays disabled until a question and both options are non-blank. That entry
 * therefore types into the three fields first, which is also the only place in this file
 * where a prop's exercise depends on the component's internal state rather than on its
 * own value.
 *
 * `isSubmitting` is read off the submit button's content description rather than its
 * enabled flag. The button is disabled both while submitting and while the form is
 * incomplete, and those two are different announcements ("Creating poll, please wait" vs
 * "Send poll button disabled") — asserting on enabled-ness alone would pass for the wrong
 * reason on an empty form.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatCreatePollComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatCreatePoll"

        const val DEFAULT_TITLE = "Poll"
        const val CUSTOM_TITLE = "Ask the room"
        /**
         * The back affordance announces the longer form: the `Icon`'s own "Go back" is
         * overridden by the `semantics {}` block on the same node, which is what an
         * assistive technology actually reads out.
         */
        const val BACK = "Go back from create poll"
        const val QUESTION_INPUT = "Poll question input"
        const val SEND = "Send poll"
        const val SEND_DISABLED = "Send poll button disabled"
        const val SENDING = "Creating poll, please wait"
        const val ERROR = "That poll needs two answers"

        const val QUESTION = "Lunch?"
        const val OPTION_ONE = "Ramen"
        const val OPTION_TWO = "Tacos"

        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
        val MAGENTA = Color(0xFFFF00FF)
    }

    /** See the bubble matrices: `captureToImage()` has no window here. */
    private fun paintedColours(): Set<Int> {
        val view = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        view.draw(Canvas(bitmap))
        val seen = HashSet<Int>()
        for (y in 0 until bitmap.height step 2) {
            for (x in 0 until bitmap.width step 2) seen += bitmap.getPixel(x, y)
        }
        return seen
    }

    private fun countByDescription(description: String) =
        composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().size

    private fun fillTheForm() {
        composeRule.onNodeWithContentDescription(QUESTION_INPUT).performTextInput(QUESTION)
        composeRule.onNodeWithContentDescription("Poll option 1").performTextInput(OPTION_ONE)
        composeRule.onNodeWithContentDescription("Poll option 2").performTextInput(OPTION_TWO)
        composeRule.waitForIdle()
    }

    @Test
    fun createPoll_propMatrix_coversEveryObservableProp() {
        var styleOverride by mutableStateOf<CometChatCreatePollStyle?>(null)
        var title by mutableStateOf<String?>(null)
        var hideToolbar by mutableStateOf(false)
        var isSubmitting by mutableStateOf(false)
        var errorMessage by mutableStateOf<String?>(null)

        var submittedQuestion: String? = null
        var submittedOptions: Int? = null
        var backPressed = false

        // `default()` is @Composable — it reads CometChatTheme — so it can only be
        // evaluated inside the composition. Captured for the style entry to copy() from.
        var defaultStyle: CometChatCreatePollStyle? = null

        composeRule.setContent {
            CometChatTheme {
                defaultStyle = CometChatCreatePollStyle.default()
                CometChatCreatePoll(
                    style = styleOverride ?: defaultStyle!!,
                    title = title,
                    hideToolbar = hideToolbar,
                    isSubmitting = isSubmitting,
                    errorMessage = errorMessage,
                    onSubmitClick = { q, opts ->
                        submittedQuestion = q
                        submittedOptions = opts.length()
                    },
                    onBackPress = { backPressed = true },
                )
            }
        }
        composeRule.waitForIdle()

        val matrix = composePropMatrix(OWNER) {
            value("style") {
                styleOverride = defaultStyle!!.copy(backgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the supplied backgroundColor should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                styleOverride = null
                composeRule.waitForIdle()
            }

            value("title") {
                composeRule.onNodeWithText(DEFAULT_TITLE).assertIsDisplayed()
                title = CUSTOM_TITLE
                composeRule.waitForIdle()
                composeRule.onNodeWithText(CUSTOM_TITLE).assertIsDisplayed()
                assertEquals(
                    "the custom title should replace the default, not sit beside it",
                    0,
                    composeRule.onAllNodesWithText(DEFAULT_TITLE).fetchSemanticsNodes().size,
                )
                title = null
                composeRule.waitForIdle()
            }

            value("hideToolbar") {
                assertEquals("the toolbar is shown by default", 1, countByDescription(BACK))
                hideToolbar = true
                composeRule.waitForIdle()
                assertEquals(
                    "hiding the toolbar takes the back affordance with it",
                    0,
                    countByDescription(BACK),
                )
                hideToolbar = false
                composeRule.waitForIdle()
            }

            value("isSubmitting") {
                // Announced differently from the merely-incomplete form, which is the
                // distinction worth pinning — both render as a disabled button.
                assertEquals(1, countByDescription(SEND_DISABLED))
                isSubmitting = true
                composeRule.waitForIdle()
                assertEquals(
                    "a submitting poll should announce itself as in flight",
                    1,
                    countByDescription(SENDING),
                )
                isSubmitting = false
                composeRule.waitForIdle()
            }

            value("errorMessage") {
                assertEquals(0, composeRule.onAllNodesWithText(ERROR).fetchSemanticsNodes().size)
                errorMessage = ERROR
                composeRule.waitForIdle()
                composeRule.onNodeWithText(ERROR).assertIsDisplayed()
                errorMessage = null
                composeRule.waitForIdle()
                assertEquals(
                    "clearing the error should remove it again",
                    0,
                    composeRule.onAllNodesWithText(ERROR).fetchSemanticsNodes().size,
                )
            }

            callback("onSubmitClick") {
                // The button is gated on the composer's own text state, so the form has to
                // be filled before the callback is reachable at all.
                fillTheForm()
                composeRule.onNodeWithContentDescription(SEND).performClick()
                composeRule.waitForIdle()

                assertEquals("the trimmed question is handed back", QUESTION, submittedQuestion)
                assertEquals("both filled options travel with it", 2, submittedOptions)
            }

            callback("onBackPress") {
                composeRule.onNodeWithContentDescription(BACK).performClick()
                composeRule.waitForIdle()
                assertTrue("the back affordance should reach the integrator", backPressed)
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [create poll prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [create poll] NOT covered: $uncovered")

        assertNotNull(defaultStyle)
        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("the poll composer has nothing worth waiving", 0, cov.waived)
    }
}
