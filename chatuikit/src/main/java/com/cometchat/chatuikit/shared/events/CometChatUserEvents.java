package com.cometchat.chatuikit.shared.events;

import com.cometchat.chat.models.User;

import java.util.HashMap;

/**
 * Abstract class for handling CometChat user events.
 */
public abstract class CometChatUserEvents {
    private static final String TAG = CometChatUserEvents.class.getSimpleName();


    /**
     * Map to store the registered user event listeners.
     */
    public static final HashMap<String, CometChatUserEvents> userEvents = new HashMap<>();

    /**
     * Called when a user is blocked.
     *
     * @param user The blocked user.
     */
    public void ccUserBlocked(User user) {
    }

    /**
     * Called when a user is unblocked.
     *
     * @param user The unblocked user.
     */
    public void ccUserUnblocked(User user) {
    }

    /**
     * Adds a user event listener with the specified tag.
     *
     * @param TAG            The tag to identify the listener.
     * @param chatUserEvents The user event listener to be added.
     */
    public static void addUserListener(String TAG, CometChatUserEvents chatUserEvents) {
        userEvents.put(TAG, chatUserEvents);
    }

    /**
     * Removes all user event listeners.
     */
    public static void removeListeners() {
        userEvents.clear();
    }

    /**
     * Removes the user event listener associated with the specified tag.
     *
     * @param id The tag of the listener to be removed.
     */
    public static void removeListener(String id) {
        userEvents.remove(id);
    }
}
