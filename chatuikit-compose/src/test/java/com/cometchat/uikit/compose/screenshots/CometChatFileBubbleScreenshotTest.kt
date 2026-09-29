package com.cometchat.uikit.compose.screenshots

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.compose.presentation.shared.messagebubble.ui.CometChatMessageBubble
import com.cometchat.uikit.compose.presentation.shared.messagebubble.ui.LocalEnableMultipleAttachments
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
 * Snapshot layer for the compose file message bubbles.
 *
 * Captured through [CometChatMessageBubble], incoming and outgoing, light and dark,
 * at the container's default size. Neither file bubble paints its own background and
 * both outgoing palettes are drawn for the container's fill, so a bare capture would
 * be near-white on near-white.
 *
 * Only the default multi-attachment route is captured. With multiple attachments
 * switched off the container reaches CometChatFileBubble, which throws on the first
 * draw: the renderer passes the per-type style unmerged when no messageBubbleStyle is
 * supplied, leaving cornerRadius as an unset sentinel. Those baselines go in once that
 * is fixed.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatFileBubbleScreenshotTest {

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
            outputDirectoryPath = "../screenshot-gallery/compose/filebubble"
        )
    )

    private companion object {
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)
        val INCOMING = UIKitConstants.MessageBubbleAlignment.LEFT
        val OUTGOING = UIKitConstants.MessageBubbleAlignment.RIGHT
        const val FILE_MIME = "application/pdf"
        const val FILE_EXT = "pdf"
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
     * that incoming/outgoing fill is exactly what these baselines exist to pin.
     */
    private fun bubble(
        count: Int,
        alignment: UIKitConstants.MessageBubbleAlignment,
        caption: String? = null,
        multipleAttachments: Boolean = true,
    ): @Composable () -> Unit = {
        CompositionLocalProvider(LocalEnableMultipleAttachments provides multipleAttachments) {
            CometChatMessageBubble(
                message = MockFactory.createMediaMessage(
                    count = count,
                    type = CometChatConstants.MESSAGE_TYPE_FILE,
                    mimeType = FILE_MIME,
                    extension = FILE_EXT,
                    caption = caption,
                ),
                alignment = alignment,
            )
        }
    }

    // ── the default route: file cards ─────────────────────────────────────

    @Test fun oneFile_incoming() = capture(content = bubble(1, INCOMING))

    @Test fun oneFile_outgoing() = capture(content = bubble(1, OUTGOING))

    @Test fun oneFile_incoming_dark() = capture(dark = true, content = bubble(1, INCOMING))

    @Test fun oneFile_outgoing_dark() = capture(dark = true, content = bubble(1, OUTGOING))

    @Test fun twoFiles_incoming() = capture(content = bubble(2, INCOMING))

    @Test fun twoFiles_outgoing() = capture(content = bubble(2, OUTGOING))

    @Test fun threeFiles_incoming_atTheCollapseThreshold() = capture(content = bubble(3, INCOMING))

    @Test fun threeFiles_outgoing_atTheCollapseThreshold() = capture(content = bubble(3, OUTGOING))

    @Test fun fiveFiles_incoming_collapsedBehindTheToggle() = capture(content = bubble(5, INCOMING))

    @Test fun fiveFiles_outgoing_collapsedBehindTheToggle() = capture(content = bubble(5, OUTGOING))

    @Test fun fiveFiles_outgoing_dark() = capture(dark = true, content = bubble(5, OUTGOING))

    // ── caption, where the fill sits behind text too ────────────────────────

    @Test
    fun withCaption_incoming() =
        capture(content = bubble(2, INCOMING, caption = "both drafts attached"))

    @Test
    fun withCaption_outgoing() =
        capture(content = bubble(2, OUTGOING, caption = "both drafts attached"))

}
