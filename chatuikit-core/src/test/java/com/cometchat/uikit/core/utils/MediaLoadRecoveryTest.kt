package com.cometchat.uikit.core.utils

import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.MediaCachingMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * ENG-41483 (D-325) — [MediaLoadRecovery] (G9, BEH-MEDIA-13): a failed thumbnail falls back to the
 * full file URL once, then asks the SDK to download once, then stops. Only a URL tile (offline on)
 * gets a recovery; offline off, a local file and a placeholder keep today's behaviour.
 */
class MediaLoadRecoveryTest : FunSpec({

    val fileUrl = "https://cdn/x/a.jpg?sig=1"
    val thumbUrl = "https://cdn/x/a_medium.jpg?sig=1"

    fun attachment(url: String? = fileUrl) = Attachment().apply { this.fileUrl = url }

    fun recovery(
        source: MediaSource? = MediaSource.Remote(fileUrl),
        att: Attachment? = attachment(),
        mode: MediaCachingMode? = MediaCachingMode.AUTO,
        downloads: MutableList<Attachment>
    ) = MediaLoadRecovery.forSource(source, att, cachingMode = { mode }, download = { downloads += it })

    test("thumbnail failure tries the file URL once") {
        val downloads = mutableListOf<Attachment>()
        val r = recovery(downloads = downloads)!!
        r.onLoadFailed(thumbUrl) shouldBe fileUrl
        downloads.size shouldBe 0
    }

    test("file URL failure asks the SDK to download once") {
        val downloads = mutableListOf<Attachment>()
        val att = attachment()
        val r = recovery(att = att, downloads = downloads)!!
        r.onLoadFailed(thumbUrl) shouldBe fileUrl
        r.onLoadFailed(fileUrl) shouldBe null
        downloads shouldBe listOf(att)
    }

    test("a tile already on the file URL goes straight to the download") {
        val downloads = mutableListOf<Attachment>()
        val r = recovery(downloads = downloads)!!
        r.onLoadFailed(fileUrl) shouldBe null
        downloads.size shouldBe 1
    }

    test("a further failure makes no more calls") {
        val downloads = mutableListOf<Attachment>()
        val r = recovery(downloads = downloads)!!
        r.onLoadFailed(thumbUrl)
        r.onLoadFailed(fileUrl)
        r.onLoadFailed(fileUrl) shouldBe null
        r.onLoadFailed(thumbUrl) shouldBe null
        downloads.size shouldBe 1
    }

    test("a bubble with its own file URL fallback asks for the download once") {
        val downloads = mutableListOf<Attachment>()
        val r = recovery(downloads = downloads)!!
        r.onFileUrlFailed()
        r.onFileUrlFailed()
        r.onLoadFailed(thumbUrl) shouldBe null
        downloads.size shouldBe 1
    }

    test("caching OFF: the file URL fallback still runs, the download does not") {
        val downloads = mutableListOf<Attachment>()
        val r = recovery(mode = MediaCachingMode.OFF, downloads = downloads)!!
        r.onLoadFailed(thumbUrl) shouldBe fileUrl
        r.onLoadFailed(fileUrl) shouldBe null
        downloads.size shouldBe 0
    }

    test("offline off, a local file, a placeholder or no attachment: no recovery") {
        val downloads = mutableListOf<Attachment>()
        recovery(source = null, downloads = downloads) shouldBe null
        recovery(source = MediaSource.Local("/data/a.jpg"), downloads = downloads) shouldBe null
        recovery(source = MediaSource.Placeholder("a.jpg", 1), downloads = downloads) shouldBe null
        recovery(att = null, downloads = downloads) shouldBe null
        downloads.size shouldBe 0
    }
})
