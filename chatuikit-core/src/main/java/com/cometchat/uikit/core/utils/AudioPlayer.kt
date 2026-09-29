package com.cometchat.uikit.core.utils

import android.media.MediaPlayer

/**
 * Singleton audio player for managing audio playback across the UI Kit.
 *
 * Ensures only one audio plays at a time — when a new audio is started,
 * the previous one is automatically stopped and its completion listener is fired.
 *
 * Shared across chatuikit-kotlin and chatuikit-jetpack modules.
 */
public class AudioPlayer private constructor() {

    private val mediaPlayer = MediaPlayer()
    private var isPrepared = false
    private var completionListener: MediaPlayer.OnCompletionListener? = null

    public companion object {
        private const val TAG = "AudioPlayer"

        @Volatile
        private var instance: AudioPlayer? = null

        @JvmStatic
        public fun getInstance(): AudioPlayer {
            return instance ?: synchronized(this) {
                instance ?: AudioPlayer().also { instance = it }
            }
        }
    }

    /**
     * Sets the audio URL and prepares the player.
     * Automatically resets any currently playing audio first.
     *
     * @param url The URL of the audio file
     * @param preparedListener Callback when the player is prepared
     * @param completionListener Callback when playback completes
     */
    public fun setAudioUrl(
        url: String,
        preparedListener: MediaPlayer.OnPreparedListener?,
        completionListener: MediaPlayer.OnCompletionListener?
    ) {
        try {
            reset()
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepare()
            this.completionListener = completionListener
            mediaPlayer.setOnPreparedListener { mp ->
                isPrepared = true
                mediaPlayer.start()
                preparedListener?.onPrepared(mp)
            }
            mediaPlayer.setOnCompletionListener { mp ->
                completionListener?.onCompletion(mp)
            }
        } catch (e: Exception) {
            CometChatLogger.e(TAG, "Error setting audio URL: ${e.message}")
        }
    }

    /**
     * Resets the media player.
     * Fires the completion listener of the previously playing audio.
     */
    public fun reset() {
        mediaPlayer.reset()
        completionListener?.onCompletion(mediaPlayer)
        isPrepared = false
    }

    /**
     * Starts playback if prepared and not already playing.
     */
    public fun start() {
        if (!mediaPlayer.isPlaying && isPrepared) {
            mediaPlayer.start()
        }
    }

    /**
     * Stops playback.
     */
    public fun stop() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            isPrepared = false
        }
    }

    /**
     * Checks if audio is currently playing.
     */
    public fun isPlaying(): Boolean = mediaPlayer.isPlaying

    /**
     * Gets the underlying MediaPlayer instance.
     */
    public fun getMediaPlayer(): MediaPlayer = mediaPlayer
}
