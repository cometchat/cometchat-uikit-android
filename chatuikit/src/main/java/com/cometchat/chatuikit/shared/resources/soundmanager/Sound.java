package com.cometchat.chatuikit.shared.resources.soundmanager;

import com.cometchat.chatuikit.R;

/**
 * Enum representing different sounds used in the application.
 */
public enum Sound {
    incomingCall(R.raw.cometchat_incoming_call), outgoingCall(R.raw.cometchat_outgoing_call), incomingMessage(R.raw.comechat_incoming_message), outgoingMessage(R.raw.cometchat_outgoing_message), incomingMessageFromOther(R.raw.cometchat_incoming_message_other);
    private int rawFile;

    /**
     * Constructor for Sound enum.
     *
     * @param rawFile The raw file resource ID associated with the sound.
     */
    Sound(int rawFile) {
        this.rawFile = rawFile;
    }

    /**
     * Returns the raw file resource ID associated with the sound.
     *
     * @return The raw file resource ID.
     */
    public int getRawFile() {
        return rawFile;
    }

    /**
     * Sets the raw file resource ID associated with the sound.
     *
     * @param rawFile The raw file resource ID to be set.
     * @return The updated raw file resource ID.
     */
    public int setRawFile(int rawFile) {
        return this.rawFile = rawFile;
    }
}
