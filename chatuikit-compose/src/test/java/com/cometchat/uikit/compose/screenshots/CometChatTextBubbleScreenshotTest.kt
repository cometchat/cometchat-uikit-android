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
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatTextBubbleStyle
import com.cometchat.uikit.compose.presentation.shared.messagebubble.ui.CometChatMessageBubble
import com.cometchat.uikit.compose.presentation.shared.messagebubble.ui.CometChatTextBubble
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
 * Snapshot layer for
 * [com.cometchat.uikit.compose.presentation.shared.messagebubble.ui.CometChatTextBubble].
 *
 * The other three layers can assert *what* renders but not *how* it looks: Compose
 * exposes no semantics for colour, corner radius, or typography. Incoming vs
 * outgoing differ almost entirely in those properties, so without a baseline the
 * two are indistinguishable to a test. Each markdown segment kind gets its own
 * baseline too, since code blocks and blockquotes carry their own chrome.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatTextBubbleScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatTextBubbleScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatTextBubbleScreenshotTest {

    /**
     * The bubble renders its timestamp in the JVM's default zone. Nothing in the
     * build pins that, so a baseline recorded here (IST) would differ from one
     * compared in CI (UTC) by the width and glyphs of the time label. Pinned to
     * GMT for the life of the test so the capture is host-independent.
     */
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
            outputDirectoryPath = "../screenshot-gallery/compose/textbubble"
        )
    )

    private companion object {
        const val FIXED_SENT_AT = 1_729_011_360L
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)

    }

    private fun captureBubble(
        dark: Boolean = false,
        content: @Composable () -> Unit,
    ) {
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
     * The full bubble as a reader sees it: [CometChatMessageBubble] owns the fill,
     * corner radius, padding and left/right placement, and delegates the body to
     * [CometChatTextBubble]. Capturing the container is the only way a baseline can
     * pin incoming-vs-outgoing appearance — the text bubble alone draws just glyphs
     * and has no box of its own.
     */
    private fun messageBubble(
        text: String,
        alignment: UIKitConstants.MessageBubbleAlignment,
    ): @Composable () -> Unit = {
        CometChatMessageBubble(
            message = MockFactory.createTextMessage(text = text, sentAt = FIXED_SENT_AT),
            alignment = alignment,
        )
    }

    /** The bare content composable — used where the *text bubble itself* draws the chrome. */
    private fun bubble(
        text: String,
        alignment: UIKitConstants.MessageBubbleAlignment,
    ): @Composable () -> Unit = {
        CometChatTextBubble(
            message = MockFactory.createTextMessage(text = text, sentAt = FIXED_SENT_AT),
            alignment = alignment,
            style = when (alignment) {
                UIKitConstants.MessageBubbleAlignment.RIGHT -> CometChatTextBubbleStyle.outgoing()
                else -> CometChatTextBubbleStyle.incoming()
            },
        )
    }

    // ── incoming vs outgoing, both themes ───────────────────────────────────
    // The pair that no other layer can tell apart.

    @Test
    fun incoming_light() =
        captureBubble(content = messageBubble("Hello from the other side", UIKitConstants.MessageBubbleAlignment.LEFT))

    @Test
    fun incoming_dark() =
        captureBubble(dark = true, content = messageBubble("Hello from the other side", UIKitConstants.MessageBubbleAlignment.LEFT))

    @Test
    fun outgoing_light() =
        captureBubble(content = messageBubble("Hello from me", UIKitConstants.MessageBubbleAlignment.RIGHT))

    @Test
    fun outgoing_dark() =
        captureBubble(dark = true, content = messageBubble("Hello from me", UIKitConstants.MessageBubbleAlignment.RIGHT))

    // ── markdown segment chrome ─────────────────────────────────────────────

    @Test
    fun inlineEmphasis_light() =
        captureBubble(content = bubble("a **bold** and _italic_ word", UIKitConstants.MessageBubbleAlignment.LEFT))

    @Test
    fun inlineCode_light() =
        captureBubble(content = bubble("call `buildPositionMap()` first", UIKitConstants.MessageBubbleAlignment.LEFT))

    @Test
    fun codeBlock_light() =
        captureBubble(content = bubble("before\n```\nval x = 1\n```\nafter", UIKitConstants.MessageBubbleAlignment.LEFT))

    @Test
    fun codeBlock_dark() =
        captureBubble(dark = true, content = bubble("before\n```\nval x = 1\n```\nafter", UIKitConstants.MessageBubbleAlignment.LEFT))

    @Test
    fun blockquote_light() =
        captureBubble(content = bubble("> a quoted line\nand a reply", UIKitConstants.MessageBubbleAlignment.LEFT))

    @Test
    fun markdownLink_light() =
        captureBubble(content = bubble("see [CometChat](https://cometchat.com) for docs", UIKitConstants.MessageBubbleAlignment.LEFT))
}
