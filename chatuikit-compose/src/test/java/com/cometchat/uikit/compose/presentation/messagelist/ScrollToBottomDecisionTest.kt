package com.cometchat.uikit.compose.presentation.messagelist

import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.compose.presentation.messagelist.ui.bottomMessageIdentity
import com.cometchat.uikit.compose.presentation.messagelist.ui.shouldScrollToBottomOnUpdate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for the message-list auto-scroll decision (Compose stack).
 *
 * Mirrors the View stack's ScrollToBottomDecisionTest — same matrix, same expected values —
 * to keep the two UI stacks behaviourally identical (ENG-37938, 2x2 rule). Bottom identity is
 * the collision-free id-or-muid composite ([bottomMessageIdentity]); plain id collides for
 * optimistic own sends (all id == 0 until ack). Pure logic — no LazyColumn / Compose runtime
 * required.
 */
class ScrollToBottomDecisionTest : FunSpec({

    // ---- bottomMessageIdentity: collision-free composite ----------------------------------

    test("bottomMessageIdentity uses id when the message is acked (id > 0)") {
        val m = mock<BaseMessage> {
            whenever(it.id).thenReturn(4321L)
        }
        bottomMessageIdentity(m) shouldBe "id:4321"
    }

    test("bottomMessageIdentity falls back to muid for an un-acked own message (id == 0)") {
        val m = mock<BaseMessage> {
            whenever(it.id).thenReturn(0L)
            whenever(it.muid).thenReturn("muid-A")
        }
        bottomMessageIdentity(m) shouldBe "muid:muid-A"
    }

    test("bottomMessageIdentity is null for an empty list") {
        bottomMessageIdentity(null) shouldBe null
    }

    test("two distinct un-acked own sends get distinct identities (no id==0 collision)") {
        val a = mock<BaseMessage> {
            whenever(it.id).thenReturn(0L)
            whenever(it.muid).thenReturn("muid-A")
        }
        val b = mock<BaseMessage> {
            whenever(it.id).thenReturn(0L)
            whenever(it.muid).thenReturn("muid-B")
        }
        (bottomMessageIdentity(a) != bottomMessageIdentity(b)) shouldBe true
    }

    // ---- append vs prepend -----------------------------------------------------------------

    // Invariant 1 — a prepend (older page from pagination) grows the count but leaves the
    // bottom message id unchanged → must NOT scroll (the fix).
    test("prepend does not scroll even if user appears at bottom") {
        shouldScrollToBottomOnUpdate(
            previousCount = 20,
            newCount = 40,
            previousLastId = "id:100",
            newLastId = "id:100",        // bottom message unchanged
            wasAtBottom = true,
            scrollToBottomOnNewMessage = true,
            lastIsOwn = true,
            lastVisiblePos = 19
        ) shouldBe false
    }

    test("prepend while scrolled up does not scroll") {
        shouldScrollToBottomOnUpdate(
            previousCount = 20,
            newCount = 40,
            previousLastId = "id:100",
            newLastId = "id:100",
            wasAtBottom = false,
            scrollToBottomOnNewMessage = true,
            lastIsOwn = false,
            lastVisiblePos = 2
        ) shouldBe false
    }

    // Invariant 2 — incoming append at the bottom with scrollToBottomOnNewMessage=true scrolls.
    test("append at bottom with scrollToBottomOnNewMessage=true scrolls") {
        shouldScrollToBottomOnUpdate(
            previousCount = 20,
            newCount = 21,
            previousLastId = "id:100",
            newLastId = "id:101",
            wasAtBottom = true,
            scrollToBottomOnNewMessage = true,
            lastIsOwn = false,
            lastVisiblePos = 19
        ) shouldBe true
    }

    // Review finding — two consecutive own sends while un-acked both have id == 0; keyed on the
    // muid composite their identities differ, so the second send is still an append and scrolls
    // (plain id would collide at id:0 and wrongly suppress the scroll — the false negative).
    test("second consecutive un-acked own send still scrolls (no id==0 false negative)") {
        shouldScrollToBottomOnUpdate(
            previousCount = 21,
            newCount = 22,
            previousLastId = "muid:muid-A",
            newLastId = "muid:muid-B",
            wasAtBottom = false,
            scrollToBottomOnNewMessage = true,
            lastIsOwn = true,
            lastVisiblePos = 5
        ) shouldBe true
    }

    // Invariant 3 — scrollToBottomOnNewMessage=false: within 5 of bottom scrolls, beyond doesn't.
    test("append within 5 of bottom scrolls when policy disabled") {
        shouldScrollToBottomOnUpdate(
            previousCount = 20,
            newCount = 21,
            previousLastId = "id:100",
            newLastId = "id:101",
            wasAtBottom = true,
            scrollToBottomOnNewMessage = false,
            lastIsOwn = false,
            lastVisiblePos = 18          // (20-1)-18 = 1 < 5
        ) shouldBe true
    }

    test("append beyond 5 of bottom does not scroll when policy disabled") {
        shouldScrollToBottomOnUpdate(
            previousCount = 20,
            newCount = 21,
            previousLastId = "id:100",
            newLastId = "id:101",
            wasAtBottom = true,
            scrollToBottomOnNewMessage = false,
            lastIsOwn = false,
            lastVisiblePos = 10          // (20-1)-10 = 9 >= 5
        ) shouldBe false
    }

    // Invariant 4 — own sent message always scrolls even when wasAtBottom is stale/false.
    test("own appended message scrolls even when not at bottom") {
        shouldScrollToBottomOnUpdate(
            previousCount = 20,
            newCount = 21,
            previousLastId = "id:100",
            newLastId = "id:101",
            wasAtBottom = false,
            scrollToBottomOnNewMessage = true,
            lastIsOwn = true,
            lastVisiblePos = 5
        ) shouldBe true
    }

    // Invariant 5 — an incoming append while scrolled far up must not yank to bottom.
    test("incoming append while scrolled far up does not scroll") {
        shouldScrollToBottomOnUpdate(
            previousCount = 20,
            newCount = 21,
            previousLastId = "id:100",
            newLastId = "id:101",
            wasAtBottom = false,
            scrollToBottomOnNewMessage = true,
            lastIsOwn = false,
            lastVisiblePos = 3
        ) shouldBe false
    }

    // First load (empty → populated) is treated as an append and scrolls when at bottom.
    test("first load scrolls to bottom") {
        shouldScrollToBottomOnUpdate(
            previousCount = 0,
            newCount = 20,
            previousLastId = null,
            newLastId = "id:120",
            wasAtBottom = true,
            scrollToBottomOnNewMessage = true,
            lastIsOwn = false,
            lastVisiblePos = -1
        ) shouldBe true
    }

    // A pure in-place update (edit/delete/receipt — count unchanged) is not an append. This is
    // also the shape of an optimistic own message acking in place (0 → serverId): count is
    // unchanged, so no scroll — the effect only refreshes its tracked identity.
    test("no count change does not scroll") {
        shouldScrollToBottomOnUpdate(
            previousCount = 20,
            newCount = 20,
            previousLastId = "muid:muid-A",
            newLastId = "id:5001",       // ack flipped the identity, but count is unchanged
            wasAtBottom = true,
            scrollToBottomOnNewMessage = true,
            lastIsOwn = true,
            lastVisiblePos = 19
        ) shouldBe false
    }

    // ---- Compose tracking sequence ---------------------------------------------------------
    //
    // The Compose auto-scroll effect is keyed on (messages.size, bottomIdentity), so it runs on
    // every size change AND every in-place identity change — including an optimistic own message
    // acking 0 → serverId with NO size change. [Tracker] below mirrors that contract: each run
    // decides via shouldScrollToBottomOnUpdate, then refreshes the tracked (count, identity).
    // These sequence tests reproduce the two concrete review scenarios end to end.

    // Scenario (a): send A (scrolls), A acks in place, send B → B still scrolls.
    test("sequence: own send, in-place ack, own send — both sends scroll") {
        val t = Tracker()
        t.prime(count = 20, identity = "id:S")                                   // server page loaded
        t.onUpdate(count = 21, identity = "muid:A", lastIsOwn = true) shouldBe true   // send A appends
        t.onUpdate(count = 21, identity = "id:A", lastIsOwn = true) shouldBe false    // A acks in place
        t.onUpdate(count = 22, identity = "muid:B", lastIsOwn = true) shouldBe true   // send B appends
    }

    // Scenario (b): after sending A (which acks in place), a fling-up prepend must NOT snap to
    // bottom. This only holds because the ack refreshed the tracked identity to "id:A"; with
    // stale tracking ("muid:A") the prepend would look like a bottom change and wrongly scroll.
    test("sequence: own send, in-place ack, prepend — prepend does not scroll") {
        val t = Tracker()
        t.prime(count = 20, identity = "id:S")
        t.onUpdate(count = 21, identity = "muid:A", lastIsOwn = true) shouldBe true   // send A appends
        t.onUpdate(count = 21, identity = "id:A", lastIsOwn = true) shouldBe false    // A acks in place
        t.onUpdate(count = 51, identity = "id:A", lastIsOwn = true) shouldBe false    // fling-up prepend
    }
})

