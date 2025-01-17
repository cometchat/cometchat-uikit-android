package com.cometchat.chatuikit.shared.views.messagereceipt;

/**
 * Receipt is an enumeration representing the receipt status of a message.
 */
public enum Receipt {
    /**
     * The message has been sent.
     */
    SENT,
    /**
     * The message has been delivered.
     */
    DELIVERED,
    /**
     * The message has been read.
     */
    READ,
    /**
     * An error occurred while sending or receiving the message.
     */
    ERROR,
    /**
     * The message is currently in progress (being sent or received).
     */
    IN_PROGRESS
}
