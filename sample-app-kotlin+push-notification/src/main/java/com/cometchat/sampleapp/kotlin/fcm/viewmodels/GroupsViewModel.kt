package com.cometchat.sampleapp.kotlin.fcm.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chatuikit.shared.constants.UIKitConstants.DialogState
import com.cometchat.sampleapp.kotlin.fcm.data.repository.Repository

/**
 * ViewModel for managing the state and data of groups in the chat application.
 */
class GroupsViewModel : ViewModel() {
    /**
     * Gets the LiveData for the group that the user has joined.
     *
     * @return MutableLiveData object containing the joined Group.
     */
    val joinedGroup: MutableLiveData<Group> = MutableLiveData()

    /**
     * Gets the LiveData for the group that has been created.
     *
     * @return MutableLiveData object containing the created Group.
     */
    val createdGroup: MutableLiveData<Group> = MutableLiveData()

    /**
     * Gets the LiveData for any errors that occur during group operations.
     *
     * @return MutableLiveData object containing error messages.
     */
    val error: MutableLiveData<String?> = MutableLiveData()

    /**
     * Gets the LiveData for the state of dialog operations.
     *
     * @return MutableLiveData object containing the DialogState for group
     * operations.
     */
    val dialogState: MutableLiveData<DialogState> = MutableLiveData()

    /**
     * Joins a password-protected group.
     *
     * @param group
     * The Group to join.
     * @param password
     * The password for the group.
     */
    fun joinPasswordGroup(
        group: Group, password: String?
    ) {
        dialogState.value = DialogState.INITIATED
        Repository.joinGroup(group.guid, group.groupType, password, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(group: Group) {
                dialogState.value = DialogState.SUCCESS
                joinedGroup.value = group
            }

            override fun onError(e: CometChatException) {
                dialogState.value = DialogState.FAILURE
                error.value = e.message
                joinedGroup.value = group
            }
        })
    }

    /**
     * Creates a new group.
     *
     * @param group
     * The Group to be created.
     */
    fun createGroup(group: Group) {
        dialogState.value = DialogState.INITIATED
        Repository.createGroup(group, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(group: Group) {
                dialogState.value = DialogState.SUCCESS
                createdGroup.value = group
            }

            override fun onError(e: CometChatException) {
                dialogState.value = DialogState.FAILURE
                error.value = e.message
                createdGroup.value = group
            }
        })
    }
}
