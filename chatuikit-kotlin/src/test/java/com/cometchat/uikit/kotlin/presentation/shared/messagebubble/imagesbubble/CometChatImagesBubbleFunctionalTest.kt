package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.imagesbubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.videosbubble.CometChatVideosBubble
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The bubble an image message actually reaches: the renderer picks it whenever
 * multiple attachments are enabled, which is the default, and the singular
 * `CometChatImageBubble` is the off path.
 *
 * Its own logic is the grid — how many tiles show and what the overflow badge says.
 * No network under Robolectric, so tiles carry placeholders and the geometry is the
 * thing worth pinning.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatImagesBubbleFunctionalTest {

    private companion object {
        /** The grid shows at most this many tiles; the last becomes an overflow badge. */
        const val MAX_VISIBLE = 4
    }

    private fun images(count: Int, caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(count = count, caption = caption)

    private fun videos(count: Int): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_VIDEO,
            mimeType = "video/mp4",
            extension = "mp4",
        )

    private fun withBubble(block: (CometChatImagesBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatImagesBubble(activity))
        }
        scenario.close()
    }

    private fun withVideos(block: (CometChatVideosBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatVideosBubble(activity))
        }
        scenario.close()
    }

    private fun View.visibleText(): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v.visibility != View.VISIBLE) return
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out.toString()
    }

    private fun View.countImageViews(): Int {
        var n = 0
        fun walk(v: View) {
            if (v is android.widget.ImageView) n++
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return n
    }

    // ── the grid ────────────────────────────────────────────────────────────

    @Test fun oneImage_rendersOneTile() = withBubble { bubble ->
        bubble.setMessage(images(1))
        assertTrue("expected at least one tile", bubble.countImageViews() >= 1)
    }

    @Test fun twoImages_renderTwoTiles() = withBubble { bubble ->
        bubble.setMessage(images(2))
        assertTrue(bubble.countImageViews() >= 2)
    }

    @Test fun fourImages_fillTheGridWithNoOverflowBadge() = withBubble { bubble ->
        bubble.setMessage(images(MAX_VISIBLE))
        assertFalse(
            "exactly four fills the grid, so nothing overflows",
            bubble.visibleText().contains("+"),
        )
    }

    @Test fun sevenImages_capAtFourWithAnOverflowBadge() = withBubble { bubble ->
        bubble.setMessage(images(7))
        assertTrue(
            "expected a +3 badge, got '${bubble.visibleText()}'",
            bubble.visibleText().contains("+3"),
        )
    }

    @Test fun fiveImages_showAPlusOneBadge() = withBubble { bubble ->
        bubble.setMessage(images(5))
        assertTrue(bubble.visibleText().contains("+1"))
    }

    @Test fun withNoAttachments_rendersNoTiles() = withBubble { bubble ->
        bubble.setMessage(images(0))
        assertFalse(bubble.visibleText().contains("+"))
    }

    // ── caption ─────────────────────────────────────────────────────────────

    @Test fun aCaptionRenders() = withBubble { bubble ->
        bubble.setMessage(images(2, caption = "two from the trip"))
        assertTrue(bubble.visibleText().contains("two from the trip"))
    }

    @Test fun rebindingWithoutACaptionDropsTheOldOne() = withBubble { bubble ->
        bubble.setMessage(images(2, caption = "two from the trip"))
        bubble.setMessage(images(2))
        assertFalse(bubble.visibleText().contains("two from the trip"))
    }

    // ── the timestamp-row padding rule ──────────────────────────────────────

    @Test fun hidingTheStatusRowIsAccepted() = withBubble { bubble ->
        bubble.setMessage(images(2))
        bubble.setStatusInfoVisible(false)
        assertTrue(bubble.countImageViews() >= 2)
    }

    // ── callbacks ───────────────────────────────────────────────────────────

    @Test fun theMediaClickListenerIsHeld() = withBubble { bubble ->
        var index: Int? = null
        bubble.setOnMediaClickListener { i, _ -> index = i }
        bubble.setMessage(images(2))
        assertEquals("nothing should fire until a tile is tapped", null, index)
    }

    @Test fun theMoreClickListenerIsHeld() = withBubble { bubble ->
        var list: List<Any>? = null
        bubble.setOnMoreClickListener { list = it }
        bubble.setMessage(images(7))
        assertEquals(null, list)
    }

    @Test fun textFormattersAreAcceptedBeforeBinding() = withBubble { bubble ->
        bubble.setTextFormatters(emptyList(), UIKitConstants.MessageBubbleAlignment.RIGHT)
        bubble.setMessage(images(1, caption = "with formatters"))
        assertTrue(bubble.visibleText().contains("with formatters"))
    }

    // ── the videos subclass ─────────────────────────────────────────────────

    @Test fun theVideosBubbleRendersTheSameGrid() = withVideos { bubble ->
        bubble.setMessage(videos(3))
        assertTrue(bubble.countImageViews() >= 3)
    }

    @Test fun theVideosBubbleAddsAPlayBadgePerTile() = withVideos { bubble ->
        // decorateTile adds a play glyph on top of each tile, so a video grid carries
        // more ImageViews than the same-sized image grid.
        bubble.setMessage(videos(2))
        val videoViews = bubble.countImageViews()
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        var imageViews = 0
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val plain = CometChatImagesBubble(activity)
            plain.setMessage(MockFactory.createMediaMessage(count = 2))
            imageViews = plain.countImageViews()
        }
        scenario.close()
        assertTrue(
            "a video grid should carry the extra play glyphs ($videoViews vs $imageViews)",
            videoViews > imageViews,
        )
    }

    @Test fun theVideosBubbleHonoursTheOverflowBadge() = withVideos { bubble ->
        bubble.setMessage(videos(7))
        assertTrue(bubble.visibleText().contains("+3"))
    }
}
