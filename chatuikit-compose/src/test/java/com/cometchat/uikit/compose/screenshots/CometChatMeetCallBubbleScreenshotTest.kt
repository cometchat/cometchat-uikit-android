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
import com.cometchat.uikit.compose.presentation.shared.messagebubble.ui.CometChatMessageBubble
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.compose.presentation.utils.RoborazziConfig
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot layer for [CometChatMeetCallBubble].
 *
 * Captured through [CometChatMessageBubble]. Taken bare, the outgoing style paints
 * white text on the page with no fill behind it — the bubble background belongs to
 * the container, as it does for the text and image bubbles.
 *
 * Only alignment and theme vary here: on the container path this bubble derives its
 * own title and icon from the call type rather than reading `customData`, so passing
 * a different title produces a byte-identical baseline. Title and call-type variants
 * are covered by the functional and property layers, which drive the data overload.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMeetCallBubbleScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(outputDirectoryPath = "../screenshot-gallery/compose/meetcallbubble")
    )

    private companion object {
        val INCOMING = UIKitConstants.MessageBubbleAlignment.LEFT
        val OUTGOING = UIKitConstants.MessageBubbleAlignment.RIGHT
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)
    }

    private fun capture(dark: Boolean = false, content: @Composable () -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { a ->
            a.setContent {
                CometChatTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .background(if (dark) canvasDark else canvasLight).padding(16.dp)
                    ) { content() }
                }
            }
        }
        scenario.onActivity { a ->
            a.window.decorView.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
                .captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    private fun bubble(
        alignment: UIKitConstants.MessageBubbleAlignment,
    ): @Composable () -> Unit = {
        CometChatMessageBubble(
            message = MockFactory.createMeetCallMessage(),
            alignment = alignment,
        )
    }

    @Test fun call_incoming() = capture(content = bubble(INCOMING))

    @Test fun call_outgoing() = capture(content = bubble(OUTGOING))

    @Test fun call_incoming_dark() = capture(dark = true, content = bubble(INCOMING))

    @Test fun call_outgoing_dark() = capture(dark = true, content = bubble(OUTGOING))

}
