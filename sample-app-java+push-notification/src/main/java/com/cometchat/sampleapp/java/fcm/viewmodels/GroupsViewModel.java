package com.cometchat.sampleapp.java.fcm.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.Group;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.sampleapp.java.fcm.data.repository.Repository;

/**
 * ViewModel for managing the state and data of groups in the chat application.
 */
public class GroupsViewModel extends ViewModel {

    private final MutableLiveData<Group> joinedGroup;
    private final MutableLiveData<Group> createdGroup;
    private final MutableLiveData<String> error;
    private final MutableLiveData<UIKitConstants.DialogState> dialogState;

    /**
     * Initializes the GroupsViewModel with LiveData objects.
     */
    public GroupsViewModel() {
        joinedGroup = new MutableLiveData<>();
        createdGroup = new MutableLiveData<>();
        error = new MutableLiveData<>();
        dialogState = new MutableLiveData<>();
    }

    /**
     * Gets the LiveData for the group that the user has joined.
     *
     * @return MutableLiveData object containing the joined Group.
     */
    public MutableLiveData<Group> getJoinedGroup() {
        return joinedGroup;
    }

    /**
     * Gets the LiveData for the group that has been created.
     *
     * @return MutableLiveData object containing the created Group.
     */
    public MutableLiveData<Group> getCreatedGroup() {
        return createdGroup;
    }

    /**
     * Gets the LiveData for any errors that occur during group operations.
     *
     * @return MutableLiveData object containing error messages.
     */
    public MutableLiveData<String> getError() {
        return error;
    }

    /**
     * Gets the LiveData for the state of dialog operations.
     *
     * @return MutableLiveData object containing the DialogState for group
     * operations.
     */
    public MutableLiveData<UIKitConstants.DialogState> getDialogState() {
        return dialogState;
    }

    /**
     * Joins a password-protected group.
     *
     * @param group    The Group to join.
     * @param password The password for the group.
     */
    public void joinPasswordGroup(Group group, String password) {
        dialogState.setValue(UIKitConstants.DialogState.INITIATED);
        Repository.joinGroup(group.getGuid(), group.getGroupType(), password, new CometChat.CallbackListener<Group>() {
            @Override
            public void onSuccess(Group group) {
                dialogState.setValue(UIKitConstants.DialogState.SUCCESS);
                joinedGroup.setValue(group);
            }

            @Override
            public void onError(CometChatException e) {
                dialogState.setValue(UIKitConstants.DialogState.FAILURE);
                error.setValue(e.getMessage());
                joinedGroup.setValue(group);
            }
        });
    }

    /**
     * Creates a new group.
     *
     * @param group The Group to be created.
     */
    public void createGroup(Group group) {
        dialogState.setValue(UIKitConstants.DialogState.INITIATED);
        Repository.createGroup(group, new CometChat.CallbackListener<Group>() {
            @Override
            public void onSuccess(Group group) {
                dialogState.setValue(UIKitConstants.DialogState.SUCCESS);
                createdGroup.setValue(group);
            }

            @Override
            public void onError(CometChatException e) {
                dialogState.setValue(UIKitConstants.DialogState.FAILURE);
                error.setValue(e.getMessage());
                createdGroup.setValue(group);
            }
        });
    }
}
