package com.cometchat.uikit.compose.screenshots

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
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
 * Snapshot layer for the compose video message bubble.
 *
 * Captured the same way as the text bubble: **through [CometChatMessageBubble]**,
 * incoming and outgoing, light and dark. The video bubble draws no background of
 * its own — the fill, corner radius and side placement all come from the container.
 * Capturing bare leaves tiles floating on the page with no bubble behind them, which
 * is what these baselines exist to avoid.
 *
 * Every message here is a real `video/mp4` message. Composition locals are left at
 * their defaults, so each one routes exactly as the app routes it: under the default
 * `LocalEnableMultipleAttachments = true`, `InternalContentRenderer` picks the
 * multi-attachment grid, and the video path adds the play overlay the image path has
 * no equivalent of. These baselines therefore pin what a reader actually sees.
 *
 * Coil has no network under Robolectric and renders a deterministic placeholder,
 * which is what makes the grid geometry comparable.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatVideoBubbleScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatVideoBubbleScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatVideoBubbleScreenshotTest {

    /** The container renders a timestamp; pin the zone or the baseline encodes the host's. */
    private lateinit var originalZone: TimeZone

    @Before
    fun pinTimeZone() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    }

    @After
    fun restoreTimeZone() = TimeZone.setDefault(originalZone)

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/videobubble"
        )
    )

    private companion object {
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)
        val INCOMING = UIKitConstants.MessageBubbleAlignment.LEFT
        val OUTGOING = UIKitConstants.MessageBubbleAlignment.RIGHT
        const val VIDEO_MIME = "video/mp4"
        const val VIDEO_EXT = "mp4"
    }




    private fun capture(dark: Boolean = false, content: @Composable () -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
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
        scenario.onActivity { activity ->
            activity.window.decorView
                .findViewById<ViewGroup>(android.R.id.content)
                .getChildAt(0)
                .captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    /**
     * No `style` is passed, so the container resolves its own default per alignment —
     * that default fill is exactly what these baselines exist to pin.
     */
    /**
     * No `style` is passed, so the container resolves its own default per alignment —
     * that incoming/outgoing fill is exactly what these baselines exist to pin.
     */
    private fun bubble(
        count: Int,
        alignment: UIKitConstants.MessageBubbleAlignment,
        caption: String? = null,
    ): @Composable () -> Unit = {
        CometChatMessageBubble(
            message = MockFactory.createMediaMessage(
                count = count,
                type = CometChatConstants.MESSAGE_TYPE_VIDEO,
                mimeType = VIDEO_MIME,
                extension = VIDEO_EXT,
                caption = caption,
            ),
            alignment = alignment,
        )
    }

    // ── single video ────────────────────────────────────────────────────────

    @Test fun oneVideo_incoming() = capture(content = bubble(1, INCOMING))

    @Test fun oneVideo_outgoing() = capture(content = bubble(1, OUTGOING))

    @Test fun oneVideo_incoming_dark() = capture(dark = true, content = bubble(1, INCOMING))

    @Test fun oneVideo_outgoing_dark() = capture(dark = true, content = bubble(1, OUTGOING))

    // ── grids, both sides ───────────────────────────────────────────────────

    @Test fun twoVideos_incoming() = capture(content = bubble(2, INCOMING))

    @Test fun twoVideos_outgoing() = capture(content = bubble(2, OUTGOING))

    @Test fun threeVideos_incoming() = capture(content = bubble(3, INCOMING))

    @Test fun threeVideos_outgoing() = capture(content = bubble(3, OUTGOING))

    @Test fun fourVideos_incoming_fillsWithoutOverflow() = capture(content = bubble(4, INCOMING))

    @Test fun fourVideos_outgoing_fillsWithoutOverflow() = capture(content = bubble(4, OUTGOING))

    @Test fun sevenVideos_incoming_capsAtFourWithOverflowBadge() = capture(content = bubble(7, INCOMING))

    @Test fun sevenVideos_outgoing_capsAtFourWithOverflowBadge() = capture(content = bubble(7, OUTGOING))

    @Test fun fourVideos_outgoing_dark() = capture(dark = true, content = bubble(4, OUTGOING))

    // ── caption, where the fill sits behind text too ────────────────────────

    @Test
    fun withCaption_incoming() = capture(content = bubble(3, INCOMING, caption = "three from the trip"))

    @Test
    fun withCaption_outgoing() = capture(content = bubble(3, OUTGOING, caption = "three from the trip"))
}
