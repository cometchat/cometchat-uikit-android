package com.cometchat.chatuikit.groupmembers;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.core.GroupMembersRequest;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.Action;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.GroupMember;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKitHelper;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.events.CometChatGroupEvents;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.utils.MembersUtils;

import java.util.ArrayList;
import java.util.List;

public class GroupMembersViewModel extends ViewModel {
    private static final String TAG = GroupMembersViewModel.class.getSimpleName();

    public GroupMembersRequest.GroupMembersRequestBuilder groupMembersRequestBuilder;

    public GroupMembersRequest.GroupMembersRequestBuilder searchGroupMembersRequestBuilder;

    private GroupMembersRequest groupMembersRequest;

    public String LISTENERS_TAG;

    public User loggedInUser;

    public Group group;

    public String id = "";

    public MutableLiveData<List<GroupMember>> mutableGroupMembersList;

    public MutableLiveData<Integer> insertAtTop;

    public MutableLiveData<Integer> moveToTop;

    public List<GroupMember> groupMemberArrayList;

    public MutableLiveData<Integer> updateGroupMember;

    public MutableLiveData<Integer> removeGroupMember;

    public MutableLiveData<CometChatException> cometchatException;

    public MutableLiveData<UIKitConstants.States> states;

    public MutableLiveData<UIKitConstants.DialogState> dialogState;

    public boolean excludeOwner;

    public int limit = 30;

    public boolean hasMore = true;

    private boolean connectionListerAttached;

    public GroupMembersViewModel() {
        init();
    }

    private void init() {
        mutableGroupMembersList = new MutableLiveData<>();
        insertAtTop = new MutableLiveData<>();
        moveToTop = new MutableLiveData<>();
        groupMemberArrayList = new ArrayList<>();
        updateGroupMember = new MutableLiveData<>();
        removeGroupMember = new MutableLiveData<>();
        cometchatException = new MutableLiveData<>();
        dialogState = new MutableLiveData<>();
        states = new MutableLiveData<>();
        loggedInUser = CometChatUIKit.getLoggedInUser();
    }

    public MutableLiveData<List<GroupMember>> getMutableGroupMembersList() {
        return mutableGroupMembersList;
    }

    public MutableLiveData<Integer> insertAtTop() {
        return insertAtTop;
    }

    public MutableLiveData<Integer> moveToTop() {
        return moveToTop;
    }

    public List<GroupMember> getGroupMemberArrayList() {
        return groupMemberArrayList;
    }

    public GroupMember getGroupMember(int position) {
        return groupMemberArrayList.get(position);
    }

    public MutableLiveData<Integer> updateGroupMember() {
        return updateGroupMember;
    }

    public MutableLiveData<Integer> removeGroupMember() {
        return removeGroupMember;
    }

    public MutableLiveData<CometChatException> getCometChatException() {
        return cometchatException;
    }

    public MutableLiveData<UIKitConstants.DialogState> getDialogState() {
        return dialogState;
    }

    public void setGroup(Group group) {
        if (group != null) {
            this.group = group;
            this.id = group.getGuid();
        }
        if (groupMembersRequestBuilder == null)
            groupMembersRequestBuilder = new GroupMembersRequest.GroupMembersRequestBuilder(null).setGuid(id).setLimit(limit);
        if (searchGroupMembersRequestBuilder == null)
            searchGroupMembersRequestBuilder = new GroupMembersRequest.GroupMembersRequestBuilder(null).setGuid(id);
        groupMembersRequest = groupMembersRequestBuilder.build();
    }

    public void setExcludeOwner(boolean excludeOwner) {
        this.excludeOwner = excludeOwner;
    }

    public MutableLiveData<UIKitConstants.States> getStates() {
        return states;
    }

