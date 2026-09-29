package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.filebubble

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
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.filesbubble.CometChatFilesBubble
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
 * Snapshot layer for the **View** file bubbles.
 *
 * Captured inside [bubbleChrome] so incoming and outgoing fills are both pinned at the
 * same default size as the rest of the gallery. Neither file bubble paints its own
 * background, and the outgoing palette is drawn for the primary fill.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatFileBubbleScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/filebubble"
        )
    )

    private companion object {
        const val CANVAS_LIGHT = 0xFFEEEEEE.toInt()
        const val CANVAS_DARK = 0xFF121212.toInt()
        const val FILE_MIME = "application/pdf"
        const val FILE_EXT = "pdf"

        /** Deterministic label for the fixture's sentAt — pinned to GMT so it never shifts. */
        val TIME_LABEL: String = SimpleDateFormat("h:mm a", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("GMT") }
            .format(Date(MockFactory.FIXED_SENT_AT * 1000))
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
            // Honour a width the bubble set on itself, so it ends up at the width the
            // message list gives it rather than one this harness invented.
            addView(
                content,
                LinearLayout.LayoutParams(
                    content.layoutParams?.width ?: LinearLayout.LayoutParams.WRAP_CONTENT,
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

    private fun files(count: Int, caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_FILE,
            mimeType = FILE_MIME,
            extension = FILE_EXT,
            caption = caption,
        )

    /** The file-card list, styled for the side it is being drawn on. */
    private fun cards(
        count: Int,
        outgoing: Boolean,
        caption: String? = null,
    ): (ComponentActivity) -> View = { activity ->
        CometChatFilesBubble(activity).apply {
            setStyle(
                if (outgoing) CometChatFileBubbleStyle.outgoing(activity)
                else CometChatFileBubbleStyle.incoming(activity)
            )
            setOutgoing(outgoing)
            setMessage(files(count, caption))
        }
    }

    // ── the file-card list ────────────────────────────────────────────────

    @Test fun oneFile_incoming() = capture { cards(1, outgoing = false)(it) }

    @Test fun oneFile_outgoing() = capture(outgoing = true) { cards(1, outgoing = true)(it) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun oneFile_incoming_dark() = capture { cards(1, outgoing = false)(it) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun oneFile_outgoing_dark() = capture(outgoing = true) { cards(1, outgoing = true)(it) }

    @Test fun twoFiles_incoming() = capture { cards(2, outgoing = false)(it) }

    @Test fun twoFiles_outgoing() = capture(outgoing = true) { cards(2, outgoing = true)(it) }

    @Test fun threeFiles_incoming_atTheCollapseThreshold() =
        capture { cards(3, outgoing = false)(it) }

    @Test fun fiveFiles_incoming_collapsedBehindTheToggle() =
        capture { cards(5, outgoing = false)(it) }

    @Test fun fiveFiles_outgoing_collapsedBehindTheToggle() =
        capture(outgoing = true) { cards(5, outgoing = true)(it) }

    // ── caption ─────────────────────────────────────────────────────────────

    @Test fun withCaption_incoming() =
        capture { cards(2, outgoing = false, caption = "both drafts attached")(it) }

    @Test fun withCaption_outgoing() =
        capture(outgoing = true) { cards(2, outgoing = true, caption = "both drafts attached")(it) }

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test fun oneFile_incoming_rtl() = capture(rtl = true) { cards(1, outgoing = false)(it) }

    @Test fun oneFile_outgoing_rtl() = capture(outgoing = true, rtl = true) { cards(1, outgoing = true)(it) }
}
