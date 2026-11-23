package com.cometchat.uikit.kotlin.presentation.shared.messagebubble

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Looper
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaMessage
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.utils.MediaLoadRecovery
import com.cometchat.uikit.core.utils.MediaSource
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.imagebubble.CometChatImageBubble
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.imagesbubble.CometChatImagesBubble
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.videobubble.CometChatVideoBubble
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-41483 (D-325): a View-kit media tile or bubble whose URL fails (an expired thumbnail link)
 * tries the full file URL once, then asks the SDK to download once, then keeps its error
 * placeholder (G9, BEH-MEDIA-13). Offline off, offline with no local copy, and a local file keep
 * today's behaviour.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MediaLoadRecoveryViewsTest {

    private lateinit var cometChat: MockedStatic<CometChat>
    private var connection = CometChatConstants.WS_STATE_CONNECTED
    private var downloads = 0

    private val thumbUrl = "https://cdn/x/thumb_medium.jpg?sig=old"

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

    private fun image(): MediaMessage = MockFactory.createMediaMessage()

    private fun video(): MediaMessage = MockFactory.createMediaMessage(
        type = CometChatConstants.MESSAGE_TYPE_VIDEO, mimeType = "video/mp4", extension = "mp4"
    )

    private fun remoteRecovery(attachment: Attachment): MediaLoadRecovery =
        MediaLoadRecovery.forSource(MediaSource.Remote(attachment.fileUrl), attachment)!!

    private fun withActivity(block: (ComponentActivity) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(activity)
        }
        scenario.close()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> target(): Target<T> = Mockito.mock(Target::class.java) as Target<T>

    private fun <T : Any> RequestListener<T>.fail(model: String?) {
        onLoadFailed(null, model, target(), true)
        shadowOf(Looper.getMainLooper()).idle()
    }

    // ── the shared Glide hook ───────────────────────────────────────────────

    @Test
    fun listener_thumbnailFailure_reloadsTheFileUrlOnce_thenDownloadsOnce_thenStops() = withActivity { activity ->
        offline(true)
        val attachment = image().attachment
        val reloads = mutableListOf<String>()
        // An attached view, as a bound tile is: View.post runs once the view is in a window.
        val view = ImageView(activity).also { activity.setContentView(it) }
        val listener = MediaLoadRecoveryListener<Drawable>(view, remoteRecovery(attachment)) { reloads += it }

        listener.fail(thumbUrl)
        assertEquals("the thumbnail failure reloads the file URL", listOf(attachment.fileUrl), reloads)
        assertEquals(0, downloads)

        listener.fail(attachment.fileUrl)
        assertEquals("the file URL failure asks the SDK to download", 1, downloads)

        listener.fail(attachment.fileUrl)
        listener.fail(thumbUrl)
        assertEquals("a further failure makes no more calls", 1, reloads.size)
        assertEquals(1, downloads)
    }

    @Test
    fun listener_neverConsumesTheFailure_soTheErrorPlaceholderStillShows() = withActivity { activity ->
        offline(true)
        val listener = MediaLoadRecoveryListener<Drawable>(ImageView(activity), remoteRecovery(image().attachment)) { }
        assertFalse(listener.onLoadFailed(null, thumbUrl, target(), true))
    }

    @Test
    fun listener_disconnected_doesNotDownload() = withActivity { activity ->
        offline(true)
        val attachment = image().attachment
        val listener = MediaLoadRecoveryListener<Drawable>(ImageView(activity), remoteRecovery(attachment)) { }
        connection = CometChatConstants.WS_STATE_DISCONNECTED
        listener.fail(attachment.fileUrl)
        assertEquals(0, downloads)
    }

    // ── grid tile ───────────────────────────────────────────────────────────

    @Test
    fun gridTile_fileUrlFailure_downloadsOnce() = withActivity { activity ->
        offline(true)
        val bubble = CometChatImagesBubble(activity)
        val attachment = image().attachment
        val listener = bubble.tileRecoveryListener(ImageView(activity), remoteRecovery(attachment))
        listener.fail(thumbUrl)
        listener.fail(attachment.fileUrl)
        listener.fail(attachment.fileUrl)
        assertEquals(1, downloads)
    }

    // ── single image bubble ─────────────────────────────────────────────────

    @Test
    fun imageBubble_offlineOn_url_getsARecovery_andDownloadsOnce() = withActivity { activity ->
        offline(true)
        val bubble = CometChatImageBubble(activity)
        val message = image()
        bubble.setMessage(message)
        val recovery = assertNotNullAndGet(bubble.urlRecovery)
        val listener = bubble.bitmapRecoveryListener(ImageView(activity), ProgressBar(activity), recovery)
        // The bubble's own Glide load shares this recovery and may fail first in Robolectric, so
        // only the total is pinned here; the order is pinned by the listener tests above.
        listener.fail(thumbUrl)
        listener.fail(message.attachment.fileUrl)
        listener.fail(message.attachment.fileUrl)
        assertEquals("one download per bind, never more", 1, downloads)
    }

    @Test
    fun imageBubble_offlineOff_noRecovery_todaysErrorPlaceholder() = withActivity { activity ->
        offline(false)
        val bubble = CometChatImageBubble(activity)
        bubble.setMessage(image())
        assertNull(bubble.urlRecovery)
        assertEquals(0, downloads)
    }

    @Test
    fun imageBubble_offlineOn_disconnected_placeholder_noRecovery_noDownload() = withActivity { activity ->
        offline(true)
        connection = CometChatConstants.WS_STATE_DISCONNECTED
        val bubble = CometChatImageBubble(activity)
        bubble.setMessage(image())
        assertNull(bubble.urlRecovery)
        assertEquals(0, downloads)
    }

    // ── single video bubble (poster) ────────────────────────────────────────

    @Test
    fun videoBubble_offlineOn_url_posterFailure_downloadsOnce() = withActivity { activity ->
        offline(true)
        val bubble = CometChatVideoBubble(activity)
        val message = video()
        bubble.setMessage(message)
        val recovery = assertNotNullAndGet(bubble.urlRecovery)
        val listener: RequestListener<Bitmap> =
            bubble.posterRecoveryListener(ImageView(activity), ProgressBar(activity), ImageView(activity), recovery)
        // The bubble's own Glide load shares this recovery and may fail first in Robolectric, so
        // only the total is pinned here; the order is pinned by the listener tests above.
        listener.fail(thumbUrl)
        listener.fail(message.attachment.fileUrl)
        listener.fail(message.attachment.fileUrl)
        assertEquals("one download per bind, never more", 1, downloads)
    }

    @Test
    fun videoBubble_offlineOff_noRecovery() = withActivity { activity ->
        offline(false)
        val bubble = CometChatVideoBubble(activity)
        bubble.setMessage(video())
        assertNull(bubble.urlRecovery)
    }

    @Test
    fun videoBubble_offlineOn_disconnected_placeholder_noRecovery_noDownload() = withActivity { activity ->
        offline(true)
        connection = CometChatConstants.WS_STATE_DISCONNECTED
        val bubble = CometChatVideoBubble(activity)
        bubble.setMessage(video())
        assertNull(bubble.urlRecovery)
        assertEquals(0, downloads)
    }

    private fun <T> assertNotNullAndGet(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
