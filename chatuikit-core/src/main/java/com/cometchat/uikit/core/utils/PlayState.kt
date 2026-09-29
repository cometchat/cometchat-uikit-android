package com.cometchat.uikit.core.utils

/**
 * Represents the playback state of an audio bubble.
 */
public enum class PlayState {
    /** Initial state — no playback has occurred */
    INIT,
    /** Audio is actively playing */
    PLAYING,
    /** Audio is paused, position preserved */
    PAUSED,
    /** Playback completed, position reset to 0 */
    STOPPED
}
