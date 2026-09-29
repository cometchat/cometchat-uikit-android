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
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaMessage
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
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

/**
 * Snapshot layer for the compose image message bubble.
 *
 * Captured the same way as the text bubble: **through [CometChatMessageBubble]**,
 * incoming and outgoing, light and dark. The image bubble draws no background of
 * its own — of its style's 23 properties it applies 9, and `backgroundColor` is not
 * one of them — so the fill, corner radius and side placement all come from the
 * container. Capturing bare leaves tiles floating on the page with no bubble behind
 * them, which is what these baselines exist to avoid.
 *
 * Composition locals are left at their defaults, so each message routes exactly as
 * the app routes it: `InternalContentRenderer` picks the multi-attachment grid for
 * image messages under the default `LocalEnableMultipleAttachments = true`. These
 * baselines therefore pin what a reader actually sees.
 *
 * Coil has no network under Robolectric and renders a deterministic placeholder,
 * which is what makes the grid geometry comparable.
 *
 * Run:
 *   ./gradlew :chatuikit-compose:recordRoborazziDebug  --tests "*.CometChatImageBubbleScreenshotTest"
 *   ./gradlew :chatuikit-compose:compareRoborazziDebug --tests "*.CometChatImageBubbleScreenshotTest"
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatImageBubbleScreenshotTest {

    /** The container renders a timestamp; pin the zone or the baseline encodes the host's. */
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
            outputDirectoryPath = "../screenshot-gallery/compose/imagebubble"
        )
    )

    private companion object {
        const val FIXED_SENT_AT = MockFactory.FIXED_SENT_AT
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

    /**
     * No `style` is passed, so the container resolves its own default per alignment —
     * that default fill is exactly what these baselines exist to pin.
     */
    /**
     * No `style` is passed, so the container resolves its own default per alignment —
     * that incoming/outgoing fill is exactly what these baselines exist to pin.
     */
    private fun bubble(
        count: Int,
        alignment: UIKitConstants.MessageBubbleAlignment,
        caption: String? = null,
    ): @Composable () -> Unit = {
        CometChatMessageBubble(
            message = MockFactory.createMediaMessage(count = count, caption = caption),
            alignment = alignment,
        )
    }

    // ── single image ────────────────────────────────────────────────────────

    @Test fun oneImage_incoming() = capture(content = bubble(1, INCOMING))

    @Test fun oneImage_outgoing() = capture(content = bubble(1, OUTGOING))

    @Test fun oneImage_incoming_dark() = capture(dark = true, content = bubble(1, INCOMING))

    @Test fun oneImage_outgoing_dark() = capture(dark = true, content = bubble(1, OUTGOING))

    // ── grids, both sides ───────────────────────────────────────────────────

    @Test fun twoImages_incoming() = capture(content = bubble(2, INCOMING))

    @Test fun twoImages_outgoing() = capture(content = bubble(2, OUTGOING))

    @Test fun threeImages_incoming() = capture(content = bubble(3, INCOMING))

    @Test fun threeImages_outgoing() = capture(content = bubble(3, OUTGOING))

    @Test fun fourImages_incoming_fillsWithoutOverflow() = capture(content = bubble(4, INCOMING))

    @Test fun fourImages_outgoing_fillsWithoutOverflow() = capture(content = bubble(4, OUTGOING))

    @Test fun sevenImages_incoming_capsAtFourWithOverflowBadge() = capture(content = bubble(7, INCOMING))

    @Test fun sevenImages_outgoing_capsAtFourWithOverflowBadge() = capture(content = bubble(7, OUTGOING))

    @Test fun fourImages_outgoing_dark() = capture(dark = true, content = bubble(4, OUTGOING))

    // ── caption, where the fill sits behind text too ────────────────────────

    @Test
    fun withCaption_incoming() = capture(content = bubble(3, INCOMING, caption = "three from the trip"))

    @Test
    fun withCaption_outgoing() = capture(content = bubble(3, OUTGOING, caption = "three from the trip"))
}
