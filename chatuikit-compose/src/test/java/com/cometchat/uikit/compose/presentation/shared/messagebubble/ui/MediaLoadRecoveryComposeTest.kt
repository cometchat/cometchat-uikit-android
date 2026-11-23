package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import coil.intercept.Interceptor
import coil.request.ErrorResult
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.testutils.MockFactory
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
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
import java.io.IOException

/**
 * ENG-41483 (D-325): a Compose media tile or bubble whose URL fails (an expired thumbnail link)
 * tries the full file URL once, then asks the SDK to download once, then keeps its error
 * placeholder (G9, BEH-MEDIA-13). Offline off and offline with no local copy keep today's
 * behaviour. Every image load fails here, and the URLs Coil was asked for are recorded.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class MediaLoadRecoveryComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var cometChat: MockedStatic<CometChat>
    private var connection = CometChatConstants.WS_STATE_CONNECTED
    private var downloads = 0
    private val requested = mutableListOf<String>()

    private val thumbUrl = "https://cdn.example.com/thumb_medium.jpg?sig=old"
    private val fileUrl = "https://cdn.example.com/media_1.jpg"
    private val videoUrl = "https://cdn.example.com/media_1.mp4"

    @Before
    fun setUp() {
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<String> { CometChat.getConnectionStatus() }.thenAnswer { connection }
        cometChat.`when`<Unit> { CometChat.downloadMedia(any(), any()) }.thenAnswer { downloads++; null }
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Every load fails at once, on the main thread, so the error callbacks run deterministically.
        Coil.setImageLoader(
            ImageLoader.Builder(context)
                .interceptorDispatcher(Dispatchers.Main.immediate)
                .fetcherDispatcher(Dispatchers.Main.immediate)
                .decoderDispatcher(Dispatchers.Main.immediate)
                .transformationDispatcher(Dispatchers.Main.immediate)
                .components {
                    add(Interceptor { chain ->
                        requested += chain.request.data.toString()
                        ErrorResult(null, chain.request, IOException("expired link"))
                    })
                }
                .build()
        )
    }

    @After
    fun tearDown() {
        Coil.reset()
        cometChat.close()
    }

    private fun offline(on: Boolean) {
        cometChat.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(on)
    }

    private fun MediaMessage.withThumbnail(): MediaMessage = apply {
        metadata = JSONObject().put(
            "@injected",
            JSONObject().put(
                "extensions",
                JSONObject().put("thumbnail-generation", JSONObject().put("url_medium", thumbUrl))
            )
        )
    }

    private fun image(thumbnail: Boolean = true): MediaMessage =
        MockFactory.createMediaMessage().let { if (thumbnail) it.withThumbnail() else it }

    private fun video(): MediaMessage = MockFactory.createMediaMessage(
        type = CometChatConstants.MESSAGE_TYPE_VIDEO, mimeType = "video/mp4", extension = "mp4"
    ).withThumbnail()

    private fun renderGrid(message: MediaMessage) = render {
        CometChatImagesBubble(message = message, alignment = UIKitConstants.MessageBubbleAlignment.LEFT)
    }

    private fun render(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent { CometChatTheme { content() } }
        composeRule.waitForIdle()
    }

    private fun distinctRequests() = requested.distinct()

    // ── grid tile ───────────────────────────────────────────────────────────

    @Test
    fun grid_thumbnailFailure_triesTheFileUrlOnce_thenDownloadsOnce() {
        offline(true)
        renderGrid(image())
        assertEquals(listOf(thumbUrl, fileUrl), distinctRequests())
        assertEquals(1, downloads)
    }

    @Test
    fun grid_noThumbnail_fileUrlFailure_downloadsOnce() {
        offline(true)
        renderGrid(image(thumbnail = false))
        assertEquals(listOf(fileUrl), distinctRequests())
        assertEquals(1, downloads)
    }

    @Test
    fun grid_aSecondFailure_makesNoFurtherCalls() {
        offline(true)
        renderGrid(image())
        composeRule.mainClock.advanceTimeBy(5_000)
        composeRule.waitForIdle()
        assertEquals(listOf(thumbUrl, fileUrl), distinctRequests())
        assertEquals(1, downloads)
    }

    @Test
    fun grid_offlineOff_todaysErrorPlaceholder_noFallback_noDownload() {
        offline(false)
        renderGrid(image())
        assertEquals(listOf(thumbUrl), distinctRequests())
        assertEquals(0, downloads)
    }

    @Test
    fun grid_offlineOn_disconnected_placeholder_noLoad_noDownload() {
        offline(true)
        connection = CometChatConstants.WS_STATE_DISCONNECTED
        renderGrid(image())
        assertEquals(emptyList<String>(), distinctRequests())
        assertEquals(0, downloads)
    }

    // ── single image bubble (already fell back to the file URL) ─────────────

    @Test
    fun singleImage_thumbnailThenFileUrlFailure_downloadsOnce() {
        offline(true)
        render { CometChatImageBubble(message = image(), alignment = UIKitConstants.MessageBubbleAlignment.LEFT) }
        assertEquals(listOf(thumbUrl, fileUrl), distinctRequests())
        assertEquals(1, downloads)
    }

    @Test
    fun singleImage_offlineOff_keepsTodaysFallback_noDownload() {
        offline(false)
        render { CometChatImageBubble(message = image(), alignment = UIKitConstants.MessageBubbleAlignment.LEFT) }
        assertEquals(listOf(thumbUrl, fileUrl), distinctRequests())
        assertEquals(0, downloads)
    }

    @Test
    fun singleImage_offlineOn_disconnected_placeholder_noDownload() {
        offline(true)
        connection = CometChatConstants.WS_STATE_DISCONNECTED
        render { CometChatImageBubble(message = image(), alignment = UIKitConstants.MessageBubbleAlignment.LEFT) }
        assertEquals(emptyList<String>(), distinctRequests())
        assertEquals(0, downloads)
    }

    // ── single video bubble (poster) ────────────────────────────────────────

    @Test
    fun singleVideo_posterThenFileUrlFailure_downloadsOnce() {
        offline(true)
        render { CometChatVideoBubble(message = video(), alignment = UIKitConstants.MessageBubbleAlignment.LEFT) }
        assertEquals(listOf(thumbUrl, videoUrl), distinctRequests())
        assertEquals(1, downloads)
    }

    @Test
    fun singleVideo_offlineOff_noDownload() {
        offline(false)
        render { CometChatVideoBubble(message = video(), alignment = UIKitConstants.MessageBubbleAlignment.LEFT) }
        assertEquals(0, downloads)
    }

    // ── video grid tile ─────────────────────────────────────────────────────

    @Test
    fun videoGrid_thumbnailFailure_fallsBackToTheFileFrame_thenDownloadsOnce() {
        offline(true)
        render { CometChatVideosBubble(message = video(), alignment = UIKitConstants.MessageBubbleAlignment.LEFT) }
        // The frame is read from the file URL off the main thread; it cannot decode here.
        composeRule.waitUntil(timeoutMillis = 5_000) { downloads > 0 }
        composeRule.waitForIdle()
        assertEquals(listOf(thumbUrl), distinctRequests())
        assertEquals(1, downloads)
    }

    @Test
    fun videoGrid_offlineOff_noDownload() {
        offline(false)
        render { CometChatVideosBubble(message = video(), alignment = UIKitConstants.MessageBubbleAlignment.LEFT) }
        composeRule.waitForIdle()
        assertEquals(0, downloads)
    }
}
