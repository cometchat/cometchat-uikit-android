package com.cometchat.uikit.kotlin.presentation.shared.messagebubble.imagesbubble

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.utils.MediaSource
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.videosbubble.CometChatVideosBubble
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import java.io.File

/**
 * ENG-41462 (L2): the View kit's multi-attachment grids (images and videos, and one attachment
 * with multiple attachments on) load each tile from `MediaSourceResolver` (G9, BEH-MEDIA-13): the
 * local file, then the URL, then a placeholder that downloads on tap when online. Offline off, the
 * grid is today's.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MediaGridOfflineFunctionalTest {

    private lateinit var cometChat: MockedStatic<CometChat>
    private var connection = CometChatConstants.WS_STATE_DISCONNECTED
    private var downloads = 0

    @Before
    fun setUp() {
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<String> { CometChat.getConnectionStatus() }.thenAnswer { connection }
        cometChat.`when`<Unit> { CometChat.downloadMedia(any(), any()) }.thenAnswer { downloads++; null }
    }

    @After
    fun tearDown() = cometChat.close()

    private fun offline(on: Boolean) {
        cometChat.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(on)
    }

    private fun images(count: Int = 1): MediaMessage = MockFactory.createMediaMessage(count = count)

    private fun videos(count: Int = 1): MediaMessage = MockFactory.createMediaMessage(
        count = count,
        type = CometChatConstants.MESSAGE_TYPE_VIDEO,
        mimeType = "video/mp4",
        extension = "mp4",
    )

    private fun withBubble(video: Boolean = false, block: (CometChatImagesBubble) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(if (video) CometChatVideosBubble(activity) else CometChatImagesBubble(activity))
        }
        scenario.close()
    }

    private fun localFile(suffix: String): String =
        File.createTempFile("eng41462", suffix).apply { deleteOnExit() }.absolutePath

    private fun CometChatImagesBubble.placeholderTiles(): Int {
        var n = 0
        fun walk(v: View) {
            if (v.tag == CometChatImagesBubble.OFFLINE_PLACEHOLDER_TILE_TAG) n++
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return n
    }

    /** The grid is the bubble's first child; its children are the tiles. */
    private fun CometChatImagesBubble.tile(index: Int): View = (getChildAt(0) as ViewGroup).getChildAt(index)

    private fun View.text(): String {
        val out = StringBuilder()
        fun walk(v: View) {
            if (v is TextView) out.append(v.text).append(' ')
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(this)
        return out.toString()
    }

    // ── offline off: today's grid ───────────────────────────────────────────

    @Test
    fun offlineOff_disconnected_todaysTile_tapOpensTheViewer() = withBubble { bubble ->
        offline(false)
        var tapped = -1
        bubble.setOnMediaClickListener { i, _ -> tapped = i }
        bubble.setMessage(images())
        assertEquals(0, bubble.placeholderTiles())
        bubble.tile(0).performClick()
        assertEquals(0, tapped)
        assertEquals(0, downloads)
    }

    // ── offline on ──────────────────────────────────────────────────────────

    @Test
    fun offlineOn_localFile_noPlaceholder_tapOpensTheViewer() = withBubble { bubble ->
        offline(true)
        var tapped = -1
        bubble.setOnMediaClickListener { i, _ -> tapped = i }
        bubble.setMessage(images().apply { attachment.localPath = localFile(".jpg") })
        assertEquals(0, bubble.placeholderTiles())
        bubble.tile(0).performClick()
        assertEquals(0, tapped)
    }

    @Test
    fun offlineOn_connected_noLocalFile_usesTheUrl() = withBubble { bubble ->
        offline(true)
        connection = CometChatConstants.WS_STATE_CONNECTED
        bubble.setMessage(images())
        assertEquals(0, bubble.placeholderTiles())
    }

    @Test
    fun offlineOn_disconnected_noLocalFile_placeholder_tapDoesNotDownloadOrOpen() = withBubble { bubble ->
        offline(true)
        var tapped = -1
        bubble.setOnMediaClickListener { i, _ -> tapped = i }
        bubble.setMessage(images())
        assertEquals(1, bubble.placeholderTiles())
        assertTrue(bubble.text().contains("media_1.jpg"))
        bubble.tile(0).performClick()
        assertEquals(-1, tapped)
        assertEquals(0, downloads)
    }

    @Test
    fun offlineOn_placeholder_tapOnceOnline_downloads() = withBubble { bubble ->
        offline(true)
        bubble.setMessage(images())
        connection = CometChatConstants.WS_STATE_CONNECTED
        bubble.tile(0).performClick()
        assertEquals(1, downloads)
    }

    @Test
    fun offlineOn_reconnectRebind_swapsThePlaceholderForTheUrl() = withBubble { bubble ->
        offline(true)
        val msg = images()
        bubble.setMessage(msg)
        assertEquals(1, bubble.placeholderTiles())
        // The list rebinds media rows on reconnect (and on MediaReady); the tile re-resolves.
        connection = CometChatConstants.WS_STATE_CONNECTED
        bubble.setMessage(msg)
        assertEquals(0, bubble.placeholderTiles())
    }

    @Test
    fun offlineOn_multiImageGrid_disconnected_everyTileIsAPlaceholder() = withBubble { bubble ->
        offline(true)
        bubble.setMessage(images(count = 3))
        assertEquals(3, bubble.placeholderTiles())
    }

    @Test
    fun offlineOn_videoGrid_disconnected_placeholder_localFileIsNot() = withBubble(video = true) { bubble ->
        offline(true)
        bubble.setMessage(videos())
        assertEquals(1, bubble.placeholderTiles())
        bubble.setMessage(videos().apply { attachment.localPath = localFile(".mp4") })
        assertEquals(0, bubble.placeholderTiles())
    }

    // ── the tile's model ────────────────────────────────────────────────────

    @Test
    fun tileModel_localPathWins_overThumbnailAndUrl() {
        assertEquals(
            "/data/x.jpg",
            CometChatImagesBubble.gridTileModel(MediaSource.Local("/data/x.jpg"), 0, "https://t", "https://u")
        )
    }

    @Test
    fun tileModel_remoteOrOfflineOff_keepsTodaysOrder() {
        assertEquals("https://t", CometChatImagesBubble.gridTileModel(MediaSource.Remote("https://u"), 0, "https://t", "https://u"))
        assertEquals("https://t", CometChatImagesBubble.gridTileModel(null, 0, "https://t", "https://u"))
        assertEquals("https://u", CometChatImagesBubble.gridTileModel(null, 1, "https://t", "https://u"))
        assertNull(CometChatImagesBubble.gridTileModel(null, 1, null, null))
        assertNotNull(CometChatImagesBubble.gridTileModel(null, 0, null, "https://u"))
    }
}
