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
import com.cometchat.chat.models.AIAssistantMessage
import com.cometchat.uikit.compose.presentation.shared.messagebubble.ui.CometChatMessageBubble
import com.cometchat.uikit.core.constants.UIKitConstants
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
 * Snapshot layer for [CometChatAIAssistantBubble].
 *
 * Captured through [CometChatMessageBubble], the same as the text and image
 * bubbles, and taken **incoming and outgoing** so both bubble fills are pinned.
 * `InternalContentRenderer` routes `MessageCategory.AGENTIC` to this component, so
 * the container renders it exactly as the app does.
 *
 * Captured bare, the bubble paints only its own `style.backgroundColor` — which
 * defaults to a near-white that is invisible against a light canvas, so a direct
 * capture shows content floating with no bubble behind it.
 *
 * Markdown goes through Markwon into an `AndroidView`, which no semantics-based
 * assertion can see — making this the only layer that pins how it actually looks.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatAIAssistantBubbleScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatAIAssistantBubbleScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatAIAssistantBubbleScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/aiassistantbubble"
        )
    )

    private companion object {
        val INCOMING = UIKitConstants.MessageBubbleAlignment.LEFT
        val OUTGOING = UIKitConstants.MessageBubbleAlignment.RIGHT
        const val PLAIN = "Here is a straightforward answer."
        const val CODE = "Try this:\n```\nval x = 1\n```\nthat should work."
        const val BULLETS = "Options:\n- first\n- second\n- third"
        const val QUOTE = "> a quoted line\n\nand the reply"
        const val EMPHASIS = "a **bold** and _italic_ answer"
        const val LONG = "A noticeably longer assistant answer that has to wrap across several lines inside the bubble container."
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)
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

    /**
     * An assistant message, categorised so InternalContentRenderer routes it here.
     *
     * [inGroup] decides whether the container paints a fill. `CometChatMessageBubble`
     * forces `Color.Transparent` for AGENTIC/STREAM messages in 1:1 chats — the AI
     * bubble is meant to control its own appearance there, the same treatment
     * stickers get — and keeps the normal bubble background in groups.
     *
     * The content variants below therefore default to the **group** configuration:
     * a capture with no fill and no bubble bounds pins almost nothing, which is the
     * whole point of this layer. The 1:1 transparent branch is still covered, by the
     * explicitly named pair at the end.
     */
    private fun assistantMessage(body: String, inGroup: Boolean = true) =
        AIAssistantMessage(if (inGroup) "group-1" else "receiver-1", "text", body).apply {
            id = 1L
            sentAt = 1_729_011_360L
            category = UIKitConstants.MessageCategory.AGENTIC
            receiverType =
                if (inGroup) CometChatConstants.RECEIVER_TYPE_GROUP
                else CometChatConstants.RECEIVER_TYPE_USER
        }

    private fun bubble(
        body: String,
        alignment: UIKitConstants.MessageBubbleAlignment,
        inGroup: Boolean = true,
    ): @Composable () -> Unit = {
        CometChatMessageBubble(
            message = assistantMessage(body, inGroup),
            alignment = alignment,
        )
    }

    @Test fun plainAnswer_incoming() = capture(content = bubble(PLAIN, INCOMING))

    @Test fun plainAnswer_outgoing() = capture(content = bubble(PLAIN, OUTGOING))

    @Test fun plainAnswer_incoming_dark() = capture(dark = true, content = bubble(PLAIN, INCOMING))

    @Test fun plainAnswer_outgoing_dark() = capture(dark = true, content = bubble(PLAIN, OUTGOING))

    @Test fun inlineEmphasis_incoming() = capture(content = bubble(EMPHASIS, INCOMING))

    @Test fun inlineEmphasis_outgoing() = capture(content = bubble(EMPHASIS, OUTGOING))

    @Test fun codeBlock_incoming() = capture(content = bubble(CODE, INCOMING))

    @Test fun codeBlock_outgoing() = capture(content = bubble(CODE, OUTGOING))

    @Test fun codeBlock_incoming_dark() = capture(dark = true, content = bubble(CODE, INCOMING))

    @Test fun bulletList_incoming() = capture(content = bubble(BULLETS, INCOMING))

    @Test fun blockquote_incoming() = capture(content = bubble(QUOTE, INCOMING))

    @Test fun longAnswer_incoming_wraps() = capture(content = bubble(LONG, INCOMING))

    @Test fun emptyBody_incoming() = capture(content = bubble("", INCOMING))

    // ── the 1:1 branch, where the container deliberately paints no fill ─────
    // Kept deliberately: CometChatMessageBubble forces Color.Transparent for
    // AGENTIC/STREAM outside groups, so these two pin that real product path.
    // They are the only baselines here without a bubble background.

    @Test fun oneToOne_transparentByDesign_incoming() =
        capture(content = bubble(PLAIN, INCOMING, inGroup = false))

    @Test fun oneToOne_transparentByDesign_outgoing() =
        capture(content = bubble(PLAIN, OUTGOING, inGroup = false))
}
