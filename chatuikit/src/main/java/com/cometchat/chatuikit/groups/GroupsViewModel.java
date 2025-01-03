package com.cometchat.chatuikit.groups;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.core.GroupsRequest;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.Action;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.GroupMember;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.events.CometChatGroupEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel class for managing the UI-related data for the group
 * functionalities in the CometChat UIKit. This class interacts with the
 * CometChat API to manage groups and their members, handle group events, and
 * maintain the state of the groups in the UI.
 */
public class GroupsViewModel extends ViewModel {
    private static final String TAG = GroupsViewModel.class.getSimpleName();
    /**
     * Builder for creating group requests.
     */
    public GroupsRequest.GroupsRequestBuilder groupsRequestBuilder;

    /**
     * Builder for searching groups.
     */
    public GroupsRequest.GroupsRequestBuilder searchGroupsRequestBuilder;

    /**
     * The group request instance for fetching groups.
     */
    private GroupsRequest groupsRequest;

    /**
     * Tag for listeners to identify their callbacks.
     */
    public String LISTENERS_TAG;

    /**
     * LiveData containing the list of groups.
     */
    public MutableLiveData<List<Group>> mutableGroupsList;

    /**
     * LiveData for the currently joined group.
     */
    public MutableLiveData<Group> joinedGroupMutableLiveData;

    /**
     * LiveData indicating the position to insert a group at the top of the list.
     */
    public MutableLiveData<Integer> insertAtTop;

    /**
     * LiveData indicating the position to move a group to the top of the list.
     */
    public MutableLiveData<Integer> moveToTop;

    /**
     * Array list containing all groups.
     */
    public List<Group> groupArrayList;

    /**
     * LiveData indicating the position of an updated group.
     */
    public MutableLiveData<Integer> updateGroup;

    /**
     * LiveData indicating the position of a removed group.
     */
    public MutableLiveData<Integer> removeGroup;

    /**
     * LiveData for CometChat exceptions that may occur.
     */
    public MutableLiveData<CometChatException> cometchatException;

    /**
     * LiveData for the group that has been created.
     */
    public MutableLiveData<Group> createdGroup;

    /**
     * LiveData representing the state of the groups (e.g., loading, empty).
     */
    public MutableLiveData<UIKitConstants.States> states;

    /**
     * The limit for the number of groups to fetch in a single request.
     */
    public int limit = 30;

    /**
     * Flag indicating if the connection listener is attached.
     */
    public boolean connectionListerAttached;

    /**
     * Flag indicating if more groups are available to fetch.
     */
    public boolean hasMore = true;

    /**
     * Initializes the GroupsViewModel and sets up the necessary LiveData objects.
     */
    public GroupsViewModel() {
        mutableGroupsList = new MutableLiveData<>();
        insertAtTop = new MutableLiveData<>();
        moveToTop = new MutableLiveData<>();
        groupArrayList = new ArrayList<>();
        updateGroup = new MutableLiveData<>();
        removeGroup = new MutableLiveData<>();
        createdGroup = new MutableLiveData<>();
        joinedGroupMutableLiveData = new MutableLiveData<>();
        cometchatException = new MutableLiveData<>();
        states = new MutableLiveData<>();
        groupsRequestBuilder = new GroupsRequest.GroupsRequestBuilder().setLimit(limit);
        groupsRequest = groupsRequestBuilder.build();
        LISTENERS_TAG = System.currentTimeMillis() + "";
    }

    /**
     * Gets the LiveData containing the list of groups.
     *
     * @return MutableLiveData containing the list of groups.
     */
    public MutableLiveData<List<Group>> getMutableGroupsList() {
        return mutableGroupsList;
    }

    /**
     * Gets the LiveData indicating the position to insert a group at the top.
     *
     * @return MutableLiveData indicating the position to insert a group.
     */
    public MutableLiveData<Integer> insertAtTop() {
        return insertAtTop;
    }

    /**
     * Gets the LiveData indicating the position to move a group to the top.
     *
     * @return MutableLiveData indicating the position to move a group.
     */
    public MutableLiveData<Integer> moveToTop() {
        return moveToTop;
    }

