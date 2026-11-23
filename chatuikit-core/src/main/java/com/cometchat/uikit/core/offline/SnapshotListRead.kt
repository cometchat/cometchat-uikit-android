package com.cometchat.uikit.core.offline

import com.cometchat.chat.exceptions.CometChatException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformWhile

/**
 * One two-phase page read of a snapshot list (users, groups, group members — BEH-SNAP-01..07).
 *
 * @property gen the list generation the read started under; a phase is dropped once the list has
 * moved on (refresh, new search) or the session changed ([hubGen] vs [OfflineHub.gen]).
 * @property replace true for a read that replaces the list (refresh, search, clearing a search):
 * the rows already shown stay until this read's first non-empty phase, which replaces them — so
 * the list never blanks. False for "load the next page", which appends.
 */
internal class SnapshotReadOp(
    val gen: Long,
    val hubGen: Long,
    val replace: Boolean
) {
    /** Keys this read's cached phase added to the list — the only rows its server phase may remove. */
    var cachedKeys: Set<Any> = emptySet()

    /** For a [replace] read: whether a phase has replaced the list yet. */
    var replaced: Boolean = false
}

/**
 * The ON-path two-phase read shared by the users, groups and group members ViewModels (README §3.3,
 * §3.9 "Users, groups, group members"). The OFF path never comes here: it keeps today's
 * single-phase use-case call, whose data source answers from the server phase only.
 *
 * Rules (BEH-READ-01, BEH-SNAP-03/04):
 * - The cached page is the first paint. An empty cached page changes nothing and never ends the
 *   list; a cached row already shown is left as shown (a live update is never reverted by replay).
 * - The server page reconciles in place by id with [OfflineListReconcile]: no duplicates, rows
 *   already shown keep their place, this page's rows take the server's order.
 * - **The server page supersedes this read's cached page:** a row this read's cached phase added
 *   that the server page lacks is removed. Rows that arrived any other way are never removed by a
 *   read, and nothing is ever duplicated. The two pages usually cover the same `(signature, page)`
 *   window, but not always: the SDK's replay index runs independently of the server cursor
 *   (BEH-SNAP-03). For example, after N pages scrolled offline, a scroll after reconnecting but
 *   before `onConnected`'s refresh pairs cached page N+1 with server page 1. The removed rows then
 *   come back as later server pages arrive, and the reconnect refresh rebuilds the list in any case.
 * - Only an empty **server** page ends the list (BEH-READ-09b).
 */
internal object SnapshotListRead {

    /** The SDK's not-connected code: every offline read ends with it after its cached phase (BEH-READ-05). */
    const val NOT_CONNECTED: String = "ERROR_INTERNET_UNAVAILABLE"

    /** How a [collect] ended. */
    sealed class End {
        /** The read went stale (generation or session moved on); it touched nothing after that. */
        object Dropped : End()

        /** The server phase arrived. [pageEmpty]: the page had no rows (end of list). */
        data class Server(val pageEmpty: Boolean) : End()

        /** The read failed; rows already shown are kept. */
        data class Failed(val exception: CometChatException) : End()
    }

    fun isNotConnected(exception: CometChatException?): Boolean = exception?.code == NOT_CONNECTED

    /**
     * A single server phase from a one-shot fetch — the fallback for a custom data source that
     * has no two-phase read.
     */
    fun <T> serverPhaseOf(fetch: suspend () -> List<T>): Flow<TwoPhaseResult<T>> = flow {
        val result = try {
            TwoPhaseResult.Server(fetch())
        } catch (e: CancellationException) {
            throw e
        } catch (e: CometChatException) {
            TwoPhaseResult.Error(e)
        } catch (e: Exception) {
            TwoPhaseResult.Error(CometChatException("ERROR", e.message ?: "fetch failed"))
        }
        emit(result)
    }

