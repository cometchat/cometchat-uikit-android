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
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.User
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
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

/**
 * Snapshot layer for [CometChatPollBubble].
 *
 * Captured through [CometChatMessageBubble], like the text and image bubbles: the
 * poll draws its options and progress bars but takes its wrapper background, corner
 * radius and placement from the container, so a bare capture loses the bubble.
 *
 * Driving the container means the `CustomMessage` overload, which re-extracts the
 * poll from JSON and reads `CometChatUIKit.getLoggedInUser()` to mark the viewer's
 * own vote — hence the SDK static stub. Without it extraction returns null and the
 * poll renders empty.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatPollBubbleScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/pollbubble"
        )
    )

    private lateinit var cometChat: MockedStatic<CometChat>
    private lateinit var zone: TimeZone

    @Before
    fun setUp() {
        zone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }
            .thenReturn(User().apply { uid = "logged-in-user"; name = "Me" })
    }

    @After
    fun tearDown() {
        cometChat.close()
        TimeZone.setDefault(zone)
    }

    private companion object {
        val INCOMING = UIKitConstants.MessageBubbleAlignment.LEFT
        val OUTGOING = UIKitConstants.MessageBubbleAlignment.RIGHT
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)
    }

    private fun capture(dark: Boolean = false, content: @Composable () -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
                CometChatTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .background(if (dark) canvasDark else canvasLight)
                            .padding(16.dp)
                    ) { content() }
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

    private fun poll(
        alignment: UIKitConstants.MessageBubbleAlignment,
        question: String = "Which colour?",
        options: List<String> = listOf("Red", "Blue", "Green"),
        counts: List<Int> = listOf(2, 3, 0),
    ): @Composable () -> Unit = {
        CometChatMessageBubble(
            message = MockFactory.createPollMessage(question, options, counts),
            alignment = alignment,
        )
    }

    @Test fun threeOptions_incoming() = capture(content = poll(INCOMING))

    @Test fun threeOptions_outgoing() = capture(content = poll(OUTGOING))

    @Test fun threeOptions_incoming_dark() = capture(dark = true, content = poll(INCOMING))

    @Test fun threeOptions_outgoing_dark() = capture(dark = true, content = poll(OUTGOING))

    @Test fun twoOptions_incoming() =
        capture(content = poll(INCOMING, "Ship it?", listOf("Yes", "No"), listOf(7, 4)))

    @Test fun twoOptions_outgoing() =
        capture(content = poll(OUTGOING, "Ship it?", listOf("Yes", "No"), listOf(7, 4)))

    @Test fun noVotesYet_incoming() =
        capture(content = poll(INCOMING, counts = listOf(0, 0, 0)))

    @Test fun longQuestion_incoming() =
        capture(content = poll(INCOMING, "A noticeably longer poll question that has to wrap across more than one line"))
}
