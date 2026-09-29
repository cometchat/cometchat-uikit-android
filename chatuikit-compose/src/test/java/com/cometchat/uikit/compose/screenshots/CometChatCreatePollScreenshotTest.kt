package com.cometchat.uikit.compose.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.createpoll.ui.CometChatCreatePoll
import com.cometchat.uikit.compose.presentation.utils.RoborazziConfig
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot layer for the Compose [CometChatCreatePoll].
 *
 * The suite this replaces built its own `CreatePollScreen`, `QuestionSection`,
 * `OptionsSection` and `SubmitButton` out of raw Compose primitives with hardcoded
 * hex colours, and never called [CometChatCreatePoll] at all — so its eleven
 * baselines were pictures of a stand-in and could not have caught a regression in the
 * component. These capture the real thing.
 *
 * Two states are reached by typing rather than by parameter, because the composer owns
 * its question and option text internally (`remember { mutableStateOf("") }`); the send
 * button only enables once they are filled, so driving the form is the only way to
 * photograph an enabled one. The rest — submitting, the error line, the toolbar toggle
 * and the title override — are ordinary parameters.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatCreatePollScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatCreatePollScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatCreatePollScreenshotTest {

    @get:Rule(order = 0)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule(order = 1)
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/createpoll"
        )
    )

    private companion object {
        const val QUESTION_FIELD = "Poll question input"
        const val QUESTION = "Where should we go for the team outing?"
        const val OPTION_ONE = "The coast"
        const val OPTION_TWO = "The hills"
        const val OPTION_THREE = "Somewhere with wifi"
    }

    private fun render(
        dark: Boolean = false,
        title: String? = null,
        hideToolbar: Boolean = false,
        isSubmitting: Boolean = false,
        errorMessage: String? = null,
    ) {
        composeRule.setContent {
            CometChatTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                CometChatCreatePoll(
                    title = title,
                    hideToolbar = hideToolbar,
                    isSubmitting = isSubmitting,
                    errorMessage = errorMessage,
                    onSubmitClick = { _, _ -> },
                    onBackPress = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** The composer holds its own text, so a filled form has to be typed into. */
    private fun fillTheForm(thirdOption: Boolean = false) {
        composeRule.onNodeWithContentDescription(QUESTION_FIELD).performTextInput(QUESTION)
        composeRule.onNodeWithContentDescription("Poll option 1").performTextInput(OPTION_ONE)
        composeRule.onNodeWithContentDescription("Poll option 2").performTextInput(OPTION_TWO)
        if (thirdOption) {
            composeRule.onNodeWithText("Add").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithContentDescription("Poll option 3").performTextInput(OPTION_THREE)
        }
        composeRule.waitForIdle()
    }

    private fun capture() {
        val decor = composeRule.activity.window.decorView
        // A screenshot test that photographs an empty frame still passes, so make that
        // impossible: the capture must have a size and the composer must be in it.
        assertTrue("the composer should have been laid out", decor.width > 0 && decor.height > 0)
        assertTrue(
            "the composer itself should be in the tree, not a stand-in",
            composeRule.onAllNodesWithContentDescription(QUESTION_FIELD)
                .fetchSemanticsNodes().isNotEmpty(),
        )
        decor.captureRoboImage(roborazziOptions = RoborazziConfig.options())
    }

    // ── the empty form ──────────────────────────────────────────────────────

    @Test
    fun idle() {
        render()
        capture()
    }

    @Test
    fun idle_dark() {
        render(dark = true)
        capture()
    }

    // ── filled in ───────────────────────────────────────────────────────────

    @Test
    fun filled() {
        render()
        fillTheForm()
        capture()
    }

    @Test
    fun filled_dark() {
        render(dark = true)
        fillTheForm()
        capture()
    }

    /** "Add" appends a row; the composer starts with two and grows from there. */
    @Test
    fun threeOptions() {
        render()
        fillTheForm(thirdOption = true)
        capture()
    }

    // ── the parameterised states ────────────────────────────────────────────

    @Test
    fun submitting() {
        render(isSubmitting = true)
        fillTheForm()
        capture()
    }

    @Test
    fun withAnError() {
        render(errorMessage = "That poll needs at least two answers")
        capture()
    }

    @Test
    fun withoutTheToolbar() {
        render(hideToolbar = true)
        capture()
    }

    @Test
    fun withACustomTitle() {
        render(title = "Team outing")
        capture()
    }
}
