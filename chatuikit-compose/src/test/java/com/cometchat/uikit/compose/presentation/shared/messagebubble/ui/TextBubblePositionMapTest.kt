package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit layer for [CometChatTextBubble]'s position mapping.
 *
 * These two functions are the only genuinely pure logic in the text bubble: they
 * take a markdown source string and its markdown-stripped rendering, and build the
 * index translation that lets formatter spans (mentions, links) be re-anchored onto
 * the stripped text. Everything else in the file needs the Compose runtime.
 *
 * The mapping is what keeps a mention highlight on the mention after `**` markers
 * are removed — an off-by-one here silently paints the wrong characters.
 */
class TextBubblePositionMapTest {

    // ── buildPositionMap ────────────────────────────────────────────────────

    @Test
    fun identityText_mapsEveryPositionToItself() {
        val text = "hello"
        val map = buildPositionMap(text, text)

        // one slot per character plus the end position
        assertEquals(text.length + 1, map.size)
        assertArrayEquals(intArrayOf(0, 1, 2, 3, 4, 5), map)
    }

    @Test
    fun strippedMarkers_mapContentAndLeaveMarkersUnmapped() {
        // "**bold**" renders as "bold": the four asterisks have no home in the
        // stripped text and must stay -1 rather than collapse onto a neighbour.
        val map = buildPositionMap("**bold**", "bold")

        assertArrayEquals(
            intArrayOf(-1, -1, 0, 1, 2, 3, -1, -1, 4),
            map
        )
    }

    @Test
    fun endPositionAlwaysMapsToPlainLength() {
        // The end slot is set unconditionally, even when the walk stops early.
        val map = buildPositionMap("**bold**", "bold")
        assertEquals(4, map[map.size - 1])
    }

    @Test
    fun emptyPlainText_mapsOnlyTheEndPosition() {
        val map = buildPositionMap("ab", "")

        assertArrayEquals(intArrayOf(-1, -1, 0), map)
    }

    @Test
    fun emptyOriginal_producesSingleEndSlot() {
        val map = buildPositionMap("", "")

        assertArrayEquals(intArrayOf(0), map)
    }

    // ── mapPositionUsingMap ─────────────────────────────────────────────────

    @Test
    fun negativePosition_isRejected() {
        val map = buildPositionMap("abc", "abc")

        assertEquals(-1, mapPositionUsingMap(-1, map, 3))
    }

    @Test
    fun positionPastTheMap_clampsToPlainLength() {
        val map = buildPositionMap("**bold**", "bold")

        // map.size is 9; anything at or beyond it clamps rather than throwing.
        assertEquals(4, mapPositionUsingMap(map.size, map, 4))
        assertEquals(4, mapPositionUsingMap(map.size + 50, map, 4))
    }

    @Test
    fun mappedPosition_isReturnedDirectly() {
        val map = buildPositionMap("**bold**", "bold")

        assertEquals(0, mapPositionUsingMap(2, map, 4)) // 'b'
        assertEquals(3, mapPositionUsingMap(5, map, 4)) // 'd'
        assertEquals(4, mapPositionUsingMap(8, map, 4)) // end
    }

    @Test
    fun unmappedPosition_fallsBackToNearestEarlierMapping() {
        val map = buildPositionMap("**bold**", "bold")

        // positions 6 and 7 are the trailing "**" — both resolve back to 'd' at 3,
        // so a span ending inside the markers still closes on real content.
        assertEquals(3, mapPositionUsingMap(6, map, 4))
        assertEquals(3, mapPositionUsingMap(7, map, 4))
    }

    @Test
    fun unmappedPositionWithNoEarlierMapping_fallsBackToZero() {
        val map = buildPositionMap("**bold**", "bold")

        // Leading markers have nothing before them; the floor is 0, not -1.
        assertEquals(0, mapPositionUsingMap(0, map, 4))
        assertEquals(0, mapPositionUsingMap(1, map, 4))
    }

    @Test
    fun everyPositionInIdentityText_roundTripsUnchanged() {
        val text = "a mention here"
        val map = buildPositionMap(text, text)

        for (i in text.indices) {
            assertEquals("position $i", i, mapPositionUsingMap(i, map, text.length))
        }
    }

    @Test
    fun mappingIsMonotonic_soSpansNeverInvert() {
        // A span [start,end) must never map to an inverted range, whatever the
        // markers look like — this is the invariant buildSegmentText relies on.
        val original = "**bold** and _italic_ text"
        val plain = "bold and italic text"
        val map = buildPositionMap(original, plain)

        var previous = 0
        for (i in 0..original.length) {
            val mapped = mapPositionUsingMap(i, map, plain.length)
            assert(mapped >= previous) { "position $i mapped to $mapped, behind $previous" }
            previous = mapped
        }
    }
}
