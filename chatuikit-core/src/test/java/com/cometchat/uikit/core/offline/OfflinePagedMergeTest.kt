package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.exceptions.CometChatException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [OfflinePagedMerge]: the order-preserving cached → server page merge used by the reactions list,
 * notification feed, pinned / saved panels and AI chat history (ENG-41134).
 */
class OfflinePagedMergeTest : FunSpec({

    data class Row(val id: String, val v: Int = 0)

    val key: (Row) -> Any = { it.id }
    fun rows(vararg ids: String) = ids.map { Row(it) }

    test("cached page appends only rows not already shown and returns them as the window") {
        val r = OfflinePagedMerge.applyCached(rows("a", "b"), rows("b", "c", "c", "d"), key)
        r.rows.map { it.id } shouldBe listOf("a", "b", "c", "d")
        r.window shouldBe setOf("c", "d")
        r.added shouldBe 2
    }

    test("an empty cached page changes nothing and opens no window") {
        val current = rows("a")
        val r = OfflinePagedMerge.applyCached(current, emptyList(), key)
        r.rows shouldBe current
        r.window shouldBe emptySet()
        r.added shouldBe 0
    }

    test("server page replaces the cached window in place, in server order, with no duplicates") {
        val afterCached = OfflinePagedMerge.applyCached(rows("a", "b"), rows("c", "d", "e"), key)
        val r = OfflinePagedMerge.applyServer(afterCached.rows, rows("e", "c", "f"), afterCached.window, key)
        // d was in the cached page but not in the server page: the server proved it gone.
        r.rows.map { it.id } shouldBe listOf("a", "b", "e", "c", "f")
        r.window shouldBe emptySet()
    }

    test("server page keeps rows shown after the window (a live insert) where they are") {
        val current = rows("live", "a", "c1", "c2", "tail")
        val r = OfflinePagedMerge.applyServer(current, rows("s1", "s2"), setOf("c1", "c2"), key)
        r.rows.map { it.id } shouldBe listOf("live", "a", "s1", "s2", "tail")
    }

    test("a server row already shown outside the window is refreshed where it is, never repeated") {
        val current = listOf(Row("a", 1), Row("b", 1))
        val r = OfflinePagedMerge.applyServer(current, listOf(Row("b", 2), Row("c")), emptySet(), key)
        r.rows shouldBe listOf(Row("a", 1), Row("b", 2), Row("c"))
        r.added shouldBe 1
    }

    test("an empty server page drops the cached window rows (the server has no more)") {
        val r = OfflinePagedMerge.applyServer(rows("a", "c1"), emptyList(), setOf("c1"), key)
        r.rows.map { it.id } shouldBe listOf("a")
        r.added shouldBe -1
    }

    test("isNotConnected matches only the not-connected code") {
        OfflinePagedMerge.isNotConnected(
            CometChatException(CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE, "x")
        ) shouldBe true
        OfflinePagedMerge.isNotConnected(CometChatException("ERR_OTHER", "x")) shouldBe false
        OfflinePagedMerge.isNotConnected(null) shouldBe false
    }
})
