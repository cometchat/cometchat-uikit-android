package com.cometchat.uikit.core.offline

/**
 * The windowed two-phase reconcile shared by the message list (W2) and the conversation list (W3),
 * exactly as Raj's design §3.3 (BEH-READ-01/07/09b/10/30).
 *
 * A **pure** function over `(current rows, op, phase rows)`: it takes no locks, touches no SDK, and
 * returns a brand-new sorted list, so the caller runs it off the main thread and then diffs the
 * result in. It is generic over the row type, the server-id type and the sort direction via
 * [Spec], so messages (`Long` ids, oldest-first) and conversations (`String` ids, newest-first)
 * share it unchanged.
 *
 * The rules it enforces:
 * - **Stale gen dropped**: an op whose `gen` no longer matches the list's `gen` is ignored and the
 *   current list is returned untouched ([Outcome.applied] = false).
 * - **Upsert by row key with the alias map**: rows match on [Spec.resolveKey] applied to
 *   [Spec.keyOf] — so a pending `l:x` row and its acked `s:5` copy collapse into one row once the
 *   alias is known; the same key keeps the copy with the newer [Spec.updatedAtOf] (a live edit is
 *   never reverted — LWW).
 * - **Proven omission (server phase only)**: a row this operation's cached phase delivered
 *   ([Op.cachedKeys]), that has a server id and lies inside the server page's
 *   `(sortAt, serverId)` range but was not returned, is removed (the server proved it is gone).
 *   The range is a closed interval on that tuple's natural order, so it holds in either sort
 *   direction. A row **without** a server id is never removed (BEH-READ-30).
 * - **End of history**: only an empty **server** page ends pagination (BEH-READ-09b). A cached
 *   page, even empty, never ends history — it leaves `hasMore` as the caller had it.
 * - **Order**: rows with a server id by `(sortAt, serverId)`, ascending or [Spec.descending]; rows
 *   without one sit at the **newest** end — last when ascending, first when descending — ordered
 *   by compose time toward that end (BEH-READ-10, BEH-SEND-06/24).
 */
internal object OfflineListReconcile {

    enum class Phase { CACHED, SERVER }

    /**
     * Row-type adapters.
     *
     * @param ID the server-id type (`Long` message id, `String` conversationId).
     * @property keyOf the stable list identity, e.g. [MessageKey.rowKey].
     * @property serverIdOf the row's server id, or null when it has none (an un-acked row).
     * @property sortAtOf the primary sort value (`sentAt`, or a conversation's last-message time).
     * @property updatedAtOf the LWW clock for two copies of one row.
     * @property composeAtOf compose time, used to order rows without a server id.
     * @property descending true for a newest-first list (conversations).
     * @property resolveKey maps a key to its canonical form, e.g. via the alias map
     * (`l:x` → `s:5` once acked). Defaults to identity.
     */
    data class Spec<T, ID : Comparable<ID>>(
        val keyOf: (T) -> Any,
        val serverIdOf: (T) -> ID?,
        val sortAtOf: (T) -> Long,
        val updatedAtOf: (T) -> Long,
        val composeAtOf: (T) -> Long = { 0L },
        val descending: Boolean = false,
        val resolveKey: (Any) -> Any = { it }
    )

    /**
     * One phase of one read operation. [gen] is the operation's session/list generation.
     *
     * [cachedKeys] is meaningful on the SERVER phase only: the resolved keys of the rows the same
     * operation's CACHED phase delivered that had a server id (taken from that phase's
     * [Outcome.cachedKeys]). Only those rows can be proven gone by this server page (README §3.3);
     * a row that arrived any other way (live, swept, another page) is never pruned here. When the
     * cached phase never ran, pass the empty set: nothing is pruned.
     */
    data class Op<T>(
        val gen: Long,
        val phase: Phase,
        val rows: List<T>,
        val cachedKeys: Set<Any> = emptySet()
    )

    /**
     * @property rows the reconciled, sorted list.
     * @property hasMore whether pagination in this direction should continue.
     * @property applied false when the op was dropped for a stale [Op.gen].
     * @property cachedKeys after a CACHED phase, the resolved keys of its rows that have a server
     * id — pass these as the same operation's SERVER [Op.cachedKeys]. Empty otherwise.
     */
    data class Outcome<T>(
        val rows: List<T>,
        val hasMore: Boolean,
        val applied: Boolean,
        val cachedKeys: Set<Any> = emptySet()
    )

