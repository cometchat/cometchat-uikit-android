package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.audiobubble

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
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.audiosbubble.CometChatAudiosBubble
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
 * Snapshot layer for the **View** audio bubbles.
 *
 * Captured inside [bubbleChrome], the View suite's standard wrapper, so incoming and
 * outgoing fills are both pinned at the same default size as every other bubble in
 * the gallery. Neither audio bubble paints a background of its own — the outgoing
 * palette (white glyphs, white-tinted waves) is drawn *for* the primary fill, so a
 * bare capture would be white-on-white and worth nothing.
 *
 * Three routes get baselines, because the message list picks between them:
 *
 * - [CometChatAudiosBubble] with picker audio → the player-card list;
 * - [CometChatAudioBubble] bound directly → the single waveform player, which is what
 *   an integrator gets with multiple attachments switched off, and also what
 *   [CometChatAudiosBubble] delegates to for a recorded voice note.
 *
 * That third route has no baselines of its own on purpose: captured through the
 * wrapper, a voice note produced images byte-identical to `singleWaveform_*`, which
 * is the correct outcome — the wrapper drops its own padding and hands the whole
 * layout over. Committing the same pixels twice would only mean two files to update
 * for one change, so the routing is pinned in
 * `CometChatAudiosBubbleFunctionalTest.aVoiceNoteHandsTheWholeLayoutToTheSingleWaveformBubble`
 * instead, where it is asserted rather than eyeballed.
 *
 * Nothing downloads under Robolectric, so every capture is the pre-playback state.
 * The bars are still deterministic: `WaveformUtils` derives them from the URL alone.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAudioBubbleScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/audiobubble"
        )
    )

    private companion object {
        const val CANVAS_LIGHT = 0xFFEEEEEE.toInt()
        const val CANVAS_DARK = 0xFF121212.toInt()
        const val AUDIO_MIME = "audio/mpeg"
        const val AUDIO_EXT = "mp3"

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
            // Honour a width the bubble set on itself. `CometChatAudiosBubble` fixes its
            // own layout params to 240dp in `init`; the single `CometChatAudioBubble` is
            // laid out `match_parent` throughout and takes its width from whatever hosts
            // it, so wrapping it here would collapse the waveform onto the width of the
            // subtitle. Either way the bubble ends up at the width the message list gives
            // it, not one this harness invented.
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

    private fun audios(count: Int, caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_AUDIO,
            mimeType = AUDIO_MIME,
            extension = AUDIO_EXT,
            caption = caption,
        )

    /** The player-card list, styled for the side it is being drawn on. */
    private fun cards(
        count: Int,
        outgoing: Boolean,
        caption: String? = null,
    ): (ComponentActivity) -> View = { activity ->
        CometChatAudiosBubble(activity).apply {
            setStyle(
                if (outgoing) CometChatAudioBubbleStyle.outgoing(activity)
                else CometChatAudioBubbleStyle.incoming(activity)
            )
            setOutgoing(outgoing)
            setMessage(audios(count, caption))
        }
    }

    /**
     * The single waveform player, bound directly and given the width the message list
     * gives it: `R.dimen.cometchat_240dp`, the same value `CometChatAudiosBubble` sets
     * on itself and the same as compose's `MULTI_ATTACHMENT_BUBBLE_WIDTH`. The bubble's
     * own layout is `match_parent` end to end, so without it the waveform, the seek
     * track and the play row all shrink onto the subtitle's intrinsic width — a shape
     * that never appears in the product.
     */
    private fun waveform(outgoing: Boolean): (ComponentActivity) -> View = { activity ->
        CometChatAudioBubble(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                activity.resources.getDimensionPixelSize(R.dimen.cometchat_240dp),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setStyle(
                if (outgoing) CometChatAudioBubbleStyle.outgoing(activity)
                else CometChatAudioBubbleStyle.incoming(activity)
            )
            setMessage(audios(1))
        }
    }

    // ── the player-card list ────────────────────────────────────────────────

    @Test fun oneAudio_incoming() = capture { cards(1, outgoing = false)(it) }

    @Test fun oneAudio_outgoing() = capture(outgoing = true) { cards(1, outgoing = true)(it) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun oneAudio_incoming_dark() = capture { cards(1, outgoing = false)(it) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun oneAudio_outgoing_dark() = capture(outgoing = true) { cards(1, outgoing = true)(it) }

    @Test fun twoAudios_incoming() = capture { cards(2, outgoing = false)(it) }

    @Test fun twoAudios_outgoing() = capture(outgoing = true) { cards(2, outgoing = true)(it) }

    @Test fun threeAudios_incoming_atTheCollapseThreshold() =
        capture { cards(3, outgoing = false)(it) }

    @Test fun fiveAudios_incoming_collapsedBehindTheToggle() =
        capture { cards(5, outgoing = false)(it) }

    @Test fun fiveAudios_outgoing_collapsedBehindTheToggle() =
        capture(outgoing = true) { cards(5, outgoing = true)(it) }

    // ── caption ─────────────────────────────────────────────────────────────

    @Test fun withCaption_incoming() =
        capture { cards(2, outgoing = false, caption = "two takes of the same riff")(it) }

    @Test fun withCaption_outgoing() =
        capture(outgoing = true) { cards(2, outgoing = true, caption = "two takes of the same riff")(it) }

    // ── the single waveform player ──────────────────────────────────────────

    @Test fun singleWaveform_incoming() = capture { waveform(outgoing = false)(it) }

    @Test fun singleWaveform_outgoing() = capture(outgoing = true) { waveform(outgoing = true)(it) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun singleWaveform_incoming_dark() = capture { waveform(outgoing = false)(it) }

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test fun oneAudio_incoming_rtl() = capture(rtl = true) { cards(1, outgoing = false)(it) }

    @Test fun oneAudio_outgoing_rtl() = capture(outgoing = true, rtl = true) { cards(1, outgoing = true)(it) }
}
