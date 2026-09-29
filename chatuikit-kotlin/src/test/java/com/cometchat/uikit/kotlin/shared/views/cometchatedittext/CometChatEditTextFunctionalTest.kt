package com.cometchat.uikit.kotlin.shared.views.cometchatedittext

import android.text.Spanned
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.shared.mentions.CometChatTextWatcher
import com.cometchat.uikit.kotlin.shared.spans.NonEditableSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Functional layer for [CometChatEditText] — the composer's text field, and until now
 * untested despite being where every mention in the product is typed.
 *
 * Its whole reason to exist is that a mention must behave like one object rather than the
 * six or seven characters it is made of. Two rules follow from that, and both are the kind
 * that only misbehave in the middle of a word:
 *
 * 1. **The caret may not rest inside a mention.** Tapping into the middle of `@alice`
 *    bounces the caret to whichever edge is nearer, so arrow keys and taps skip the span
 *    instead of entering it.
 * 2. **Backspacing into a mention removes all of it.** Deleting one character would leave
 *    a mention whose text no longer matches the user it points at — a mention of nobody —
 *    so the whole span goes, and the watcher is told which one.
 *
 * The tie-break in rule 1 is worth stating because it is invisible in the source: the
 * comparison is `abs(sel - start) < abs(sel - end)`, strictly less, so a caret at the exact
 * midpoint of a mention lands on the **end**, not the start. Anyone rewriting this will
 * reach for `<=` half the time.
 *
 * Note also that an adjusted move is silent: when the caret is bounced, the
 * selection-changed listener is not told about the position it was bounced *from*, because
 * the adjustment path returns before notifying and the re-entrant call is suppressed by
 * the guard flag. Integrators watching the caret see the corrected position or nothing.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatEditTextFunctionalTest {

    private companion object {
        /** "Hello @alice there" — the mention occupies [6, 12). */
        const val TEXT = "Hello @alice there"
        const val MENTION = "@alice"
        const val SPAN_START = 6
        const val SPAN_END = 12
    }

    private class RecordingWatcher : CometChatTextWatcher() {
        val deleted = mutableListOf<NonEditableSpan>()
        val events = mutableListOf<String>()

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            events += "before"
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            events += "changed"
        }

        override fun afterTextChanged(s: android.text.Editable?) {
            events += "after"
        }

        override fun onSpanDeleted(span: NonEditableSpan) {
            deleted += span
        }
    }

    private class Harness(
        val editText: CometChatEditText,
        val span: NonEditableSpan,
    )

    /**
     * An attached CometChatEditText holding [TEXT] with a mention span over [MENTION].
     * `withSpan = false` gives the same text with no span, for the control cases.
     */
    private fun withEditText(withSpan: Boolean = true, block: (Harness) -> Unit) {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setTheme(R.style.CometChatTheme_DayNight)
                val editText = CometChatEditText(activity)
                activity.setContentView(editText)

                editText.setText(TEXT)
                val span = NonEditableSpan('@', MENTION, null)
                if (withSpan) {
                    editText.text!!.setSpan(
                        span,
                        SPAN_START,
                        SPAN_END,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
                block(Harness(editText, span))
            }
        }
    }

    // ── the caret may not rest inside a mention ─────────────────────────────

    @Test
    fun aCaretPlacedNearTheStartOfAMentionBouncesToItsStart() {
        withEditText { h ->
            h.editText.setSelection(SPAN_START + 2)
            assertEquals(SPAN_START, h.editText.selectionStart)
        }
    }

    @Test
    fun aCaretPlacedNearTheEndOfAMentionBouncesToItsEnd() {
        withEditText { h ->
            h.editText.setSelection(SPAN_END - 1)
            assertEquals(SPAN_END, h.editText.selectionStart)
        }
    }

    @Test
    fun aCaretAtTheExactMidpointBouncesForwardsNotBackwards() {
        // abs(sel - start) < abs(sel - end) is strictly less, so a tie resolves to the
        // end. Invisible in the source and a coin-flip for anyone rewriting it.
        val midpoint = (SPAN_START + SPAN_END) / 2
        assertEquals("the fixture must actually be a tie", midpoint - SPAN_START, SPAN_END - midpoint)

        withEditText { h ->
            h.editText.setSelection(midpoint)
            assertEquals(SPAN_END, h.editText.selectionStart)
        }
    }

    @Test
    fun aCaretAtTheMentionsOwnEdgesIsLeftAlone() {
        // The edges are legal resting places — that is what makes the span skippable
        // rather than unreachable.
        withEditText { h ->
            h.editText.setSelection(SPAN_START)
            assertEquals(SPAN_START, h.editText.selectionStart)

            h.editText.setSelection(SPAN_END)
            assertEquals(SPAN_END, h.editText.selectionStart)
        }
    }

    @Test
    fun aCaretOutsideAnyMentionIsLeftAlone() {
        withEditText { h ->
            h.editText.setSelection(2)
            assertEquals(2, h.editText.selectionStart)

            h.editText.setSelection(TEXT.length)
            assertEquals(TEXT.length, h.editText.selectionStart)
        }
    }

    @Test
    fun withoutAMentionEveryPositionIsReachable() {
        // The control: proves the bouncing above is the span's doing and not something
        // this EditText does to every caret move.
        withEditText(withSpan = false) { h ->
            h.editText.setSelection(SPAN_START + 2)
            assertEquals(SPAN_START + 2, h.editText.selectionStart)
        }
    }

    @Test
    fun aDegenerateSpanIsIgnoredRatherThanTrappingTheCaret() {
        // A zero-length span (start == end) is skipped explicitly. Without that guard the
        // caret could be bounced against a span that occupies nothing.
        withEditText(withSpan = false) { h ->
            val empty = NonEditableSpan('@', "", null)
            h.editText.text!!.setSpan(empty, 8, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            h.editText.setSelection(8)
            assertEquals(8, h.editText.selectionStart)
        }
    }

    // ── a selection that runs into a mention is clipped ─────────────────────

    @Test
    fun aSelectionEndingInsideAMentionIsClippedToItsStart() {
        // Dragging a selection from before the mention into its middle stops at the
        // boundary, so a partial mention can never be selected and then replaced.
        withEditText { h ->
            h.editText.setSelection(2, SPAN_START + 2)

            assertEquals(2, h.editText.selectionStart)
            assertEquals(SPAN_START, h.editText.selectionEnd)
        }
    }

    @Test
    fun aSelectionEnclosingAWholeMentionIsAllowed() {
        // Selecting past the far edge is fine — the mention is being taken as a unit.
        withEditText { h ->
            h.editText.setSelection(2, SPAN_END + 3)

            assertEquals(2, h.editText.selectionStart)
            assertEquals(SPAN_END + 3, h.editText.selectionEnd)
        }
    }

    // ── backspacing into a mention takes all of it ──────────────────────────

    @Test
    fun deletingOneCharacterOfAMentionRemovesTheWholeMention() {
        withEditText { h ->
            h.editText.setSelection(SPAN_END)
            h.editText.text!!.delete(SPAN_END - 1, SPAN_END)

            assertTrue(
                "no fragment of the mention should survive, was '${h.editText.text}'",
                !h.editText.text!!.toString().contains("@ali"),
            )
        }
    }

    @Test
    fun deletingAMentionLeavesTheSurroundingTextIntact() {
        withEditText { h ->
            h.editText.setSelection(SPAN_END)
            h.editText.text!!.delete(SPAN_END - 1, SPAN_END)

            val remaining = h.editText.text!!.toString()
            assertTrue("the leading text should survive: '$remaining'", remaining.startsWith("Hello"))
            assertTrue("the trailing text should survive: '$remaining'", remaining.endsWith("there"))
        }
    }

    @Test
    fun theWatcherIsToldWhichMentionWasDeleted() {
        // The composer needs the identity of the removed mention to drop it from the
        // outgoing message's mention list; "something was deleted" is not enough.
        withEditText { h ->
            val watcher = RecordingWatcher()
            h.editText.setTextWatcher(watcher)

            h.editText.setSelection(SPAN_END)
            h.editText.text!!.delete(SPAN_END - 1, SPAN_END)

            assertEquals(1, watcher.deleted.size)
            assertSame(h.span, watcher.deleted.first())
        }
    }

    @Test
    fun deletingOrdinaryTextLeavesMentionsAlone() {
        // Editing elsewhere in the line must not disturb the mention — the deletion path
        // runs on every single-character delete, not only on ones near a span.
        withEditText { h ->
            val watcher = RecordingWatcher()
            h.editText.setTextWatcher(watcher)

            h.editText.setSelection(3)
            h.editText.text!!.delete(2, 3)

            assertTrue("no mention should have been deleted", watcher.deleted.isEmpty())
            assertTrue(
                "the mention should still be present, was '${h.editText.text}'",
                h.editText.text!!.toString().contains(MENTION),
            )
        }
    }

    // ── the watcher relay ───────────────────────────────────────────────────

    @Test
    fun theWatcherRoundTrips() {
        withEditText { h ->
            val watcher = RecordingWatcher()
            h.editText.setTextWatcher(watcher)
            assertSame(watcher, h.editText.getTextWatcher())
        }
    }

    @Test
    fun theWatcherCanBeCleared() {
        withEditText { h ->
            h.editText.setTextWatcher(RecordingWatcher())
            h.editText.setTextWatcher(null)
            assertNull(h.editText.getTextWatcher())
        }
    }

    @Test
    fun everyTextChangeStageIsRelayedToTheWatcher() {
        // The internal watcher forwards all three stages by hand, so a missed one would
        // be a silent gap for that stage alone.
        withEditText(withSpan = false) { h ->
            val watcher = RecordingWatcher()
            h.editText.setTextWatcher(watcher)

            h.editText.append("!")

            assertTrue("before missing: ${watcher.events}", "before" in watcher.events)
            assertTrue("changed missing: ${watcher.events}", "changed" in watcher.events)
            assertTrue("after missing: ${watcher.events}", "after" in watcher.events)
        }
    }

    @Test
    fun aClearedWatcherStopsReceivingEvents() {
        withEditText(withSpan = false) { h ->
            val watcher = RecordingWatcher()
            h.editText.setTextWatcher(watcher)
            h.editText.setTextWatcher(null)

            h.editText.append("!")

            assertTrue("a detached watcher should hear nothing", watcher.events.isEmpty())
        }
    }

    // ── the selection listener ──────────────────────────────────────────────

    @Test
    fun anOrdinaryCaretMoveIsReportedToTheListener() {
        withEditText(withSpan = false) { h ->
            var reported: Pair<Int, Int>? = null
            h.editText.setOnSelectionChangedListener { start, end -> reported = start to end }

            h.editText.setSelection(4)

            assertEquals(4 to 4, reported)
        }
    }

    @Test
    fun aSelectionRangeIsReportedWithBothEnds() {
        withEditText(withSpan = false) { h ->
            var reported: Pair<Int, Int>? = null
            h.editText.setOnSelectionChangedListener { start, end -> reported = start to end }

            h.editText.setSelection(2, 5)

            assertEquals(2 to 5, reported)
        }
    }

    @Test
    fun aBouncedCaretIsNotReportedAtThePositionItWasBouncedFrom() {
        // The adjustment path returns before notifying, and the re-entrant call it
        // triggers is suppressed by the guard flag. So an integrator watching the caret
        // never sees the illegal position — which is the point, but it also means they
        // see nothing at all for that move.
        withEditText { h ->
            val reported = mutableListOf<Pair<Int, Int>>()
            h.editText.setOnSelectionChangedListener { start, end -> reported += start to end }

            h.editText.setSelection(SPAN_START + 2)

            assertTrue(
                "the illegal position must never be reported, saw $reported",
                reported.none { it.first == SPAN_START + 2 },
            )
        }
    }

    @Test
    fun theSelectionListenerCanBeCleared() {
        withEditText(withSpan = false) { h ->
            var calls = 0
            h.editText.setOnSelectionChangedListener { _, _ -> calls++ }
            h.editText.setOnSelectionChangedListener(null)

            h.editText.setSelection(4)

            assertEquals(0, calls)
        }
    }

    // ── the media listener is a plain property ──────────────────────────────

    @Test
    fun theMediaListenerRoundTrips() {
        withEditText { h ->
            val listener = object : CometChatEditText.OnEditTextMediaListener {
                override fun onMediaSelected(uri: android.net.Uri, mimeType: String) = Unit
            }
            h.editText.onEditTextMediaListener = listener
            assertSame(listener, h.editText.onEditTextMediaListener)

            h.editText.onEditTextMediaListener = null
            assertNull(h.editText.onEditTextMediaListener)
        }
    }
}
