package com.cometchat.uikit.core.offline

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * G8 for a list or panel ViewModel whose only hub events are the session end and the cache reset
 * (users, groups, group members, search, reactions, pinned, saved, notification feed, AI chat
 * history). The gen rule is the conversation list's (`CometChatConversationsViewModel`
 * `onOfflineHubEvent`):
 *
 * - A newer [OfflineHubEvent.SessionEnded] (its gen above the one held) adopts that gen and runs
 *   [onSessionEnded]: the ViewModel bumps its read generations, empties its rows and resets paging,
 *   before the next frame. A duplicate or older one changes nothing.
 * - An [OfflineHubEvent.CacheReset] of the held gen runs [onCacheReset] once: the ViewModel clears
 *   as above, then reloads once from page one, quiet if that fails over rows (D84 rule 5). One from
 *   another gen is dropped. The hub posts the reset notice; the ViewModel posts nothing.
 * - Every other event is ignored here.
 *
 * Started only with offline messaging on (G1): the hub publishes `SessionEnded` on every logout,
 * offline on or off, so an offline-off ViewModel never subscribes and stays as it is today.
 */
internal class OfflineListSession(
    private val onSessionEnded: () -> Unit,
    private val onCacheReset: () -> Unit
) {
    /** The hub session generation this ViewModel accepts events from. */
    private var hubGen: Long = OfflineHub.gen
    private var job: Job? = null

    /** Collects the hub on [scope] (the ViewModel's, so it ends with the ViewModel). */
    fun start(scope: CoroutineScope) {
        job?.cancel()
        hubGen = OfflineHub.gen
        job = scope.launch { OfflineHub.events.collect { event -> route(event) } }
    }

    @VisibleForTesting
    internal fun route(event: OfflineHubEvent) {
        when (event) {
            is OfflineHubEvent.SessionEnded -> {
                if (event.gen <= hubGen) return
                hubGen = event.gen
                onSessionEnded()
            }
            is OfflineHubEvent.CacheReset -> {
                if (event.gen != hubGen) return
                onCacheReset()
            }
            else -> Unit
        }
    }
}
