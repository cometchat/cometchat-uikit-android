package com.cometchat.uikit.kotlin.shared.views.cometchatedittext

import android.graphics.Color
import android.text.Spanned
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.utils.RoborazziConfig
import com.cometchat.uikit.kotlin.shared.formatters.style.PromptTextStyle
import com.cometchat.uikit.kotlin.shared.spans.NonEditableSpan
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

/**
 * Roborazzi baselines for [CometChatEditText] — the composer's text field, and where every
 * mention in the product is typed.
 *
 * [CometChatEditTextFunctionalTest] covers the two rules that make a mention behave like one
 * object (the caret bounces out of it, backspace removes all of it), and it asserts those in
 * indices. What it cannot assert is what a mention *looks like*, which is the other half of
 * the same promise: a mention the user cannot see is one they will try to edit.
 *
 * So these captures are mostly about the span. A [NonEditableSpan] draws nothing on its own —
 * it overrides `updateDrawState` and applies only what its [PromptTextStyle] carries, without
 * calling `super`, so a mention with no style is invisibly a mention. The styled captures pin
 * the rest: the colour, and the background applied at 20% alpha, which the class documents and
 * nothing else can show.
 *
 * The caret is switched off in every capture. A focused `EditText` blinks, and a blinking
 * caret in a baseline is a test that fails on a timer.
 *
 * Run:
 *   ./gradlew :chatuikit-kotlin:recordRoborazziDebug --tests "*.CometChatEditTextScreenshotTest"
 *   ./gradlew :chatuikit-kotlin:verifyRoborazziDebug --tests "*.CometChatEditTextScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h200dp-xxhdpi")
class CometChatEditTextScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/edittext",
        ),
    )

    private companion object {
        const val VIEW_WIDTH = 1200
        const val VIEW_HEIGHT = 400

        const val HINT = "Type your message…"
        const val PLAIN = "Ship it on Friday"
        const val MENTION = "@ironman"
        const val WITH_MENTION = "Hey $MENTION, are we shipping?"
        const val LONG = "Hey $MENTION, are we shipping on Friday or has that slipped to " +
            "the week after, because the release notes still say Friday everywhere"

        val MENTION_START = WITH_MENTION.indexOf(MENTION)
        val MENTION_END = MENTION_START + MENTION.length
    }

    private fun styledMention(): PromptTextStyle = PromptTextStyle()
        .setColor(Color.rgb(60, 120, 240))
        .setBackgroundColor(Color.rgb(60, 120, 240))

    /**
     * Builds the field the way the composer does, lays it out and captures it.
     *
     * [mentionStyle] null renders an unstyled span — deliberately, because that is what the
     * component does when a formatter supplies no appearance.
     */
    private fun capture(
        text: String? = null,
        spanRange: Pair<Int, Int>? = null,
        mentionStyle: PromptTextStyle? = null,
        theme: Int = R.style.CometChatTheme_DayNight,
        rtl: Boolean = false,
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(theme)

            val editText = CometChatEditText(activity).apply {
                hint = HINT
                isCursorVisible = false // see the class doc: a blinking caret is not a baseline
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                if (text != null) setText(text)
            }
            if (spanRange != null) {
                editText.text!!.setSpan(
                    NonEditableSpan('@', MENTION, mentionStyle),
                    spanRange.first,
                    spanRange.second,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }

            val container = FrameLayout(activity).apply {
                setBackgroundColor(Color.WHITE)
                addView(editText)
            }
            activity.setContentView(container)
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) container.layoutDirection = View.LAYOUT_DIRECTION_RTL
            ShadowLooper.idleMainLooper()

            // Idle first, then lay out: the activity runs its own traversal off the
            // looper and sizes the container to the window. Idling after the manual
            // layout is a race, and the captured canvas flips size between runs.
            ShadowLooper.idleMainLooper()
            container.measure(
                View.MeasureSpec.makeMeasureSpec(VIEW_WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(VIEW_HEIGHT, View.MeasureSpec.EXACTLY),
            )
            container.layout(0, 0, VIEW_WIDTH, VIEW_HEIGHT)

            container.captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    @Test
    fun stateEmptyShowsItsHint() {
        capture()
    }

    @Test
    fun stateOrdinaryText() {
        capture(text = PLAIN)
    }

    /** A mention whose formatter supplied no appearance: it reads as ordinary text. */
    @Test
    fun stateMentionWithoutAStyle() {
        capture(text = WITH_MENTION, spanRange = MENTION_START to MENTION_END)
    }

    /** The same mention, styled — colour, and the background at the documented 20% alpha. */
    @Test
    fun stateStyledMention() {
        capture(
            text = WITH_MENTION,
            spanRange = MENTION_START to MENTION_END,
            mentionStyle = styledMention(),
        )
    }

    /** The mention has to survive being wrapped onto another line. */
    @Test
    fun stateStyledMentionInWrappingText() {
        capture(
            text = LONG,
            spanRange = LONG.indexOf(MENTION) to LONG.indexOf(MENTION) + MENTION.length,
            mentionStyle = styledMention(),
        )
    }

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test
    fun stateOrdinaryText_rtl() {
        capture(text = PLAIN, rtl = true)
    }

    @Test
    fun stateStyledMention_rtl() {
        capture(
            text = WITH_MENTION,
            spanRange = MENTION_START to MENTION_END,
            mentionStyle = styledMention(),
            rtl = true,
        )
    }
}
