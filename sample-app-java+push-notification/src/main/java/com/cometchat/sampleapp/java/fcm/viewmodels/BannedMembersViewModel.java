package com.cometchat.sampleapp.java.fcm.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.chat.core.BannedGroupMembersRequest;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.Action;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.GroupMember;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.events.CometChatGroupEvents;
import com.cometchat.chatuikit.shared.utils.MembersUtils;
import com.cometchat.sampleapp.java.fcm.data.repository.Repository;

import java.util.ArrayList;
import java.util.List;

public class BannedMembersViewModel extends ViewModel {


    private final String LISTENERS_TAG;
    private final Action action;
    private final MutableLiveData<List<GroupMember>> mutableBannedGroupMembersList;
    private final MutableLiveData<UIKitConstants.DialogState> dialogStates;
    private final MutableLiveData<Integer> insertAtTop;
    private final MutableLiveData<Integer> moveToTop;
    private final List<GroupMember> groupMemberArrayList;
    private final MutableLiveData<Integer> updateGroupMember;
    private final MutableLiveData<Integer> removeGroupMember;
    @NonNull
    private final MutableLiveData<CometChatException> cometchatException;
    private final MutableLiveData<UIKitConstants.States> states;
    private String id = "";
    private Group group;
    private boolean hasMore = true;

    private BannedGroupMembersRequest bannedGroupMembersRequest;
    private BannedGroupMembersRequest.BannedGroupMembersRequestBuilder bannedGroupMembersRequestBuilder;
    private BannedGroupMembersRequest.BannedGroupMembersRequestBuilder searchBannedGroupMembersRequestBuilder;

    public BannedMembersViewModel() {
        mutableBannedGroupMembersList = new MutableLiveData<>();
        insertAtTop = new MutableLiveData<>();
        moveToTop = new MutableLiveData<>();
        groupMemberArrayList = new ArrayList<>();
        updateGroupMember = new MutableLiveData<>();
        removeGroupMember = new MutableLiveData<>();
        cometchatException = new MutableLiveData<>();
        action = new Action();
        LISTENERS_TAG = System.currentTimeMillis() + this.getClass().getSimpleName();
        action.setOldScope(UIKitConstants.GroupMemberScope.PARTICIPANTS);
        states = new MutableLiveData<>();
        dialogStates = new MutableLiveData<>();
    }

    /**
     * Gets the LiveData object for the list of banned group members.
     *
     * @return MutableLiveData containing a list of banned GroupMember objects.
     */
    public MutableLiveData<List<GroupMember>> getMutableBannedGroupMembersList() {
        return mutableBannedGroupMembersList;
    }

    /**
     * Gets the LiveData object for the insert position at the top of the banned
     * group members list.
     *
     * @return MutableLiveData containing the index of the member to insert at the
     * top.
     */
    public MutableLiveData<Integer> insertAtTop() {
        return insertAtTop;
    }

    /**
     * Gets the LiveData object for moving a member to the top of the banned group
     * members list.
     *
     * @return MutableLiveData containing the index of the member being moved to the
     * top.
     */
    public MutableLiveData<Integer> moveToTop() {
        return moveToTop;
    }

    /**
     * Gets the list of group members.
     *
     * @return List of GroupMember objects.
     */
    public List<GroupMember> getGroupMemberArrayList() {
        return groupMemberArrayList;
    }

    /**
     * Gets the LiveData object for updating a group member.
     *
     * @return MutableLiveData containing the index of the updated group member.
     */
    public MutableLiveData<Integer> updateGroupMember() {
        return updateGroupMember;
    }

    /**
     * Gets the LiveData object for removing a group member.
     *
     * @return MutableLiveData containing the index of the removed group member.
     */
    public MutableLiveData<Integer> removeGroupMember() {
        return removeGroupMember;
    }

    /**
     * Gets the LiveData object for CometChat exceptions that may occur.
     *
     * @return MutableLiveData containing the CometChatException object.
     */
    public MutableLiveData<CometChatException> getCometChatException() {
        return cometchatException;
    }

    /**
     * Gets the LiveData object for dialog states related to the banned members
     * view.
     *
     * @return MutableLiveData containing the current dialog state.
     */
    public MutableLiveData<UIKitConstants.DialogState> getDialogStates() {
        return dialogStates;
    }

