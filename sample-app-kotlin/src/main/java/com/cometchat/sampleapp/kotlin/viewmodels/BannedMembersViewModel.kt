package com.cometchat.sampleapp.kotlin.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cometchat.chat.core.BannedGroupMembersRequest
import com.cometchat.chat.core.BannedGroupMembersRequest.BannedGroupMembersRequestBuilder
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.CometChat.GroupListener
import com.cometchat.chat.core.CometChat.UserListener
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.constants.UIKitConstants
import com.cometchat.chatuikit.shared.constants.UIKitConstants.DialogState
import com.cometchat.chatuikit.shared.constants.UIKitConstants.States
import com.cometchat.chatuikit.shared.events.CometChatGroupEvents
import com.cometchat.chatuikit.shared.utils.MembersUtils
import com.cometchat.sampleapp.kotlin.data.repository.Repository

class BannedMembersViewModel : ViewModel() {
    private val LISTENERS_TAG = System.currentTimeMillis().toString() + javaClass.simpleName
    private val action = Action()

    /**
     * Gets the LiveData object for the list of banned group members.
     *
     * @return MutableLiveData containing a list of banned GroupMember objects.
     */
    val mutableBannedGroupMembersList: MutableLiveData<List<GroupMember>> = MutableLiveData()

    /**
     * Gets the LiveData object for dialog states related to the banned members
     * view.
     *
     * @return MutableLiveData containing the current dialog state.
     */
    val dialogStates: MutableLiveData<DialogState>
    private val insertAtTop = MutableLiveData<Int>()
    private val moveToTop = MutableLiveData<Int>()
    private val groupMemberArrayList: MutableList<GroupMember> = ArrayList()
    private val updateGroupMember = MutableLiveData<Int>()
    private val removeGroupMember = MutableLiveData<Int>()

    /**
     * Gets the LiveData object for CometChat exceptions that may occur.
     *
     * @return MutableLiveData containing the CometChatException object.
     */
    val cometChatException: MutableLiveData<CometChatException> = MutableLiveData()

    /**
     * Gets the LiveData object for the states of the banned members view.
     *
     * @return MutableLiveData containing the current state.
     */
    val states: MutableLiveData<States>
    private var id = ""
    private var group: Group? = null
    private var hasMore = true
    private var bannedGroupMembersRequest: BannedGroupMembersRequest? = null
    private var bannedGroupMembersRequestBuilder: BannedGroupMembersRequestBuilder? = null
    private var searchBannedGroupMembersRequestBuilder: BannedGroupMembersRequestBuilder? = null

    init {
        action.oldScope = UIKitConstants.GroupMemberScope.PARTICIPANTS
        states = MutableLiveData()
        dialogStates = MutableLiveData()
    }

    /**
     * Gets the LiveData object for the insert position at the top of the banned
     * group members list.
     *
     * @return MutableLiveData containing the index of the member to insert at the
     * top.
     */
    fun insertAtTop(): MutableLiveData<Int> {
        return insertAtTop
    }

    /**
     * Gets the LiveData object for moving a member to the top of the banned group
     * members list.
     *
     * @return MutableLiveData containing the index of the member being moved to the
     * top.
     */
    fun moveToTop(): MutableLiveData<Int> {
        return moveToTop
    }

    /**
     * Gets the list of group members.
     *
     * @return List of GroupMember objects.
     */
    fun getGroupMemberArrayList(): List<GroupMember> {
        return groupMemberArrayList
    }

    /**
     * Gets the LiveData object for updating a group member.
     *
     * @return MutableLiveData containing the index of the updated group member.
     */
    fun updateGroupMember(): MutableLiveData<Int> {
        return updateGroupMember
    }

    /**
     * Gets the LiveData object for removing a group member.
     *
     * @return MutableLiveData containing the index of the removed group member.
     */
    fun removeGroupMember(): MutableLiveData<Int> {
        return removeGroupMember
    }

