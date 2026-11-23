package com.cometchat.uikit.kotlin.presentation.shared.statusindicator

import com.cometchat.uikit.core.offline.OfflinePresence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Runs [rebind] each time the connection flips the answer of `OfflinePresence.hideOnline()`
 * (ENG-41551 A), so rows and headers already on screen re-read presence. Views rows otherwise
 * re-bind only when a user's status value changes, which a connection flip does not do.
 *
 * The current value is skipped: only a later flip re-binds. With offline off [changes] never
 * ticks, so nothing is ever re-bound.
 */
internal fun CoroutineScope.rebindOnPresenceChange(
    changes: Flow<Int> = OfflinePresence.presenceChanges(),
    rebind: () -> Unit
): Job = launch {
    changes.drop(1).collect { rebind() }
}

/** Re-binds every row of [adapter] (no payload), so each row reads presence again. */
internal fun rebindAllRows(adapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>) {
    val count = adapter.itemCount
    if (count > 0) adapter.notifyItemRangeChanged(0, count)
}
