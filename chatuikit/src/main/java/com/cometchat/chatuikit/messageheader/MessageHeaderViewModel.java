package com.cometchat.chatuikit.messageheader;

import android.os.Handler;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.Action;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.GroupMember;
import com.cometchat.chat.models.TypingIndicator;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitUtilityConstants;
import com.cometchat.chatuikit.shared.events.CometChatGroupEvents;
import com.cometchat.chatuikit.shared.events.CometChatMessageEvents;
import com.cometchat.chatuikit.shared.events.CometChatUserEvents;
import com.cometchat.chatuikit.shared.resources.utils.Utils;

import java.util.HashMap;
import java.util.List;

/**
 * ViewModel for managing the message header data and state.
 *
 * <p>
 * This ViewModel holds the user and group information, along with member count,
 * typing indicators, and user presence status. It provides LiveData objects to
 * observe changes in these states.
 */
public class MessageHeaderViewModel extends ViewModel {
    private static final String TAG = MessageHeaderViewModel.class.getSimpleName();
    public User user;
    public Group group;
    public String id;
    public MutableLiveData<Integer> memberCount;
    public MutableLiveData<HashMap<TypingIndicator, Boolean>> typing;
    public MutableLiveData<CometChatException> exception;
    public MutableLiveData<User> userPresenceStatus;
    public HashMap<TypingIndicator, Boolean> typingIndicatorHashMap;
    private final Runnable updateTypingRunnable = new Runnable() {
        @Override
        public void run() {
            typing.setValue(typingIndicatorHashMap);
        }
    };
    public MutableLiveData<Group> updateGroup;
    public MutableLiveData<User> updateUser;
    private String LISTENERS_TAG;
    private Handler handler = new Handler();

    public MessageHeaderViewModel() {
        memberCount = new MutableLiveData<>();
        exception = new MutableLiveData<>();
        typing = new MutableLiveData<>();
        userPresenceStatus = new MutableLiveData<>();
        updateGroup = new MutableLiveData<>();
        updateUser = new MutableLiveData<>();
        typingIndicatorHashMap = new HashMap<>();
    }

    public MutableLiveData<CometChatException> getException() {
        return exception;
    }

    /**
     * Gets the count of members in the group.
     *
     * @return A {@link MutableLiveData} object containing the member count.
     */
    public MutableLiveData<Integer> getMemberCount() {
        return memberCount;
    }

    /**
     * Gets the typing indicator status for the members.
     *
     * @return A {@link MutableLiveData} object containing a map of
     * {@link TypingIndicator} and its corresponding status.
     */
    public MutableLiveData<HashMap<TypingIndicator, Boolean>> getTyping() {
        return typing;
    }

    /**
     * Gets the presence status of the user.
     *
     * @return A {@link MutableLiveData} object containing the presence status of a
     * {@link User}.
     */
    public MutableLiveData<User> getUserPresenceStatus() {
        return userPresenceStatus;
    }

    /**
     * Gets the updated group information.
     *
     * @return A {@link MutableLiveData} object containing the updated
     * {@link Group}.
     */
    public MutableLiveData<Group> getUpdatedGroup() {
        return updateGroup;
    }

    /**
     * Gets the updated user information.
     *
     * @return A {@link MutableLiveData} object containing the updated {@link User}.
     */
    public MutableLiveData<User> getUpdatedUser() {
        return updateUser;
    }

    /**
     * Gets the current user.
     *
     * @return The current {@link User} object.
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the current user and clears the group reference.
     *
     * @param user The {@link User} to set. If null, the user reference will not be
     *             updated.
     */
    public void setUser(User user) {
        if (user != null) {
            this.user = user;
            this.group = null;
            this.id = user.getUid();
        }
    }

    /**
     * Gets the current group.
     *
     * @return The current {@link Group} object.
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Sets the current group and clears the user reference.
     *
     * @param group The {@link Group} to set. If null, the group reference will not be
     *              updated.
     */
    public void setGroup(Group group) {
        if (group != null) {
            this.group = group;
            this.user = null;
            this.id = group.getGuid();
        }
    }

