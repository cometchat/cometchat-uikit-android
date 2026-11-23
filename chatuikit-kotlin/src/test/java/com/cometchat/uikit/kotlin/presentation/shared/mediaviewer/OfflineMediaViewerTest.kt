package com.cometchat.uikit.kotlin.presentation.shared.mediaviewer

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Looper
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Attachment
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.shared.messagebubble.InternalContentRenderer
import com.cometchat.uikit.kotlin.shared.resources.utils.MediaUtils
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/**
 * ENG-41482 (D-324, BEH-MEDIA-18) — View kit: the full-screen viewers open the local copy when there
 * is one, and Download and Share use it with no network call. With no local copy, and with offline
 * off, today's URL path runs unchanged. ENG-41635 (D-362, BEH-MEDIA-21): a path from the message is
 * never opened, downloaded or shared; only a file inside the SDK's media folder is.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OfflineMediaViewerTest {

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

    private fun attachment(url: String = URL, localPath: String? = local.absolutePath) = Attachment().apply {
        fileUrl = url
        this.localPath = localPath
        fileName = "photo.jpg"
        fileMimeType = "image/jpeg"
    }

    private fun launchedImageUrls(attachment: Attachment): List<String> {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        InternalContentRenderer.openImagesPreview(activity, listOf(attachment), 0)
        val intent = shadowOf(activity).nextStartedActivity
        assertNotNull(intent)
        @Suppress("UNCHECKED_CAST")
        return intent.getSerializableExtra("ARGS_IMAGE_URLS") as List<String>
    }

    private fun downloadRequests(): Int =
        shadowOf(app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).requestCount

    private fun publicDownload(name: String): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name)

    @Test
    fun `the viewer gets the local copy when present`() {
        offline(true)
        assertEquals(listOf("file://${local.absolutePath}"), launchedImageUrls(attachment()))
    }

    @Test
    fun `no local copy - the viewer gets the URL`() {
        offline(true)
        assertEquals(listOf(URL), launchedImageUrls(attachment(localPath = null)))
    }

    @Test
    fun `a queued own image opens with its local file`() {
        offline(true)
        assertEquals(listOf("file://${local.absolutePath}"), launchedImageUrls(attachment(url = "")))
    }

    @Test
    fun `offline off - today's URL even with a local copy`() {
        offline(false)
        assertEquals(listOf(URL), launchedImageUrls(attachment()))
    }

    @Test
    fun `Download of a local copy saves it with no network call and leaves the SDK file untouched`() {
        offline(true)
        MediaUtils.downloadViewerSource(app, "file://${local.absolutePath}", "photo.jpg", "image/jpeg") { it() }

        assertEquals(0, downloadRequests())
        assertArrayEquals(bytes, publicDownload("photo.jpg").readBytes())
        assertArrayEquals(bytes, local.readBytes())
    }

    @Test
    fun `Download with no local copy runs today's DownloadManager path`() {
        offline(true)
        MediaUtils.downloadViewerSource(app, URL, "photo.jpg", "image/jpeg") { it() }
        assertEquals(1, downloadRequests())
    }

    @Test
    fun `offline off - Download never reads a local file`() {
        offline(false)
        MediaUtils.downloadViewerSource(app, "file://${local.absolutePath}", "photo.jpg", "image/jpeg") { it() }
        assertFalse(publicDownload("photo.jpg").exists())
    }

    @Test
    fun `Share of a local copy shares it through the kit FileProvider with no network call`() {
        offline(true)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        MediaUtils.shareViewerSource(activity, "file://${local.absolutePath}", "photo.jpg", "image/jpeg") { it() }
        shadowOf(Looper.getMainLooper()).idle()

        val chooser = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        @Suppress("DEPRECATION")
        val stream = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", stream.scheme)
        assertEquals("${app.packageName}.cometchat.kotlin.provider", stream.authority)
        assertEquals("image/jpeg", send.type)
        assertEquals(0, downloadRequests())
        assertArrayEquals(bytes, local.readBytes())
    }

    private fun video(url: String = URL, localPath: String? = local.absolutePath) = attachment(url, localPath).apply {
        fileName = "clip.mp4"
        fileMimeType = "video/mp4"
    }

    private fun gridTileLaunch(attachment: Attachment): Intent {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        InternalContentRenderer.openVideoInPlayer(activity, attachment)
        return shadowOf(activity).nextStartedActivity!!
    }

    @Test
    fun `grid video tile with a local copy plays it in the in-app player, never handing the file to another app`() {
        offline(true)
        val intent = gridTileLaunch(video())
        assertEquals(CometChatVideoViewerActivity::class.java.name, intent.component?.className)
        @Suppress("UNCHECKED_CAST")
        val urls = intent.getSerializableExtra("ARGS_VIDEO_URLS") as List<String>
        assertEquals(listOf("file://${local.absolutePath}"), urls)
    }

    @Test
    fun `grid video tile with no local copy opens today's external player on the URL`() {
        offline(true)
        val intent = gridTileLaunch(video(localPath = null))
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse(URL), intent.data)
    }

    @Test
    fun `offline off - grid video tile opens today's external player on the URL`() {
        offline(false)
        val intent = gridTileLaunch(video())
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse(URL), intent.data)
    }

    @Test
    fun `file or audio bubble Download with a local copy saves it with no network call`() {
        offline(true)
        assertTrue(MediaUtils.downloadLocalAttachment(app, attachment(), "report.pdf") { it() })
        assertEquals(0, downloadRequests())
        assertArrayEquals(bytes, publicDownload("report.pdf").readBytes())
        assertArrayEquals(bytes, local.readBytes())
    }

    @Test
    fun `file or audio bubble Download with no local copy, or offline off, leaves today's download to run`() {
        offline(true)
        assertFalse(MediaUtils.downloadLocalAttachment(app, attachment(localPath = null), "report.pdf") { it() })
        offline(false)
        assertFalse(MediaUtils.downloadLocalAttachment(app, attachment(), "report.pdf") { it() })
        assertFalse(publicDownload("report.pdf").exists())
    }

    @Test
    fun `menu Share with a local copy shares a cache copy through the kit FileProvider`() {
        offline(true)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        assertTrue(MediaUtils.shareLocalAttachment(activity, attachment()) { it() })
        shadowOf(Looper.getMainLooper()).idle()
        assertSharedContentUri(shadowOf(activity).nextStartedActivity)
    }

    @Test
    fun `menu Share with no local copy, or offline off, leaves today's share to run`() {
        offline(true)
        assertFalse(MediaUtils.shareLocalAttachment(app, attachment(localPath = null)) { it() })
        offline(false)
        assertFalse(MediaUtils.shareLocalAttachment(app, attachment()) { it() })
    }

    /** Opens a file bubble bound to [attachment] and returns the intent it started (waits for the copy thread). */
    private fun openFromFileBubble(attachment: Attachment): Intent? {
        val activity = Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity::class.java).create().get()
        activity.setTheme(R.style.CometChatTheme_DayNight)
        val message = Mockito.mock(com.cometchat.chat.models.MediaMessage::class.java)
        Mockito.`when`(message.attachment).thenReturn(attachment)
        val bubble = com.cometchat.uikit.kotlin.presentation.shared.messagebubble.filebubble.CometChatFileBubble(activity)
        bubble.setMessage(message)
        bubble.openFile()
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            shadowOf(activity).nextStartedActivity?.let { return it }
            Thread.sleep(20)
        }
        return null
    }

    /** Taps a card of the multi-file bubble for [attachment]; returns the intent started (waits for the copy thread). */
    private fun openFromFilesBubbleCard(attachment: Attachment): Intent? {
        val activity = Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity::class.java).create().get()
        activity.setTheme(R.style.CometChatTheme_DayNight)
        val bubble = com.cometchat.uikit.kotlin.presentation.shared.messagebubble.filesbubble.CometChatFilesBubble(activity)
        bubble.openAttachment(attachment)
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            shadowOf(activity).nextStartedActivity?.let { return it }
            Thread.sleep(20)
        }
        return null
    }

    @Test
    fun `multi-file card open with a local copy opens a cache copy through the kit FileProvider`() {
        offline(true)
        val intent = openFromFilesBubbleCard(pdf())!!
        assertEquals(Intent.ACTION_VIEW, intent.action)
        val uri = intent.data!!
        assertEquals("content", uri.scheme)
        assertEquals("${app.packageName}.cometchat.kotlin.provider", uri.authority)
        assertFalse("the SDK's no_backup file is never exposed", uri.toString().contains("no_backup"))
        assertTrue(uri.toString().endsWith("report.pdf"))
        assertEquals("application/pdf", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertArrayEquals(bytes, local.readBytes())
    }

    @Test
    fun `multi-file card open with no local copy runs today's open on the URL`() {
        offline(true)
        val intent = openFromFilesBubbleCard(pdf(localPath = null))!!
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse(URL), intent.data)
    }

    @Test
    fun `offline off - multi-file card open runs today's open on the URL`() {
        offline(false)
        val intent = openFromFilesBubbleCard(pdf())!!
        assertEquals(Uri.parse(URL), intent.data)
        assertEquals("application/pdf", intent.type)
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
        assertEquals("${app.packageName}.cometchat.kotlin.provider", uri.authority)
        assertFalse("the SDK's no_backup file is never exposed", uri.toString().contains("no_backup"))
        assertTrue(uri.toString().endsWith("report.pdf"))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertArrayEquals(bytes, local.readBytes())
    }

    @Test
    fun `file bubble open with no local copy runs today's open on the URL`() {
        offline(true)
        // Online, so the bubble resolves the URL (offline it shows today's placeholder instead).
        cometChat.`when`<String> { CometChat.getConnectionStatus() }
            .thenReturn(com.cometchat.chat.constants.CometChatConstants.WS_STATE_CONNECTED)
        val intent = openFromFileBubble(pdf(localPath = null))!!
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse(URL), intent.data)
    }

    @Test
    fun `offline off - file bubble open runs today's open on the URL`() {
        offline(false)
        val intent = openFromFileBubble(pdf())!!
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Uri.parse(URL), intent.data)
    }

    private fun assertSharedContentUri(chooser: Intent?) {
        assertNotNull(chooser)
        assertEquals(chooser.toString(), Intent.ACTION_CHOOSER, chooser!!.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        @Suppress("DEPRECATION")
        val stream = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", stream.scheme)
        assertEquals("${app.packageName}.cometchat.kotlin.provider", stream.authority)
        assertEquals("image/jpeg", send.type)
        assertEquals(0, downloadRequests())
        assertArrayEquals(bytes, local.readBytes())
    }

    @Test
    fun `offline with no local copy - the image viewer shows the placeholder, not a spinner`() {
        offline(true)
        val intent = CometChatImageViewerActivity.createIntent(
            app, listOf(UNREACHABLE_URL), listOf("image/jpeg"), listOf("photo.jpg")
        )
        ActivityScenario.launch<CometChatImageViewerActivity>(intent).use { scenario ->
            var spinnerGone = false
            val deadline = System.currentTimeMillis() + 10_000
            while (!spinnerGone && System.currentTimeMillis() < deadline) {
                shadowOf(Looper.getMainLooper()).idle()
                scenario.onActivity { spinnerGone = it.findViewById<View>(R.id.progress_bar).visibility == View.GONE }
                if (!spinnerGone) Thread.sleep(50)
            }
            assertTrue("the spinner must stop when the page cannot load", spinnerGone)
        }
    }

    // ---- ENG-41635 (D-362, BEH-MEDIA-21): only a file the SDK saved is opened, downloaded or shared ----

    /** An app-private file outside the SDK's media folder, named by a message's fileUrl. */
    private fun secret(): File = File(app.filesDir, "secret.db").apply {
        parentFile!!.mkdirs()
        writeBytes(byteArrayOf(9, 9, 9))
    }

    /** Runs [open] on a fresh activity and returns what it started, polling briefly for a copy thread. */
    private fun startedBy(open: (Activity) -> Unit): Intent? {
        val activity = Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity::class.java).create().get()
        activity.setTheme(R.style.CometChatTheme_DayNight)
        open(activity)
        val deadline = System.currentTimeMillis() + 500
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            shadowOf(activity).nextStartedActivity?.let { return it }
            Thread.sleep(20)
        }
        return null
    }

    private fun online() {
        cometChat.`when`<String> { CometChat.getConnectionStatus() }
            .thenReturn(com.cometchat.chat.constants.CometChatConstants.WS_STATE_CONNECTED)
    }

    @Test
    fun `a local-path fileUrl with no SDK copy - the viewer gets no source`() {
        offline(true)
        val path = secret().absolutePath
        assertEquals(listOf(""), launchedImageUrls(attachment(url = "file://$path", localPath = null)))
        assertEquals(listOf(""), launchedImageUrls(attachment(url = path, localPath = null)))
    }

    @Test
    fun `viewer Download of a file outside the media folder copies nothing`() {
        offline(true)
        val path = secret().absolutePath
        val traversal = "${local.parentFile!!.absolutePath}/../../../../../files/secret.db"
        for (source in listOf("file://$path", path, "file://$traversal")) {
            MediaUtils.downloadViewerSource(app, source, "secret.db", null) { it() }
        }
        assertFalse(publicDownload("secret.db").exists())
    }

    @Test
    fun `viewer Share of a file outside the media folder shares nothing`() {
        offline(true)
        val path = secret().absolutePath
        val intent = startedBy { MediaUtils.shareViewerSource(it, "file://$path", "secret.db", "*/*") { work -> work() } }
        assertTrue(intent == null || intent.action != Intent.ACTION_CHOOSER)
    }

    @Test
    fun `bubble Download and menu Share with a local-path fileUrl and no SDK copy copy nothing`() {
        offline(true)
        val bad = attachment(url = "file://${secret().absolutePath}", localPath = null)
        assertFalse(MediaUtils.downloadLocalAttachment(app, bad, "secret.db") { it() })
        assertFalse(MediaUtils.shareLocalAttachment(app, bad) { it() })
        assertFalse(publicDownload("secret.db").exists())
    }

    @Test
    fun `a localPath outside the media folder is refused too (defence in depth)`() {
        offline(true)
        val bad = attachment(localPath = secret().absolutePath)
        assertFalse(MediaUtils.downloadLocalAttachment(app, bad, "secret.db") { it() })
        assertFalse(MediaUtils.shareLocalAttachment(app, bad) { it() })
    }

    @Test
    fun `multi-file card open with a local-path fileUrl and no SDK copy opens nothing`() {
        offline(true)
        online()
        val bad = pdf(localPath = null).apply { fileUrl = "file://${secret().absolutePath}" }
        val intent = startedBy {
            com.cometchat.uikit.kotlin.presentation.shared.messagebubble.filesbubble.CometChatFilesBubble(it)
                .openAttachment(bad)
        }
        assertEquals(null, intent)
    }

    @Test
    fun `file bubble open with a local-path fileUrl and no SDK copy opens nothing`() {
        offline(true)
        online()
        val bad = pdf(localPath = null).apply { fileUrl = "file://${secret().absolutePath}" }
        val message = Mockito.mock(com.cometchat.chat.models.MediaMessage::class.java)
        Mockito.`when`(message.attachment).thenReturn(bad)
        val intent = startedBy {
            com.cometchat.uikit.kotlin.presentation.shared.messagebubble.filebubble.CometChatFileBubble(it).apply {
                setMessage(message)
                openFile()
            }
        }
        assertEquals(null, intent)
    }

    @Test
    fun `grid video tile with a local-path fileUrl and no SDK copy opens nothing`() {
        offline(true)
        online()
        val bad = video(url = "file://${secret().absolutePath}", localPath = null)
        assertEquals(null, startedBy { InternalContentRenderer.openVideoInPlayer(it, bad) })
    }

    @Test
    fun `offline off - a local-path fileUrl keeps today's open`() {
        offline(false)
        val url = "file://${secret().absolutePath}"
        val intent = startedBy {
            com.cometchat.uikit.kotlin.presentation.shared.messagebubble.filesbubble.CometChatFilesBubble(it)
                .openAttachment(pdf().apply { fileUrl = url })
        }
        assertEquals(Uri.parse(url), intent?.data)
    }

    /** A media message of [type] carrying [attachment], as the message list's Share option sees it. */
    private fun mediaMessage(attachment: Attachment, type: String): com.cometchat.chat.models.MediaMessage {
        val message = Mockito.mock(com.cometchat.chat.models.MediaMessage::class.java)
        Mockito.`when`(message.attachment).thenReturn(attachment)
        Mockito.`when`(message.type).thenReturn(type)
        Mockito.`when`(message.id).thenReturn(42L)
        return message
    }

    /**
     * Runs the message list's Share option on [messages] with Glide mocked, then [verify] with the
     * Glide mock (still open) and whatever activity the share started.
     */
    private fun menuShare(
        vararg messages: com.cometchat.chat.models.MediaMessage,
        verify: (MockedStatic<com.bumptech.glide.Glide>, Intent?) -> Unit
    ) {
        val scenario = ActivityScenario.launch(androidx.activity.ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val list = com.cometchat.uikit.kotlin.presentation.messagelist.ui.CometChatMessageList(activity)
            Mockito.mockStatic(com.bumptech.glide.Glide::class.java, Mockito.RETURNS_DEEP_STUBS).use { glide ->
                messages.forEach { list.shareMessage(it) }
                shadowOf(Looper.getMainLooper()).idle()
                verify(glide, shadowOf(activity).nextStartedActivity)
            }
        }
        scenario.close()
    }

    @Test
    fun `menu Share of a local-path fileUrl with no SDK copy loads, inserts and shares nothing`() {
        offline(true)
        val url = "file://${secret().absolutePath}"
        menuShare(
            mediaMessage(attachment(url = url, localPath = null), "image"),
            mediaMessage(pdf(localPath = null).apply { fileUrl = url }, "file")
        ) { glide, started ->
            glide.verifyNoInteractions()
            assertEquals(null, started)
        }
    }

    @Test
    fun `offline off - menu Share of an image runs today's Glide load`() {
        offline(false)
        menuShare(mediaMessage(attachment(url = "file://${secret().absolutePath}"), "image")) { glide, _ ->
            glide.verify({ com.bumptech.glide.Glide.with(Mockito.any(Context::class.java)) }, Mockito.times(1))
        }
    }

    private companion object {
        const val URL = "https://cdn.example/x/photo.jpg?sig=1"
        const val UNREACHABLE_URL = "http://127.0.0.1:1/photo.jpg"
    }
}
