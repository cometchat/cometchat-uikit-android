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
import coil.Coil
import coil.ImageLoader
import kotlinx.coroutines.Dispatchers
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
 * Snapshot layer for the compose sticker bubble.
 *
 * Captured through [CometChatMessageBubble]. Both sticker styles are transparent by
 * design, so incoming and outgoing differ only in placement and timestamp colour --
 * these baselines are what pins that. Coil renders a placeholder with no network.
 *
 * The sticker name reaches only the accessible label, never the pixels, so it has no
 * baseline of its own -- the functional test asserts it instead.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatStickerBubbleScreenshotTest {

    /** The container renders a timestamp; pin the zone or the baseline encodes the host's. */
    private lateinit var originalZone: TimeZone

    /**
     * Coil resolves on background dispatchers, so whether the load has failed by the
     * time Roborazzi captures is a race: the error slot ("Failed to load sticker")
     * appeared in some runs and not others, and the dark baselines caught it. Pinning
     * every dispatcher to the main thread makes the failure happen during composition,
     * so the captured state is the same every run.
     */
    @Before
    fun installSynchronousImageLoader() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        Coil.setImageLoader(
            ImageLoader.Builder(context)
                .fetcherDispatcher(Dispatchers.Main.immediate)
                .decoderDispatcher(Dispatchers.Main.immediate)
                .transformationDispatcher(Dispatchers.Main.immediate)
                .build(),
        )
    }

    @After
    fun resetImageLoader() = Coil.reset()

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
            outputDirectoryPath = "../screenshot-gallery/compose/stickerbubble"
        )
    )

    private companion object {
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)
        val INCOMING = UIKitConstants.MessageBubbleAlignment.LEFT
        val OUTGOING = UIKitConstants.MessageBubbleAlignment.RIGHT
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

    private fun bubble(
        alignment: UIKitConstants.MessageBubbleAlignment,
        name: String? = "Party Popper",
    ): @Composable () -> Unit = {
        CometChatMessageBubble(
            message = MockFactory.createStickerMessage(name = name),
            alignment = alignment,
        )
    }

    @Test fun sticker_incoming() = capture(content = bubble(INCOMING))

    @Test fun sticker_outgoing() = capture(content = bubble(OUTGOING))

    @Test fun sticker_incoming_dark() = capture(dark = true, content = bubble(INCOMING))

    @Test fun sticker_outgoing_dark() = capture(dark = true, content = bubble(OUTGOING))
}
