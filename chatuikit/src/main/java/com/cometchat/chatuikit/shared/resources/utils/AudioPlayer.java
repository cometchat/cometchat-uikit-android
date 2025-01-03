package com.cometchat.chatuikit.shared.resources.utils;

import android.media.MediaPlayer;

import com.cometchat.chatuikit.logger.CometChatLogger;

public class AudioPlayer {
    private static final String TAG = AudioPlayer.class.getSimpleName();

    private static AudioPlayer instance;
    private final MediaPlayer mediaPlayer;
    private boolean isPrepared;

    private AudioPlayer() {
        mediaPlayer = new MediaPlayer();
    }

    public static AudioPlayer getInstance() {
        if (instance == null) {
            instance = new AudioPlayer();
        }
        return instance;
    }

    public void reset() {
        mediaPlayer.reset();
        isPrepared = false; // Reset prepared state
    }

    public void setAudioUrl(String url, MediaPlayer.OnPreparedListener preparedListener, MediaPlayer.OnCompletionListener completionListener) {
        try {
            reset();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare();
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                mediaPlayer.start();
                if (preparedListener != null) preparedListener.onPrepared(mediaPlayer);
            });
            mediaPlayer.setOnCompletionListener(mediaPlayer -> {
                if (completionListener != null) completionListener.onCompletion(mediaPlayer);
            });
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
    }

    public void start() {
        if (!mediaPlayer.isPlaying() && isPrepared) {
            mediaPlayer.start();
        }
    }

    public void stop() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            isPrepared = false; // MediaPlayer needs to be re-prepared after stop
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}
