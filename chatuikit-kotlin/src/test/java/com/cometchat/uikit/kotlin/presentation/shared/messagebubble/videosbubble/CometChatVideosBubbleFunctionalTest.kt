package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.videosbubble

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.imagesbubble.CometChatImagesBubble
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Functional layer for the **View** [CometChatVideosBubble] — the bubble a video message
 * actually reaches, since the renderer picks it whenever multiple attachments are enabled
 * (the default) and the singular `CometChatVideoBubble` is the off path.
 *
 * The grid geometry is [CometChatImagesBubble]'s and is pinned in its own functional test.
 * What belongs to *this* bubble is `decorateTile`: a play badge and a duration pill per
 * tile. Both are worth their own assertions because neither ever carries a thumbnail here —
 * off-network (and on a real device for the first moment of every tile) `MediaMetadataRetriever`
 * returns nothing, so the badge is the only thing distinguishing a video from a grey square,
 * and the duration pill stays hidden.
 *
 * Two places where the View bubble deliberately differs from its Compose twin, both pinned
 * below so a future alignment shows up as a failing test rather than a silent baseline shift:
 *
 *  - **The overflow tile keeps its play badge.** `createTile` calls `decorateTile` before it
 *    adds the "+N" scrim, so a seven-video message draws four tiles and *four* badges. The
 *    Compose grid draws three — its fourth tile trades the badge for the count.
 *  - **The badge is invisible to accessibility.** The Compose tile announces "Play"; the View
 *    play glyph has no content description, so these tests locate it structurally.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatVideosBubbleFunctionalTest {

    private companion object {
        const val VIDEO_MIME = "video/mp4"
        const val VIDEO_EXT = "mp4"

        /** The grid draws at most this many tiles; the rest become the "+N" badge. */
        const val MAX_VISIBLE = 4
    }

    private fun videos(count: Int, caption: String? = null): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_VIDEO,
            mimeType = VIDEO_MIME,
            extension = VIDEO_EXT,
            caption = caption,
        )

    /** A video message whose attachments are images — the server-sent mixed payload. */
    private fun mismatched(count: Int): MediaMessage =
        MockFactory.createMediaMessage(
            count = count,
            type = CometChatConstants.MESSAGE_TYPE_VIDEO,
            mimeType = "image/jpeg",
            extension = "jpg",
        )

    private fun withBubble(block: (CometChatVideosBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatVideosBubble(activity))
        }
        scenario.close()
    }

    private fun View.descendants(): List<View> {
        val out = mutableListOf<View>()
        fun walk(v: View) {
            out += v
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out
    }

    /**
     * The play badge is the only `GradientDrawable`-backed `FrameLayout` in the tree —
     * `decorateTile` builds it as an oval scrim, and nothing else in the bubble uses one.
     * The glyph itself carries no content description, so there is nothing to match on.
     */
    private fun View.playBadges(): List<FrameLayout> = descendants()
        .filterIsInstance<FrameLayout>()
        .filter { (it.background as? GradientDrawable)?.shape == GradientDrawable.OVAL }

    private fun View.tiles(): List<MaterialCardView> = descendants().filterIsInstance<MaterialCardView>()

    /** Duration pills and the "+N" scrim are the only TextViews parented by a tile. */
    private fun View.tileLabels(): List<TextView> = descendants()
        .filterIsInstance<TextView>()
        .filter { it.parent is MaterialCardView }

    private fun View.durationPills(): List<TextView> = tileLabels().filter { it.text.isNullOrEmpty() }

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

    // ── the play badge ──────────────────────────────────────────────────────

    @Test fun oneVideo_carriesAPlayBadge() = withBubble { bubble ->
        bubble.setMessage(videos(1))
        assertEquals(1, bubble.playBadges().size)
    }

    @Test fun everyTileOfAnUnderFilledGridCarriesItsOwnBadge() = withBubble { bubble ->
        bubble.setMessage(videos(3))
        assertEquals(3, bubble.playBadges().size)
    }

    @Test fun aFullGridCarriesFourBadges() = withBubble { bubble ->
        bubble.setMessage(videos(MAX_VISIBLE))
        assertEquals(MAX_VISIBLE, bubble.playBadges().size)
    }

    @Test fun thePlayBadgeHoldsTheVideoGlyph() = withBubble { bubble ->
        bubble.setMessage(videos(1))
        val badge = bubble.playBadges().single()
        assertEquals("the badge wraps exactly one glyph", 1, badge.childCount)
        assertTrue("the glyph should be an ImageView", badge.getChildAt(0) is ImageView)
    }

    /**
     * A plain images bubble runs the same grid with `decorateTile` as a no-op, so nothing
     * there should look like a play badge. This is the assertion that would catch the
     * subclass hook being lifted into the parent.
     */
    @Test fun theImagesBubbleDrawsNoPlayBadges() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val plain = CometChatImagesBubble(activity)
            plain.setMessage(MockFactory.createMediaMessage(count = 3))
            assertEquals(0, plain.playBadges().size)
        }
        scenario.close()
    }

    // ── where the badge and the overflow rule meet ──────────────────────────

    /**
     * Divergence from Compose, pinned deliberately: `createTile` decorates first and adds
     * the "+N" scrim afterwards, so the overflow tile shows the count *over* a play badge
     * rather than instead of one. The Compose grid reports three badges for this message.
     */
    @Test fun theOverflowTileKeepsItsBadgeUnderTheCount() = withBubble { bubble ->
        bubble.setMessage(videos(7))
        assertEquals(
            "the View overflow tile decorates before it overlays, so the badge survives",
            MAX_VISIBLE,
            bubble.playBadges().size,
        )
        assertTrue(
            "expected a +3 badge, got '${bubble.visibleText()}'",
            bubble.visibleText().contains("+3"),
        )
    }

    @Test fun fiveVideos_showFourTilesAndAPlusOne() = withBubble { bubble ->
        bubble.setMessage(videos(5))
        assertEquals(MAX_VISIBLE, bubble.tiles().size)
        assertTrue(bubble.visibleText().contains("+1"))
    }

    @Test fun exactlyFourVideosOverflowIntoNothing() = withBubble { bubble ->
        bubble.setMessage(videos(MAX_VISIBLE))
        assertFalse(
            "four fills the grid, so nothing overflows",
            bubble.visibleText().contains("+"),
        )
    }

    // ── the duration pill ───────────────────────────────────────────────────

    /**
     * The pill is built up front and populated from a background thread. Without a real
     * file there is no metadata to read, so it must stay `GONE` rather than flashing an
     * empty black chip — which is exactly what the committed baselines show.
     */
    @Test fun theDurationPillIsBuiltButStaysHiddenWithoutMetadata() = withBubble { bubble ->
        bubble.setMessage(videos(2))
        val pills = bubble.durationPills()
        assertEquals("one pill per tile", 2, pills.size)
        pills.forEach { assertEquals(View.GONE, it.visibility) }
    }

    @Test fun aHiddenDurationPillContributesNoText() = withBubble { bubble ->
        bubble.setMessage(videos(2))
        assertEquals("", bubble.visibleText().trim())
    }

    // ── the kind-mismatched attachment ──────────────────────────────────────

    /**
     * A video message carrying image attachments renders broken tiles — placeholder glyph,
     * no play badge, no duration pill — rather than promising a video that will not play.
     */
    @Test fun imageAttachmentsInAVideoMessageRenderAsBrokenTiles() = withBubble { bubble ->
        bubble.setMessage(mismatched(2))
        assertEquals("a broken tile is not decorated", 0, bubble.playBadges().size)
        assertEquals(0, bubble.durationPills().size)
        assertEquals("the tiles are still laid out", 2, bubble.tiles().size)
    }

    @Test fun aBrokenTileStillCarriesAGlyph() = withBubble { bubble ->
        bubble.setMessage(mismatched(1))
        val tile = bubble.tiles().single()
        assertTrue(
            "the unsupported-file icon stands in for the missing frame",
            tile.descendants().any { it is ImageView },
        )
    }

    // ── the grid, caption and edited label ──────────────────────────────────

    @Test fun withNoAttachments_rendersNoTiles() = withBubble { bubble ->
        bubble.setMessage(videos(0))
        assertEquals(0, bubble.tiles().size)
        assertEquals(0, bubble.playBadges().size)
    }

    @Test fun withNoAttachments_aCaptionStillRenders() = withBubble { bubble ->
        bubble.setMessage(videos(0, caption = "the upload failed"))
        assertTrue(bubble.visibleText().contains("the upload failed"))
    }

    @Test fun aCaptionRendersBeneathTheGrid() = withBubble { bubble ->
        bubble.setMessage(videos(2, caption = "both takes"))
        assertTrue(bubble.visibleText().contains("both takes"))
    }

    @Test fun rebindingWithoutACaptionDropsTheOldOne() = withBubble { bubble ->
        bubble.setMessage(videos(2, caption = "both takes"))
        bubble.setMessage(videos(2))
        assertFalse(bubble.visibleText().contains("both takes"))
    }

    /**
     * Recycling is the real path in a message list, and the badge count is the thing that
     * would drift if `populateGrid` ever stopped clearing the grid first.
     */
    @Test fun rebindingWithFewerVideosDropsTheStaleBadges() = withBubble { bubble ->
        bubble.setMessage(videos(MAX_VISIBLE))
        assertEquals(MAX_VISIBLE, bubble.playBadges().size)
        bubble.setMessage(videos(1))
        assertEquals("a recycled bubble must not keep the old tiles", 1, bubble.playBadges().size)
    }

    @Test fun textFormattersAreAcceptedBeforeBinding() = withBubble { bubble ->
        bubble.setTextFormatters(emptyList(), UIKitConstants.MessageBubbleAlignment.RIGHT)
        bubble.setMessage(videos(1, caption = "with formatters"))
        assertTrue(bubble.visibleText().contains("with formatters"))
    }

    // ── the timestamp-row padding rule ──────────────────────────────────────

    /**
     * The bubble pads its own bottom only when the timestamp row below is hidden, and a
     * recycled bubble has to forget that padding on the next bind — `setMessage` restores
     * the default before the adapter re-hides.
     */
    @Test fun hidingTheStatusRowPadsTheBottomAndRebindingRestoresIt() = withBubble { bubble ->
        bubble.setMessage(videos(2))
        val padded = bubble.paddingBottom
        bubble.setStatusInfoVisible(false)
        assertTrue(
            "hiding the row should add bottom padding (was $padded, now ${bubble.paddingBottom})",
            bubble.paddingBottom > padded,
        )
        bubble.setMessage(videos(2))
        assertEquals("the next bind restores the default", padded, bubble.paddingBottom)
    }

    // ── callbacks ───────────────────────────────────────────────────────────

    @Test fun tappingATileReportsItsIndexAndAttachment() = withBubble { bubble ->
        var index: Int? = null
        var attachment: Attachment? = null
        bubble.setOnMediaClickListener { i, a -> index = i; attachment = a }
        bubble.setMessage(videos(3))

        bubble.tiles()[1].performClick()

        assertEquals("the second tile is index 1", 1, index)
        assertEquals("media_2.$VIDEO_EXT", attachment?.fileName)
    }

    @Test fun theOverflowTileFiresOnMoreClickRatherThanOnMediaClick() = withBubble { bubble ->
        var mediaIndex: Int? = null
        var all: List<Attachment>? = null
        bubble.setOnMediaClickListener { i, _ -> mediaIndex = i }
        bubble.setOnMoreClickListener { all = it }
        bubble.setMessage(videos(7))

        bubble.tiles()[MAX_VISIBLE - 1].performClick()

        assertNull("the overflow tile must not report itself as a media tap", mediaIndex)
        assertEquals("onMoreClick receives every attachment", 7, all?.size)
    }

    @Test fun aBrokenTileStillReportsAMediaTap() = withBubble { bubble ->
        var index: Int? = null
        bubble.setOnMediaClickListener { i, _ -> index = i }
        bubble.setMessage(mismatched(2))

        bubble.tiles()[0].performClick()

        assertEquals(
            "a broken tile routes to the viewer's no-preview page like any other",
            0,
            index,
        )
    }

    @Test fun everyTileIsLongClickable() = withBubble { bubble ->
        bubble.setMessage(videos(2))
        bubble.tiles().forEach {
            assertTrue("a tile must forward the long press to the row", it.isLongClickable)
        }
    }

    @Test fun nothingFiresUntilATileIsTapped() = withBubble { bubble ->
        var index: Int? = null
        var all: List<Attachment>? = null
        bubble.setOnMediaClickListener { i, _ -> index = i }
        bubble.setOnMoreClickListener { all = it }
        bubble.setMessage(videos(7))

        assertNull(index)
        assertNull(all)
    }

    @Test fun aTapWithNoListenerAttachedIsHarmless() = withBubble { bubble ->
        bubble.setMessage(videos(2))
        bubble.tiles().forEach { it.performClick() }
        assertNotNull("the bubble should survive an unwired tap", bubble.tiles().firstOrNull())
    }
}
