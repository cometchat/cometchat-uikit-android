package com.cometchat.uikit.core.utils

import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaCachingMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files

/**
 * ENG-41482 (D-324, BEH-MEDIA-18) — [MediaViewerSource]: what the full-screen viewers and players
 * load, and the local file their Download and Share use. The local copy wins in any connection
 * state; with no local copy it is today's URL; offline off it is always today's URL.
 * ENG-41635 (D-362, BEH-MEDIA-21): only a file the SDK saved, inside its media folder, is ever local.
 */
class MediaViewerSourceTest : FunSpec({

    fun attachment(url: String? = "https://cdn/x/a.jpg?sig=1", local: String? = null) = Attachment().apply {
        fileUrl = url
        localPath = local
        fileName = "a.jpg"
        fileSize = 2048
    }

    fun source(a: Attachment, on: Boolean = true, online: Boolean = true, exists: Boolean = true): String =
        MediaViewerSource.of(a, MediaSourceResolver.resolve(a, on, { MediaCachingMode.AUTO }, { online }, { exists }))

    test("the viewer gets the local copy when present, online") {
        source(attachment(local = "/p/a.jpg"), online = true) shouldBe "file:///p/a.jpg"
    }

    test("the viewer gets the local copy when present, offline") {
        source(attachment(local = "/p/a.jpg"), online = false) shouldBe "file:///p/a.jpg"
    }

    test("no local copy and online: the viewer gets the URL") {
        source(attachment(), online = true) shouldBe "https://cdn/x/a.jpg?sig=1"
    }

    test("no local copy and offline: the URL, so the viewer shows its error state and Download/Share fail as today") {
        source(attachment(), online = false) shouldBe "https://cdn/x/a.jpg?sig=1"
    }

    test("a local path whose file is gone falls back to the URL") {
        source(attachment(local = "/p/gone.jpg"), exists = false) shouldBe "https://cdn/x/a.jpg?sig=1"
    }

    test("a queued own image (no URL yet) opens with its local file") {
        source(attachment(url = "", local = "/p/queued.jpg"), online = false) shouldBe "file:///p/queued.jpg"
    }

    test("offline off: today's URL, even with a local copy") {
        source(attachment(local = "/p/a.jpg"), on = false) shouldBe "https://cdn/x/a.jpg?sig=1"
    }

    test("offline off with no URL: empty, as today") {
        MediaViewerSource.of(attachment(url = null), null) shouldBe ""
    }

    // ---- ENG-41635 (D-362, BEH-MEDIA-21): message data is never a local file ----------------------

    test("offline on, no SDK copy: a file:// or bare-path fileUrl gives no source") {
        source(attachment(url = "file:///data/data/app/files/secret.db"), online = true) shouldBe ""
        source(attachment(url = "/data/data/app/files/secret.db"), online = true) shouldBe ""
        source(attachment(url = "FILE:///data/data/app/files/secret.db"), online = false) shouldBe ""
        source(attachment(url = "content://other.app/secret"), online = true) shouldBe ""
    }

    test("offline on, no SDK copy, media caching OFF: a local-path fileUrl still gives no source") {
        val a = attachment(url = "file:///data/data/app/files/secret.db")
        MediaViewerSource.of(a, MediaSourceResolver.resolve(a, true, { MediaCachingMode.OFF }, { true }, { true })) shouldBe ""
    }

    test("offline on: the SDK copy wins even when the fileUrl is a local path") {
        source(attachment(url = "file:///elsewhere/x", local = "/p/a.jpg")) shouldBe "file:///p/a.jpg"
    }

    test("offline off: today's fileUrl, unchanged") {
        MediaViewerSource.of(attachment(url = "file:///data/x"), null) shouldBe "file:///data/x"
    }

    test("openableUrl: offline on gives an http(s) URL only; offline off is today's fileUrl") {
        MediaViewerSource.openableUrl("https://cdn/x/a.pdf", true) shouldBe "https://cdn/x/a.pdf"
        MediaViewerSource.openableUrl("HTTP://cdn/x/a.pdf", true) shouldBe "HTTP://cdn/x/a.pdf"
        MediaViewerSource.openableUrl("file:///data/x", true) shouldBe ""
        MediaViewerSource.openableUrl("/data/x", true) shouldBe ""
        MediaViewerSource.openableUrl(null, true) shouldBe ""
        MediaViewerSource.openableUrl("file:///data/x", false) shouldBe "file:///data/x"
        MediaViewerSource.openableUrl(null, false) shouldBe ""
    }

    // A temp no-backup dir standing in for <noBackupFilesDir>; the media root is <it>/cometchat/media.
    fun noBackup(): File = Files.createTempDirectory("eng41635-nobackup").toFile().apply { deleteOnExit() }
    fun mediaRoot(dir: File) = File(dir, "cometchat/media")
    fun write(f: File): File = f.apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1, 2, 3)) }

    test("localFile accepts an SDK copy inside the media root, as file:// or a bare path") {
        val dir = noBackup()
        val f = write(File(mediaRoot(dir), "app1/u1/att1"))
        MediaViewerSource.localFile("file://${f.path}", true, mediaRoot(dir)) shouldBe f.canonicalFile
        MediaViewerSource.localFile(f.path, true, mediaRoot(dir)) shouldBe f.canonicalFile
    }

    test("localFile accepts a copy under any appId (a user or app switch keeps working)") {
        val dir = noBackup()
        val f = write(File(mediaRoot(dir), "otherApp/u9/att2"))
        MediaViewerSource.localFile(f.path, true, mediaRoot(dir)) shouldBe f.canonicalFile
    }

    test("localFile rejects a file outside the media root") {
        val dir = noBackup()
        val f = write(File(dir, "files/secret.db"))
        MediaViewerSource.localFile("file://${f.path}", true, mediaRoot(dir)) shouldBe null
        MediaViewerSource.localFile(f.path, true, mediaRoot(dir)) shouldBe null
    }

    test("localFile rejects a .. traversal out of the media root") {
        val dir = noBackup()
        write(File(dir, "files/secret.db"))
        mediaRoot(dir).mkdirs()
        val escape = "${mediaRoot(dir).path}/../../files/secret.db"
        MediaViewerSource.localFile("file://$escape", true, mediaRoot(dir)) shouldBe null
        MediaViewerSource.localFile(escape, true, mediaRoot(dir)) shouldBe null
    }

    test("localFile rejects a sibling folder whose name only starts like the root") {
        val dir = noBackup()
        val f = write(File(dir, "cometchat/media-evil/x"))
        MediaViewerSource.localFile(f.path, true, mediaRoot(dir)) shouldBe null
    }

    test("localFile rejects a symlink inside the media root that points outside it") {
        val dir = noBackup()
        val secret = write(File(dir, "files/secret.db"))
        val link = File(mediaRoot(dir), "app1/u1/att-link").apply { parentFile!!.mkdirs() }
        Files.createSymbolicLink(link.toPath(), secret.toPath())
        MediaViewerSource.localFile(link.path, true, mediaRoot(dir)) shouldBe null
    }

    test("localFile rejects the media root itself and a folder inside it") {
        val dir = noBackup()
        File(mediaRoot(dir), "app1/u1").mkdirs()
        MediaViewerSource.localFile(mediaRoot(dir).path, true, mediaRoot(dir)) shouldBe null
        MediaViewerSource.localFile(File(mediaRoot(dir), "app1/u1").path, true, mediaRoot(dir)) shouldBe null
    }

    test("localFile is null for a URL, an empty source, a missing file or no media root: the caller keeps the network path") {
        val dir = noBackup()
        val root = mediaRoot(dir)
        MediaViewerSource.localFile("https://cdn/x/a.jpg", true, root) shouldBe null
        MediaViewerSource.localFile("", true, root) shouldBe null
        MediaViewerSource.localFile(null, true, root) shouldBe null
        MediaViewerSource.localFile("file://", true, root) shouldBe null
        MediaViewerSource.localFile("file://${File(root, "app1/u1/gone").path}", true, root) shouldBe null
        val f = write(File(root, "app1/u1/att1"))
        MediaViewerSource.localFile(f.path, true, null) shouldBe null
    }

    test("localFile is null with offline off: Download and Share keep today's path") {
        val dir = noBackup()
        val f = write(File(mediaRoot(dir), "app1/u1/att1"))
        MediaViewerSource.localFile("file://${f.path}", false, mediaRoot(dir)) shouldBe null
    }

    // ENG-41644 (D-371): the Downloads name comes from message data (BEH-MEDIA-21).
    val stored = File("/data/user/0/app/no_backup/cometchat/media/app1/u1/att1")

    test("saveName_stripsDirectories") {
        MediaViewerSource.saveName("../../x", stored) shouldBe "x"
        MediaViewerSource.saveName("/sdcard/Android/data/evil.apk", stored) shouldBe "evil.apk"
        MediaViewerSource.saveName("a/b/../c.txt", stored) shouldBe "c.txt"
    }

    test("saveName_dotOrDotDotOrBlank_fallsBackToFileName") {
        MediaViewerSource.saveName("", stored) shouldBe "att1"
        MediaViewerSource.saveName("   ", stored) shouldBe "att1"
        MediaViewerSource.saveName(".", stored) shouldBe "att1"
        MediaViewerSource.saveName("..", stored) shouldBe "att1"
        MediaViewerSource.saveName("x/..", stored) shouldBe "att1"
        MediaViewerSource.saveName("../", stored) shouldBe "att1"
    }

    test("saveName_plainName_unchanged") {
        MediaViewerSource.saveName("photo.jpg", stored) shouldBe "photo.jpg"
        MediaViewerSource.saveName("report final.pdf", stored) shouldBe "report final.pdf"
        MediaViewerSource.saveName("फ़ोटो 写真 ü.png", stored) shouldBe "फ़ोटो 写真 ü.png"
        MediaViewerSource.saveName(".hidden", stored) shouldBe ".hidden"
    }

    test("saveToDownloads_preQ_neverWritesOutsideDownloads") {
        val downloads = File(Files.createTempDirectory("eng41644-root").toFile(), "Download").apply { mkdirs() }
        // A cleaned name lands directly inside Downloads, with uniqueFile's -1, -2 suffixes kept.
        val name = MediaViewerSource.saveName("../../photo.jpg", stored)
        MediaViewerSource.downloadsTarget(downloads, name) shouldBe File(downloads, "photo.jpg")
        write(File(downloads, "photo.jpg"))
        MediaViewerSource.downloadsTarget(downloads, name) shouldBe File(downloads, "photo-1.jpg")
        write(File(downloads, "photo-1.jpg"))
        MediaViewerSource.downloadsTarget(downloads, name) shouldBe File(downloads, "photo-2.jpg")
        // The canonical-parent check refuses any name that would leave Downloads or nest below it.
        MediaViewerSource.downloadsTarget(downloads, "../x") shouldBe null
        MediaViewerSource.downloadsTarget(downloads, "../../x") shouldBe null
        MediaViewerSource.downloadsTarget(downloads, "sub/x") shouldBe null
        MediaViewerSource.downloadsTarget(downloads, "x/..") shouldBe null
    }
})