    /**
     * Sets the group for which banned members will be managed.
     *
     * @param group The Group object to set.
     */
    public void setGroup(Group group) {
        if (group != null) {
            this.group = group;
            this.id = group.getGuid();
        }
        int limit = 30;
        if (bannedGroupMembersRequestBuilder == null)
            bannedGroupMembersRequestBuilder = new BannedGroupMembersRequest.BannedGroupMembersRequestBuilder(null).setGuid(id).setLimit(limit);
        if (searchBannedGroupMembersRequestBuilder == null)
            searchBannedGroupMembersRequestBuilder = new BannedGroupMembersRequest.BannedGroupMembersRequestBuilder(null).setGuid(id);
        bannedGroupMembersRequest = bannedGroupMembersRequestBuilder.setGuid(id).build();
    }

    /**
     * Gets the LiveData object for the states of the banned members view.
     *
     * @return MutableLiveData containing the current state.
     */
    public MutableLiveData<UIKitConstants.States> getStates() {
        return states;
    }

    /**
     * Adds listeners for group member ban/unban events.
     */
    public void addListeners() {
        CometChat.addGroupListener(LISTENERS_TAG, new CometChat.GroupListener() {
            @Override
            public void onGroupMemberBanned(Action action, User bannedUser, User bannedBy, Group group_) {
                if (group_ != null && group_.equals(group)) {
                    updateGroupMember(bannedUser, false, false, action);
                }
            }

            @Override
            public void onGroupMemberUnbanned(Action action, User unbannedUser, User unbannedBy, Group group_) {
                removeGroupMember(MembersUtils.userToGroupMember(unbannedUser, false, ""));
            }
        });
        CometChatGroupEvents.addGroupListener(LISTENERS_TAG, new CometChatGroupEvents() {
            @Override
            public void ccGroupMemberBanned(Action actionMessage, User bannedUser, User bannedBy, Group bannedFrom) {
                if (bannedFrom != null && bannedFrom.equals(group)) {
                    updateGroupMember(bannedUser, false, false, actionMessage);
                }
            }

            @Override
            public void ccGroupMemberUnBanned(
                Action actionMessage, User unbannedUser, User unBannedBy, Group unBannedFrom
            ) {
                if (unBannedFrom != null && unBannedFrom.equals(group)) {
                    updateGroupMember(unbannedUser, true, false, actionMessage);
                }
            }
        });
        CometChat.addUserListener(LISTENERS_TAG, new CometChat.UserListener() {
            @Override
            public void onUserOnline(User user) {
                updateGroupMember(MembersUtils.userToGroupMember(user, false, action.getOldScope()));
            }

            @Override
            public void onUserOffline(User user) {
                updateGroupMember(MembersUtils.userToGroupMember(user, false, action.getOldScope()));
            }
        });
    }

    /**
     * Searches for a group member by their ID.
     *
     * @param id The ID of the group member to search for.
     * @return The GroupMember object if found, otherwise null.
     */
    private GroupMember searchMemberById(String id) {
        GroupMember member = null;
        for (GroupMember groupMember : groupMemberArrayList) {
            if (groupMember.getUid().equalsIgnoreCase(id)) {
                member = groupMember;
                break;
            }
        }
        return member;
    }

    /**
     * Updates a group member based on the given parameters.
     *
     * @param user          The User object representing the group member.
     * @param isRemoved     Indicates if the member is being removed.
     * @param isScopeUpdate Indicates if the member's scope is being updated.
     * @param action        The Action object representing the ban/unban action.
     */
    private void updateGroupMember(User user, boolean isRemoved, boolean isScopeUpdate, Action action) {
        if (!isRemoved && !isScopeUpdate) {
            addToTop(MembersUtils.userToGroupMember(user, false, action.getOldScope()));
        } else if (isRemoved && !isScopeUpdate) {
            removeGroupMember(MembersUtils.userToGroupMember(user, false, action.getOldScope()));
        } else if (!isRemoved) {
            updateGroupMember(MembersUtils.userToGroupMember(user, true, action.getNewScope()));
        }
    }

    /**
     * Removes listeners for group member events.
     */
    public void removeListeners() {
        CometChat.removeGroupListener(LISTENERS_TAG);
        CometChatGroupEvents.removeListener(LISTENERS_TAG);
        CometChat.removeUserListener(LISTENERS_TAG);
    }

    /**
     * Updates a group member in the list.
     *
     * @param groupMember The GroupMember object to update.
     */
    public void updateGroupMember(GroupMember groupMember) {
        if (groupMemberArrayList.contains(groupMember)) {
            groupMemberArrayList.set(groupMemberArrayList.indexOf(groupMember), groupMember);
            updateGroupMember.setValue(groupMemberArrayList.indexOf(groupMember));
        }
    }