    /**
     * Sets the group for which banned members will be managed.
     *
     * @param group
     * The Group object to set.
     */
    fun setGroup(group: Group?) {
        if (group != null) {
            this.group = group
            this.id = group.guid
        }
        val limit = 30
        if (bannedGroupMembersRequestBuilder == null) bannedGroupMembersRequestBuilder =
            BannedGroupMembersRequestBuilder(null).setGuid(id).setLimit(limit)
        if (searchBannedGroupMembersRequestBuilder == null) searchBannedGroupMembersRequestBuilder = BannedGroupMembersRequestBuilder(
            null
        ).setGuid(id)
        bannedGroupMembersRequest = bannedGroupMembersRequestBuilder!!.setGuid(id).build()
    }

    /** Adds listeners for group member ban/unban events.  */
    fun addListeners() {
        CometChat.addGroupListener(LISTENERS_TAG, object : GroupListener() {
            override fun onGroupMemberBanned(
                action: Action, bannedUser: User, bannedBy: User, mGroup: Group
            ) {
                if (mGroup == group) {
                    updateGroupMember(bannedUser, false, false, action)
                }
            }

            override fun onGroupMemberUnbanned(
                action: Action, unbannedUser: User, unbannedBy: User, group_: Group
            ) {
                removeGroupMember(MembersUtils.userToGroupMember(unbannedUser, false, ""))
            }
        })
        CometChatGroupEvents.addGroupListener(LISTENERS_TAG, object : CometChatGroupEvents() {
            override fun ccGroupMemberBanned(
                actionMessage: Action, bannedUser: User, bannedBy: User, bannedFrom: Group
            ) {
                if (bannedFrom == group) {
                    updateGroupMember(bannedUser, false, false, actionMessage)
                }
            }

            override fun ccGroupMemberUnBanned(
                actionMessage: Action, unbannedUser: User, unBannedBy: User, unBannedFrom: Group
            ) {
                if (unBannedFrom == group) {
                    updateGroupMember(unbannedUser, true, false, actionMessage)
                }
            }
        })
        CometChat.addUserListener(LISTENERS_TAG, object : UserListener() {
            override fun onUserOnline(user: User) {
                updateGroupMember(MembersUtils.userToGroupMember(user, false, action.oldScope))
            }

            override fun onUserOffline(user: User) {
                updateGroupMember(MembersUtils.userToGroupMember(user, false, action.oldScope))
            }
        })
    }

    /**
     * Searches for a group member by their ID.
     *
     * @param id
     * The ID of the group member to search for.
     * @return The GroupMember object if found, otherwise null.
     */
    private fun searchMemberById(id: String): GroupMember? {
        var member: GroupMember? = null
        for (groupMember in groupMemberArrayList) {
            if (groupMember.uid.equals(id, ignoreCase = true)) {
                member = groupMember
                break
            }
        }
        return member
    }

    /**
     * Updates a group member based on the given parameters.
     *
     * @param user
     * The User object representing the group member.
     * @param isRemoved
     * Indicates if the member is being removed.
     * @param isScopeUpdate
     * Indicates if the member's scope is being updated.
     * @param action
     * The Action object representing the ban/unban action.
     */
    private fun updateGroupMember(
        user: User, isRemoved: Boolean, isScopeUpdate: Boolean, action: Action
    ) {
        if (!isRemoved && !isScopeUpdate) {
            addToTop(MembersUtils.userToGroupMember(user, false, action.oldScope))
        } else if (isRemoved && !isScopeUpdate) {
            removeGroupMember(MembersUtils.userToGroupMember(user, false, action.oldScope))
        } else if (!isRemoved) {
            updateGroupMember(MembersUtils.userToGroupMember(user, true, action.newScope))
        }
    }

    /** Removes listeners for group member events.  */
    fun removeListeners() {
        CometChat.removeGroupListener(LISTENERS_TAG)
        CometChatGroupEvents.removeListener(LISTENERS_TAG)
        CometChat.removeUserListener(LISTENERS_TAG)
    }

    /**
     * Updates a group member in the list.
     *
     * @param groupMember
     * The GroupMember object to update.
     */
    fun updateGroupMember(groupMember: GroupMember) {
        if (groupMemberArrayList.contains(groupMember)) {
            groupMemberArrayList[groupMemberArrayList.indexOf(groupMember)] = groupMember
            updateGroupMember.value = groupMemberArrayList.indexOf(groupMember)
        }
    }

