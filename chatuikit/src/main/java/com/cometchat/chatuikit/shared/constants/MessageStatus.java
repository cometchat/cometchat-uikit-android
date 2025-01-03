package com.cometchat.chatuikit.shared.constants;

/**
 * Defines the status values for a message.
 */
public @interface MessageStatus {
    /**
     * The message is in progress.
     */
    int IN_PROGRESS = 0;

    /**
     * The message was successfully sent.
     */
    int SUCCESS = 1;
    /**
     * An error occurred while sending the message.
     */
    int ERROR = -1;
}
