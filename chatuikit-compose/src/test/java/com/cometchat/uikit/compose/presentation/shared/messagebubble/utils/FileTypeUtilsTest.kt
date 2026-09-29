package com.cometchat.uikit.compose.presentation.shared.messagebubble.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Unit layer for the file bubble's pure helpers. Both file bubbles and both toolkits
 * derive their icon, subtitle and type badge from these, so a change here moves every
 * file baseline at once.
 *
 * Robolectric only because [getFileTypeIcon] returns R.drawable ids.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class FileTypeUtilsTest {

    // ── getFileType: mime wins, extension is the fallback ────────────────────

    @Test fun pdfByMime() = assertEquals(FileType.PDF, getFileType("application/pdf", null))

    @Test fun pdfByExtension() = assertEquals(FileType.PDF, getFileType(null, "report.pdf"))

    @Test fun docByMime() =
        assertEquals(FileType.DOC, getFileType("application/msword", null))

    @Test fun xlsByExtension() = assertEquals(FileType.XLS, getFileType(null, "budget.xlsx"))

    @Test fun pptByExtension() = assertEquals(FileType.PPT, getFileType(null, "deck.pptx"))

    @Test fun zipByExtension() = assertEquals(FileType.ZIP, getFileType(null, "bundle.zip"))

    @Test fun audioByMimePrefix() = assertEquals(FileType.AUDIO, getFileType("audio/mpeg", null))

    @Test fun videoByMimePrefix() = assertEquals(FileType.VIDEO, getFileType("video/mp4", null))

    @Test fun imageByMimePrefix() = assertEquals(FileType.IMAGE, getFileType("image/jpeg", null))

    @Test fun textByMimePrefix() = assertEquals(FileType.TEXT, getFileType("text/plain", null))

    @Test fun unknownWhenNothingMatches() =
        assertEquals(FileType.UNKNOWN, getFileType("application/octet-stream", "blob.bin"))

    @Test fun unknownWhenBothAreNull() = assertEquals(FileType.UNKNOWN, getFileType(null, null))

    @Test
    fun mimeIsCaseInsensitive() =
        assertEquals(FileType.PDF, getFileType("APPLICATION/PDF", null))

    @Test
    fun extensionIsCaseInsensitive() = assertEquals(FileType.PDF, getFileType(null, "REPORT.PDF"))

    @Test
    fun aMimeMatchBeatsAConflictingExtension() {
        // Order matters: the mime branch is checked first in each arm, so a PDF served
        // with a .zip name is still a PDF.
        assertEquals(FileType.PDF, getFileType("application/pdf", "archive.zip"))
    }

    // ── getFileTypeIcon: every enum value resolves ───────────────────────────

    @Test
    fun everyFileTypeHasItsOwnIcon() {
        val icons = FileType.entries.associateWith { getFileTypeIcon(it) }
        icons.forEach { (type, id) -> assertEquals("$type should resolve an icon", true, id != 0) }
        assertEquals("icons must not be shared between types", FileType.entries.size, icons.values.toSet().size)
    }

    // ── formatFileSize ──────────────────────────────────────────────────────

    @Test fun zeroBytes() = assertEquals("0 B", formatFileSize(0))

    @Test fun negativeSizeIsTreatedAsZero() = assertEquals("0 B", formatFileSize(-1))

    @Test fun bytesStayBytes() = assertEquals("512 B", formatFileSize(512))

    @Test fun wholeKilobytesDropTheDecimal() = assertEquals("1 KB", formatFileSize(1024))

    @Test fun wholeMegabytesDropTheDecimal() = assertEquals("1 MB", formatFileSize(1024L * 1024))

    @Test fun wholeGigabytes() = assertEquals("1 GB", formatFileSize(1024L * 1024 * 1024))

    @Test
    fun sizesBeyondTerabytesClampToTheLargestUnit() {
        // digitGroups is coerced into the units array rather than overflowing it.
        assertEquals("1024 TB", formatFileSize(1024L * 1024 * 1024 * 1024 * 1024))
    }

    @Test
    fun fractionalSizesKeepOneDecimal() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("1.5 KB", formatFileSize(1536))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun fractionalSizesFollowTheDeviceLocale() {
        // Unlike the audio bubble's clock, which pins Locale.US, this formats with the
        // default locale -- so a device set to a comma-decimal locale reads "1,5 KB".
        // Pinned because it is the difference between the two, not because it is wrong.
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1,5 KB", formatFileSize(1536))
        } finally {
            Locale.setDefault(original)
        }
    }

    // ── getFileExtension ────────────────────────────────────────────────────

    @Test fun extensionIsUppercased() = assertEquals("PDF", getFileExtension("report.pdf"))

    @Test fun noExtension() = assertEquals("", getFileExtension("README"))

    @Test fun nullName() = assertEquals("", getFileExtension(null))

    @Test fun blankName() = assertEquals("", getFileExtension("   "))

    @Test fun aTrailingDotHasNoExtension() = assertEquals("", getFileExtension("archive."))

    @Test
    fun onlyTheLastSegmentCounts() =
        assertEquals("GZ", getFileExtension("backup.tar.gz"))

    @Test
    fun queryParametersAreStripped() =
        assertEquals("PDF", getFileExtension("report.pdf?token=abc"))

    // ── formatFileSubtitle ──────────────────────────────────────────────────

    @Test
    fun subtitleJoinsSizeAndExtension() =
        assertEquals("512 B • PDF", formatFileSubtitle(512, "report.pdf"))

    @Test
    fun subtitleIsSizeOnlyWithoutAnExtension() =
        assertEquals("512 B", formatFileSubtitle(512, "README"))

    @Test
    fun subtitleWithNoNameIsSizeOnly() = assertEquals("0 B", formatFileSubtitle(0, null))
}
