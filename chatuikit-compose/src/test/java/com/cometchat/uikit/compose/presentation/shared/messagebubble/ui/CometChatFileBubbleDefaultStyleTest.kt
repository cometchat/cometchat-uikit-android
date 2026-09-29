package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatFileBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins a defect: [CometChatFileBubble] cannot be called with its own default style.
 *
 * `CometChatFileBubbleStyle.incoming()` leaves `cornerRadius` as an unset sentinel
 * (`Dp.Unspecified`) for `InternalContentRenderer` to fill during the style merge, but
 * that same factory is the composable's declared default argument, and the bubble
 * feeds the value straight into `RoundedCornerShape`. So the message list is fine and
 * an integrator calling the public composable directly is not.
 *
 * These tests assert the behaviour that exists today. When the sentinel is given a
 * fallback they will fail, which is the point — that is the signal to delete them.
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
    fun theStyleFactoriesLeaveCornerRadiusUnset() {
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
        assertEquals("the inner radius is a real value; only the outer one is a sentinel", 2.dp, incoming!!.innerCornerRadius)
    }

    @Test
    fun theDefaultStyleThrowsOnFirstRender() {
        val result = runCatching {
            composeRule.setContent {
                CometChatTheme {
                    CometChatFileBubble(
                        message = fileMessage(1),
                        alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    )
                }
            }
            composeRule.waitForIdle()
        }
        val cause = generateSequence(result.exceptionOrNull()) { it.cause }
            .firstOrNull { it is IllegalArgumentException }
        assertTrue(
            "expected a corner-size failure, got ${result.exceptionOrNull()}",
            cause?.message?.contains("Corner size") == true,
        )
    }

    @Test
    fun aResolvedRadiusRendersAndDrawsCleanly() {
        // The same call with the radius the renderer would have merged in.
        composeRule.setContent {
            CometChatTheme {
                CometChatFileBubble(
                    message = fileMessage(2),
                    alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                    style = CometChatFileBubbleStyle.incoming().copy(cornerRadius = 12.dp),
                )
            }
        }
        composeRule.waitForIdle()
        draw()
    }
}
