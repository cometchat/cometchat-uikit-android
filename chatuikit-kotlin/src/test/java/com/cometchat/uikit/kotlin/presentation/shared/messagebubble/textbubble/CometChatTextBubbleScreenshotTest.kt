package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.textbubble

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
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
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
 * Snapshot layer for the **View** [CometChatTextBubble].
 *
 * Like its Compose counterpart, this bubble draws no fill of its own — the bubble
 * background, corner radius and side placement belong to the message-bubble chrome
 * around it. The View suite's established answer is [bubbleChrome], which wraps the
 * content in a `MaterialCardView` filled with the very colours
 * `CometChatMessageBubbleStyle.outgoing()/incoming()` resolve to
 * (`primary` vs `neutral300`), so incoming and outgoing are pinned rather than
 * absent. Same approach as `MultiAttachmentBubblesScreenshotTest`.
 *
 * The timestamp is a fixed GMT label rather than a formatted `sentAt`, so the
 * baseline cannot encode the recorder's time zone — the defect that had
 * `CometChatThreadHeaderScreenshotTest` committing "5:30 AM" for midnight UTC.
 *
 * Run:
 *   ./gradlew :chatuikit-kotlin:recordRoborazziDebug  --tests "*.CometChatTextBubbleScreenshotTest"
 *   ./gradlew :chatuikit-kotlin:compareRoborazziDebug --tests "*.CometChatTextBubbleScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatTextBubbleScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/textbubble"
        )
    )

    private companion object {
        const val CANVAS_LIGHT = 0xFFEEEEEE.toInt()
        const val CANVAS_DARK = 0xFF121212.toInt()
        const val FIXED_SENT_AT = 1_729_011_360L

        /** Deterministic label for [FIXED_SENT_AT] — pinned to GMT so it never shifts with the host TZ. */
        val TIME_LABEL: String = SimpleDateFormat("h:mm a", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("GMT") }
            .format(Date(FIXED_SENT_AT * 1000))
    }

    private fun textMessage(body: String) =
        TextMessage("receiver-1", body, CometChatConstants.RECEIVER_TYPE_USER).apply {
            id = 1L
            sender = User().apply { uid = "sender-1"; name = "Alice" }
            sentAt = FIXED_SENT_AT
            category = CometChatConstants.CATEGORY_MESSAGE
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

    /** A styled bubble carrying [body]; a style is required or renderMarkdown bails out. */
    private fun bubble(body: String, outgoing: Boolean): (ComponentActivity) -> View = { activity ->
        CometChatTextBubble(activity).apply {
            setStyle(
                CometChatTextBubbleStyle(
                    textColor = if (outgoing) Color.WHITE
                    else resolveThemeColor(activity, R.attr.cometchatTextColorPrimary)
                )
            )
            setMessage(
                textMessage(body),
                emptyList(),
                if (outgoing) UIKitConstants.MessageBubbleAlignment.RIGHT
                else UIKitConstants.MessageBubbleAlignment.LEFT,
            )
        }
    }

    // ── incoming vs outgoing ────────────────────────────────────────────────

    @Test
    fun plainText_incoming() = capture { bubble("Hello from the other side", outgoing = false)(it) }

    @Test
    fun plainText_outgoing() =
        capture(outgoing = true) { bubble("Hello from me", outgoing = true)(it) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun plainText_incoming_dark() = capture { bubble("Hello from the other side", outgoing = false)(it) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test
    fun plainText_outgoing_dark() =
        capture(outgoing = true) { bubble("Hello from me", outgoing = true)(it) }

    // ── markdown block rendering ────────────────────────────────────────────

    @Test
    fun inlineEmphasis_incoming() = capture { bubble("a **bold** and _italic_ word", outgoing = false)(it) }

    @Test
    fun codeBlock_incoming() = capture { bubble("before\n```\nval x = 1\n```\nafter", outgoing = false)(it) }

    @Test
    fun blockquote_incoming() = capture { bubble("> a quoted line\nand a reply", outgoing = false)(it) }

    @Test
    fun bulletList_incoming() = capture { bubble("- one\n- two\n- three", outgoing = false)(it) }

    @Test
    fun longBody_wrapsInsideTheBubble() =
        capture { bubble("A noticeably longer message body that has to wrap onto several lines inside the bubble.", outgoing = false)(it) }

    // ── link preview ────────────────────────────────────────────────────────

    @Test
    fun linkPreview_incoming() = capture { activity ->
        CometChatTextBubble(activity).apply {
            setStyle(CometChatTextBubbleStyle(textColor = resolveThemeColor(activity, R.attr.cometchatTextColorPrimary)))
            setMessage(
                textMessage("see https://cometchat.com"),
                emptyList(),
                UIKitConstants.MessageBubbleAlignment.LEFT,
            )
            setLinkPreview("CometChat", "Chat and calling APIs", "https://cometchat.com", null, null)
        }
    }

    // ── translation row ─────────────────────────────────────────────────────

    @Test
    fun translatedText_incoming() = capture { activity ->
        CometChatTextBubble(activity).apply {
            setStyle(CometChatTextBubbleStyle(textColor = resolveThemeColor(activity, R.attr.cometchatTextColorPrimary)))
            setMessage(textMessage("hola"), emptyList(), UIKitConstants.MessageBubbleAlignment.LEFT)
            setTranslatedText("hello")
        }
    }

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test
    fun plainText_incoming_rtl() =
        capture(rtl = true) { bubble("Hello from the other side", outgoing = false)(it) }

    @Test
    fun plainText_outgoing_rtl() =
        capture(outgoing = true, rtl = true) { bubble("Hello from me", outgoing = true)(it) }
}
