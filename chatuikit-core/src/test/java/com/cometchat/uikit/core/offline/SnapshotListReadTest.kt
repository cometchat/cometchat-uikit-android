package com.cometchat.uikit.core.offline

import com.cometchat.chat.exceptions.CometChatException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/**
 * The snapshot-list two-phase read (BEH-SNAP-03, BEH-READ-01/09b) shared by the users, groups and
 * group members lists: cached first paint, server reconcile by id, proven omission for a replayed
 * page, stale generations dropped, replace reads never blank.
 */
class SnapshotListReadTest : FunSpec({

    val key: (String) -> String = { it.substringBefore('@') }
    val cached = OfflineListReconcile.Phase.CACHED
    val server = OfflineListReconcile.Phase.SERVER

    test("append: cached page appends new rows only; server page reconciles in place with no duplicates") {
        val op = SnapshotReadOp(gen = 1, hubGen = 0, replace = false)
        val shown = listOf("a", "b")
        val afterCached = SnapshotListRead.apply(shown, 1, op, cached, listOf("b@stale", "c", "d"), key)!!
        afterCached shouldBe listOf("a", "b", "c", "d") // b already shown is left as shown
        val afterServer = SnapshotListRead.apply(afterCached, 1, op, server, listOf("d@new", "c@new", "e"), key)!!
        afterServer shouldBe listOf("a", "b", "d@new", "c@new", "e") // server order for this page's slot
    }

    test("server page removes this read's cached rows it does not hold, never rows from elsewhere") {
        val op = SnapshotReadOp(gen = 1, hubGen = 0, replace = false)
        val afterCached = SnapshotListRead.apply(listOf("a"), 1, op, cached, listOf("x", "y"), key)!!
        val afterServer = SnapshotListRead.apply(afterCached + "live", 1, op, server, listOf("y"), key)!!
        afterServer shouldBe listOf("a", "y", "live")
    }

    test("an empty server page removes this read's cached rows; an empty cached page changes nothing") {
        val op = SnapshotReadOp(gen = 1, hubGen = 0, replace = false)
        SnapshotListRead.apply(listOf("a"), 1, op, cached, emptyList(), key) shouldBe null
        val afterCached = SnapshotListRead.apply(listOf("a"), 1, op, cached, listOf("x"), key)!!
        SnapshotListRead.apply(afterCached, 1, op, server, emptyList(), key) shouldBe listOf("a")
    }

    test("replace: rows shown stay until the first non-empty phase, which replaces them") {
        val op = SnapshotReadOp(gen = 2, hubGen = 0, replace = true)
        val old = listOf("old1", "old2")
        SnapshotListRead.apply(old, 2, op, cached, emptyList(), key) shouldBe null // never blanks
        SnapshotListRead.apply(old, 2, op, server, listOf("n1", "n2"), key) shouldBe listOf("n1", "n2")
    }

    test("replace: cached page replaces, then the server page reconciles it") {
        val op = SnapshotReadOp(gen = 2, hubGen = 0, replace = true)
        val afterCached = SnapshotListRead.apply(listOf("old"), 2, op, cached, listOf("a", "b"), key)!!
        afterCached shouldBe listOf("a", "b")
        SnapshotListRead.apply(afterCached, 2, op, server, listOf("b", "c"), key) shouldBe listOf("b", "c")
    }

    test("a stale generation is dropped") {
        val op = SnapshotReadOp(gen = 1, hubGen = 0, replace = false)
        SnapshotListRead.apply(listOf("a"), 2, op, cached, listOf("x"), key) shouldBe null
        SnapshotListRead.apply(listOf("a"), 2, op, server, listOf("x"), key) shouldBe null
    }

    test("collect: a read whose generation moved on reports Dropped and leaves the rows alone") {
        runTest {
            var gen = 1L
            val rows = MutableStateFlow(listOf("a"))
            val op = SnapshotReadOp(gen = 1, hubGen = OfflineHub.gen, replace = false)
            val phases = flowOf<TwoPhaseResult<String>>(TwoPhaseResult.Cached(listOf("x")), TwoPhaseResult.Server(listOf("y")))
            var painted = 0
            val end = SnapshotListRead.collect(phases, op, { gen.also { gen = 2 } }, rows, key) { painted++ }
            end shouldBe SnapshotListRead.End.Dropped
            painted shouldBe 0
            rows.value shouldBe listOf("a")
        }
    }

    test("collect: cached then server ends Server; a cached-then-error read ends Failed with cached rows kept") {
        runTest {
            val rows = MutableStateFlow(emptyList<String>())
            val ok = SnapshotListRead.collect(
                flowOf(TwoPhaseResult.Cached(listOf("a")), TwoPhaseResult.Server(listOf("a", "b"))),
                SnapshotReadOp(1, OfflineHub.gen, false), { 1L }, rows, key
            ) {}
            ok shouldBe SnapshotListRead.End.Server(pageEmpty = false)
            rows.value shouldBe listOf("a", "b")

            val offline = CometChatException(SnapshotListRead.NOT_CONNECTED, "offline")
            val failed = SnapshotListRead.collect(
                flowOf(TwoPhaseResult.Cached(listOf("c")), TwoPhaseResult.Error(offline)),
                SnapshotReadOp(1, OfflineHub.gen, false), { 1L }, rows, key
            ) {}
            failed.shouldBeInstanceOf<SnapshotListRead.End.Failed>()
            SnapshotListRead.isNotConnected(failed.exception) shouldBe true
            rows.value shouldBe listOf("a", "b", "c")
        }
    }
})