    /**
     * Collects one read's phases into [rows].
     *
     * @param listGen the list's live generation, read at every phase.
     * @param transform applied to each phase's rows before they are placed (defensive copies,
     * owner exclusion) — the same treatment today's path gives them.
     * @param onPainted called after a cached phase changed [rows] (the first paint).
     */
    suspend fun <T> collect(
        phases: Flow<TwoPhaseResult<T>>,
        op: SnapshotReadOp,
        listGen: () -> Long,
        rows: MutableStateFlow<List<T>>,
        keyOf: (T) -> String,
        transform: (List<T>) -> List<T> = { it },
        onPainted: () -> Unit
    ): End {
        var end: End = End.Dropped
        try {
            // The server or error phase ends the read, even if the source flow stays open.
            phases.transformWhile { emit(it); it is TwoPhaseResult.Cached }.collect { phase ->
                // Nothing after the terminal phase, and nothing once the read has gone stale.
                if (end is End.Server || end is End.Failed) return@collect
                if (op.gen != listGen() || op.hubGen != OfflineHub.gen) return@collect
                when (phase) {
                    is TwoPhaseResult.Cached -> {
                        val next = apply(rows.value, listGen(), op, OfflineListReconcile.Phase.CACHED, transform(phase.rows), keyOf)
                        if (next != null) {
                            rows.value = next
                            onPainted()
                        }
                    }
                    is TwoPhaseResult.Server -> {
                        val page = transform(phase.rows)
                        apply(rows.value, listGen(), op, OfflineListReconcile.Phase.SERVER, page, keyOf)
                            ?.let { rows.value = it }
                        end = End.Server(page.isEmpty())
                    }
                    is TwoPhaseResult.Error -> end = End.Failed(phase.exception)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: CometChatException) {
            end = End.Failed(e)
        } catch (e: Exception) {
            end = End.Failed(CometChatException("ERROR", e.message ?: "fetch failed"))
        }
        // A read that went stale at any point reports Dropped, so the caller touches no state.
        return if (op.gen != listGen() || op.hubGen != OfflineHub.gen) End.Dropped else end
    }

    /**
     * Places one phase's [page] into [current]. Pure apart from mutating [op]'s bookkeeping.
     *
     * @return the new list, or null when nothing changes (stale [op], or a cached phase that adds
     * no row).
     */
    fun <T> apply(
        current: List<T>,
        listGen: Long,
        op: SnapshotReadOp,
        phase: OfflineListReconcile.Phase,
        page: List<T>,
        keyOf: (T) -> String
    ): List<T>? {
        if (op.gen != listGen) return null
        val base = if (op.replace && !op.replaced) emptyList() else current
        val ordinal = HashMap<Any, Long>(base.size + page.size)
        return when (phase) {
            OfflineListReconcile.Phase.CACHED -> {
                val shown = base.mapTo(HashSet()) { keyOf(it) }
                val fresh = page.filter { keyOf(it) !in shown }.distinctBy(keyOf)
                if (fresh.isEmpty()) return null
                base.forEachIndexed { i, row -> ordinal[keyOf(row)] = i.toLong() }
                fresh.forEachIndexed { j, row -> ordinal[keyOf(row)] = (base.size + j).toLong() }
                val out = reconcile(base, listGen, op, phase, fresh, keyOf, ordinal)
                op.cachedKeys = fresh.mapTo(HashSet()) { keyOf(it) }
                op.replaced = true
                out
            }
            OfflineListReconcile.Phase.SERVER -> {
                val serverRows = page.distinctBy(keyOf)
                val returned = serverRows.mapTo(HashSet()) { keyOf(it) }
                // The server page supersedes this read's cached rows (see the class KDoc for when the
                // two pages cover different windows).
                val kept = base.filter { val k = keyOf(it); k !in op.cachedKeys || k in returned }
                op.replaced = true
                if (serverRows.isEmpty()) return kept
                // Ordinals: rows before this read's slot keep their place; the page takes the slot in
                // server order; rows after the slot shift past it. A row already shown that is not
                // this read's cached row keeps its place even when the page returns it again.
                val slot = kept.indexOfFirst { keyOf(it) in op.cachedKeys }.let { if (it < 0) kept.size else it }
                kept.forEachIndexed { i, row ->
                    ordinal[keyOf(row)] = if (i < slot) i.toLong() else (i + serverRows.size).toLong()
                }
                serverRows.forEachIndexed { j, row ->
                    val k = keyOf(row)
                    if (k !in ordinal || k in op.cachedKeys) ordinal[k] = (slot + j).toLong()
                }
                reconcile(kept, listGen, op, phase, serverRows, keyOf, ordinal)
            }
        }
    }

    private fun <T> reconcile(
        base: List<T>,
        listGen: Long,
        op: SnapshotReadOp,
        phase: OfflineListReconcile.Phase,
        page: List<T>,
        keyOf: (T) -> String,
        ordinal: Map<Any, Long>
    ): List<T> {
        // Every row of a snapshot list has a server id (uid / guid), and the list is in server
        // order, so the sort value is the row's ordinal. updatedAt is constant: the incoming copy
        // wins a tie, so the server copy replaces what was shown.
        val spec = OfflineListReconcile.Spec<T, String>(
            keyOf = { keyOf(it) },
            serverIdOf = { keyOf(it) },
            sortAtOf = { ordinal[keyOf(it)] ?: Long.MAX_VALUE },
            updatedAtOf = { 0L }
        )
        return OfflineListReconcile.reconcile(
            current = base,
            listGen = listGen,
            op = OfflineListReconcile.Op(op.gen, phase, page),
            spec = spec,
            hadMore = true
        ).rows
    }
}