    /**
     * @param current the rows currently shown.
     * @param listGen the list's live generation; an [op] from another generation is dropped.
     * @param hadMore the current pagination state, preserved across a cached phase.
     */
    fun <T, ID : Comparable<ID>> reconcile(
        current: List<T>,
        listGen: Long,
        op: Op<T>,
        spec: Spec<T, ID>,
        hadMore: Boolean
    ): Outcome<T> {
        if (op.gen != listGen) {
            return Outcome(current, hadMore, applied = false)
        }

        val merged = upsert(current, op.rows, spec)

        return when (op.phase) {
            Phase.CACHED -> {
                // A cached page never ends history and never proves omission: place its rows and
                // record which of them the server phase may later prove gone.
                val keys = op.rows.filter { spec.serverIdOf(it) != null }.mapTo(HashSet()) { keyOf(it, spec) }
                Outcome(sort(merged, spec), hadMore, applied = true, cachedKeys = keys)
            }

            Phase.SERVER -> {
                val pruned = pruneProvenOmissions(merged, op, spec)
                Outcome(sort(pruned, spec), op.rows.isNotEmpty(), applied = true)
            }
        }
    }

    private fun <T, ID : Comparable<ID>> keyOf(row: T, spec: Spec<T, ID>): Any = spec.resolveKey(spec.keyOf(row))

    private fun <T, ID : Comparable<ID>> upsert(current: List<T>, incoming: List<T>, spec: Spec<T, ID>): List<T> {
        val byKey = LinkedHashMap<Any, T>(current.size + incoming.size)
        // Current rows go through the same merge, so two rows already shown under one resolved key
        // (a pending row and its acked copy) collapse too.
        for (row in current + incoming) {
            val key = keyOf(row, spec)
            val existing = byKey[key]
            byKey[key] = if (existing == null || spec.updatedAtOf(row) >= spec.updatedAtOf(existing)) {
                row
            } else {
                existing
            }
        }
        return byKey.values.toList()
    }

    /**
     * Removes a row only when all hold: it came from this operation's cached phase
     * ([Op.cachedKeys]), it has a server id, the server page did not return it, and it lies inside
     * the page's closed `(sortAt, serverId)` range — `sortAt` alone is not enough because `sentAt`
     * is in seconds and ties are common at the range edges.
     */
    private fun <T, ID : Comparable<ID>> pruneProvenOmissions(merged: List<T>, op: Op<T>, spec: Spec<T, ID>): List<T> {
        if (op.rows.isEmpty() || op.cachedKeys.isEmpty()) return merged
        val returnedIds = HashSet<ID>()
        var minAt = 0L
        var minId: ID? = null
        var maxAt = 0L
        var maxId: ID? = null
        for (row in op.rows) {
            val id = spec.serverIdOf(row) ?: continue
            returnedIds.add(id)
            val at = spec.sortAtOf(row)
            if (minId == null || compareTuple(at, id, minAt, minId) < 0) { minAt = at; minId = id }
            if (maxId == null || compareTuple(at, id, maxAt, maxId) > 0) { maxAt = at; maxId = id }
        }
        val lowId = minId ?: return merged
        val highId = maxId ?: return merged
        return merged.filter { row ->
            val id = spec.serverIdOf(row)
            when {
                id == null -> true                                // never remove a row without a server id
                id in returnedIds -> true                          // the server returned it
                keyOf(row, spec) !in op.cachedKeys -> true         // not from this op's cached phase
                else -> {
                    val at = spec.sortAtOf(row)
                    val inside = compareTuple(at, id, minAt, lowId) >= 0 && compareTuple(at, id, maxAt, highId) <= 0
                    !inside                                        // inside the proven range → gone
                }
            }
        }
    }

    private fun <ID : Comparable<ID>> compareTuple(at: Long, id: ID, otherAt: Long, otherId: ID): Int =
        if (at != otherAt) at.compareTo(otherAt) else id.compareTo(otherId)

    private fun <T, ID : Comparable<ID>> sort(rows: List<T>, spec: Spec<T, ID>): List<T> {
        val (acked, unacked) = rows.partition { spec.serverIdOf(it) != null }
        val ackedAsc = acked.sortedWith { a, b ->
            compareTuple(spec.sortAtOf(a), spec.serverIdOf(a)!!, spec.sortAtOf(b), spec.serverIdOf(b)!!)
        }
        val unackedAsc = unacked.sortedBy { spec.composeAtOf(it) }
        // Rows without a server id sit at the newest end in either direction.
        return if (spec.descending) unackedAsc.asReversed() + ackedAsc.asReversed() else ackedAsc + unackedAsc
    }
}
