package com.cometchat.uikit.compose.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.messagecomposer.ui.CometChatLinkEditDialog
import com.cometchat.uikit.compose.presentation.messagecomposer.ui.CometChatLinkPopupDialog
import com.cometchat.uikit.compose.presentation.utils.RoborazziConfig
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/**
 * Roborazzi baselines for the composer's two link dialogs, neither of which had any test.
 *
 * **Why this layer and not a matrix.** The popup dialog does have one
 * ([com.cometchat.uikit.compose.presentation.messagecomposer.ui.CometChatLinkPopupDialogComposePropMatrixTest],
 * 5/5). The edit dialog cannot get one at this layer: it is built from Material3
 * `OutlinedTextField`s, and a Material3 text field in this harness never reports itself idle,
 * so every `waitForIdle` a compose test rule performs — including the one inside `setContent`
 * — times out after a minute instead of failing. It is not the component: a bare
 * `OutlinedTextField` with nothing else in the composition reproduces it. So these captures
 * deliberately use no compose rule at all. `ActivityScenario` sets the content, the looper is
 * idled by hand, and the dialog's own window is drawn — a path with no idling resource in it.
 *
 * The states are chosen to be the dialog's branches rather than a gallery of one screen:
 * add versus edit changes the title, and the second button is Cancel or Remove depending on
 * *two* conditions the dialog folds into one (edit mode **and** a remove handler), so the
 * mode-without-a-handler case is captured too. Save's disabled state is a branch as well —
 * it is gated on the url, so the empty-url capture is the one that shows it greyed.
 *
 * What these cannot pin is the dialog's logic: that a url with no scheme is saved with
 * `https://` in front, and that a blank label falls back to the url. Both live in the Save
 * button's lambda, and reaching them needs a click, which needs the rule that cannot run here.
 * That gap is real and is not covered anywhere else.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug --tests "*.CometChatLinkDialogsScreenshotTest"
 *   ./gradlew :chatuikit-compose:verifyRoborazziDebug --tests "*.CometChatLinkDialogsScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatLinkDialogsScreenshotTest {

    /**
     * Empty on purpose: it registers no composition and so performs no idle wait. The rule is
     * here only because Roborazzi's capture wants a test-scoped lifecycle around it.
     */
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val testName = TestName()

    private companion object {
        const val TEXT = "the release notes"
        const val URL = "https://example.invalid/release-notes"
    }

    /**
     * Sets the content on a real Activity, lets the looper settle, then draws the dialog's own
     * window — the Activity behind it holds nothing.
     */
    private fun capture(content: @Composable () -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent { content() }
            ShadowLooper.idleMainLooper()

            val decor: View? = ShadowDialog.getLatestDialog()?.window?.decorView
            assertNotNull("the dialog should have opened its own window", decor)
            ShadowLooper.idleMainLooper()

            // A capture of an empty frame still passes, so make that impossible.
            assertTrue(
                "the dialog should have been laid out",
                decor!!.width > 0 && decor.height > 0,
            )

            val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(bitmap))
            bitmap.captureRoboImage(
                filePath = "../screenshot-gallery/compose/linkdialogs/" +
                    "${javaClass.name}.${testName.methodName}.png",
                roborazziOptions = RoborazziConfig.options(),
            )
        }
        scenario.close()
    }

    // ==================== add / edit link dialog ====================

    /** Nothing typed yet: Add Link, Cancel, and Save greyed out because there is no url. */
    @Test
    fun addLinkEmpty() = capture {
        CometChatTheme {
            CometChatLinkEditDialog(initialText = "", initialUrl = "")
        }
    }

    /** With a url, Save comes alive. */
    @Test
    fun addLinkFilled() = capture {
        CometChatTheme {
            CometChatLinkEditDialog(initialText = TEXT, initialUrl = URL)
        }
    }

    /** Edit mode with a remove handler: the title changes and Cancel becomes Remove. */
    @Test
    fun editLinkWithRemove() = capture {
        CometChatTheme {
            CometChatLinkEditDialog(
                initialText = TEXT,
                initialUrl = URL,
                isEditMode = true,
                onRemove = {},
            )
        }
    }

    /**
     * Edit mode without one. The dialog folds "editing" and "can remove" into a single branch,
     * so this is the case where the title says edit but the button is still Cancel.
     */
    @Test
    fun editLinkWithoutRemove() = capture {
        CometChatTheme {
            CometChatLinkEditDialog(
                initialText = TEXT,
                initialUrl = URL,
                isEditMode = true,
                onRemove = null,
            )
        }
    }

    @Test
    fun addLinkDark() = capture {
        CometChatTheme(colorScheme = darkColorScheme()) {
            CometChatLinkEditDialog(initialText = TEXT, initialUrl = URL)
        }
    }

    // ==================== link popup dialog ====================

    @Test
    fun linkPopup() = capture {
        CometChatTheme {
            CometChatLinkPopupDialog(url = URL)
        }
    }

    @Test
    fun linkPopupDark() = capture {
        CometChatTheme(colorScheme = darkColorScheme()) {
            CometChatLinkPopupDialog(url = URL)
        }
    }
}
