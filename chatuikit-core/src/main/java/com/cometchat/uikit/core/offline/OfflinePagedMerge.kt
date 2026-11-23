package com.cometchat.uikit.core.offline

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.exceptions.CometChatException

/**
 * The order-preserving two-phase page merge for the panel lists that keep the **server's order**
 * (README §3.9: reactions list, notification feed, pinned / saved panels, AI chat history).
 *
 * [OfflineListReconcile] re-sorts by `(sortAt, serverId)`, which is right for the message and
 * conversation lists but would reorder these panels (pinned is newest-**pin** first, saved is
 * newest-**save** first, reactions and the feed come in the server's own order). This merge never
 * sorts: each page is placed where the server put it, and rows match by key, so a row is never
 * shown twice (BEH-PIN-02, BEH-SAVE-02, BEH-SNAP-03).
 *
 * One read operation = one cached phase (optional) then one server phase or an error:
 * - [applyCached] appends the cached rows not already shown and returns their keys — the
 *   operation's **window**. An empty cached page changes nothing and never ends the list.
 * - [applyServer] replaces that window with the server page **in place** (at the window's first
 *   row, or at the end when the window is empty). A server row whose key is already shown outside
 *   the window is a re-delivery: it replaces that row where it is and is not added again.
 *
 * Pure: no locks, no SDK, returns a new list.
 */
internal object OfflinePagedMerge {

    /**
     * @property rows the merged list.
     * @property window after [applyCached], the keys this phase appended; pass them to the same
     * operation's [applyServer]. Empty after [applyServer].
     * @property added `rows.size - current.size` (negative when the server dropped window rows).
     */
    data class Result<T>(val rows: List<T>, val window: Set<Any>, val added: Int)

    fun <T> applyCached(current: List<T>, cached: List<T>, keyOf: (T) -> Any): Result<T> {
        if (cached.isEmpty()) return Result(current, emptySet(), 0)
        val shown = current.mapTo(HashSet()) { keyOf(it) }
        val window = LinkedHashSet<Any>()
        val out = ArrayList<T>(current.size + cached.size)
        out.addAll(current)
        for (row in cached) {
            val key = keyOf(row)
            if (key in shown || !window.add(key)) continue
            out.add(row)
        }
        return Result(out, window, out.size - current.size)
    }

    fun <T> applyServer(current: List<T>, server: List<T>, window: Set<Any>, keyOf: (T) -> Any): Result<T> {
        val insertAt = current.indexOfFirst { keyOf(it) in window }.let { if (it < 0) current.size else it }
        val serverByKey = LinkedHashMap<Any, T>(server.size)
        for (row in server) serverByKey.putIfAbsent(keyOf(row), row)

        val before = ArrayList<T>()
        val after = ArrayList<T>()
        current.forEachIndexed { index, row ->
            val key = keyOf(row)
            if (key in window) return@forEachIndexed // the server page replaces the window
            val target = if (index < insertAt) before else after
            val fresh = serverByKey.remove(key)
            target.add(fresh ?: row)                 // re-delivery: refreshed where it is
        }
        val out = ArrayList<T>(before.size + serverByKey.size + after.size)
        out.addAll(before)
        out.addAll(serverByKey.values)
        out.addAll(after)
        return Result(out, emptySet(), out.size - current.size)
    }

    /** True for the platform's not-connected error, which ends every offline read (BEH-READ-05). */
    fun isNotConnected(e: CometChatException?): Boolean =
        e?.code == CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE
}
