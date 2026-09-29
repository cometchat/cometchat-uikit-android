package com.cometchat.uikit.compose.presentation.shared.inlineaudiorecorder.utils

import com.cometchat.uikit.core.viewmodel.CometChatInlineAudioRecorderViewModel

/**
 * Shares a single [InlineAudioRecorderManager] across every [CometChatInlineAudioRecorder]
 * that is bound to the same [CometChatInlineAudioRecorderViewModel].
 *
 * ### Why this exists
 * A host can end up composing the message composer's recording UI more than once under the same
 * ViewModel store owner — a recomposition that produces two recording-branch subtrees, an animated
 * navigation transition, a split layout. Each [CometChatInlineAudioRecorder] used to create its own
 * manager in a `remember {}` block, so two managers would auto-start recording, open two
 * `MediaRecorder`s, and fight over the single microphone. The loser was auto-paused on
 * `AUDIOFOCUS_LOSS` while the winner kept recording, which surfaced as a recorder that shows
 * "paused" while audio is still being captured, and as flicker on pause/resume (ENG-39525).
 *
 * Binding one engine to the shared ViewModel removes the conflict deterministically: the second
 * composer instance reuses the same manager, and that manager's own `currentStatus` guard turns the
 * duplicate `startRecording()` into a no-op, so only one microphone capture ever runs.
 *
 * The map is keyed on the ViewModel instance (which the duplicate composers already share) and is
 * reference counted, so the manager is released only when the last composer using it leaves
 * composition. Entries are held weakly so a discarded ViewModel cannot leak its manager.
 */
internal object InlineAudioRecorderManagerRegistry {

    private class Entry(val manager: InlineAudioRecorderManager, var refCount: Int)

    private val entries = java.util.WeakHashMap<CometChatInlineAudioRecorderViewModel, Entry>()

    /**
     * Returns the manager bound to [owner], creating it with [factory] on the first acquire.
     * Every acquire must be matched by exactly one [release].
     */
    @Synchronized
    fun acquire(
        owner: CometChatInlineAudioRecorderViewModel,
        factory: () -> InlineAudioRecorderManager
    ): InlineAudioRecorderManager {
        val existing = entries[owner]
        if (existing != null) {
            existing.refCount++
            return existing.manager
        }
        val manager = factory()
        entries[owner] = Entry(manager, 1)
        return manager
    }

    /**
     * Drops one reference for [owner]. When the last reference is released the manager is
     * released and the entry removed.
     *
     * @return `true` when this was the last reference (the manager was released), so the caller
     *   can also release shared state such as the ViewModel exactly once. `false` while other
     *   composer instances are still using the manager.
     */
    @Synchronized
    fun release(owner: CometChatInlineAudioRecorderViewModel): Boolean {
        val entry = entries[owner] ?: return true
        entry.refCount--
        if (entry.refCount <= 0) {
            entries.remove(owner)
            entry.manager.release()
            return true
        }
        return false
    }
}
