package com.cometchat.chatuikit.shared.resources.soundmanager;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.cometchat.chat.helpers.Logger;

import java.io.IOException;

/**
 * OutgoingAudioHelper class is used to provide audio tone when a there is a
 * outgoing call. It provides various method which can set the ringtone or
 * vibrate on outgoing call
 *
 * <p>
 * Created at: 29th March 2020
 *
 * <p>
 * Modified at 29th March 2020
 */
public class OutgoingAudioManager {
    private static final String TAG = OutgoingAudioManager.class.getSimpleName();

    public enum Type {
        IN_COMMUNICATION, RINGING,
    }

    private final Context context;

    private MediaPlayer mediaPlayer;

    public OutgoingAudioManager(@NonNull Context context) {
        this.context = context;
    }

    /**
     * This method is used to start the outgoing call ringtone.
     *
     * @param type
     */
    public void start(final Type type, int rawID) {
        int soundId;
        if (type == Type.IN_COMMUNICATION || type == Type.RINGING) soundId = rawID;
        else throw new IllegalArgumentException("Not a valid sound type");

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
        mediaPlayer.setLooping(true);
        String packageName = context.getPackageName();
        Uri dataUri = Uri.parse("android.resource://" + packageName + "/" + soundId);

        try {
            mediaPlayer.setDataSource(context, dataUri);
            mediaPlayer.prepare();

            mediaPlayer.start();

        } catch (IllegalArgumentException | SecurityException | IllegalStateException |
                 IOException e) {
            Logger.error(TAG, e.getMessage());
        }
    }

    /**
     * This method is used to stop the outgoing call ringtone.
     */
    public void stop() {
        if (mediaPlayer == null) return;
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }
}
