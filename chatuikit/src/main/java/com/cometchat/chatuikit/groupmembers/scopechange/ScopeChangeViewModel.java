package com.cometchat.chatuikit.groupmembers.scopechange;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.Action;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.GroupMember;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.events.CometChatGroupEvents;
import com.cometchat.chatuikit.shared.resources.utils.Utils;

public class ScopeChangeViewModel extends ViewModel {
    private static final String TAG = ScopeChangeViewModel.class.getSimpleName();


    private Group group;
    private GroupMember groupMember;
    private final MutableLiveData<UIKitConstants.DialogState> states;
    private CometChat.CallbackListener<GroupMember> scopeChangeCallback;

    public ScopeChangeViewModel() {
        this.states = new MutableLiveData<>();
    }

    public MutableLiveData<UIKitConstants.DialogState> getStates() {
        return states;
    }

    public Group getGroup() {
        return group;
    }

    public GroupMember getGroupMember() {
        return groupMember;
    }

    public void setGroupData(Group group, GroupMember groupMember) {
        this.group = group;
        this.groupMember = groupMember;
    }

    public void changeScope(String scopeChangedTo) {
        states.setValue(UIKitConstants.DialogState.INITIATED);
        String newScope = scopeChangedTo.toLowerCase();
        if (!newScope.equalsIgnoreCase(groupMember.getScope())) {
            CometChat.updateGroupMemberScope(groupMember.getUid(), group.getGuid(), newScope, new CometChat.CallbackListener<String>() {
                @Override
                public void onSuccess(String successMessage) {
                    states.setValue(UIKitConstants.DialogState.SUCCESS);
                    Action action = Utils.getGroupActionMessage(groupMember, group, group, group.getGuid());
                    action.setNewScope(scopeChangedTo);
                    action.setAction(CometChatConstants.ActionKeys.ACTION_SCOPE_CHANGED);
                    for (CometChatGroupEvents e : CometChatGroupEvents.groupEvents.values()) {
                        e.ccGroupMemberScopeChanged(action, groupMember, newScope, groupMember.getScope(), group);
                    }
                    groupMember.setScope(scopeChangedTo);
                    if (scopeChangeCallback != null) scopeChangeCallback.onSuccess(groupMember);
                }

                @Override
                public void onError(CometChatException e) {
                    states.setValue(UIKitConstants.DialogState.FAILURE);
                    if (scopeChangeCallback != null) scopeChangeCallback.onError(e);
                }
            });
        } else {
            if (scopeChangeCallback != null) scopeChangeCallback.onSuccess(groupMember);
        }
    }

    public void setScopeChangeCallback(CometChat.CallbackListener<GroupMember> scopeChangeCallback) {
        this.scopeChangeCallback = scopeChangeCallback;
    }
}