    public void addListeners() {
        LISTENERS_TAG = System.currentTimeMillis() + "";
        CometChat.addGroupListener(TAG, new CometChat.GroupListener() {
            @Override
            public void onGroupMemberJoined(Action action, User joinedUser, Group group_) {
                if (group_ != null && group_.equals(group)) {
                    updateGroupMember(joinedUser, false, false, action);
                }
            }

            @Override
            public void onGroupMemberLeft(Action action, User leftUser, Group group_) {
                if (group_ != null && group_.equals(group)) {
                    updateGroupMember(leftUser, true, false, action);
                }
            }

            @Override
            public void onGroupMemberKicked(Action action, User kickedUser, User kickedBy, Group group_) {
                if (group_ != null && group_.equals(group)) {
                    updateGroupMember(kickedUser, true, false, action);
                }
            }

            @Override
            public void onGroupMemberBanned(Action action, User bannedUser, User bannedBy, Group group_) {
                if (group_ != null && group_.equals(group)) {
                    updateGroupMember(bannedUser, true, false, action);
                }
            }

            @Override
            public void onGroupMemberScopeChanged(Action action, User updatedBy, User updatedUser, String scopeChangedTo, String scopeChangedFrom, Group group_) {
                if (group_ != null && group_.equals(group)) {
                    updateGroupMember(updatedUser, false, true, action);
                }
            }

            @Override
            public void onMemberAddedToGroup(Action action, User addedBy, User userAdded, Group group_) {
                if (group_ != null && group_.equals(group)) {
                    updateGroupMember(userAdded, false, false, action);
                }
            }
        });

        CometChatGroupEvents.addGroupListener(LISTENERS_TAG, new CometChatGroupEvents() {
            @Override
            public void ccGroupMemberBanned(Action actionMessage, User bannedUser, User bannedBy, Group bannedFrom) {
                if (bannedFrom != null && bannedFrom.equals(group)) {
                    removeGroupMember(MembersUtils.userToGroupMember(bannedUser, false, ""));
                }
            }

            @Override
            public void ccGroupMemberKicked(Action actionMessage, User kickedUser, User kickedBy, Group kickedFrom) {
                if (kickedFrom != null && kickedFrom.equals(group)) {
                    removeGroupMember(MembersUtils.userToGroupMember(kickedUser, false, ""));
                }
            }

            @Override
            public void ccGroupMemberScopeChanged(Action actionMessage, User updatedUser, String scopeChangedTo, String scopeChangedFrom, Group group_) {
                if (group_.equals(group)) {
                    updateGroupMember(MembersUtils.userToGroupMember(updatedUser, true, scopeChangedTo));
                }
            }

            @Override
            public void ccGroupMemberAdded(List<Action> actionMessages, List<User> usersAdded, Group group_, User addedBy) {
                if (group_ != null && group_.equals(group)) {
                    for (User user : usersAdded)
                        addToTop(MembersUtils.userToGroupMember(user, false, ""));
                }
            }

            @Override
            public void ccGroupMemberUnBanned(Action actionMessage, User unbannedUser, User unBannedBy, Group unBannedFrom) {
                if (unBannedFrom.equals(group)) {
                    setGroup(unBannedFrom);
                    addToTop(MembersUtils.userToGroupMember(unbannedUser, false, ""));
                }
            }

            @Override
            public void ccGroupMemberJoined(User joinedUser, Group joinedGroup) {
                if (joinedGroup.equals(group)) {
                    setGroup(group);
                    addToTop(MembersUtils.userToGroupMember(joinedUser, false, ""));
                }
            }

            @Override
            public void ccOwnershipChanged(Group group_, GroupMember newOwner) {
                if (group_.equals(group)) {
                    setGroup(group_);
                    updateGroupMember(newOwner);
                }
            }
        });
        CometChat.addUserListener(LISTENERS_TAG, new CometChat.UserListener() {
            @Override
            public void onUserOnline(User user) {
                updateGroupMember(getGroupMemberWithUpdatedStatus(user, UIKitConstants.UserStatus.ONLINE));
            }

            @Override
            public void onUserOffline(User user) {
                updateGroupMember(getGroupMemberWithUpdatedStatus(user, UIKitConstants.UserStatus.OFFLINE));
            }
        });
    }

