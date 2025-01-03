package com.cometchat.chatuikit.shared.events;

import com.cometchat.chat.core.Call;
import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chatuikit.shared.constants.MessageStatus;

import java.util.HashMap;

/**
 * Abstract class for handling CometChat call events.
 */
public abstract class CometChatCallEvents {
    private static final String TAG = CometChatCallEvents.class.getSimpleName();
    /**
     * Map to store the registered call event listeners.
     */
    public static final HashMap<String, CometChatCallEvents> callingEvents = new HashMap<>();

    /**
     * Called when an outgoing call is initiated.
     *
     * @param call The outgoing call object.
     */
    public void ccOutgoingCall(Call call) {
    }

    /**
     * Called when a call is accepted by the recipient.
     *
     * @param call The accepted call object.
     */
    public void ccCallAccepted(Call call) {
    }

    /**
     * Called when a call is rejected by the recipient.
     *
     * @param call The rejected call object.
     */
    public void ccCallRejected(Call call) {
    }

    /**
     * Called when a call is ended by either the sender or the recipient.
     *
     * @param call The ended call object.
     */
    public void ccCallEnded(Call call) {
    }

    /**
     * Called when a message is sent during a call.
     *
     * @param message The sent message object.
     * @param status  The status of the message (@link MessageStatus}.
     */
    public void ccMessageSent(BaseMessage message, @MessageStatus int status) {
    }

    /**
     * Adds a call event listener with the specified tag.
     *
     * @param tag        The tag to identify the listener.
     * @param callEvents The call event listener to be added.
     */
    public static void addListener(String tag, CometChatCallEvents callEvents) {
        callingEvents.put(tag, callEvents);
    }

    /**
     * Removes all call event listeners.
     */
    public static void removeListeners() {
        callingEvents.clear();
    }

    /**
     * Removes the call event listener associated with the specified tag.
     *
     * @param tag The tag of the listener to be removed.
     */
    public static void removeListener(String tag) {
        callingEvents.remove(tag);
    }
}
