package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.utils.MediaSource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config
import java.io.File

/**
 * ENG-41462 (L2): the multi-attachment grids (images and videos, and one attachment with
 * multiple attachments on) load each tile from `MediaSourceResolver` (G9, BEH-MEDIA-13): the
 * local file, then the URL, then a placeholder that downloads on tap when online. Offline off,
 * the grid is today's.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class MediaGridOfflineComposeFunctionalTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

    private fun message(type: String = CometChatConstants.MESSAGE_TYPE_IMAGE, count: Int = 1): MediaMessage =
        if (type == CometChatConstants.MESSAGE_TYPE_VIDEO) {
            MockFactory.createMediaMessage(count = count, type = type, mimeType = "video/mp4", extension = "mp4")
        } else {
            MockFactory.createMediaMessage(count = count, type = type)
        }

    private fun render(message: MediaMessage, onMediaClick: ((Int, Attachment) -> Unit)? = null) {
        composeRule.setContent {
            CometChatTheme {
                if (message.type == CometChatConstants.MESSAGE_TYPE_VIDEO) {
                    CometChatVideosBubble(
                        message = message,
                        alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                        onMediaClick = onMediaClick
                    )
                } else {
                    CometChatImagesBubble(
                        message = message,
                        alignment = UIKitConstants.MessageBubbleAlignment.LEFT,
                        onMediaClick = onMediaClick
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun placeholders(): Int =
        composeRule.onAllNodesWithTag(GRID_TILE_PLACEHOLDER_TAG, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun localFile(name: String): String =
        File.createTempFile("eng41462", name).apply { deleteOnExit() }.absolutePath

    // ── offline off: today's grid ───────────────────────────────────────────

    @Test
    fun offlineOff_disconnected_showsTodaysTile_andTapOpensTheViewer() {
        offline(false)
        var tapped = -1
        render(message()) { i, _ -> tapped = i }
        assertEquals(0, placeholders())
        composeRule.onNodeWithContentDescription("media_1.jpg").assertIsDisplayed().performClick()
        assertEquals(0, tapped)
        assertEquals(0, downloads)
    }

    // ── offline on ──────────────────────────────────────────────────────────

    @Test
    fun offlineOn_localFile_isShown_noPlaceholder_evenDisconnected() {
        offline(true)
        val msg = message().apply { attachment.localPath = localFile(".jpg") }
        var tapped = -1
        render(msg) { i, _ -> tapped = i }
        assertEquals(0, placeholders())
        composeRule.onNodeWithContentDescription("media_1.jpg").assertIsDisplayed().performClick()
        assertEquals("a local tile opens the viewer as today", 0, tapped)
    }

    @Test
    fun offlineOn_connected_noLocalFile_usesTheUrl() {
        offline(true)
        connection = CometChatConstants.WS_STATE_CONNECTED
        render(message())
        assertEquals(0, placeholders())
        composeRule.onNodeWithContentDescription("media_1.jpg").assertIsDisplayed()
    }

    @Test
    fun offlineOn_disconnected_noLocalFile_showsAPlaceholder_andTapDoesNotDownload() {
        offline(true)
        var tapped = -1
        render(message()) { i, _ -> tapped = i }
        assertEquals(1, placeholders())
        composeRule.onNodeWithTag(GRID_TILE_PLACEHOLDER_TAG, useUnmergedTree = true).performClick()
        assertEquals("a placeholder tile does not open the viewer", -1, tapped)
        assertEquals("offline, a tap does not download", 0, downloads)
    }

    @Test
    fun offlineOn_placeholder_tapOnceOnline_downloads() {
        offline(true)
        render(message())
        assertEquals(1, placeholders())
        connection = CometChatConstants.WS_STATE_CONNECTED
        composeRule.onNodeWithTag(GRID_TILE_PLACEHOLDER_TAG, useUnmergedTree = true).performClick()
        assertEquals(1, downloads)
    }

    @Test
    fun offlineOn_multiImageGrid_disconnected_everyTileIsAPlaceholder() {
        offline(true)
        render(message(count = 3))
        assertEquals(3, placeholders())
    }

    @Test
    fun offlineOn_videoGrid_disconnected_showsAPlaceholder() {
        offline(true)
        render(message(type = CometChatConstants.MESSAGE_TYPE_VIDEO))
        assertEquals(1, placeholders())
    }

    @Test
    fun offlineOn_mediaReady_swapsThePlaceholderForTheLocalFile() {
        offline(true)
        val before = message()
        var current by mutableStateOf(before)
        composeRule.setContent {
            CometChatTheme {
                CometChatImagesBubble(message = current, alignment = UIKitConstants.MessageBubbleAlignment.LEFT)
            }
        }
        composeRule.waitForIdle()
        assertEquals(1, placeholders())
        // MediaReady replaces the row (same id) with a copy whose attachment carries the local path.
        val after = message().apply { attachment.localPath = localFile(".jpg") }
        current = after
        composeRule.waitForIdle()
        assertEquals(0, placeholders())
    }

    // ── the tile's model ────────────────────────────────────────────────────

    @Test
    fun tileModel_localPathWins_overThumbnailAndUrl() {
        assertEquals("/data/x.jpg", gridTileImageModel(MediaSource.Local("/data/x.jpg"), "https://t", "https://u"))
    }

    @Test
    fun tileModel_remoteOrOfflineOff_keepsTodaysOrder() {
        assertEquals("https://t", gridTileImageModel(MediaSource.Remote("https://u"), "https://t", "https://u"))
        assertEquals("https://t", gridTileImageModel(null, "https://t", "https://u"))
        assertEquals("https://u", gridTileImageModel(null, null, "https://u"))
        assertEquals("", gridTileImageModel(null, null, null))
    }
}
