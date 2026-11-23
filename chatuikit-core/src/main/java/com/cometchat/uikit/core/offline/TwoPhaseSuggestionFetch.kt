package com.cometchat.uikit.core.offline

import androidx.annotation.RestrictTo
import com.cometchat.chat.exceptions.CometChatException

/**
 * One two-phase mentions-suggestion fetch (README §3.9 "Mentions suggestions"), shared by both
 * kits' `CometChatMentionsFormatter`. Create one per `fetchNext(UserFetchListener /
 * GroupMemberFetchListener)` call and route its three callbacks through it. Pure: no SDK, no UI.
 *
 * - **No keyword**: the cached page is shown first ([onCached]); the server page then replaces
 *   exactly the rows that cached page added and appends the rest by id ([onServer]) — no
 *   duplicates.
 * - **Keyword**: server only — a keyword has no cache (BEH-SNAP-04), so [onCached] ignores it.
 * - **Error** ([keepRowsOnError]): a not-connected error after a cached page keeps the cached rows
 *   (the fetch was answered from the cache). Anything else — including a keyword search offline —
 *   takes today's path: the list is cleared, with no error toast.
 *
 * With offline messaging off the SDK fires no cached phase, so only [onServer] runs: rows whose id
 * is already listed are skipped, the rest appended in server order.
 *
 * Public for the UI modules, but [RestrictTo] `LIBRARY_GROUP` — internal to the kit.
 *
 * @param keyword the request's search keyword; null or empty means "no keyword".
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TwoPhaseSuggestionFetch(keyword: String?) {

    /** False for a keyword search: its cached phase is ignored. */
    public val acceptsCached: Boolean = keyword.isNullOrEmpty()

    private val addedByCache = HashSet<String>()
    private var cachedDelivered = false

    /**
     * @return the list with the cached page appended (ids already listed are skipped), or null
     * when this fetch ignores its cached phase (a keyword search) — leave the list untouched.
     */
    public fun <T> onCached(current: List<T>, rows: List<T>, idOf: (T) -> String): List<T>? {
        if (!acceptsCached) return null
        cachedDelivered = true
        val ids = current.mapTo(HashSet(), idOf)
        val out = current.toMutableList()
        for (row in rows) {
            val id = idOf(row)
            if (ids.add(id)) {
                out.add(row)
                addedByCache.add(id)
            }
        }
        return out
    }

    /**
     * @return the list with the rows this fetch's cached page added replaced by the server page:
     * those rows are removed, then every server row whose id is not listed is appended.
     */
    public fun <T> onServer(current: List<T>, rows: List<T>, idOf: (T) -> String): List<T> {
        val base = if (addedByCache.isEmpty()) current else current.filterNot { idOf(it) in addedByCache }
        addedByCache.clear()
        val ids = base.mapTo(HashSet(), idOf)
        val out = base.toMutableList()
        for (row in rows) if (ids.add(idOf(row))) out.add(row)
        return out
    }

    /** True when the shown rows must be kept: a not-connected error after a cached page. */
    public fun keepRowsOnError(e: CometChatException?): Boolean =
        cachedDelivered && SearchOffline.isNotConnected(e)
}
