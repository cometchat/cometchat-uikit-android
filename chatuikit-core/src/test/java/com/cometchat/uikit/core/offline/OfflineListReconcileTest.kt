package com.cometchat.uikit.core.offline

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The windowed reconcile (§2.4, BEH-READ-01/07/09b/10/30). Exercised over a tiny local row type,
 * since the function is generic over key and sort.
 */
class OfflineListReconcileTest : FunSpec({

    data class Row(val key: String, val sid: Long, val sortAt: Long, val upd: Long = 1L, val compose: Long = 0L)

    val spec = OfflineListReconcile.Spec<Row, Long>(
        keyOf = { it.key },
        serverIdOf = { if (it.sid > 0) it.sid else null },
        sortAtOf = { it.sortAt },
        updatedAtOf = { it.upd },
        composeAtOf = { it.compose }
    )

    fun server(gen: Long, rows: List<Row>, cachedKeys: Set<Any> = emptySet()) =
        OfflineListReconcile.Op(gen, OfflineListReconcile.Phase.SERVER, rows, cachedKeys)
    fun cached(gen: Long, rows: List<Row>) = OfflineListReconcile.Op(gen, OfflineListReconcile.Phase.CACHED, rows)

    fun acked(id: Long, at: Long, upd: Long = 1L) = Row("s:$id", id, at, upd)
    fun unacked(key: String, at: Long, compose: Long) = Row(key, 0L, at, 1L, compose)

    test("cached then server: rows placed in sorted order, upserted in place") {
        val afterCached = OfflineListReconcile.reconcile(
            emptyList(), 0L, cached(0L, listOf(acked(2, 20), acked(1, 10))), spec, hadMore = true
        )
        afterCached.applied shouldBe true
        afterCached.rows.map { it.sid } shouldBe listOf(1L, 2L)

        val afterServer = OfflineListReconcile.reconcile(
            afterCached.rows, 0L, server(0L, listOf(acked(1, 10, upd = 5), acked(2, 20, upd = 5), acked(3, 30))), spec, hadMore = true
        )
        afterServer.rows.map { it.sid } shouldBe listOf(1L, 2L, 3L)
    }

    test("cached phase reports the keys of its rows with a server id") {
        val out = OfflineListReconcile.reconcile(
            emptyList(), 0L, cached(0L, listOf(acked(1, 10), unacked("l:a", 25, 5))), spec, hadMore = true
        )
        out.cachedKeys shouldBe setOf<Any>("s:1")
    }

    test("proven omission removes an in-range cached row that is absent, keeps out-of-range rows") {
        val cachedRows = listOf(acked(1, 10), acked(2, 20), acked(3, 30), acked(4, 40))
        val afterCached = OfflineListReconcile.reconcile(emptyList(), 0L, cached(0L, cachedRows), spec, hadMore = true)
        val out = OfflineListReconcile.reconcile(
            afterCached.rows, 0L,
            server(0L, listOf(acked(1, 10), acked(3, 30)), afterCached.cachedKeys), spec, hadMore = true
        )
        // range is (10,1)..(30,3): id 2 was in range and absent → removed; id 4 at 40 is out of range → kept.
        out.rows.map { it.sid } shouldBe listOf(1L, 3L, 4L)
    }

    test("range uses the (sortAt, id) tuple: a tie on sortAt past the last returned id is kept") {
        val cachedRows = listOf(acked(1, 10), acked(3, 30), acked(4, 30))
        val keys = cachedRows.mapTo(HashSet<Any>()) { it.key }
        val out = OfflineListReconcile.reconcile(
            cachedRows, 0L, server(0L, listOf(acked(1, 10), acked(3, 30)), keys), spec, hadMore = true
        )
        // id4@30 sorts after (30,3), the page's max — outside the proven range, so it survives.
        out.rows.map { it.sid } shouldBe listOf(1L, 3L, 4L)
    }

    test("a row that did not come from this op's cached phase is never pruned") {
        // id 2 is in range and absent, but arrived live (not in cachedKeys).
        val current = listOf(acked(1, 10), acked(2, 20), acked(3, 30))
        val out = OfflineListReconcile.reconcile(
            current, 0L,
            server(0L, listOf(acked(1, 10), acked(3, 30)), cachedKeys = setOf("s:1", "s:3")), spec, hadMore = true
        )
        out.rows.map { it.sid } shouldBe listOf(1L, 2L, 3L)
    }

    test("a row without a server id is never removed by the server phase") {
        val current = listOf(acked(1, 10), unacked("l:a", 25, compose = 5), acked(3, 30))
        val out = OfflineListReconcile.reconcile(
            current, 0L,
            server(0L, listOf(acked(1, 10), acked(3, 30)), cachedKeys = setOf("s:1", "s:3", "l:a")),
            spec, hadMore = true
        )
        out.rows.map { it.key } shouldBe listOf("s:1", "s:3", "l:a")
    }

    test("newer updatedAt wins; an older server copy does not revert a live edit") {
        val current = listOf(acked(1, 10, upd = 9))
        val out = OfflineListReconcile.reconcile(
            current, 0L, server(0L, listOf(acked(1, 10, upd = 3))), spec, hadMore = true
        )
        out.rows.single().upd shouldBe 9L
    }

    test("empty cached page never ends history; empty online server page does") {
        val cachedEmpty = OfflineListReconcile.reconcile(
            listOf(acked(1, 10)), 0L, cached(0L, emptyList()), spec, hadMore = true
        )
        cachedEmpty.hasMore shouldBe true

        val serverEmpty = OfflineListReconcile.reconcile(
            listOf(acked(1, 10)), 0L, server(0L, emptyList()), spec, hadMore = true
        )
        serverEmpty.hasMore shouldBe false
        serverEmpty.rows.map { it.sid } shouldBe listOf(1L) // nothing pruned on an empty page
    }

    test("stale gen is dropped, list untouched") {
        val current = listOf(acked(1, 10))
        val out = OfflineListReconcile.reconcile(
            current, listGen = 2L, op = server(1L, listOf(acked(5, 50))), spec, hadMore = true
        )
        out.applied shouldBe false
        out.rows shouldBe current
    }

    test("sort places un-acked rows last, by compose time, regardless of sortAt") {
        val current = listOf(unacked("l:a", 5, compose = 100), acked(2, 20), acked(1, 10), unacked("l:b", 99, compose = 50))
        val out = OfflineListReconcile.reconcile(
            current, 0L, cached(0L, emptyList()), spec, hadMore = true
        )
        // acked by sortAt (1 then 2), then un-acked by compose (l:b=50 then l:a=100).
        out.rows.map { it.key } shouldBe listOf("s:1", "s:2", "l:b", "l:a")
    }

    test("upsert uses the alias resolver: a pending l:x row and its acked s:5 copy collapse into one row") {
        val aliases = mapOf<Any, Any>("l:x" to "s:5")
        val aliasSpec = spec.copy(resolveKey = { aliases[it] ?: it })
        val pending = Row("l:x", 0L, 50, upd = 0L, compose = 7)
        val acked = Row("s:5", 5L, 50, upd = 3L)
        val out = OfflineListReconcile.reconcile(
            listOf(acked(1, 10), pending), 0L, server(0L, listOf(acked)), aliasSpec, hadMore = true
        )
        out.rows.map { it.key } shouldBe listOf("s:1", "s:5")

        // Without the resolver (identity default) they stay two rows.
        val plain = OfflineListReconcile.reconcile(
            listOf(acked(1, 10), pending), 0L, server(0L, listOf(acked)), spec, hadMore = true
        )
        plain.rows.map { it.key } shouldBe listOf("s:1", "s:5", "l:x")
    }

    // ---- conversation-shaped: String ids, newest-first ----

    data class Conv(val id: String?, val lastAt: Long, val upd: Long = 1L, val local: String = "p:$id") {
        val key: String get() = id ?: local
    }

    val convSpec = OfflineListReconcile.Spec<Conv, String>(
        keyOf = { it.key },
        serverIdOf = { it.id },
        sortAtOf = { it.lastAt },
        updatedAtOf = { it.upd },
        composeAtOf = { it.lastAt },
        descending = true
    )

    test("conversations: String ids, newest-first order, proven omission and a row without a server id") {
        val cachedRows = listOf(Conv("c1", 40), Conv("c2", 30), Conv("c3", 20), Conv("c4", 10))
        val afterCached = OfflineListReconcile.reconcile(
            emptyList(), 0L, OfflineListReconcile.Op(0L, OfflineListReconcile.Phase.CACHED, cachedRows), convSpec, true
        )
        afterCached.rows.map { it.key } shouldBe listOf("c1", "c2", "c3", "c4")

        val pending = Conv(null, 25, local = "p:new")
        val withPending = afterCached.rows + pending
        // Server page (newest-first) covers (20,c3)..(40,c1) and omits c2 → c2 proven gone;
        // c4 at 10 is outside the range → kept; the row without a server id is never removed.
        val out = OfflineListReconcile.reconcile(
            withPending, 0L,
            OfflineListReconcile.Op(0L, OfflineListReconcile.Phase.SERVER, listOf(Conv("c1", 40), Conv("c3", 20)), afterCached.cachedKeys),
            convSpec, true
        )
        out.rows.map { it.key } shouldBe listOf("p:new", "c1", "c3", "c4")
    }

    test("conversations: a sortAt tie is resolved on the String id in descending order too") {
        val cachedRows = listOf(Conv("a", 30), Conv("b", 30), Conv("c", 30), Conv("z", 10))
        val keys = cachedRows.mapTo(HashSet<Any>()) { it.key }
        val out = OfflineListReconcile.reconcile(
            cachedRows, 0L,
            OfflineListReconcile.Op(0L, OfflineListReconcile.Phase.SERVER, listOf(Conv("c", 30), Conv("b", 30)), keys),
            convSpec, true
        )
        // Range is (30,"b")..(30,"c"): "a" sorts before "b" → outside, kept; "z" at 10 → outside, kept.
        out.rows.map { it.key } shouldBe listOf("c", "b", "a", "z")
    }
})
