package com.cometchat.uikit.core.offline

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * The fetch-all loop shared by the pinned and saved panels, on the SDK 5.1.0 two-phase
 * `MessagesRequest.fetchNext(MessageFetchListener)` (BEH-PIN-02, BEH-SAVE-02).
 *
 * Each iteration is one `fetchNext`: an optional cached page, then the server page or an error.
 * - **Offline off** ([offlineOn] false): the cached phase is ignored, so the loop is exactly
 *   today's: accumulate server pages, deduped by id, until an empty server page or [maxRows].
 *   Any error fails the load with no rows.
 * - **Offline on**: every cached page is painted at once (rows not yet confirmed by a server page
 *   follow the confirmed ones). Server rows are kept in **server order**, deduped by id, so "load
 *   more" never repeats a row. The loop ends only on an **empty server page** (the server's
 *   `hasNext` = false) — never on an empty cached page — and then drops cached rows the server
 *   never returned. A not-connected error after a cached page that added rows reads the next
 *   cached page (the SDK advances its cursor past them, BEH-PIN-02); otherwise the error ends the
 *   load and the rows shown so far are kept.
 */
internal object PinnedSavedTwoPhaseLoad {

    sealed class Outcome {
        /** The server confirmed the whole list (or [maxRows] was reached). */
        data class Loaded(val rows: List<BaseMessage>) : Outcome()

        /** The load ended on [exception]; [rows] are what to keep (always empty when offline is off). */
        data class Failed(val exception: CometChatException, val rows: List<BaseMessage>) : Outcome()
    }

    private sealed class Page {
        data class Server(val rows: List<BaseMessage>) : Page()
        data class Error(val exception: CometChatException) : Page()
    }

    /**
     * @param paint called with the rows to show after every cached page and every server page
     * (offline on only); the caller drops it for a stale generation.
     */
    suspend fun loadAll(
        request: MessagesRequest,
        offlineOn: Boolean,
        maxRows: Int,
        paint: (List<BaseMessage>) -> Unit
    ): Outcome {
        val confirmed = ArrayList<BaseMessage>()
        val confirmedIds = HashSet<Long>()
        // Cached rows no server page has returned yet, in cache order.
        val unconfirmed = LinkedHashMap<Long, BaseMessage>()
        fun shown(): List<BaseMessage> = confirmed + unconfirmed.values

        while (confirmed.size < maxRows) {
            var cachedAddedRows = false
            val page = fetchNext(request) { cached ->
                if (!offlineOn) return@fetchNext
                for (message in cached) {
                    if (message.id in confirmedIds || unconfirmed.containsKey(message.id)) continue
                    unconfirmed[message.id] = message
                    cachedAddedRows = true
                }
                paint(shown())
            }
            when (page) {
                is Page.Server -> {
                    // The server's hasNext = false: cached rows it never returned are gone.
                    if (page.rows.isEmpty()) return Outcome.Loaded(ArrayList(confirmed))
                    for (message in page.rows) {
                        if (confirmedIds.add(message.id)) confirmed.add(message)
                        unconfirmed.remove(message.id)
                    }
                    if (offlineOn) paint(shown())
                }

                is Page.Error -> {
                    val readNextCachedPage = offlineOn &&
                        OfflinePagedMerge.isNotConnected(page.exception) &&
                        cachedAddedRows &&
                        confirmed.size + unconfirmed.size < maxRows
                    if (readNextCachedPage) continue
                    return Outcome.Failed(page.exception, if (offlineOn) shown() else emptyList())
                }
            }
        }
        return Outcome.Loaded(ArrayList(confirmed))
    }

    private suspend fun fetchNext(request: MessagesRequest, onCached: (List<BaseMessage>) -> Unit): Page =
        suspendCancellableCoroutine { cont ->
            request.fetchNext(object : CometChat.MessageFetchListener() {
                override fun onCachedMessages(messages: List<BaseMessage>) {
                    if (cont.isActive) onCached(messages)
                }

                override fun onServerMessages(messages: List<BaseMessage>) {
                    if (cont.isActive) cont.resume(Page.Server(messages))
                }

                override fun onError(exception: CometChatException) {
                    if (cont.isActive) cont.resume(Page.Error(exception))
                }
            })
        }
}