    public void addListener() {
        LISTENERS_TAG = System.currentTimeMillis() + "";

        CometChat.addGroupListener(LISTENERS_TAG, new CometChat.GroupListener() {
            @Override
            public void onGroupMemberJoined(Action action, User joinedUser, Group joinedGroup) {
                super.onGroupMemberJoined(action, joinedUser, joinedGroup);
                if (joinedGroup.getGuid().equals(id)) {
                    group.setMembersCount(joinedGroup.getMembersCount());
                    if (joinedUser.getUid().equals(CometChat.getLoggedInUser().getUid())) {
                        group.setHasJoined(true);
                    }
                    updateGroup.setValue(group);
                }
            }

            @Override
            public void onGroupMemberLeft(Action action, User leftUser, Group leftGroup) {
                if (leftGroup.getGuid().equals(id)) {
                    group.setMembersCount(leftGroup.getMembersCount());
                    if (leftUser.getUid().equals(CometChat.getLoggedInUser().getUid())) {
                        group.setHasJoined(false);
                    }
                    updateGroup.setValue(group);
                }
            }

            @Override
            public void onGroupMemberKicked(Action action, User kickedUser, User kickedBy, Group kickedFrom) {
                if (kickedFrom.getGuid().equals(id)) {
                    group.setMembersCount(kickedFrom.getMembersCount());
                    if (kickedUser.getUid().equals(CometChat.getLoggedInUser().getUid())) {
                        group.setHasJoined(false);
                    }
                    updateGroup.setValue(group);
                }
            }

            @Override
            public void onGroupMemberBanned(Action action, User bannedUser, User bannedBy, Group bannedFrom) {
                if (bannedFrom.getGuid().equals(id)) {
                    group.setMembersCount(bannedFrom.getMembersCount());
                    if (bannedUser.getUid().equals(CometChat.getLoggedInUser().getUid())) {
                        group.setHasJoined(false);
                    }
                    updateGroup.setValue(group);
                }
            }

            @Override
            public void onMemberAddedToGroup(Action action, User addedBy, User userAdded, Group addedTo) {
                if (addedTo.getGuid().equals(id)) {
                    group.setMembersCount(addedTo.getMembersCount());
                    if (userAdded.getUid().equals(CometChat.getLoggedInUser().getUid())) {
                        group.setHasJoined(true);
                    }
                    updateGroup.setValue(group);
                }
            }
        });
        CometChatMessageEvents.addListener(LISTENERS_TAG, new CometChatMessageEvents() {
            @Override
            public void onTypingStarted(TypingIndicator typingIndicator) {
                if (!Utils.isBlocked(user)) {
                    setTypingIndicator(typingIndicator, true);
                }
            }

            @Override
            public void onTypingEnded(TypingIndicator typingIndicator) {
                if (!Utils.isBlocked(user)) {
                    setTypingIndicator(typingIndicator, false);
                }
            }
        });

        CometChat.addUserListener(LISTENERS_TAG, new CometChat.UserListener() {
            @Override
            public void onUserOnline(User mUser) {
                if (mUser.getUid().equals(id) && !Utils.isBlocked(user)) {
                    userPresenceStatus.setValue(mUser);
                }
            }

            @Override
            public void onUserOffline(User mUser) {
                if (mUser.getUid().equals(id) && !Utils.isBlocked(user)) {
                    userPresenceStatus.setValue(mUser);
                }
            }
        });

        CometChatUserEvents.addUserListener(LISTENERS_TAG, new CometChatUserEvents() {
            @Override
            public void ccUserBlocked(User user) {
                updateUser.setValue(user);
            }

            @Override
            public void ccUserUnblocked(User user) {
                updateUser.setValue(user);
            }
        });

        CometChatGroupEvents.addGroupListener(LISTENERS_TAG, new CometChatGroupEvents() {
            @Override
            public void ccGroupMemberAdded(List<Action> actionMessages, List<User> usersAdded, Group userAddedIn, User addedBy) {
                if (userAddedIn != null) updateGroup.setValue(userAddedIn);
            }

            @Override
            public void ccGroupMemberKicked(Action actionMessage, User kickedUser, User kickedBy, Group kickedFrom) {
                if (kickedFrom != null) updateGroup.setValue(kickedFrom);
            }

            @Override
            public void ccGroupMemberBanned(Action actionMessage, User bannedUser, User bannedBy, Group bannedFrom) {
                if (bannedFrom != null) updateGroup.setValue(bannedFrom);
            }

            @Override
            public void ccOwnershipChanged(Group group, GroupMember newOwner) {
                if (group != null) updateGroup.setValue(group);
            }
        });

        CometChat.addConnectionListener(LISTENERS_TAG, new CometChat.ConnectionListener() {
            @Override
            public void onConnected() {
                refreshMessageHeader();
            }

            @Override
            public void onConnecting() {
            }

            @Override
            public void onDisconnected() {
            }

            @Override
            public void onFeatureThrottled() {
            }

            @Override
            public void onConnectionError(CometChatException e) {
            }
        });
    }

