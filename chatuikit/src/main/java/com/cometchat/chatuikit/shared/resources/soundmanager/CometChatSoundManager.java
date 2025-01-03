package com.cometchat.chatuikit.shared.resources.soundmanager;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.resources.utils.Utils;

/**
 * The CometChatSoundManager class handles audio-related functionality in the
 * CometChat SDK.
 *
 * <p>
 * It provides methods for playing different types of sounds and managing audio
 * settings.
 */
public class CometChatSoundManager {
    private static final String TAG = CometChatSoundManager.class.getSimpleName();

    private final Context context;

    private final IncomingAudioManager incomingAudioHelper;

    private final OutgoingAudioManager outgoingAudioHelper;
    private final Vibrator vibrator;

    private final SoundPool soundPool;
    private static final long[] VIBRATE_PATTERN = {0, 1000, 1000};

    private final int disconnectedSoundId;

    /**
     * Constructs a new CometChatSoundManager instance.
     *
     * @param context The context of the application.
     */
    public CometChatSoundManager(Context context) {
        this.context = context;
        this.incomingAudioHelper = new IncomingAudioManager(context);
        this.outgoingAudioHelper = new OutgoingAudioManager(context);
        this.soundPool = new SoundPool(1, AudioManager.STREAM_VOICE_CALL, 0);
        this.disconnectedSoundId = this.soundPool.load(context, R.raw.cometchat_beep2, 1);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void initAudio() {
        AudioManager audioManager = Utils.getAudioManager(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
    }

    /**
     * Plays the specified sound.
     *
     * @param sound The sound to be played.
     */
    public void play(Sound sound) {

        if (sound.equals(Sound.incomingCall)) {
            startIncomingAudio(Uri.parse("android.resource://" + context.getPackageName() + "/" + Sound.incomingCall.getRawFile()), true);
        } else if (sound.equals(Sound.outgoingCall)) {
            startOutgoingAudio(OutgoingAudioManager.Type.IN_COMMUNICATION, Sound.outgoingCall.getRawFile());
        } else if (sound.equals(Sound.incomingMessage)) {
            playMessageSound(context, Sound.incomingMessage.getRawFile());
        } else if (sound.equals(Sound.outgoingMessage)) {
            playMessageSound(context, Sound.outgoingMessage.getRawFile());
        } else if (sound.equals(Sound.incomingMessageFromOther)) {
            playMessageSound(context, Sound.incomingMessageFromOther.getRawFile());
        }
    }

    /**
     * Plays the specified sound with a custom raw file resource. If the raw file is
     * set to 0, it plays the default sound for the specified sound type.
     *
     * @param sound   The sound to be played.
     * @param rawFile The custom raw file resource ID.
     */
    public void play(Sound sound, int rawFile) {
        if (rawFile == 0) {
            play(sound);
            return;
        }
        if (sound.equals(Sound.incomingCall)) {
            startIncomingAudio(Uri.parse("android.resource://" + context.getPackageName() + "/" + rawFile), true);
        } else if (sound.equals(Sound.outgoingCall)) {
            startOutgoingAudio(OutgoingAudioManager.Type.IN_COMMUNICATION, rawFile);
        } else if (sound.equals(Sound.incomingMessage) || sound.equals(Sound.outgoingMessage) || sound.equals(Sound.incomingMessageFromOther)) {
            playMessageSound(context, rawFile);
        }
    }

    private void startIncomingAudio(Uri ringtone, boolean isVibrate) {
        AudioManager audioManager = Utils.getAudioManager(context);
        boolean speaker = !audioManager.isWiredHeadsetOn() && !audioManager.isBluetoothScoOn();

        audioManager.setMode(AudioManager.MODE_RINGTONE);
        audioManager.setMicrophoneMute(false);
        audioManager.setSpeakerphoneOn(speaker);

        incomingAudioHelper.start(ringtone, isVibrate);
    }

    private void startOutgoingAudio(OutgoingAudioManager.Type type, int rawID) {
        AudioManager audioManager = Utils.getAudioManager(context);
        audioManager.setMicrophoneMute(false);

        if (type == OutgoingAudioManager.Type.IN_COMMUNICATION) {
            audioManager.setSpeakerphoneOn(false);
        }

        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        outgoingAudioHelper.start(type, rawID);
    }

    private void silenceIncomingRinger() {
        incomingAudioHelper.stop();
    }

    private void startCall(boolean preserveSpeakerphone) {
        AudioManager audioManager = Utils.getAudioManager(context);
        incomingAudioHelper.stop();
        outgoingAudioHelper.stop();
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        if (!preserveSpeakerphone) {
            audioManager.setSpeakerphoneOn(false);
        }
    }

    /**
     * Pauses the audio playback and resets the audio settings to the normal state.
     */
    public void pause() {
        pauseSilently();
        soundPool.play(disconnectedSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
    }

    public void pauseSilently() {
        AudioManager audioManager = Utils.getAudioManager(context);
        audioManager.setSpeakerphoneOn(false);
        audioManager.setMicrophoneMute(false);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.abandonAudioFocus(null);
        incomingAudioHelper.stop();
        outgoingAudioHelper.stop();
        if (audioManager.isBluetoothScoOn()) {
            audioManager.setBluetoothScoOn(false);
            audioManager.stopBluetoothSco();
        }
    }

    private void playMessageSound(Context context, int ringId) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if ((audioManager.isMusicActive() && audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) || audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        } else {
            if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT && audioManager.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE) {
                MediaPlayer mMediaPlayer = MediaPlayer.create(context, ringId);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.start();
                mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }
                });
            }
        }
    }
}
