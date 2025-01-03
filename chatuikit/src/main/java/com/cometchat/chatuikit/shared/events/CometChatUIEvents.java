package com.cometchat.chatuikit.shared.events;

import android.content.Context;
import android.view.View;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.interfaces.Function1;

import java.util.HashMap;

/**
 * Abstract class for handling CometChat UI events.
 */
public abstract class CometChatUIEvents {
    private static final String TAG = CometChatUIEvents.class.getSimpleName();
    /**
     * Map to store the registered UI event listeners.
     */
    public static final HashMap<String, CometChatUIEvents> uiEvents = new HashMap<>();

    /**
     * Shows a panel with the specified ID, alignment, and view.
     *
     * @param id        The ID of the panel.
     * @param alignment The alignment position of the panel.
     * @param view      The view associated with the panel.
     */
    public void showPanel(HashMap<String, String> id, UIKitConstants.CustomUIPosition alignment, Function1<Context, View> view) {
    }

    /**
     * Hides a panel with the specified ID and alignment.
     *
     * @param id        The ID of the panel to hide.
     * @param alignment The alignment position of the panel.
     */
    public void hidePanel(HashMap<String, String> id, UIKitConstants.CustomUIPosition alignment) {
    }

    public void ccComposeMessage(String id, String text) {
    }

    /**
     * Called when the active chat is changed.
     *
     * @param id      The ID of the active chat.
     * @param message The last message in the chat.
     * @param user    The user associated with the chat (if it's a user chat).
     * @param group   The group associated with the chat (if it's a group chat).
     */
    public void ccActiveChatChanged(HashMap<String, String> id, BaseMessage message, User user, Group group) {
    }

    /**
     * Called when the active chat is changed.
     *
     * @param id          The ID of the active chat.
     * @param message     The last message in the chat.
     * @param user        The user associated with the chat (if it's a user chat).
     * @param group       The group associated with the chat (if it's a group chat).
     * @param unreadCount The unread count of the chat.
     */
    public void ccActiveChatChanged(HashMap<String, String> id, BaseMessage message, User user, Group group, int unreadCount) {
    }

    public void ccOpenChat(User user, Group group) {
    }

    /**
     * Adds a UI event listener with the specified tag.
     *
     * @param tag    The tag to identify the listener.
     * @param events The UI event listener to be added.
     */
    public static void addListener(String tag, CometChatUIEvents events) {
        uiEvents.put(tag, events);
    }

    /**
     * Removes all UI event listeners.
     */
    public static void removeListeners() {
        uiEvents.clear();
    }

    /**
     * Removes the UI event listener associated with the specified tag.
     *
     * @param TAG The tag of the listener to be removed.
     */
    public static void removeListener(String TAG) {
        uiEvents.remove(TAG);
    }
}
