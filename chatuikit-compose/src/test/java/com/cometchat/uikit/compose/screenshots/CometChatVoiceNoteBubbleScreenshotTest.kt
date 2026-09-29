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
import com.cometchat.chat.models.MediaMessage
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
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot layer for `CometChatVoiceNoteBubble`.
 *
 * Recorded voice notes are the third route out of an audio message: the renderer sends
 * them here rather than to `CometChatAudiosBubble` when `metaData["audioType"]` is
 * `voice_note`. [CometChatAudioBubbleScreenshotTest] covers the other two routes and
 * says so; this file is the unit that route was missing.
 *
 * Captured **through [CometChatMessageBubble]** for the same reason the audio baselines
 * are: the bubble paints no background of its own, and the outgoing palette is drawn for
 * the container's fill — bare on a light page it collapses to near-white on near-white.
 *
 * Nothing downloads under Robolectric, so every capture is the pre-playback state, which
 * is what a reader sees when the message arrives. It is deterministic because
 * `WaveformUtils` derives the bars from the URL alone.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatVoiceNoteBubbleScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatVoiceNoteBubbleScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatVoiceNoteBubbleScreenshotTest {

    /** The container renders a timestamp; pin the zone or the baseline encodes the host's. */
    private lateinit var originalZone: java.util.TimeZone

    @Before
    fun pinTimeZone() {
        originalZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("GMT"))
        AudioBubbleStateManager.clearAll()
    }

    @After
    fun restore() {
        java.util.TimeZone.setDefault(originalZone)
        AudioBubbleStateManager.clearAll()
    }

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/voicenotebubble"
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

    /** An audio message carrying the cross-platform voice-note marker the renderer routes on. */
    private fun voiceNote(caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = 1,
            type = CometChatConstants.MESSAGE_TYPE_AUDIO,
            mimeType = AUDIO_MIME,
            extension = AUDIO_EXT,
            caption = caption,
        ).apply {
            metadata = JSONObject().put(
                UIKitConstants.JSONKeys.AUDIO_TYPE,
                UIKitConstants.JSONKeys.AUDIO_TYPE_VOICE_NOTE,
            )
        }

    /**
     * No `style` is passed, so the container resolves its own default per alignment —
     * that incoming/outgoing fill is exactly what these baselines exist to pin.
     */
    private fun bubble(
        alignment: UIKitConstants.MessageBubbleAlignment,
        caption: String? = null,
    ): @Composable () -> Unit = {
        CompositionLocalProvider(LocalEnableMultipleAttachments provides true) {
            CometChatMessageBubble(
                message = voiceNote(caption),
                alignment = alignment,
            )
        }
    }

    @Test fun voiceNote_incoming() = capture(content = bubble(INCOMING))

    @Test fun voiceNote_outgoing() = capture(content = bubble(OUTGOING))

    @Test fun voiceNote_incoming_dark() = capture(dark = true, content = bubble(INCOMING))

    @Test fun voiceNote_outgoing_dark() = capture(dark = true, content = bubble(OUTGOING))

    @Test
    fun voiceNote_withCaption_incoming() =
        capture(content = bubble(INCOMING, caption = "Listen when you get a minute"))

    @Test
    fun voiceNote_withCaption_outgoing() =
        capture(content = bubble(OUTGOING, caption = "Listen when you get a minute"))
}
