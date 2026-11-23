package com.cometchat.uikit.core.offline

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Backs a list ViewModel's `offlineNotices` (§2.8) so **no notice is lost** when the list's UI is
 * not collecting at that moment (rotation, a list in the back stack).
 *
 * Mechanism: notices queue in an unbounded channel and are emitted on [notices] only once the
 * shared flow has at least one subscriber. The list's own notices ([raise]) wait in this relay's
 * queue; list-less notices from [OfflineHub.postNotice] are taken from the hub only while this
 * list has a subscriber, and handed straight back if the subscriber left while this relay was
 * waiting, so a notice is shown once, on a visible list, and is never swallowed by a list nobody
 * sees or one that was cleared.
 */
internal class OfflineNoticeRelay {

    private val pending = Channel<OfflineNotice>(Channel.UNLIMITED)
    private val _notices = MutableSharedFlow<OfflineNotice>(replay = 0, extraBufferCapacity = 16)

    /** The flow the ViewModel exposes as `offlineNotices`. */
    val notices: SharedFlow<OfflineNotice> = _notices.asSharedFlow()

    /** Queue a notice raised by this list (W4/W5). Never dropped; delivered once a subscriber exists. */
    fun raise(notice: OfflineNotice) {
        pending.trySend(notice)
    }

    /** Start forwarding in [scope] (the ViewModel's scope). Cancel the returned job to stop. */
    fun start(scope: CoroutineScope): Job = scope.launch {
        launch {
            for (notice in pending) {
                awaitSubscriber()
                _notices.emit(notice)
            }
        }
        launch {
            while (isActive) {
                awaitSubscriber()
                val notice = OfflineHub.receiveNotice()
                deliverOrHandBack(notice)
            }
        }
    }

    /**
     * Shows a hub notice only if this list's UI is still collecting **now**. While this relay was
     * waiting for a notice its UI may have stopped collecting (e.g. the conversations screen was
     * covered by a chat); then the notice goes straight back to the hub so a visible list — or this
     * one's next subscriber — takes it. The check and the emit run without a suspension in between
     * on the collector's dispatcher, so the notice is shown once, never twice; cancellation while
     * emitting also hands it back.
     */
    private suspend fun deliverOrHandBack(notice: OfflineNotice) {
        if (_notices.subscriptionCount.value == 0) {
            OfflineHub.postNotice(notice)
            return
        }
        try {
            _notices.emit(notice)
        } catch (e: CancellationException) {
            OfflineHub.postNotice(notice)
            throw e
        }
    }

    private suspend fun awaitSubscriber() {
        _notices.subscriptionCount.first { it > 0 }
    }
}
