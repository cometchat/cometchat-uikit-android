package com.cometchat.uikit.compose.presentation.imageviewer.ui

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import com.cometchat.uikit.compose.presentation.shared.messagebubble.InternalContentRenderer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * ENG-41482 (D-324, BEH-MEDIA-18) — Compose kit: the full-screen viewers open the local copy when
 * there is one, and Download and Share use it with no network call. With no local copy, and with
 * offline off, today's URL path runs unchanged. ENG-41635 (D-362, BEH-MEDIA-21): a path from the
 * message is never opened, downloaded or shared; only a file inside the SDK's media folder is.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class OfflineMediaViewerTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var cometChat: MockedStatic<CometChat>
    private val app: Context = ApplicationProvider.getApplicationContext()
    private lateinit var local: File
    private val bytes = byteArrayOf(7, 1, 2, 3)

    @Before
    fun setUp() {
        cometChat = Mockito.mockStatic(CometChat::class.java)
        // The SDK's media store: <noBackupFilesDir>/cometchat/media/<appId>/…, no extension. No
        // FileProvider path covers it, so nothing here may hand it to another app directly.
        local = File(app.noBackupFilesDir, "cometchat/media/app1/u1/att1").apply {
            parentFile!!.mkdirs()
            writeBytes(bytes)
        }
        // FileProvider caches each authority's roots statically; Robolectric gives every test new
        // data dirs, so drop the cache or later tests resolve against the first test's dirs.
        androidx.core.content.FileProvider::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && Map::class.java.isAssignableFrom(it.type) }
            .forEach { field -> field.isAccessible = true; (field.get(null) as MutableMap<*, *>).clear() }
    }

    @After
    fun tearDown() = cometChat.close()

    private fun offline(on: Boolean) {
        cometChat.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(on)
    }

    private fun attachment(url: String = URL, localPath: String? = local.absolutePath, mime: String = "image/jpeg") =
        Attachment().apply {
            fileUrl = url
            this.localPath = localPath
            fileName = "photo.jpg"
            fileMimeType = mime
        }

    private fun launched(open: (Activity) -> Unit): Intent? {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        open(activity)
        return shadowOf(activity).nextStartedActivity
    }

    private fun imageUrls(attachment: Attachment): List<String>? =
        launched { InternalContentRenderer.openImageViewer(it, listOf(attachment), 0) }
            ?.getStringArrayListExtra("extra_image_urls")

    private fun downloadRequests(): Int =
        shadowOf(app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).requestCount

    private fun publicDownload(name: String): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name)

    @Test
    fun `the image viewer gets the local copy when present`() {
        offline(true)
        assertEquals(listOf("file://${local.absolutePath}"), imageUrls(attachment()))
    }

    @Test
    fun `the video player gets the local copy when present`() {
        offline(true)
        val intent = launched {
            InternalContentRenderer.openVideoViewer(it, listOf(attachment(mime = "video/mp4")), 0)
        }
        assertEquals(listOf("file://${local.absolutePath}"), intent?.getStringArrayListExtra("extra_video_urls"))
    }

    @Test
    fun `no local copy - the viewer gets the URL`() {
        offline(true)
        assertEquals(listOf(URL), imageUrls(attachment(localPath = null)))
    }

    @Test
    fun `a queued own image opens with its local file`() {
        offline(true)
        assertEquals(listOf("file://${local.absolutePath}"), imageUrls(attachment(url = "")))
    }

    @Test
    fun `offline off - today's URL even with a local copy`() {
        offline(false)
        assertEquals(listOf(URL), imageUrls(attachment()))
    }

    @Test
    fun `offline off - a queued own image does nothing on tap, as today`() {
        offline(false)
        assertNull(imageUrls(attachment(url = "")))
    }

    @Test
    fun `Download of a local copy saves it with no network call and leaves the SDK file untouched`() {
        offline(true)
        val handled = ViewerLocalMedia.download(app, "file://${local.absolutePath}", "photo.jpg", "image/jpeg") { it() }

        assertTrue(handled)
        assertEquals(0, downloadRequests())
        assertArrayEquals(bytes, publicDownload("photo.jpg").readBytes())
        assertArrayEquals(bytes, local.readBytes())
    }

    @Test
    fun `Download and Share with no local copy fall through to today's network path`() {
        offline(true)
        assertFalse(ViewerLocalMedia.download(app, URL, "photo.jpg", "image/jpeg") { it() })
        assertFalse(ViewerLocalMedia.share(app, URL, "photo.jpg", "image/jpeg") { it() })
    }

    @Test
    fun `offline off - Download and Share never read a local file`() {
        offline(false)
        assertFalse(ViewerLocalMedia.download(app, "file://${local.absolutePath}", "photo.jpg", "image/jpeg") { it() })
        assertFalse(ViewerLocalMedia.share(app, "file://${local.absolutePath}", "photo.jpg", "image/jpeg") { it() })
        assertFalse(publicDownload("photo.jpg").exists())
    }

    @Test
    fun `Share of a local copy shares it through the kit FileProvider with no network call`() {
        offline(true)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        assertTrue(ViewerLocalMedia.share(activity, "file://${local.absolutePath}", "photo.jpg", "image/jpeg") { it() })
        shadowOf(Looper.getMainLooper()).idle()

        val chooser = shadowOf(activity).nextStartedActivity
        assertNotNull(chooser)
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        @Suppress("DEPRECATION")
        val stream = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", stream.scheme)
        assertEquals("${app.packageName}.cometchat.compose.provider", stream.authority)
        assertEquals("image/jpeg", send.type)
        assertEquals(0, downloadRequests())
        assertArrayEquals(bytes, local.readBytes())
    }

    private fun mediaMessage(attachment: Attachment, type: String = "file"): MediaMessage = mock {
        on { attachments } doReturn listOf(attachment)
        on { this.attachment } doReturn attachment
        on { this.type } doReturn type
        on { id } doReturn 42L
    }

    @Test
    fun `file or audio bubble Download with a local copy saves it with no network call`() {
        offline(true)
        InternalContentRenderer.downloadFileAttachment(app, mediaMessage(attachment()), 0)
        // The copy runs on a background thread; wait for it.
        val saved = publicDownload("photo.jpg")
        val deadline = System.currentTimeMillis() + 5_000
        while (!saved.exists() && System.currentTimeMillis() < deadline) Thread.sleep(20)

        assertEquals(0, downloadRequests())
        assertArrayEquals(bytes, saved.readBytes())
        assertArrayEquals(bytes, local.readBytes())
    }

    @Test
    fun `file or audio bubble Download with no local copy runs today's DownloadManager path`() {
        offline(true)
        InternalContentRenderer.downloadFileAttachment(app, mediaMessage(attachment(localPath = null)), 0)
        assertEquals(1, downloadRequests())
    }

    @Test
    fun `offline off - file or audio bubble Download runs today's DownloadManager path`() {
        offline(false)
        InternalContentRenderer.downloadFileAttachment(app, mediaMessage(attachment()), 0)
        assertEquals(1, downloadRequests())
        assertFalse(publicDownload("photo.jpg").exists())
    }

    @Test
    fun `menu Share with a local copy shares a cache copy through the kit FileProvider`() {
        offline(true)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val scope = TestScope()
        assertTrue(ViewerLocalMedia.shareAttachment(activity, attachment()) { it() })
        shadowOf(Looper.getMainLooper()).idle()
        assertSharedContentUri(shadowOf(activity).nextStartedActivity)
        // The menu entry point takes the same route and starts none of today's network work.
        com.cometchat.uikit.compose.presentation.messagelist.ui.shareMessage(
            activity, mediaMessage(attachment(), type = "image"), scope
        )
        assertEquals(0, scope.coroutineContext[Job]!!.children.count())
    }

    @Test
    fun `menu Share with no local copy, or offline off, runs today's share`() {
        listOf(true to attachment(localPath = null), false to attachment()).forEach { (on, att) ->
            offline(on)
            val scope = TestScope()
            com.cometchat.uikit.compose.presentation.messagelist.ui.shareMessage(
                app, mediaMessage(att, type = "image"), scope
            )
            // Today's image share: a Coil fetch launched in the list's scope.
            assertEquals(1, scope.coroutineContext[Job]!!.children.count())
            scope.cancel()
        }
    }

    /** Taps open on a file bubble bound to [attachment]; returns the intent started (waits for the copy thread). */
    private fun openFromFileBubble(attachment: Attachment): Intent? {
        // A PDF viewer, so `resolveActivity` finds a handler as on a device (content and https data).
        val component = android.content.ComponentName(app.packageName, "test.PdfViewer")
        shadowOf(app.packageManager).apply {
            addActivityIfNotPresent(component)
            addIntentFilterForActivity(component, android.content.IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addDataScheme("content")
                addDataScheme("https")
                addDataType("application/pdf")
            })
        }
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        InternalContentRenderer.openFileAttachment(activity, mediaMessage(attachment))
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            shadowOf(activity).nextStartedActivity?.let { return it }
            Thread.sleep(20)
        }
        return null
    }

    private fun pdf(localPath: String? = local.absolutePath) = attachment(localPath = localPath).apply {
        fileName = "report.pdf"
        fileMimeType = "application/pdf"
    }

    @Test
    fun `file bubble open with a local copy opens a cache copy through the kit FileProvider`() {
        offline(true)
        val intent = openFromFileBubble(pdf())!!
        assertEquals(Intent.ACTION_VIEW, intent.action)
        val uri = intent.data!!
        assertEquals("content", uri.scheme)
        assertEquals("${app.packageName}.cometchat.compose.provider", uri.authority)
        assertFalse("the SDK's no_backup file is never exposed", uri.toString().contains("no_backup"))
        assertTrue(uri.toString().endsWith("report.pdf"))
        assertEquals("application/pdf", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertArrayEquals(bytes, local.readBytes())
    }

    @Test
    fun `file bubble open with no local copy runs today's open on the URL`() {
        offline(true)
        val intent = openFromFileBubble(pdf(localPath = null))!!
        assertEquals(Uri.parse(URL), intent.data)
    }

    @Test
    fun `offline off - file bubble open runs today's open on the URL`() {
        offline(false)
        val intent = openFromFileBubble(pdf())!!
        assertEquals(Uri.parse(URL), intent.data)
    }

    // ---- ENG-41635 (D-362, BEH-MEDIA-21): only a file the SDK saved is opened, downloaded or shared ----

    /** An app-private file outside the SDK's media folder, named by a message's fileUrl. */
    private fun secret(): File = File(app.filesDir, "secret.db").apply {
        parentFile!!.mkdirs()
        writeBytes(byteArrayOf(9, 9, 9))
    }

    private fun online() {
        cometChat.`when`<String> { CometChat.getConnectionStatus() }
            .thenReturn(com.cometchat.chat.constants.CometChatConstants.WS_STATE_CONNECTED)
    }

    @Test
    fun `a local-path fileUrl with no SDK copy - the viewer is not opened on it`() {
        offline(true)
        val path = secret().absolutePath
        assertNull(imageUrls(attachment(url = "file://$path", localPath = null)))
        assertNull(imageUrls(attachment(url = path, localPath = null)))
    }

    @Test
    fun `viewer Download and Share of a file outside the media folder copy nothing`() {
        offline(true)
        val path = secret().absolutePath
        val traversal = "${local.parentFile!!.absolutePath}/../../../../../files/secret.db"
        for (source in listOf("file://$path", path, "file://$traversal")) {
            assertFalse(ViewerLocalMedia.download(app, source, "secret.db", null) { it() })
            assertFalse(ViewerLocalMedia.share(app, source, "secret.db", "*/*") { it() })
        }
        assertFalse(publicDownload("secret.db").exists())
    }

    @Test
    fun `bubble Download and menu Share with a local-path fileUrl and no SDK copy copy nothing`() {
        offline(true)
        val bad = attachment(url = "file://${secret().absolutePath}", localPath = null)
        assertFalse(ViewerLocalMedia.downloadAttachment(app, bad, "secret.db") { it() })
        assertFalse(ViewerLocalMedia.shareAttachment(app, bad) { it() })
        assertFalse(publicDownload("secret.db").exists())
    }

    @Test
    fun `a localPath outside the media folder is refused too (defence in depth)`() {
        offline(true)
        val bad = attachment(localPath = secret().absolutePath)
        assertFalse(ViewerLocalMedia.downloadAttachment(app, bad, "secret.db") { it() })
        assertFalse(ViewerLocalMedia.shareAttachment(app, bad) { it() })
    }

    @Test
    fun `file bubble open with a local-path fileUrl and no SDK copy opens nothing`() {
        offline(true)
        online()
        val bad = pdf(localPath = null).apply { fileUrl = "file://${secret().absolutePath}" }
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        InternalContentRenderer.openFileAttachment(activity, mediaMessage(bad))
        val deadline = System.currentTimeMillis() + 500
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            assertNull(shadowOf(activity).nextStartedActivity)
            Thread.sleep(20)
        }
    }

    @Test
    fun `menu Share of a local-path fileUrl with no SDK copy loads, inserts and shares nothing`() {
        offline(true)
        val url = "file://${secret().absolutePath}"
        listOf("image", "file").forEach { type ->
            val scope = TestScope()
            val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
            com.cometchat.uikit.compose.presentation.messagelist.ui.shareMessage(
                activity, mediaMessage(attachment(url = url, localPath = null), type = type), scope
            )
            // Today's share would launch a Coil load (image) or a download (file) in the list's scope.
            assertEquals(0, scope.coroutineContext[Job]!!.children.count())
            shadowOf(Looper.getMainLooper()).idle()
            assertNull(shadowOf(activity).nextStartedActivity)
        }
    }

    @Test
    fun `offline off - menu Share of a local-path fileUrl runs today's share`() {
        offline(false)
        val url = "file://${secret().absolutePath}"
        listOf("image", "file").forEach { type ->
            val scope = TestScope()
            com.cometchat.uikit.compose.presentation.messagelist.ui.shareMessage(
                app, mediaMessage(attachment(url = url, localPath = null), type = type), scope
            )
            assertEquals(1, scope.coroutineContext[Job]!!.children.count())
            scope.cancel()
        }
    }

    private fun assertSharedContentUri(chooser: Intent?) {
        assertNotNull(chooser)
        assertEquals(Intent.ACTION_CHOOSER, chooser!!.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        @Suppress("DEPRECATION")
        val stream = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", stream.scheme)
        assertEquals("${app.packageName}.cometchat.compose.provider", stream.authority)
        assertEquals("image/jpeg", send.type)
        assertEquals(0, downloadRequests())
        assertArrayEquals(bytes, local.readBytes())
    }

    @Test
    fun `offline with no local copy - the image viewer shows no endless spinner`() {
        offline(true)
        composeRule.setContent {
            CometChatImageViewerScreen(
                imageUrls = listOf(UNREACHABLE_URL),
                fileNames = listOf("photo.jpg"),
                mimeTypes = listOf("image/jpeg"),
                onBack = {},
                onShare = { _, _, _ -> }
            )
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private companion object {
        const val URL = "https://cdn.example/x/photo.jpg?sig=1"
        const val UNREACHABLE_URL = "http://127.0.0.1:1/photo.jpg"
    }
}
