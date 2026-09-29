package com.cometchat.uikit.kotlin.presentation.shared.messagebubble

import android.content.res.Configuration
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.utils.RoborazziConfig
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.TimeZone

/**
 * Snapshot layer for the **View** bubble container, [CometChatMessageBubble].
 *
 * The Compose twin
 * ([com.cometchat.uikit.compose.screenshots.CometChatMessageBubbleScreenshotTest])
 * captures the same eight situations, and the pair is the only place either toolkit's
 * container chrome is pinned: every other bubble baseline captures its own unit
 * *through* the container and treats this as scenery.
 *
 * The content is a plain text message on purpose, so what varies is the container's
 * own work — which edge the bubble sits on, the fill it resolves per alignment, the
 * avatar column, the timestamp row, and the slots.
 *
 * There are no right-to-left baselines here, deliberately. The Compose twin has them
 * and they mirror; under Robolectric this side does not, whether the direction comes
 * from the `ldrtl` qualifier or from forcing `layoutDirection` on the root — the
 * capture comes out byte-identical to the left-to-right one. The bubble's layouts are
 * direction-aware (`toEndOf`, `alignParentEnd`), so this looks like the harness rather
 * than the component, but an RTL baseline that equals the LTR one pins nothing, so
 * none is recorded until it can be shown to mirror.
 *
 * Grouping is expressed differently in the two toolkits and the baselines say so: the
 * View container hides the avatar and status row with `INVISIBLE`, keeping the cells'
 * space, where Compose fades them with `leadingAlpha` / `statusInfoAlpha`. The
 * resulting layout is the promise both sides make.
 *
 * Run:
 *   ./gradlew :chatuikit-kotlin:recordRoborazziDebug  --tests "*.CometChatMessageBubbleScreenshotTest"
 *   ./gradlew :chatuikit-kotlin:compareRoborazziDebug --tests "*.CometChatMessageBubbleScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessageBubbleScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/kotlin/messagebubble"
        )
    )

    private companion object {
        const val CANVAS_LIGHT = 0xFFF4F4F4.toInt()
        const val CANVAS_DARK = 0xFF121212.toInt()
        const val BODY = "the container decides where this sits"
        val INCOMING = UIKitConstants.MessageBubbleAlignment.LEFT
        val OUTGOING = UIKitConstants.MessageBubbleAlignment.RIGHT
    }

    private lateinit var cometChat: MockedStatic<CometChat>
    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After
    fun tearDown() {
        cometChat.close()
        TimeZone.setDefault(originalZone)
    }

    private fun capture(
        rtl: Boolean = false,
        configure: (ComponentActivity, CometChatMessageBubble) -> Unit,
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val bubble = CometChatMessageBubble(activity)

            val isNight = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val container = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundColor(if (isNight) CANVAS_DARK else CANVAS_LIGHT)
                addView(
                    bubble,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
            activity.setContentView(container)
            // The root has to be attached before the direction is set; the ldrtl
            // qualifier does nothing here. See RtlLayoutDirectionHarnessTest.
            if (rtl) container.layoutDirection = View.LAYOUT_DIRECTION_RTL
            ShadowLooper.idleMainLooper()

            configure(activity, bubble)
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

    private fun message() = MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, text = BODY)

    /** A slot's content brings its own typography; size it as a host would. */
    private fun slot(activity: ComponentActivity, text: String, sizeSp: Float): TextView =
        TextView(activity).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        }

    // ── sent vs received ────────────────────────────────────────────────────

    @Test fun incoming() = capture { _, b -> b.setMessage(message(), INCOMING) }

    @Test fun outgoing() = capture { _, b -> b.setMessage(message(), OUTGOING) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun incoming_dark() = capture { _, b -> b.setMessage(message(), INCOMING) }

    @Config(qualifiers = "w400dp-h800dp-night-xxhdpi")
    @Test fun outgoing_dark() = capture { _, b -> b.setMessage(message(), OUTGOING) }

    // ── grouped runs ────────────────────────────────────────────────────────

    /** Mid-run: avatar and timestamp both suppressed, both cells still reserved. */
    @Test
    fun grouped_middleOfARun() = capture { _, b ->
        b.setMessage(message(), INCOMING)
        b.setAvatarVisibility(View.INVISIBLE)
        b.setStatusInfoViewVisibility(View.INVISIBLE)
    }

    /** End of a run: the timestamp comes back, the avatar stays hidden. */
    @Test
    fun grouped_lastOfARun() = capture { _, b ->
        b.setMessage(message(), INCOMING)
        b.setAvatarVisibility(View.INVISIBLE)
        b.setStatusInfoViewVisibility(View.VISIBLE)
    }

    // ── slots ───────────────────────────────────────────────────────────────

    @Test
    fun withHeaderAndFooterSlots() = capture { activity, b ->
        b.setMessage(message(), INCOMING)
        b.setHeaderView(slot(activity, "Alice · Design", 12f))
        b.setFooterView(slot(activity, "edited", 11f))
    }

    @Test
    fun withoutTheAvatar() = capture { _, b ->
        b.setMessage(message(), INCOMING)
        b.setAvatarVisibility(View.GONE)
    }

    // ── right-to-left ───────────────────────────────────────────────────────

    @Test fun incoming_rtl() = capture(rtl = true) { _, b -> b.setMessage(message(), INCOMING) }

    @Test fun outgoing_rtl() = capture(rtl = true) { _, b -> b.setMessage(message(), OUTGOING) }
}
