package com.cometchat.uikit.compose.presentation.shared.messagebubble.aiassistantbubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.models.AIAssistantMessage
import com.cometchat.uikit.core.domain.model.StreamMessage
import com.cometchat.uikit.compose.theme.CometChatTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Instrumented layer for [CometChatAIAssistantBubble].
 *
 * This bubble renders its markdown through Markwon inside an `AndroidView`, so the
 * body lands in a real `TextView` and Compose semantics never see it. That is why
 * the pre-existing `CometChatAIAssistantBubbleTest` extracted a pure state function
 * instead of rendering — but that function lives **in the test file itself**, so
 * those twelve assertions exercise a re-implementation and cover none of the
 * component. Hence 0% on a class that looked tested.
 *
 * These tests host the real composable and read the Android view tree underneath
 * it, which is where the rendered text actually is.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAIAssistantBubbleComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun assistantMessage(text: String) =
        AIAssistantMessage("receiver-1", "text", text)

    private fun streamMessage(text: String) =
        StreamMessage("receiver-1", "text", text)

    /** Every TextView under the activity — where Markwon puts the rendered markdown. */
    private fun renderedText(): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(composeRule.activity.window.decorView)
        return out.toString()
    }

    // ── static mode ─────────────────────────────────────────────────────────

    @Test
    fun staticMessage_rendersItsTextThroughMarkwon() {
        composeRule.setContent {
            CometChatTheme {
                CometChatAIAssistantBubble(aiAssistantMessage = assistantMessage("Here is an answer"))
            }
        }
        composeRule.waitForIdle()

        assertTrue(
            "the body should reach a TextView; got: ${renderedText()}",
            renderedText().contains("Here is an answer"),
        )
    }

    @Test
    fun staticMessage_stripsMarkdownMarkers() {
        composeRule.setContent {
            CometChatTheme {
                CometChatAIAssistantBubble(aiAssistantMessage = assistantMessage("a **bold** word"))
            }
        }
        composeRule.waitForIdle()

        val rendered = renderedText()
        assertTrue("markers should be consumed; got: $rendered", rendered.contains("a bold word"))
    }

    @Test
    fun staticMessage_withEmptyText_rendersWithoutCrashing() {
        composeRule.setContent {
            CometChatTheme {
                CometChatAIAssistantBubble(aiAssistantMessage = assistantMessage(""))
            }
        }
        composeRule.waitForIdle()

        // Nothing to assert beyond this: the test is that composing and
        // settling above did not throw. A constant assertion here would only
        // disguise that.
    }

    @Test
    fun staticMessage_rendersAFencedCodeBlock() {
        composeRule.setContent {
            CometChatTheme {
                CometChatAIAssistantBubble(
                    aiAssistantMessage = assistantMessage("before\n```\nval x = 1\n```\nafter"),
                )
            }
        }
        composeRule.waitForIdle()

        assertTrue(renderedText().contains("val x = 1"))
    }

    // ── streaming mode ──────────────────────────────────────────────────────

    @Test
    fun streamMessage_composesWithoutAStreamService() {
        // aiStreamService defaults to the singleton; passing null exercises the
        // no-service path, which must not crash before any event arrives.
        composeRule.setContent {
            CometChatTheme {
                CometChatAIAssistantBubble(
                    streamMessage = streamMessage("streaming body"),
                    aiStreamService = null,
                )
            }
        }
        composeRule.waitForIdle()

        // Nothing to assert beyond this: the test is that composing and
        // settling above did not throw. A constant assertion here would only
        // disguise that.
    }

    // ── mode selection ──────────────────────────────────────────────────────

    @Test
    fun neitherMessage_rendersNothing() {
        // Both params default to null; the `when` falls through with no branch.
        composeRule.setContent { CometChatTheme { CometChatAIAssistantBubble() } }
        composeRule.waitForIdle()

        assertEquals("nothing should be rendered", "", renderedText().trim())
    }

    @Test
    fun streamMessageWins_whenBothAreSupplied() {
        // The `when` checks streamMessage first, so the static body must not appear.
        composeRule.setContent {
            CometChatTheme {
                CometChatAIAssistantBubble(
                    streamMessage = streamMessage("from the stream"),
                    aiAssistantMessage = assistantMessage("from the static message"),
                    aiStreamService = null,
                )
            }
        }
        composeRule.waitForIdle()

        assertTrue(
            "the static body must not render when a stream message is present",
            !renderedText().contains("from the static message"),
        )
    }
}