    /**
     * Moves a group member to the top of the list.
     *
     * @param groupMember
     * The GroupMember object to move.
     */
    fun moveToTop(groupMember: GroupMember) {
        if (groupMemberArrayList.contains(groupMember)) {
            val oldIndex = groupMemberArrayList.indexOf(groupMember)
            groupMemberArrayList.remove(groupMember)
            groupMemberArrayList.add(0, groupMember)
            moveToTop.value = oldIndex
        }
    }

    /**
     * Adds a group member to the top of the banned members list.
     *
     * @param groupMember
     * The GroupMember object to add.
     */
    fun addToTop(groupMember: GroupMember?) {
        if (groupMember != null && !groupMemberArrayList.contains(groupMember)) {
            groupMemberArrayList.add(0, groupMember)
            insertAtTop.value = 0
            states.value = checkIsEmpty(groupMemberArrayList)
        }
    }

    /**
     * Removes a group member from the banned members list.
     *
     * @param groupMember
     * The GroupMember object to remove.
     */
    fun removeGroupMember(groupMember: GroupMember) {
        if (groupMemberArrayList.contains(groupMember)) {
            val index = groupMemberArrayList.indexOf(groupMember)
            groupMemberArrayList.remove(groupMember)
            removeGroupMember.value = index
            states.value = checkIsEmpty(groupMemberArrayList)
        }
    }

    /** Fetches banned group members.  */
    fun fetchGroupMember() {
        if (groupMemberArrayList.isEmpty()) states.value = States.LOADING
        if (hasMore) {
            bannedGroupMembersRequest!!.fetchNext(object : CometChat.CallbackListener<List<GroupMember>>() {
                override fun onSuccess(bannedGroupMembers: List<GroupMember>) {
                    hasMore = !bannedGroupMembers.isEmpty()
                    if (hasMore) addList(bannedGroupMembers)
                    states.value = States.LOADED
                    states.value = checkIsEmpty(groupMemberArrayList)
                }

                override fun onError(exception: CometChatException) {
                    cometChatException.value = exception
                    states.value = States.ERROR
                }
            })
        }
    }

    /**
     * Searches for banned group members based on a search keyword.
     *
     * @param search
     * The search keyword.
     */
    fun searchBannedGroupMembers(search: String?) {
        clear()
        hasMore = true
        bannedGroupMembersRequest = if (search != null) searchBannedGroupMembersRequestBuilder!!.setGuid(id).setSearchKeyword(search).build()
        else bannedGroupMembersRequestBuilder!!.build()
        fetchGroupMember()
    }

    /**
     * Adds a list of group members to the banned members list.
     *
     * @param groupMemberList
     * The list of GroupMember objects to add.
     */
    fun addList(groupMemberList: List<GroupMember>) {
        for (groupMember in groupMemberList) {
            if (groupMemberArrayList.contains(groupMember)) {
                val index = groupMemberArrayList.indexOf(groupMember)
                groupMemberArrayList.removeAt(index)
                groupMemberArrayList.add(index, groupMember)
            } else {
                groupMemberArrayList.add(groupMember)
            }
        }
        mutableBannedGroupMembersList.value = groupMemberArrayList
    }

    /**
     * Checks if the banned group members list is empty.
     *
     * @param bannedGroupMembers
     * The list of banned group members.
     * @return The current state indicating whether the list is empty or not.
     */
    private fun checkIsEmpty(bannedGroupMembers: List<GroupMember>): States {
        if (bannedGroupMembers.isEmpty()) return States.EMPTY
        return States.NON_EMPTY
    }

    /** Clears the banned group members list.  */
    fun clear() {
        groupMemberArrayList.clear()
        mutableBannedGroupMembersList.value = groupMemberArrayList
    }

    /**
     * Unbans a group member and updates the dialog state.
     *
     * @param groupMember
     * The GroupMember object to unban.
     */
    fun unBanGroupMember(groupMember: GroupMember) {
        dialogStates.value = DialogState.INITIATED
        if (group == null) return
        Repository.unBanGroupMember(group!!, groupMember, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(group: Group) {
                dialogStates.value = DialogState.SUCCESS
            }

            override fun onError(e: CometChatException) {
                dialogStates.value = DialogState.FAILURE
            }
        })
    }
}