    /**
     * Sets the typing indicator for a user or group.
     *
     * @param typingIndicator The typing indicator object representing the typing state.
     * @param show            Indicates whether to show or hide the typing indicator.
     */
    public void setTypingIndicator(TypingIndicator typingIndicator, boolean show) {
        if (typingIndicator.getReceiverType().equalsIgnoreCase(CometChatConstants.RECEIVER_TYPE_USER)) {
            if (id != null && id.equalsIgnoreCase(typingIndicator.getSender().getUid())) {
                sendTypingEvent(typingIndicator, show);
            }
        } else {
            if (id != null && id.equalsIgnoreCase(typingIndicator.getReceiverId())) {
                sendTypingEvent(typingIndicator, show);
            }
        }
    }

    /**
     * Refreshes the message header by checking if a user or group is present. If a
     * user is available, it refreshes the user's details; otherwise, it refreshes
     * the group's details.
     */
    public void refreshMessageHeader() {
        if (user != null) {
            refreshUser(user.getUid());
        } else if (group != null) {
            refreshGroup(group.getGuid());
        }
    }

    /**
     * Sends the typing event to the UI.
     *
     * @param typingIndicator The typing indicator object representing the typing state.
     * @param show            Indicates whether to show or hide the typing indicator.
     */
    private void sendTypingEvent(TypingIndicator typingIndicator, boolean show) {
        typingIndicatorHashMap.clear();
        if (show) {
            typingIndicatorHashMap.put(typingIndicator, true);
            typing.setValue(typingIndicatorHashMap);
        } else {
            typingIndicatorHashMap.put(typingIndicator, false);
            handler.removeCallbacks(updateTypingRunnable);
            handler = new Handler();
            handler.postDelayed(updateTypingRunnable, UIKitUtilityConstants.TYPING_INDICATOR_DEBOUNCER);
        }
    }

    /**
     * Refreshes the details of the specified user.
     *
     * @param uid The unique identifier of the user to be refreshed.
     */
    public void refreshUser(String uid) {
        if (uid != null) {
            CometChat.getUser(uid, new CometChat.CallbackListener<User>() {
                @Override
                public void onSuccess(User user) {
                    setUser(user);
                    updateUser.setValue(user);
                }

                @Override
                public void onError(CometChatException e) {
                    exception.setValue(e);
                    CometChatLogger.e(TAG, e.toString());
                }
            });
        }
    }

    /**
     * Refreshes the details of the specified group.
     *
     * @param guid The unique identifier of the group to be refreshed.
     */
    public void refreshGroup(String guid) {
        if (guid != null) {
            CometChat.getGroup(guid, new CometChat.CallbackListener<Group>() {
                @Override
                public void onSuccess(Group group) {
                    setGroup(group);
                    updateGroup.setValue(group);
                }

                @Override
                public void onError(CometChatException e) {
                    exception.setValue(e);
                    CometChatLogger.e(TAG, e.toString());
                }
            });
        }
    }

    /**
     * Removes all listeners associated with the current ViewModel. This includes
     * user, group, and connection listeners to prevent memory leaks.
     */
    public void removeListeners() {
        CometChat.removeUserListener(LISTENERS_TAG);
        CometChat.removeGroupListener(LISTENERS_TAG);
        CometChatMessageEvents.removeListener(LISTENERS_TAG);
        CometChatGroupEvents.removeListener(LISTENERS_TAG);
        CometChat.removeConnectionListener(LISTENERS_TAG);
        CometChatUserEvents.removeListener(LISTENERS_TAG);
    }
}
