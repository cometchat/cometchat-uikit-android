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
import com.cometchat.uikit.core.utils.AudioBubbleStateManager
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
 * Snapshot layer for the compose audio message bubbles.
 *
 * Captured **through [CometChatMessageBubble]**, incoming and outgoing, light and
 * dark, at the container's own default size. Neither audio bubble paints a background
 * of its own: the fill, the corner radius and the side placement all come from the
 * container, and both bubbles' outgoing palettes are drawn *for* that fill — hosted
 * bare on a light page, outgoing collapses to near-white on near-white. The prop
 * matrix measures exactly that, and it is why nothing here is captured bare.
 *
 * Both routes are covered, because an audio message reaches a different bubble
 * depending on one composition local:
 *
 * - default (`LocalEnableMultipleAttachments = true`) → `CometChatAudiosBubble`,
 *   the player-card list;
 * - switched off → `CometChatAudioBubble`, the single waveform player.
 *
 * The second is a supported integrator configuration, not a harness contrivance, so
 * it gets baselines too. Recorded voice notes take a third route
 * (`CometChatVoiceNoteBubble`) and are a separate bubble unit.
 *
 * Nothing downloads under Robolectric, so every capture is the pre-playback state —
 * which is what a reader sees when a message arrives, and it is deterministic because
 * `WaveformUtils` derives the bars from the URL alone.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatAudioBubbleScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatAudioBubbleScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAudioBubbleScreenshotTest {

    /** The container renders a timestamp; pin the zone or the baseline encodes the host's. */
    private lateinit var originalZone: TimeZone

    @Before
    fun pinTimeZone() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
        AudioBubbleStateManager.clearAll()
    }

    @After
    fun restore() {
        TimeZone.setDefault(originalZone)
        AudioBubbleStateManager.clearAll()
    }

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/audiobubble"
        )
    )

    private companion object {
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)
        val INCOMING = UIKitConstants.MessageBubbleAlignment.LEFT
        val OUTGOING = UIKitConstants.MessageBubbleAlignment.RIGHT
        const val AUDIO_MIME = "audio/mpeg"
        const val AUDIO_EXT = "mp3"
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
                    type = CometChatConstants.MESSAGE_TYPE_AUDIO,
                    mimeType = AUDIO_MIME,
                    extension = AUDIO_EXT,
                    caption = caption,
                ),
                alignment = alignment,
            )
        }
    }

    // ── the default route: player cards ─────────────────────────────────────

    @Test fun oneAudio_incoming() = capture(content = bubble(1, INCOMING))

    @Test fun oneAudio_outgoing() = capture(content = bubble(1, OUTGOING))

    @Test fun oneAudio_incoming_dark() = capture(dark = true, content = bubble(1, INCOMING))

    @Test fun oneAudio_outgoing_dark() = capture(dark = true, content = bubble(1, OUTGOING))

    @Test fun twoAudios_incoming() = capture(content = bubble(2, INCOMING))

    @Test fun twoAudios_outgoing() = capture(content = bubble(2, OUTGOING))

    @Test fun threeAudios_incoming_atTheCollapseThreshold() = capture(content = bubble(3, INCOMING))

    @Test fun threeAudios_outgoing_atTheCollapseThreshold() = capture(content = bubble(3, OUTGOING))

    @Test fun fiveAudios_incoming_collapsedBehindTheToggle() = capture(content = bubble(5, INCOMING))

    @Test fun fiveAudios_outgoing_collapsedBehindTheToggle() = capture(content = bubble(5, OUTGOING))

    @Test fun fiveAudios_outgoing_dark() = capture(dark = true, content = bubble(5, OUTGOING))

    // ── caption, where the fill sits behind text too ────────────────────────

    @Test
    fun withCaption_incoming() =
        capture(content = bubble(2, INCOMING, caption = "two takes of the same riff"))

    @Test
    fun withCaption_outgoing() =
        capture(content = bubble(2, OUTGOING, caption = "two takes of the same riff"))

    // ── the single-bubble route (multi-attachments off) ─────────────────────

    @Test
    fun singleWaveform_incoming() =
        capture(content = bubble(1, INCOMING, multipleAttachments = false))

    @Test
    fun singleWaveform_outgoing() =
        capture(content = bubble(1, OUTGOING, multipleAttachments = false))

    @Test
    fun singleWaveform_incoming_dark() =
        capture(dark = true, content = bubble(1, INCOMING, multipleAttachments = false))

    @Test
    fun singleWaveform_outgoing_dark() =
        capture(dark = true, content = bubble(1, OUTGOING, multipleAttachments = false))
}
