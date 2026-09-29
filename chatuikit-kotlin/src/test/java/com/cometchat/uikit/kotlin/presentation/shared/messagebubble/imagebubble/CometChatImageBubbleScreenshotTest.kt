package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.imagebubble

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
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.testutils.MockFactory
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
 * Snapshot layer for the **View** [CometChatImageBubble].
 *
 * Grid geometry — the 1/2/3/4 layouts, the four-tile cap and the "+N" overflow —
 * is pure layout that only a baseline can hold. As with the text bubble the fill
 * comes from [bubbleChrome], filled with the colours
 * `CometChatMessageBubbleStyle.outgoing()/incoming()` resolve to, so incoming and
 * outgoing are pinned rather than absent.
 *
 * Remote images have no network under Robolectric and render as Glide's empty
 * state — deterministic, which is what makes the geometry comparable.
 *
 * The timestamp is a fixed GMT label, so the baseline cannot encode the recorder's
 * time zone.
 *
 * Run:
 *   ./gradlew :chatuikit-kotlin:recordRoborazziDebug  --tests "*.CometChatImageBubbleScreenshotTest"
 *   ./gradlew :chatuikit-kotlin:compareRoborazziDebug --tests "*.CometChatImageBubbleScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatImageBubbleScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/imagebubble"
        )
    )

    private companion object {
        const val CANVAS_LIGHT = 0xFFEEEEEE.toInt()
        const val CANVAS_DARK = 0xFF121212.toInt()
        const val FIXED_SENT_AT = MockFactory.FIXED_SENT_AT

        /** Deterministic label for [FIXED_SENT_AT] — pinned to GMT so it never shifts with the host TZ. */
        val TIME_LABEL: String = SimpleDateFormat("h:mm a", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("GMT") }
            .format(Date(FIXED_SENT_AT * 1000))
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

    private fun bubble(
        count: Int,
        outgoing: Boolean,
        caption: String? = null,
    ): (ComponentActivity) -> View = { activity ->
        CometChatImageBubble(activity).apply {
            if (count == 1) setMessage(MockFactory.createMediaMessage(count = 1))
            else setAttachments((1..count).map { MockFactory.createAttachment(it) })
            caption?.let { setCaption(it) }
        }
    }

    // ── single image ────────────────────────────────────────────────────────

    @Test
    fun oneImage_incoming() = capture { bubble(1, outgoing = false)(it) }

    @Test
    fun oneImage_outgoing() = capture(outgoing = true) { bubble(1, outgoing = true)(it) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun oneImage_incoming_dark() = capture { bubble(1, outgoing = false)(it) }

    // ── grid layouts ────────────────────────────────────────────────────────

    @Test
    fun twoImages_incoming() = capture { bubble(2, outgoing = false)(it) }

    @Test
    fun twoImages_outgoing() = capture(outgoing = true) { bubble(2, outgoing = true)(it) }

    @Test
    fun threeImages_incoming() = capture { bubble(3, outgoing = false)(it) }

    @Test
    fun fourImages_incoming_fillsWithoutOverflow() = capture { bubble(4, outgoing = false)(it) }

    @Test
    fun fourImages_outgoing_fillsWithoutOverflow() =
        capture(outgoing = true) { bubble(4, outgoing = true)(it) }

    @Test
    fun sevenImages_incoming_capsAtFourWithOverflowBadge() = capture { bubble(7, outgoing = false)(it) }

    @Test
    fun sevenImages_outgoing_capsAtFourWithOverflowBadge() =
        capture(outgoing = true) { bubble(7, outgoing = true)(it) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun fourImages_outgoing_dark() = capture(outgoing = true) { bubble(4, outgoing = true)(it) }

    // ── caption ─────────────────────────────────────────────────────────────

    @Test
    fun withCaption_incoming() =
        capture { bubble(3, outgoing = false, caption = "three from the trip")(it) }

    @Test
    fun withCaption_outgoing() =
        capture(outgoing = true) { bubble(3, outgoing = true, caption = "three from the trip")(it) }

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test fun oneImage_incoming_rtl() = capture(rtl = true) { bubble(1, outgoing = false)(it) }

    @Test fun fourImages_incoming_rtl() = capture(rtl = true) { bubble(4, outgoing = false)(it) }
}
