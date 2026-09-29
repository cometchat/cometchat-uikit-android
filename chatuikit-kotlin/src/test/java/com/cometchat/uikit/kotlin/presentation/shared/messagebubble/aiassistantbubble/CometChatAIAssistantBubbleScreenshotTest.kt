package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.aiassistantbubble

import android.content.res.Configuration
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.AIAssistantMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.utils.RoborazziConfig
import com.cometchat.uikit.kotlin.theme.CometChatTheme
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.android.material.card.MaterialCardView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Snapshot layer for the **View** [CometChatAIAssistantBubble].
 *
 * Captured inside [bubbleChrome], the View suite's standard wrapper, which fills a
 * MaterialCardView with the colours `CometChatMessageBubbleStyle.outgoing()` and
 * `incoming()` resolve to — so both bubble fills are pinned here regardless of the
 * container's own agentic transparency rule.
 *
 * Markdown goes through Markwon into a `TextView`, which no assertion in the
 * functional layer can compare visually — this is the layer that pins how it looks.
 *
 * There is deliberately no empty-body capture: with no text the View bubble measures
 * to zero and the chrome wraps to nothing, producing a blank image that would pass
 * forever while checking nothing. The empty case is covered in the functional layer
 * instead, which asserts it composes rather than crashing.
 *
 * Run:
 *   ./gradlew :chatuikit-kotlin:recordRoborazziDebug  --tests "*.CometChatAIAssistantBubbleScreenshotTest"
 *   ./gradlew :chatuikit-kotlin:compareRoborazziDebug --tests "*.CometChatAIAssistantBubbleScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAIAssistantBubbleScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/aiassistantbubble"
        )
    )

    private companion object {
        const val CANVAS_LIGHT = 0xFFEEEEEE.toInt()
        const val CANVAS_DARK = 0xFF121212.toInt()
        const val FIXED_SENT_AT = 1_729_011_360L
        const val PLAIN = "Here is a straightforward answer."
        const val CODE = "Try this:\n```\nval x = 1\n```\nthat should work."
        const val BULLETS = "Options:\n- first\n- second\n- third"
        const val QUOTE = "> a quoted line\n\nand the reply"

        /** Deterministic label for [FIXED_SENT_AT] — pinned to GMT so it never shifts with the host TZ. */
        val TIME_LABEL: String = SimpleDateFormat("h:mm a", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("GMT") }
            .format(Date(FIXED_SENT_AT * 1000))
    }

    private fun assistantMessage(body: String) =
        AIAssistantMessage("receiver-1", "text", body).apply {
            id = 1L
            sentAt = FIXED_SENT_AT
        }

    private fun resolveThemeColor(activity: ComponentActivity, attr: Int): Int {
        val value = TypedValue()
        return if (activity.theme.resolveAttribute(attr, value, true)) value.data else Color.WHITE
    }

    private fun bubbleChrome(activity: ComponentActivity, outgoing: Boolean, content: View): View {
        // Exactly what CometChatMessageBubbleStyle.outgoing()/incoming() use as their
        // default bubble background: primary vs neutral300.
        val bubbleColor =
            if (outgoing) CometChatTheme.getPrimaryColor(activity)
            else CometChatTheme.getNeutralColor300(activity)
        val density = activity.resources.displayMetrics.density
        val timeView = TextView(activity).apply {
            text = TIME_LABEL
            textSize = 11f
            gravity = Gravity.END
            setTextColor(
                if (outgoing) 0xE6FFFFFF.toInt()
                else resolveThemeColor(activity, R.attr.cometchatTextColorTertiary)
            )
            setPadding((8 * density).toInt(), (2 * density).toInt(), (8 * density).toInt(), (6 * density).toInt())
        }
        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                content,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                timeView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val card = MaterialCardView(activity).apply {
            radius = 16 * resources.displayMetrics.density
            cardElevation = 0f
            strokeWidth = 0
            setCardBackgroundColor(bubbleColor)
            addView(
                column,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        return FrameLayout(activity).apply {
            addView(
                card,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    if (outgoing) Gravity.END else Gravity.START
                )
            )
        }
    }

    private fun capture(
        outgoing: Boolean = false,
        rtl: Boolean = false,
        configure: (ComponentActivity) -> View,
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = bubbleChrome(activity, outgoing, configure(activity))

            val isNight = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val container = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(if (isNight) CANVAS_DARK else CANVAS_LIGHT)
                val pad = (16 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
                addView(
                    bubble,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }
            activity.setContentView(container)
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) container.layoutDirection = View.LAYOUT_DIRECTION_RTL
            ShadowLooper.idleMainLooper()

            // Idle first, then lay out: the activity runs its own traversal off the
            // looper and sizes the container to the window (1200x2400 at these
            // qualifiers). Idling after the manual layout is a race, and the captured
            // canvas flips size between runs. Our layout has to be the last thing
            // before the capture.
            ShadowLooper.idleMainLooper()
            val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(2160, View.MeasureSpec.EXACTLY)
            container.measure(widthSpec, heightSpec)
            container.layout(0, 0, 1080, 2160)

            container.captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    private fun bubble(body: String): (ComponentActivity) -> View = { activity ->
        CometChatAIAssistantBubble(activity).apply {
            setAIStreamService(null)
            setMessage(assistantMessage(body))
        }
    }

    @Test fun plainAnswer_incoming() = capture { bubble(PLAIN)(it) }

    @Test fun plainAnswer_outgoing() = capture(outgoing = true) { bubble(PLAIN)(it) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun plainAnswer_incoming_dark() = capture { bubble(PLAIN)(it) }

    @Test fun inlineEmphasis_incoming() = capture { bubble("a **bold** and _italic_ answer")(it) }

    @Test fun codeBlock_incoming() = capture { bubble(CODE)(it) }

    @Test fun codeBlock_outgoing() = capture(outgoing = true) { bubble(CODE)(it) }

    @Test fun bulletList_incoming() = capture { bubble(BULLETS)(it) }

    @Test fun blockquote_incoming() = capture { bubble(QUOTE)(it) }

    @Test fun longAnswer_wraps() =
        capture { bubble("A noticeably longer assistant answer that has to wrap across several lines inside the bubble.")(it) }

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test fun plainAnswer_incoming_rtl() = capture(rtl = true) { bubble(PLAIN)(it) }

    @Test fun plainAnswer_outgoing_rtl() = capture(outgoing = true, rtl = true) { bubble(PLAIN)(it) }
}
