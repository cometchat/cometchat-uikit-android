package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatFileBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regression coverage for ENG-39093.
 *
 * `CometChatFileBubbleStyle.incoming()` / `.outgoing()` leave `cornerRadius` as an unset
 * sentinel (`Dp.Unspecified`) for the message-list style merge to fill. That same factory is
 * the composable's declared default argument, and the bubble used to feed the sentinel straight
 * into `RoundedCornerShape`, which throws on first draw. Two things now guard against that:
 *
 * - [CometChatFileBubble] falls back to the message-bubble default radius when the value is
 *   unspecified, so a direct call with the default style renders.
 * - `InternalContentRenderer` always merges the per-type style with a base message-bubble
 *   style, so [CometChatMessageBubble] with no explicit style and multi-attachments disabled
 *   renders too.
 *
 * The factories still return the sentinel on purpose: that is what lets the merge tell an
 * explicit override from "inherit from the base style".
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatFileBubbleDefaultStyleTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun fileMessage(count: Int) = MockFactory.createMediaMessage(
        count = count,
        type = CometChatConstants.MESSAGE_TYPE_FILE,
        mimeType = "application/pdf",
        extension = "pdf",
    )

    private fun draw() {
        val v = composeRule.activity.window.decorView
        val b = Bitmap.createBitmap(v.width.coerceAtLeast(1), v.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        v.draw(Canvas(b))
    }

    @Test
    fun theStyleFactoriesStillLeaveCornerRadiusUnsetForTheMerge() {
        var incoming: CometChatFileBubbleStyle? = null
        var outgoing: CometChatFileBubbleStyle? = null
        composeRule.setContent {
            CometChatTheme {
                incoming = CometChatFileBubbleStyle.incoming()
                outgoing = CometChatFileBubbleStyle.outgoing()
            }
        }
        composeRule.waitForIdle()
        assertEquals(Dp.Unspecified, incoming!!.cornerRadius)
        assertEquals(Dp.Unspecified, outgoing!!.cornerRadius)
    }

    @Test
    fun aSingleFileWithTheDefaultStyleRendersAndDraws() {
        composeRule.setContent {
            CometChatTheme {
                CometChatFileBubble(
                    message = fileMessage(1),
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                )
            }
        }
        composeRule.waitForIdle()
        draw()
    }

    @Test
    fun aMultiFilePayloadWithTheDefaultStyleRendersAndDraws() {
        // Exercises getItemCornerRadius, which mixes the outer radius with the inner one.
        composeRule.setContent {
            CometChatTheme {
                CometChatFileBubble(
                    message = fileMessage(3),
                    alignment = UIKitConstants.MessageBubbleAlignment.RIGHT,
                )
            }
        }
        composeRule.waitForIdle()
        draw()
    }

    @Test
    fun theMessageBubbleWithNoBaseStyleAndMultiAttachmentsOffRendersAndDraws() {
        // The ticket's original repro: no messageBubbleStyle, so the renderer used to skip the
        // merge and hand the bubble the raw sentinel.
        composeRule.setContent {
            CometChatTheme {
                CompositionLocalProvider(LocalEnableMultipleAttachments provides false) {
                    CometChatMessageBubble(
                        message = fileMessage(1),
                        alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        draw()
    }
}
