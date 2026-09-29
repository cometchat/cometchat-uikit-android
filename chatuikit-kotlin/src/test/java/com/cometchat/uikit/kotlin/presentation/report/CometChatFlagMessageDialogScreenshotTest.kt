package com.cometchat.uikit.kotlin.presentation.report

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.FlagReason
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.utils.RoborazziConfig
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * Snapshot layer for the **View** [CometChatFlagMessageDialog].
 *
 * The suite this replaces inflated the dialog's layout straight into the Activity and
 * poked at it by hand, never constructing [CometChatFlagMessageDialog] — so its twelve
 * baselines were pictures of a stand-in. Its header gave the reason: "bypassing Dialog
 * window issues with Robolectric/Roborazzi".
 *
 * Those issues are real but avoidable. A `Dialog` composes into its own window, and
 * Roborazzi's view capture resolves views through Espresso against the *active* root,
 * which under Robolectric stays the Activity — so the dialog's decor is never matched.
 * Drawing that decor into a bitmap sidesteps the lookup entirely, and the path below
 * reproduces the rule's own naming so these sit in the gallery beside every other
 * component's. The same approach is used for the Compose message popup menu.
 *
 * Run:
 *   ./gradlew :chatuikit-kotlin:recordRoborazziDebug  --tests "*.CometChatFlagMessageDialogScreenshotTest"
 *   ./gradlew :chatuikit-kotlin:compareRoborazziDebug --tests "*.CometChatFlagMessageDialogScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatFlagMessageDialogScreenshotTest {

    @get:Rule
    val testName = TestName()

    private fun message(): BaseMessage = MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, text = "the reported message")

    /** The SDK's FlagReason is an opaque model; the kit only reads id and name. */
    private fun reasons(count: Int): List<FlagReason> {
        val ids = listOf("spam", "sexual", "harassment", "violence", "hate", "other")
        val names = listOf("Spam", "Sexual content", "Harassment", "Violence", "Hate speech", "Something else")
        return (0 until count).map { i ->
            mock<FlagReason>().also {
                whenever(it.id).thenReturn(ids[i])
                whenever(it.name).thenReturn(names[i])
            }
        }
    }

    private fun View.visibleText(): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v.visibility != View.VISIBLE) return
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out.toString()
    }

    private fun capture(
        reasonCount: Int = 4,
        configure: (CometChatFlagMessageDialog) -> Unit = {},
        afterShow: (CometChatFlagMessageDialog, View) -> Unit = { _, _ -> },
        rtl: Boolean = false,
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val dialog = CometChatFlagMessageDialog(activity, message())
            // Everything the dialog exposes writes into views that only exist once
            // show() has inflated them — setting reasons or copy beforehand is silently
            // dropped, which is how the real caller does it too: the message list sets
            // them from the SDK callback, after the dialog is up.
            dialog.show()
            ShadowLooper.idleMainLooper()
            dialog.setFlagReasons(reasons(reasonCount))
            configure(dialog)
            ShadowLooper.idleMainLooper()

            val decor = requireNotNull(dialog.window).decorView
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) decor.layoutDirection = View.LAYOUT_DIRECTION_RTL
            afterShow(dialog, decor)
            ShadowLooper.idleMainLooper()

            val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY)
            decor.measure(widthSpec, heightSpec)
            decor.layout(0, 0, 1080, 2160)
            ShadowLooper.idleMainLooper()

            // A screenshot test that photographs an empty frame still passes, so make
            // that impossible: the capture must have a size and carry the dialog's chrome.
            assertTrue("the dialog should have been laid out", decor.width > 0 && decor.height > 0)
            assertTrue(
                "the captured frame should carry the dialog, not an empty window",
                decor.findViewById<View>(R.id.root_card) != null &&
                    decor.visibleText().isNotBlank(),
            )

            val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(bitmap))
            bitmap.captureRoboImage(
                filePath = "../screenshot-gallery/kotlin/flagmessagedialog/" +
                    "${javaClass.name}.${testName.methodName}.png",
                roborazziOptions = RoborazziConfig.options(),
            )
            dialog.dismiss()
        }
        scenario.close()
    }

    /** Taps the first reason chip, which is what enables the report button. */
    private fun selectFirstChip(decor: View) {
        val chips = decor.findViewById<ViewGroup>(R.id.flexbox_chips)
        assertTrue("expected reason chips to bind", chips.childCount > 0)
        chips.getChildAt(0).performClick()
    }

    // ── the resting states ──────────────────────────────────────────────────

    @Test fun initial() = capture()

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun initial_dark() = capture()

    @Test
    fun chipSelected() = capture(afterShow = { _, decor -> selectFirstChip(decor) })

    @Test
    fun withARemark() = capture(afterShow = { _, decor ->
        selectFirstChip(decor)
        decor.findViewById<EditText>(R.id.et_remark).setText("They keep posting the same link")
    })

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun withARemark_dark() = capture(afterShow = { _, decor ->
        selectFirstChip(decor)
        decor.findViewById<EditText>(R.id.et_remark).setText("They keep posting the same link")
    })

    // ── the driven states ───────────────────────────────────────────────────

    @Test
    fun reportInProgress() = capture(afterShow = { dialog, decor ->
        selectFirstChip(decor)
        dialog.hidePositiveButtonProgressBar(false)
    })

    @Test
    fun afterAnError() = capture(afterShow = { dialog, decor ->
        selectFirstChip(decor)
        dialog.onFlagMessageError()
    })

    // ── the configurable surface ────────────────────────────────────────────

    @Test
    fun withoutTheRemarkField() = capture(configure = {
        it.setFlagRemarkInputFieldVisibility(View.GONE)
    })

    @Test
    fun withCustomCopy() = capture(configure = {
        it.setTitle("Report this message")
        it.setDescription("Tell us what is wrong and a moderator will take a look.")
        it.setRemarkHint("Anything else we should know?")
        it.setCancelButtonText("Never mind")
        it.setReportButtonText("Send report")
    })

    @Test fun withASingleReason() = capture(reasonCount = 1)

    @Test fun withSixReasons() = capture(reasonCount = 6)

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test
    fun initial_rtl() = capture(rtl = true)
}
