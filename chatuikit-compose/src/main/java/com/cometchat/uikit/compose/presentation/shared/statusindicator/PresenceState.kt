package com.cometchat.uikit.compose.presentation.shared.statusindicator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.cometchat.uikit.core.offline.OfflinePresence

/**
 * `OfflinePresence.hideOnline()` for a composable that paints presence (ENG-41551 A).
 *
 * Reading the presence-change tick subscribes the caller, so a row or header already on screen
 * recomposes when the connection flips: `User.status` does not change on a flip, so nothing else
 * would recompose it. With offline off the tick never changes and the answer is always `false`.
 */
@Composable
internal fun rememberHideOnline(): Boolean {
    val changes = remember { OfflinePresence.presenceChanges() }
    changes.collectAsState().value
    return OfflinePresence.hideOnline()
}
