package com.cometchat.uikit.core.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton manager for all audio bubble playback states.
 * Ensures only one audio plays at a time, state survives LazyColumn/RecyclerView recycling.
 */
public object AudioBubbleStateManager {
    private const val TAG = "AudioBubbleStateMgr"
    private val states = ConcurrentHashMap<Int, AudioBubblePlaybackState>()

    public fun getOrCreate(id: Int, audioUrl: String?, localPath: String?): AudioBubblePlaybackState {
        return states.getOrPut(id) { AudioBubblePlaybackState(id = id, audioUrl = audioUrl, localPath = localPath) }
    }

    /** Returns the existing state without creating one — for restoring UI on RecyclerView re-bind. */
    public fun peek(id: Int): AudioBubblePlaybackState? = states[id]

    public fun pauseAllExcept(excludeId: Int) {
        states.values.toList().forEach { state ->
            if (state.id != excludeId && state.playState == PlayState.PLAYING) state.pause()
        }
    }

    public fun clearAll() {
        CometChatLogger.d(TAG, "Clearing all audio bubble states (${states.size} entries)")
        states.values.toList().forEach { it.release() }
        states.clear()
    }

    public fun remove(id: Int) { states.remove(id)?.release() }
}
