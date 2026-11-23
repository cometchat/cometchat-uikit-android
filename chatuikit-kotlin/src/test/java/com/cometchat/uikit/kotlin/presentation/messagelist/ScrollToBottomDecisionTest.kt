package com.cometchat.uikit.kotlin.presentation.messagelist

import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.kotlin.presentation.messagelist.ui.bottomMessageIdentity
import com.cometchat.uikit.kotlin.presentation.messagelist.ui.shouldScrollToBottomOnUpdate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for the message-list auto-scroll decision (View / XML stack).
 *
 * Covers the append-vs-prepend fix (ENG-37938) and the pre-existing branch behaviour that
 * must survive it. Bottom identity is the collision-free id-or-muid composite produced by
 * [bottomMessageIdentity] — plain id collides for optimistic own sends (all id == 0 until
 * ack). Pure logic — no RecyclerView required.
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
            wasAtBottom = true,          // even a stale wasAtBottom must not trip it
            scrollToBottomOnNewMessage = true,
            lastIsOwn = true,            // even if the bottom happens to be own
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

    // Regression for the review finding: after an own send acks in place (muid:A → id:Aserver),
    // a following prepend leaves the (now id-keyed) bottom identity unchanged → must NOT scroll.
    // Depends on tracking being refreshed to the acked identity (see the Compose tracking test).
    test("prepend after in-place ack (identity already id-keyed) does not scroll") {
        shouldScrollToBottomOnUpdate(
            previousCount = 21,
            newCount = 51,
            previousLastId = "id:5001",  // tracked identity refreshed on ack
            newLastId = "id:5001",       // same bottom message, older page prepended above
            wasAtBottom = true,
            scrollToBottomOnNewMessage = true,
            lastIsOwn = true,
            lastVisiblePos = 20
        ) shouldBe false
    }

    // Invariant 2 — incoming append at the bottom with scrollToBottomOnNewMessage=true scrolls.
    test("append at bottom with scrollToBottomOnNewMessage=true scrolls") {
        shouldScrollToBottomOnUpdate(
            previousCount = 20,
            newCount = 21,
            previousLastId = "id:100",
            newLastId = "id:101",        // new bottom message
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
            wasAtBottom = false,         // exercise the own-message path
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
})