    /**
     * Moves a group member to the top of the list.
     *
     * @param groupMember The GroupMember object to move.
     */
    public void moveToTop(GroupMember groupMember) {
        if (groupMemberArrayList.contains(groupMember)) {
            int oldIndex = groupMemberArrayList.indexOf(groupMember);
            groupMemberArrayList.remove(groupMember);
            groupMemberArrayList.add(0, groupMember);
            moveToTop.setValue(oldIndex);
        }
    }

    /**
     * Adds a group member to the top of the banned members list.
     *
     * @param groupMember The GroupMember object to add.
     */
    public void addToTop(GroupMember groupMember) {
        if (groupMember != null && !groupMemberArrayList.contains(groupMember)) {
            groupMemberArrayList.add(0, groupMember);
            insertAtTop.setValue(0);
            states.setValue(checkIsEmpty(groupMemberArrayList));
        }
    }

    /**
     * Removes a group member from the banned members list.
     *
     * @param groupMember The GroupMember object to remove.
     */
    public void removeGroupMember(GroupMember groupMember) {
        if (groupMemberArrayList.contains(groupMember)) {
            int index = groupMemberArrayList.indexOf(groupMember);
            this.groupMemberArrayList.remove(groupMember);
            removeGroupMember.setValue(index);
            states.setValue(checkIsEmpty(groupMemberArrayList));
        }
    }

    /**
     * Searches for banned group members based on a search keyword.
     *
     * @param search The search keyword.
     */
    public void searchBannedGroupMembers(@Nullable String search) {
        clear();
        hasMore = true;
        if (search != null) bannedGroupMembersRequest = searchBannedGroupMembersRequestBuilder.setGuid(id).setSearchKeyword(search).build();
        else bannedGroupMembersRequest = bannedGroupMembersRequestBuilder.build();
        fetchGroupMember();
    }

    /**
     * Clears the banned group members list.
     */
    public void clear() {
        groupMemberArrayList.clear();
        mutableBannedGroupMembersList.setValue(groupMemberArrayList);
    }

    /**
     * Fetches banned group members.
     */
    public void fetchGroupMember() {
        if (groupMemberArrayList.isEmpty()) states.setValue(UIKitConstants.States.LOADING);
        if (hasMore) {
            bannedGroupMembersRequest.fetchNext(new CometChat.CallbackListener<List<GroupMember>>() {
                @Override
                public void onSuccess(List<GroupMember> bannedGroupMembers) {
                    hasMore = !bannedGroupMembers.isEmpty();
                    if (hasMore) addList(bannedGroupMembers);
                    states.setValue(UIKitConstants.States.LOADED);
                    states.setValue(checkIsEmpty(groupMemberArrayList));
                }

                @Override
                public void onError(CometChatException exception) {
                    cometchatException.setValue(exception);
                    states.setValue(UIKitConstants.States.ERROR);
                }
            });
        }
    }

    /**
     * Adds a list of group members to the banned members list.
     *
     * @param groupMemberList The list of GroupMember objects to add.
     */
    public void addList(List<GroupMember> groupMemberList) {
        for (GroupMember groupMember : groupMemberList) {
            if (groupMemberArrayList.contains(groupMember)) {
                int index = groupMemberArrayList.indexOf(groupMember);
                groupMemberArrayList.remove(index);
                groupMemberArrayList.add(index, groupMember);
            } else {
                groupMemberArrayList.add(groupMember);
            }
        }
        mutableBannedGroupMembersList.setValue(groupMemberArrayList);
    }

    /**
     * Checks if the banned group members list is empty.
     *
     * @param bannedGroupMembers The list of banned group members.
     * @return The current state indicating whether the list is empty or not.
     */
    private UIKitConstants.States checkIsEmpty(List<GroupMember> bannedGroupMembers) {
        if (bannedGroupMembers.isEmpty()) return UIKitConstants.States.EMPTY;
        return UIKitConstants.States.NON_EMPTY;
    }

    /**
     * Unbans a group member and updates the dialog state.
     *
     * @param groupMember The GroupMember object to unban.
     */
    public void unBanGroupMember(GroupMember groupMember) {
        dialogStates.setValue(UIKitConstants.DialogState.INITIATED);

        Repository.unBanGroupMember(group, groupMember, new CometChat.CallbackListener<Group>() {
            @Override
            public void onSuccess(Group group) {
                dialogStates.setValue(UIKitConstants.DialogState.SUCCESS);
            }

            @Override
            public void onError(CometChatException e) {
                dialogStates.setValue(UIKitConstants.DialogState.FAILURE);
            }
        });
    }
}