    /**
     * Gets the list of groups.
     *
     * @return List of groups.
     */
    public List<Group> getGroupArrayList() {
        return groupArrayList;
    }

    /**
     * Gets the LiveData for the created group.
     *
     * @return MutableLiveData containing the created group.
     */
    public MutableLiveData<Group> getCreatedGroup() {
        return createdGroup;
    }

    /**
     * Gets the LiveData for the joined group.
     *
     * @return MutableLiveData containing the joined group.
     */
    public MutableLiveData<Group> getJoinedGroupMutableLiveData() {
        return joinedGroupMutableLiveData;
    }

    /**
     * Gets the LiveData indicating the position of an updated group.
     *
     * @return MutableLiveData indicating the position of an updated group.
     */
    public MutableLiveData<Integer> updateGroup() {
        return updateGroup;
    }

    /**
     * Gets the LiveData indicating the position of a removed group.
     *
     * @return MutableLiveData indicating the position of a removed group.
     */
    public MutableLiveData<Integer> removeGroup() {
        return removeGroup;
    }

    /**
     * Gets the LiveData for CometChat exceptions.
     *
     * @return MutableLiveData containing CometChat exceptions.
     */
    public MutableLiveData<CometChatException> getCometChatException() {
        return cometchatException;
    }

    /**
     * Gets the LiveData for the state of the groups.
     *
     * @return MutableLiveData representing the state of the groups.
     */
    public MutableLiveData<UIKitConstants.States> getStates() {
        return states;
    }

    /**
     * Adds listeners for group events such as member joining, leaving, being
     * kicked, or banned.
     */
    public void addListeners() {
        CometChat.addGroupListener(LISTENERS_TAG, new CometChat.GroupListener() {
            @Override
            public void onGroupMemberJoined(Action action, User user, Group group) {
                if (user.getUid().equals(CometChatUIKit.getLoggedInUser().getUid())) {
                    group.setHasJoined(true);
                }
                updateGroup(group);
            }

            @Override
            public void onGroupMemberLeft(Action action, User user, Group group) {
                super.onGroupMemberLeft(action, user, group);
                updateGroup(group);
            }

            @Override
            public void onGroupMemberKicked(Action action, User kickedUser, User kickedBy, Group group) {
                super.onGroupMemberKicked(action, kickedUser, kickedBy, group);
                updateGroup(group);
            }

            @Override
            public void onGroupMemberBanned(Action action, User bannedUser, User bannedBy, Group group) {
                super.onGroupMemberBanned(action, bannedUser, bannedBy, group);
                updateGroup(group);
            }

            @Override
            public void onGroupMemberUnbanned(Action action, User user, User user1, Group group) {
                super.onGroupMemberUnbanned(action, user, user1, group);
                updateGroup(group);
            }

            @Override
            public void onGroupMemberScopeChanged(Action action, User user, User user1, String s, String s1, Group group) {
                super.onGroupMemberScopeChanged(action, user, user1, s, s1, group);
                updateGroup(group);
            }

            @Override
            public void onMemberAddedToGroup(Action action, User user, User user1, Group group) {
                super.onMemberAddedToGroup(action, user, user1, group);
                updateGroup(group);
            }
        });

        CometChatGroupEvents.addGroupListener(LISTENERS_TAG, new CometChatGroupEvents() {
            @Override
            public void ccGroupCreated(Group group) {
                if (group != null) {
                    createdGroup.setValue(group);
                    addToTop(group);
                }
            }

            @Override
            public void ccGroupLeft(Action actionMessage, User leftUser, Group leftGroup) {
                if (leftUser != null && leftUser.getUid().equalsIgnoreCase(CometChatUIKit.getLoggedInUser().getUid()) && leftGroup != null) {
                    if (leftGroup.getGroupType().equalsIgnoreCase(UIKitConstants.GroupType.PRIVATE))
                        removeGroup(leftGroup);
                    else updateGroup(leftGroup);
                }
            }

            @Override
            public void ccGroupMemberBanned(Action actionMessage, User bannedUser, User bannedBy, Group bannedFrom) {
                if (bannedFrom != null) {
                    updateGroup(bannedFrom);
                }
            }

            @Override
            public void ccGroupMemberAdded(List<Action> actionMessages, List<User> usersAdded, Group userAddedIn, User addedBy) {
                if (userAddedIn != null) {
                    updateGroup(userAddedIn);
                }
            }

            @Override
            public void ccGroupMemberKicked(Action actionMessage, User kickedUser, User kickedBy, Group kickedFrom) {
                if (kickedFrom != null) {
                    updateGroup(kickedFrom);
                }
            }

            @Override
            public void ccGroupMemberUnBanned(Action actionMessage, User unbannedUser, User unBannedBy, Group unBannedFrom) {
            }

            @Override
            public void ccGroupMemberJoined(User joinedUser, Group joinedGroup) {
                if (joinedGroup != null) {
                    updateGroup(joinedGroup);
                }
            }

            @Override
            public void ccOwnershipChanged(Group group, GroupMember newOwner) {
                updateGroup(group);
            }

            @Override
            public void ccGroupDeleted(Group group) {
                if (group != null) {
                    removeGroup(group);
                }
            }
        });
    }

