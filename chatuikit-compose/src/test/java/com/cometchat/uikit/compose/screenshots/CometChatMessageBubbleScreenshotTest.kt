package com.cometchat.uikit.compose.screenshots

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.compose.presentation.shared.messagebubble.ui.CometChatMessageBubble
import com.cometchat.uikit.compose.presentation.utils.RoborazziConfig
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

/**
 * Snapshot layer for the bubble **container**, [CometChatMessageBubble].
 *
 * Every other bubble baseline in this package captures its own unit *through* the
 * container and treats the surrounding chrome as scenery. This file is the other way
 * round: the content is a plain text message precisely so that what varies is the
 * container's own work — which edge the bubble sits on, the fill and corner treatment
 * it resolves per alignment, the avatar column, the timestamp row, and the slots.
 *
 * Grouping is the reason `leadingAlpha` and `statusInfoAlpha` exist: a run of messages
 * from one sender hides the avatar on all but the first and the timestamp on all but
 * the last, while keeping both cells' space so the column stays aligned. That is a
 * layout promise a screenshot can hold and an assertion cannot, so both positions in a
 * run get baselines.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatMessageBubbleScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatMessageBubbleScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessageBubbleScreenshotTest {

    /** The container renders a timestamp; pin the zone or the baseline encodes the host's. */
    private lateinit var originalZone: TimeZone

    @Before
    fun pinTimeZone() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    }

    @After
    fun restore() {
        TimeZone.setDefault(originalZone)
    }

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/messagebubble"
        )
    )

    private companion object {
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)
        val INCOMING = UIKitConstants.MessageBubbleAlignment.LEFT
        val OUTGOING = UIKitConstants.MessageBubbleAlignment.RIGHT
        const val BODY = "the container decides where this sits"
    }

    private fun capture(
        dark: Boolean = false,
        rtl: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                CometChatTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (dark) canvasDark else canvasLight)
                            .padding(16.dp)
                    ) {
                        content()
                    }
                }
                }
            }
        }
        scenario.onActivity { activity ->
            activity.window.decorView
                .findViewById<ViewGroup>(android.R.id.content)
                .getChildAt(0)
                .captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    private fun bubble(
        alignment: UIKitConstants.MessageBubbleAlignment,
        leadingAlpha: Float = 1f,
        statusInfoAlpha: Float = 1f,
        shouldShowDefaultAvatar: Boolean = true,
        headerView: (@Composable () -> Unit)? = null,
        footerView: (@Composable () -> Unit)? = null,
    ): @Composable () -> Unit = {
        CometChatMessageBubble(
            message = MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, text = BODY),
            alignment = alignment,
            shouldShowDefaultAvatar = shouldShowDefaultAvatar,
            leadingAlpha = leadingAlpha,
            statusInfoAlpha = statusInfoAlpha,
            headerView = headerView,
            footerView = footerView,
        )
    }

    // ── sent vs received ────────────────────────────────────────────────────

    @Test fun incoming() = capture(content = bubble(INCOMING))

    @Test fun outgoing() = capture(content = bubble(OUTGOING))

    @Test fun incoming_dark() = capture(dark = true, content = bubble(INCOMING))

    @Test fun outgoing_dark() = capture(dark = true, content = bubble(OUTGOING))

    // ── grouped runs ────────────────────────────────────────────────────────

    /** Mid-run: avatar and timestamp both suppressed, both cells still reserved. */
    @Test
    fun grouped_middleOfARun() =
        capture(content = bubble(INCOMING, leadingAlpha = 0f, statusInfoAlpha = 0f))

    /** End of a run: the timestamp comes back, the avatar stays hidden. */
    @Test
    fun grouped_lastOfARun() =
        capture(content = bubble(INCOMING, leadingAlpha = 0f, statusInfoAlpha = 1f))

    // ── slots ───────────────────────────────────────────────────────────────

    @Test
    fun withHeaderAndFooterSlots() = capture(
        content = bubble(
            INCOMING,
            // Sized explicitly: a slot inherits no typography of its own, and an
            // unstyled Text renders at Material's display size, which is not what a
            // host puts in these.
            headerView = { Text("Alice · Design", fontSize = 12.sp) },
            footerView = { Text("edited", fontSize = 11.sp) },
        ),
    )

    @Test
    fun withoutTheDefaultAvatar() =
        capture(content = bubble(INCOMING, shouldShowDefaultAvatar = false))

    // ── right-to-left ───────────────────────────────────────────────────────

    /**
     * The container is the piece a right-to-left locale actually moves: the avatar
     * column, the side the bubble sits on and the timestamp row all mirror. Nothing in
     * the kit checked either direction before these, so both sides get one.
     */
    @Test fun incoming_rtl() = capture(rtl = true, content = bubble(INCOMING))

    @Test fun outgoing_rtl() = capture(rtl = true, content = bubble(OUTGOING))
}
