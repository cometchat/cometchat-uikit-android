package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * Unit layer for [formatDurationMs], the audio bubble's clock.
 *
 * Public, pure, and the source of every `mm:ss / mm:ss` string in the compose audio
 * bubble — including the one baked into the screenshot baselines. Worth its own tests
 * because two of its behaviours are easy to change by accident:
 *
 * - it formats with an explicit `Locale.US`, so a device in a locale with
 *   non-ASCII digits still renders the same glyphs the baselines contain;
 * - minutes are **not** rolled into hours, so a 90-minute recording reads `90:00`,
 *   not `1:30:00`. That is a deliberate two-field format, and a future "fix" that
 *   adds an hours field would silently break every baseline and every layout that
 *   sized itself for five characters.
 */
class AudioDurationFormatTest {

    @Test
    fun zero_readsAsAZeroClock() = assertEquals("00:00", formatDurationMs(0L))

    @Test
    fun negative_readsAsAZeroClockRatherThanAMinusSign() {
        // MediaPlayer reports -1 for containers with no duration header; the bubble
        // shows that as 00:00 rather than "-1:-1" while the retriever fallback runs.
        assertEquals("00:00", formatDurationMs(-1L))
    }

    @Test
    fun subSecond_truncatesDownRatherThanRoundingUp() =
        assertEquals("00:00", formatDurationMs(999L))

    @Test
    fun oneSecond_isPaddedToTwoDigits() = assertEquals("00:01", formatDurationMs(1_000L))

    @Test
    fun secondsAreTruncatedNotRounded() = assertEquals("00:09", formatDurationMs(9_900L))

    @Test
    fun oneMinute_rollsOverIntoTheMinutesField() =
        assertEquals("01:00", formatDurationMs(60_000L))

    @Test
    fun aTypicalVoiceNote() = assertEquals("01:23", formatDurationMs(83_000L))

    @Test
    fun fiftyNineSeconds_staysInTheSecondsField() =
        assertEquals("00:59", formatDurationMs(59_999L))

    @Test
    fun anHour_readsAsSixtyMinutesNotOneHour() {
        // Deliberate: the format is mm:ss with no hours field.
        assertEquals("60:00", formatDurationMs(3_600_000L))
    }

    @Test
    fun aVeryLongRecording_keepsCountingInMinutes() =
        assertEquals("150:00", formatDurationMs(150L * 60_000L))

    @Test
    fun formattingIgnoresTheDefaultLocale() {
        // Arabic-Indic digits would otherwise replace the ASCII ones and diverge from
        // every recorded baseline.
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("01:05", formatDurationMs(65_000L))
        } finally {
            Locale.setDefault(original)
        }
    }
}