    /**
     * Adds a connection listener to handle connection events.
     */
    public void addConnectionListener() {
        CometChat.addConnectionListener(LISTENERS_TAG, new CometChat.ConnectionListener() {
            @Override
            public void onConnected() {
                refreshList();
            }

            @Override
            public void onConnecting() {
                // Connection is in progress.
            }

            @Override
            public void onDisconnected() {
                // Connection has been lost.
            }

            @Override
            public void onFeatureThrottled() {
                // Feature is being throttled.
            }

            @Override
            public void onConnectionError(CometChatException e) {
                // Handle connection error.
            }
        });
    }

    /**
     * Removes all listeners added to handle group and connection events.
     */
    public void removeListeners() {
        CometChat.removeGroupListener(LISTENERS_TAG);
        CometChatGroupEvents.removeListener(LISTENERS_TAG);
        CometChat.removeConnectionListener(LISTENERS_TAG);
    }

    /**
     * Updates the specified group in the group array list.
     *
     * @param group The group to update.
     */
    public void updateGroup(Group group) {
        if (groupArrayList.contains(group)) {
            groupArrayList.set(groupArrayList.indexOf(group), group);
            updateGroup.setValue(groupArrayList.indexOf(group));
        }
    }

    /**
     * Moves the specified group to the top of the group array list.
     *
     * @param group The group to move.
     */
    public void moveToTop(Group group) {
        if (groupArrayList.contains(group)) {
            int oldIndex = groupArrayList.indexOf(group);
            groupArrayList.remove(group);
            groupArrayList.add(0, group);
            moveToTop.setValue(oldIndex);
        }
    }

    /**
     * Adds the specified group to the top of the group array list if it is not
     * already present.
     *
     * @param group The group to add to the top.
     */
    public void addToTop(@Nullable Group group) {
        if (group != null && !groupArrayList.contains(group)) {
            groupArrayList.add(0, group);
            insertAtTop.setValue(0);
        }
    }

    /**
     * Removes the specified group from the group array list.
     *
     * @param group The group to remove.
     */
    public void removeGroup(Group group) {
        if (groupArrayList.contains(group)) {
            int index = groupArrayList.indexOf(group);
            this.groupArrayList.remove(group);
            removeGroup.setValue(index);
            states.setValue(checkIsEmpty(groupArrayList));
        }
    }

    /**
     * Initiates a fetch for groups. If the group array list is empty, it updates
     * the state to loading.
     */
    public void fetchGroup() {
        if (groupArrayList.isEmpty()) {
            states.setValue(UIKitConstants.States.LOADING);
        }
        if (hasMore) {
            fetchGroupList(false);
        }
    }

