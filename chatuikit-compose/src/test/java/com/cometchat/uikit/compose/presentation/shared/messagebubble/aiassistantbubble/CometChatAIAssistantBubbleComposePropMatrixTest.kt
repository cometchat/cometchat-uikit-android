package com.cometchat.uikit.compose.presentation.shared.messagebubble.aiassistantbubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.models.AIAssistantMessage
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.domain.model.StreamMessage
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Property (prop-matrix) layer for [CometChatAIAssistantBubble].
 *
 * Four integrator params (`modifier` excluded by [Denominator]). The bubble renders
 * markdown through Markwon inside an `AndroidView`, so effects are read from the
 * Android view tree rather than Compose semantics.
 *
 * The `style` entry resolves the **real** `incoming()` factory inside a Compose
 * context and asserts a supplied `textColor` reaches the rendered body. The
 * pre-existing `CometChatAIAssistantBubbleStyleTest` cannot do either — it
 * hand-builds a style from `SIMULATED_*` constants and asserts those same constants
 * back, because the factories are `@Composable` and it runs without a Compose
 * context. That file therefore never exercises the factories it is named for.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatAIAssistantBubbleComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatAIAssistantBubble"
        const val STATIC_BODY = "a static assistant answer"
        const val STREAM_BODY = "a streamed assistant answer"
        const val CUSTOM_TEXT_COLOR = 0xFF1188AA.toInt()
    }

    /** The first TextView carrying text — where Markwon puts the body. */
    private fun firstNonEmptyTextView(): TextView? {
        var found: TextView? = null
        fun walk(v: View) {
            if (found != null) return
            if (v is TextView && v.text.isNotEmpty()) { found = v; return }
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(composeRule.activity.window.decorView)
        return found
    }

    private fun renderedText(): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(composeRule.activity.window.decorView)
        return out.toString()
    }

    @Test
    fun aiAssistantBubble_propMatrix_coversEveryObservableProp() {
        var useStream by mutableStateOf(false)
        var incoming: CometChatAIAssistantBubbleStyle? = null
        var outgoing: CometChatAIAssistantBubbleStyle? = null

        composeRule.setContent {
            CometChatTheme {
                // Resolve both factories in a real Compose context so the style entry
                // can assert on what they actually produce.
                incoming = CometChatAIAssistantBubbleStyle.incoming()
                outgoing = CometChatAIAssistantBubbleStyle.outgoing()
                CometChatAIAssistantBubble(
                    streamMessage = if (useStream) StreamMessage("r", "text", STREAM_BODY) else null,
                    aiAssistantMessage = AIAssistantMessage("r", "text", STATIC_BODY),
                    style = incoming!!.copy(textColor = androidx.compose.ui.graphics.Color(CUSTOM_TEXT_COLOR)),
                    aiStreamService = null,
                )
            }
        }

        val matrix = composePropMatrix(OWNER) {
            value("aiAssistantMessage") {
                composeRule.waitForIdle()
                assertTrue(
                    "the static body should reach a TextView; got: ${renderedText()}",
                    renderedText().contains(STATIC_BODY),
                )
            }
            // Supplying a stream message must take precedence over the static one —
            // observable because the static body stops rendering.
            value("streamMessage") {
                useStream = true
                composeRule.waitForIdle()
                assertTrue(
                    "the static body must give way to the stream",
                    !renderedText().contains(STATIC_BODY),
                )
                useStream = false
                composeRule.waitForIdle()
            }
            // A supplied style must change the rendering, not merely be accepted.
            // Markwon paints the body with style.textColor, so the resolved TextView
            // colour is the measurable effect.
            //
            // Note: incoming() and outgoing() resolve *identically* for this bubble.
            // That reads as deliberate — an assistant message is always from the other
            // party, so there is no outgoing variant to differ — rather than a defect,
            // so the assertion is on an explicitly supplied style instead.
            value("style") {
                composeRule.waitForIdle()
                val body = firstNonEmptyTextView()
                assertEquals(
                    "the supplied textColor should reach the rendered body",
                    CUSTOM_TEXT_COLOR,
                    body?.currentTextColor,
                )
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [aiassistant compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [aiassistant] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only aiStreamService is waived", 1, cov.waived)
    }

    /**
     * `aiStreamService` is dependency injection rather than integrator styling or
     * behaviour: without a live stream there is no event to drive, and substituting
     * a fake would assert the fake. The no-service path is covered in the functional
     * test instead, which pins that it composes rather than crashing.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "aiStreamService", PropKind.VALUE, waived = true),
    )
}