    public void addConnectionListener() {
        CometChat.addConnectionListener(LISTENERS_TAG, new CometChat.ConnectionListener() {
            @Override
            public void onConnected() {
                refreshList();
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

    public GroupMember getGroupMemberWithUpdatedStatus(User user, String status) {
        GroupMember tempGroupMember = null;
        for (GroupMember groupMember : groupMemberArrayList) {
            if (groupMember.getUid().equalsIgnoreCase(user.getUid())) {
                tempGroupMember = groupMember;
                tempGroupMember.setStatus(status);
            }
        }
        return tempGroupMember;
    }

    public void scopeChange(GroupMember updateMember, String scopeChangedTo) {
        CometChat.updateGroupMemberScope(updateMember.getUid(), id, scopeChangedTo, new CometChat.CallbackListener<String>() {
            @Override
            public void onSuccess(String successMessage) {
                updateMember.setScope(scopeChangedTo);
                Action action = Utils.getGroupActionMessage(updateMember, group, group, group.getGuid());
                action.setNewScope(scopeChangedTo);
                action.setAction(CometChatConstants.ActionKeys.ACTION_SCOPE_CHANGED);
                for (CometChatGroupEvents e : CometChatGroupEvents.groupEvents.values()) {
                    e.ccGroupMemberScopeChanged(action, updateMember, scopeChangedTo, updateMember.getScope(), group);
                }
            }

            @Override
            public void onError(CometChatException e) {
                states.setValue(UIKitConstants.States.ERROR);
                updateGroupMember(updateMember);
                onErrorTrigger(e);
            }
        });
    }

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

    private void updateGroupMember(User user, boolean isRemoved, boolean isScopeUpdate, Action action) {
        if (!isRemoved && !isScopeUpdate) {
            addToTop(MembersUtils.userToGroupMember(user, false, action.getOldScope()));
        } else if (isRemoved && !isScopeUpdate) {
            removeGroupMember(MembersUtils.userToGroupMember(user, false, action.getOldScope()));
        } else if (!isRemoved) {
            updateGroupMember(MembersUtils.userToGroupMember(user, true, action.getNewScope()));
        }
    }

    public void removeListeners() {
        CometChat.removeGroupListener(LISTENERS_TAG);
        CometChatGroupEvents.removeListener(LISTENERS_TAG);
        CometChat.removeUserListener(LISTENERS_TAG);
        CometChat.removeConnectionListener(LISTENERS_TAG);
    }

    public void updateGroupMember(GroupMember GroupMember) {
        if (groupMemberArrayList.contains(GroupMember)) {
            groupMemberArrayList.set(groupMemberArrayList.indexOf(GroupMember), GroupMember);
            updateGroupMember.setValue(groupMemberArrayList.indexOf(GroupMember));
        }
    }

    public void moveToTop(GroupMember GroupMember) {
        if (groupMemberArrayList.contains(GroupMember)) {
            int oldIndex = groupMemberArrayList.indexOf(GroupMember);
            groupMemberArrayList.remove(GroupMember);
            groupMemberArrayList.add(0, GroupMember);
            moveToTop.setValue(oldIndex);
        }
    }

    public void addToTop(GroupMember GroupMember) {
        if (GroupMember != null && !groupMemberArrayList.contains(GroupMember)) {
            groupMemberArrayList.add(0, GroupMember);
            insertAtTop.setValue(0);
        }
    }

    public void removeGroupMember(GroupMember GroupMember) {
        if (groupMemberArrayList.contains(GroupMember)) {
            int index = groupMemberArrayList.indexOf(GroupMember);
            this.groupMemberArrayList.remove(GroupMember);
            removeGroupMember.setValue(index);
            states.setValue(checkIsEmpty(groupMemberArrayList));
        }
    }

    public void fetchGroupMember() {
        if (groupMemberArrayList.isEmpty()) states.setValue(UIKitConstants.States.LOADING);
        if (hasMore) {
            fetchGroupMemberList(false);
        }
    }

    private void fetchGroupMemberList(boolean cleanAndLoad) {
        groupMembersRequest.fetchNext(new CometChat.CallbackListener<List<GroupMember>>() {
            @Override
            public void onSuccess(List<GroupMember> GroupMembers) {
                if (excludeOwner)
                    GroupMembers.removeIf(groupMember -> groupMember.getUid().equals(group.getOwner()));
                if (cleanAndLoad) clear();
                hasMore = !GroupMembers.isEmpty();
                if (hasMore) addList(GroupMembers);
                states.setValue(UIKitConstants.States.LOADED);
                states.setValue(checkIsEmpty(groupMemberArrayList));
                if (!connectionListerAttached) {
                    addConnectionListener();
                    connectionListerAttached = true;
                }
            }

            @Override
            public void onError(CometChatException e) {
                onErrorTrigger(e);
                states.setValue(UIKitConstants.States.ERROR);
            }
        });
    }

    public void refreshList() {
        if (groupMembersRequestBuilder != null) {
            clear();
            groupMembersRequest = groupMembersRequestBuilder.build();
            hasMore = true;
            fetchGroupMemberList(true);
        }
    }

    public void searchGroupMembers(String search) {
        groupMemberArrayList.clear();
        hasMore = true;
        if (search != null)
            groupMembersRequest = searchGroupMembersRequestBuilder.setSearchKeyword(search).build();
        else groupMembersRequest = groupMembersRequestBuilder.build();
        fetchGroupMember();
    }

    public void addList(List<GroupMember> GroupMemberList) {
        for (GroupMember GroupMember : GroupMemberList) {
            if (groupMemberArrayList.contains(GroupMember)) {
                int index = groupMemberArrayList.indexOf(GroupMember);
                groupMemberArrayList.remove(index);
                groupMemberArrayList.add(index, GroupMember);
            } else {
                groupMemberArrayList.add(GroupMember);
            }
        }
        mutableGroupMembersList.setValue(groupMemberArrayList);
    }

    private UIKitConstants.States checkIsEmpty(List<GroupMember> GroupMembers) {
        if (GroupMembers.isEmpty()) return UIKitConstants.States.EMPTY;
        return UIKitConstants.States.NON_EMPTY;
    }

    public void setGroupMembersRequestBuilder(GroupMembersRequest.GroupMembersRequestBuilder groupMembersRequestBuilder) {
        if (groupMembersRequestBuilder != null) {
            this.groupMembersRequestBuilder = groupMembersRequestBuilder;
            this.groupMembersRequest = this.groupMembersRequestBuilder.setGuid(id).build();
        }
    }

    public void setSearchRequestBuilder(GroupMembersRequest.GroupMembersRequestBuilder GroupMembersRequestBuilder) {
        if (GroupMembersRequestBuilder != null)
            this.searchGroupMembersRequestBuilder = GroupMembersRequestBuilder;
    }

    public void clear() {
        groupMemberArrayList.clear();
        mutableGroupMembersList.setValue(groupMemberArrayList);
    }

    public void banGroupMember(GroupMember groupMember) {
        dialogState.setValue(UIKitConstants.DialogState.INITIATED);
        CometChat.banGroupMember(groupMember.getUid(), id, new CometChat.CallbackListener<String>() {
            @Override
            public void onSuccess(String successMessage) {
                dialogState.setValue(UIKitConstants.DialogState.SUCCESS);
                group.setMembersCount(group.getMembersCount() - 1);
                Action action = Utils.getGroupActionMessage(groupMember, group, group, group.getGuid());
                action.setAction(CometChatConstants.ActionKeys.ACTION_BANNED);
                CometChatUIKitHelper.onGroupMemberBanned(action, groupMember, loggedInUser, group);
            }

            @Override
            public void onError(CometChatException e) {
                dialogState.setValue(UIKitConstants.DialogState.FAILURE);
            }
        });
    }

    public void kickGroupMember(GroupMember groupMember) {
        dialogState.setValue(UIKitConstants.DialogState.INITIATED);
        CometChat.kickGroupMember(groupMember.getUid(), id, new CometChat.CallbackListener<String>() {
            @Override
            public void onSuccess(String successMessage) {
                dialogState.setValue(UIKitConstants.DialogState.SUCCESS);
                group.setMembersCount(group.getMembersCount() - 1);
                Action action = Utils.getGroupActionMessage(groupMember, group, group, group.getGuid());
                action.setAction(CometChatConstants.ActionKeys.ACTION_KICKED);
                for (CometChatGroupEvents events : CometChatGroupEvents.groupEvents.values()) {
                    events.ccGroupMemberKicked(action, groupMember, loggedInUser, group);
                }
            }

            @Override
            public void onError(CometChatException e) {
                dialogState.setValue(UIKitConstants.DialogState.FAILURE);
            }
        });
    }

    private void onErrorTrigger(CometChatException e) {
        cometchatException.setValue(e);
    }
}