    /**
     * Fetches the next list of groups. If cleanAndLoad is true, clears the existing
     * group list before loading new groups.
     *
     * @param cleanAndLoad Boolean indicating whether to clear the existing list.
     */
    public void fetchGroupList(boolean cleanAndLoad) {
        groupsRequest.fetchNext(new CometChat.CallbackListener<List<Group>>() {
            @Override
            public void onSuccess(List<Group> groups) {
                if (cleanAndLoad) clear();

                hasMore = !groups.isEmpty();
                if (hasMore) addList(groups);

                // Update state to either LOADED or EMPTY based on the list size
                states.setValue(checkIsEmpty(groupArrayList));

                // Attach the connection listener if not already attached
                if (!connectionListerAttached) {
                    addConnectionListener();
                    connectionListerAttached = true;
                }
            }

            @Override
            public void onError(CometChatException exception) {
                cometchatException.setValue(exception);
                if (groupArrayList.isEmpty()) {
                    states.setValue(UIKitConstants.States.ERROR);
                }
            }
        });
    }

    /**
     * Refreshes the group list by re-building the groups request and fetching the
     * group list again.
     */
    public void refreshList() {
        if (groupsRequestBuilder != null) {
            groupsRequest = groupsRequestBuilder.build();
            hasMore = true;
            fetchGroupList(true);
        }
    }

    /**
     * Joins a group with the specified group ID.
     *
     * @param groupId The ID of the group to join.
     */
    public void joinGroup(String groupId) {
        CometChat.joinGroup(groupId, CometChatConstants.GROUP_TYPE_PUBLIC, "", new CometChat.CallbackListener<Group>() {
            @Override
            public void onSuccess(Group joinedGroup) {
                joinedGroup.setHasJoined(true);
                joinedGroup.setScope(UIKitConstants.GroupMemberScope.PARTICIPANTS);
                joinedGroupMutableLiveData.setValue(joinedGroup);
            }

            @Override
            public void onError(CometChatException e) {
                cometchatException.setValue(e);
            }
        });
    }

    /**
     * Searches for groups based on the provided search keyword. Clears existing
     * data and initiates a new fetch.
     *
     * @param search The search keyword for finding groups.
     */
    public void searchGroups(String search) {
        clear();
        hasMore = true;
        if (searchGroupsRequestBuilder == null)
            groupsRequest = groupsRequestBuilder.setSearchKeyWord(search).build();
        else groupsRequest = searchGroupsRequestBuilder.setSearchKeyWord(search).build();
        fetchGroup();
    }

    /**
     * Adds a list of groups to the group array list, updating their positions if
     * they already exist in the list.
     *
     * @param groupList The list of groups to add.
     */
    public void addList(List<Group> groupList) {
        for (Group group : groupList) {
            if (groupArrayList.contains(group)) {
                int index = groupArrayList.indexOf(group);
                groupArrayList.remove(index);
                groupArrayList.add(index, group);
            } else {
                groupArrayList.add(group);
            }
        }
        mutableGroupsList.setValue(groupArrayList);
    }

    /**
     * Checks if the provided list of groups is empty and returns the appropriate
     * state.
     *
     * @param groupList The list of groups to check.
     * @return UIKitConstants.States indicating whether the list is empty or not.
     */
    public UIKitConstants.States checkIsEmpty(List<Group> groupList) {
        if (groupList.isEmpty()) return UIKitConstants.States.EMPTY;
        return UIKitConstants.States.NON_EMPTY;
    }

    /**
     * Sets the GroupsRequestBuilder for fetching groups.
     *
     * @param groupsRequestBuilder The builder to set.
     */
    public void setGroupsRequestBuilder(GroupsRequest.GroupsRequestBuilder groupsRequestBuilder) {
        if (groupsRequestBuilder != null) {
            this.groupsRequestBuilder = groupsRequestBuilder;
            this.groupsRequest = this.groupsRequestBuilder.build();
        }
    }

    /**
     * Sets the search request builder for searching groups.
     *
     * @param groupsRequestBuilder The builder to set for searching.
     */
    public void setSearchRequestBuilder(GroupsRequest.GroupsRequestBuilder groupsRequestBuilder) {
        if (groupsRequestBuilder != null) this.searchGroupsRequestBuilder = groupsRequestBuilder;
    }

    /**
     * Clears the group array list and updates the LiveData.
     */
    public void clear() {
        groupArrayList.clear();
        mutableGroupsList.setValue(groupArrayList);
    }
}