/**
 * Test harness mirroring the CometChatMessageList Compose auto-scroll effect's tracking
 * contract. The real effect is keyed on (size, bottomIdentity) and, after deciding, writes
 * previousMessageCount / previousLastMessageId. This models the same book-keeping so a whole
 * update sequence (append → in-place ack → prepend) can be asserted without a Compose runtime.
 */
private class Tracker {
    private var prevCount = 0
    private var prevId: String? = null

    /** Seed tracking with the first non-empty page (the dedicated initial-scroll effect owns
     *  the first scroll, so this never scrolls). */
    fun prime(count: Int, identity: String?) {
        prevCount = count
        prevId = identity
    }

    fun onUpdate(
        count: Int,
        identity: String?,
        wasAtBottom: Boolean = true,
        scrollToBottomOnNewMessage: Boolean = true,
        lastIsOwn: Boolean = false,
        lastVisiblePos: Int = Int.MAX_VALUE
    ): Boolean {
        val scroll = prevCount > 0 && shouldScrollToBottomOnUpdate(
            previousCount = prevCount,
            newCount = count,
            previousLastId = prevId,
            newLastId = identity,
            wasAtBottom = wasAtBottom,
            scrollToBottomOnNewMessage = scrollToBottomOnNewMessage,
            lastIsOwn = lastIsOwn,
            lastVisiblePos = lastVisiblePos
        )
        prevCount = count
        prevId = identity
        return scroll
    }
}
