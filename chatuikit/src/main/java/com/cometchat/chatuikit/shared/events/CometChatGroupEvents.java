package com.cometchat.chatuikit.shared.events;

import com.cometchat.chat.models.Action;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.GroupMember;
import com.cometchat.chat.models.User;

import java.util.HashMap;
import java.util.List;

/**
 * Abstract class for handling CometChat group events.
 */
public abstract class CometChatGroupEvents {
    private static final String TAG = CometChatGroupEvents.class.getSimpleName();
    /**
     * Map to store the registered group event listeners.
     */
    public static final HashMap<String, CometChatGroupEvents> groupEvents = new HashMap<>();

    /**
     * Called when a group is created.
     *
     * @param group The created group object.
     */
    public void ccGroupCreated(Group group) {
    }

    /**
     * Called when a group is deleted.
     *
     * @param group The deleted group object.
     */
    public void ccGroupDeleted(Group group) {
    }

    /**
     * Called when a user leaves a group.
     *
     * @param actionMessage The action message indicating the user left the group.
     * @param leftUser      The user who left the group.
     * @param leftGroup     The group from which the user left.
     */
    public void ccGroupLeft(Action actionMessage, User leftUser, Group leftGroup) {
    }

    /**
     * Called when a user joins a group.
     *
     * @param joinedUser  The user who joined the group.
     * @param joinedGroup The group to which the user joined.
     */
    public void ccGroupMemberJoined(User joinedUser, Group joinedGroup) {
    }

    /**
     * Called when one or more users are added to a group.
     *
     * @param actionMessages The action messages indicating the users added to the group.
     * @param usersAdded     The users who were added to the group.
     * @param userAddedIn    The group in which the users were added.
     * @param addedBy        The user who added the other users to the group.
     */
    public void ccGroupMemberAdded(List<Action> actionMessages, List<User> usersAdded, Group userAddedIn, User addedBy) {
    }

    /**
     * Called when a user is kicked from a group.
     *
     * @param actionMessage The action message indicating the user was kicked from the group.
     * @param kickedUser    The user who was kicked from the group.
     * @param kickedBy      The user who kicked the other user.
     * @param kickedFrom    The group from which the user was kicked.
     */
    public void ccGroupMemberKicked(Action actionMessage, User kickedUser, User kickedBy, Group kickedFrom) {
    }

    /**
     * Called when a user is banned from a group.
     *
     * @param actionMessage The action message indicating the user was banned from the group.
     * @param bannedUser    The user who was banned from the group.
     * @param bannedBy      The user who banned the other user.
     * @param bannedFrom    The group from which the user was banned.
     */
    public void ccGroupMemberBanned(Action actionMessage, User bannedUser, User bannedBy, Group bannedFrom) {
    }

    /**
     * Called when a user is unbanned from a group.
     *
     * @param actionMessage The action message indicating the user was unbanned from the
     *                      group.
     * @param unbannedUser  The user who was unbanned from the group.
     * @param unBannedBy    The user who unbanned the other user.
     * @param unBannedFrom  The group from which the user was unbanned.
     */
    public void ccGroupMemberUnBanned(Action actionMessage, User unbannedUser, User unBannedBy, Group unBannedFrom) {
    }

    /**
     * Called when the scope of a group member is changed.
     *
     * @param actionMessage    The action message indicating the change in scope.
     * @param updatedUser      The user whose scope was changed.
     * @param scopeChangedTo   The new scope value.
     * @param scopeChangedFrom The previous scope value.
     * @param group            The group in which the scope was changed.
     */
    public void ccGroupMemberScopeChanged(Action actionMessage, User updatedUser, String scopeChangedTo, String scopeChangedFrom, Group group) {
    }

    /**
     * Called when the ownership of a group is changed.
     *
     * @param group    The group for which the ownership is changed.
     * @param newOwner The new owner of the group.
     */
    public void ccOwnershipChanged(Group group, GroupMember newOwner) {
    }

    /**
     * Adds a group event listener with the specified tag.
     *
     * @param TAG             The tag to identify the listener.
     * @param chatGroupEvents The group event listener to be added.
     */
    public static void addGroupListener(String TAG, CometChatGroupEvents chatGroupEvents) {
        groupEvents.put(TAG, chatGroupEvents);
    }

    /**
     * Removes all group event listeners.
     */
    public static void removeListeners() {
        groupEvents.clear();
    }

    /**
     * Removes the group event listener associated with the specified tag.
     *
     * @param id The tag of the listener to be removed.
     */
    public static void removeListener(String id) {
        groupEvents.remove(id);
    }
}
