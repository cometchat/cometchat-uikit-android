package com.cometchat.sampleapp.compose.ui.messages

import android.util.Log
import androidx.lifecycle.ViewModel
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.SyncOutcome
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatUserEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Messages screen.
 * Handles user block status, group membership, and related operations.
 */
class MessagesViewModel : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    /** Either direction of blocking — neither side can message or call the other. */
    private val _isBlocked = MutableStateFlow(false)
    val isBlocked: StateFlow<Boolean> = _isBlocked.asStateFlow()

    /**
     * Only "I blocked them" — drives the unblock banner and composer, since being blocked by the
     * other user is not something this user can undo.
     */
    private val _isBlockedByMe = MutableStateFlow(false)
    val isBlockedByMe: StateFlow<Boolean> = _isBlockedByMe.asStateFlow()

    private val _isGroupMember = MutableStateFlow(true)
    val isGroupMember: StateFlow<Boolean> = _isGroupMember.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    companion object {
        private const val TAG = "MessagesViewModel"
        private const val SYNC_LISTENER_ID = "MessagesViewModel_SyncListener"
    }

    /**
     * The server's copy of the group this chat opened on. A search hit opens on the group the result
     * carried; [initialize] is keyed on the guid and does not run again, so the fresh copy and its
     * membership come in here. A different group, or one before [initialize], is ignored.
     */
    fun adoptServerGroup(fresh: Group) {
        if (_group.value?.guid != fresh.guid) return
        _group.value = fresh
        _isGroupMember.value = fresh.isJoined
    }

    /**
     * Initialize the ViewModel with user or group.
     */
    fun initialize(user: User?, group: Group?) {
        _user.value = user
        _group.value = group

        user?.let {
            _isBlocked.value = it.isBlockedByMe || it.isHasBlockedMe
            _isBlockedByMe.value = it.isBlockedByMe
            addUserListener()

            // Fetch fresh user data from server to get current blocked status
            CometChat.getUser(it.uid, object : CometChat.CallbackListener<User>() {
                override fun onSuccess(freshUser: User) {
                    _user.value = freshUser
                    _isBlocked.value = freshUser.isBlockedByMe || freshUser.isHasBlockedMe
                    _isBlockedByMe.value = freshUser.isBlockedByMe
                }

                override fun onError(e: CometChatException) {
                    Log.e(TAG, "Failed to fetch fresh user data: ${e.message}")
                }
            })
        }

        group?.let {
            _isGroupMember.value = it.isJoined
            addGroupListener()
        }
    }

    /**
     * Unblock the current user.
     */
    fun unblockUser() {
        val currentUser = _user.value ?: return

        _isLoading.value = true

        CometChat.unblockUsers(
            listOf(currentUser.uid),
            object : CometChat.CallbackListener<HashMap<String, String>>() {
                override fun onSuccess(result: HashMap<String, String>?) {
                    _isLoading.value = false
                    currentUser.isBlockedByMe = false
                    CometChatEvents.emitUserEvent(CometChatUserEvent.UserUnblocked(currentUser))
                    _isBlocked.value = currentUser.isHasBlockedMe
                    _isBlockedByMe.value = false
                    Log.d(TAG, "User unblocked successfully")
                }

                override fun onError(exception: CometChatException?) {
                    _isLoading.value = false
                    _error.value = exception?.message
                    Log.e(TAG, "Failed to unblock user: ${exception?.message}")
                }
            }
        )
    }

    /**
     * Add user listener for real-time updates.
     */
    private fun addUserListener() {
        val listenerID = "MessagesViewModel_UserListener"

        CometChat.addUserListener(listenerID, object : CometChat.UserListener() {
            override fun onUserOnline(user: User?) {
                if (user?.uid == _user.value?.uid) {
                    _user.value = user
                }
            }

            override fun onUserOffline(user: User?) {
                if (user?.uid == _user.value?.uid) {
                    _user.value = user
                }
            }
        })
    }

    /**
     * Add group listener for real-time updates.
     */
    private fun addGroupListener() {
        val listenerID = "MessagesViewModel_GroupListener"

        val groupListener = object : CometChat.GroupListener() {
            override fun onGroupMemberJoined(action: Action, joinedUser: User, joinedGroup: Group) {
                if (joinedGroup.guid == _group.value?.guid) {
                    _group.value = joinedGroup
                    val loggedInUser = CometChat.getLoggedInUser()
                    if (joinedUser.uid == loggedInUser?.uid) {
                        _isGroupMember.value = true
                    }
                }
            }

            override fun onGroupMemberLeft(action: Action, leftUser: User, leftGroup: Group) {
                if (leftGroup.guid == _group.value?.guid) {
                    _group.value = leftGroup
                    val loggedInUser = CometChat.getLoggedInUser()
                    if (leftUser.uid == loggedInUser?.uid) {
                        _isGroupMember.value = false
                    }
                }
            }

            override fun onGroupMemberKicked(action: Action, kickedUser: User, kickedBy: User, kickedFrom: Group) {
                if (kickedFrom.guid == _group.value?.guid) {
                    _group.value = kickedFrom
                    val loggedInUser = CometChat.getLoggedInUser()
                    if (kickedUser.uid == loggedInUser?.uid) {
                        _isGroupMember.value = false
                    }
                }
            }

            override fun onGroupMemberBanned(action: Action, bannedUser: User, bannedBy: User, bannedFrom: Group) {
                if (bannedFrom.guid == _group.value?.guid) {
                    _group.value = bannedFrom
                    val loggedInUser = CometChat.getLoggedInUser()
                    if (bannedUser.uid == loggedInUser?.uid) {
                        _isGroupMember.value = false
                    }
                }
            }
        }
        CometChat.addGroupListener(listenerID, groupListener)

        // Offline reconnect: a kick, ban, leave or join that happened while offline reaches the app
        // only through the sync listener's onMessageSyncCompleted. Replay it through the same handler
        // as the live event. Fires only when offline messaging is on.
        CometChat.addSyncListener(SYNC_LISTENER_ID, object : CometChat.SyncListener() {
            override fun onMessageSyncCompleted(messages: List<BaseMessage>, outcome: SyncOutcome) {
                replaySweptGroupActions(messages, groupListener)
            }
        })
    }

    /** Routes each swept group action for this group to the live [CometChat.GroupListener] callback. */
    private fun replaySweptGroupActions(messages: List<BaseMessage>, listener: CometChat.GroupListener) {
        val guid = _group.value?.guid ?: return
        for (action in messages.filterIsInstance<Action>()) {
            val group = action.actionFor as? Group ?: continue
            if (group.guid != guid) continue
            val on = action.actionOn as? User ?: continue
            val by = action.actionBy as? User ?: on
            when (action.action) {
                CometChatConstants.ActionKeys.ACTION_KICKED -> listener.onGroupMemberKicked(action, on, by, group)
                CometChatConstants.ActionKeys.ACTION_BANNED -> listener.onGroupMemberBanned(action, on, by, group)
                CometChatConstants.ActionKeys.ACTION_LEFT -> listener.onGroupMemberLeft(action, on, group)
                CometChatConstants.ActionKeys.ACTION_JOINED -> listener.onGroupMemberJoined(action, on, group)
            }
        }
    }

    /**
     * Remove listeners when ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        CometChat.removeUserListener("MessagesViewModel_UserListener")
        CometChat.removeGroupListener("MessagesViewModel_GroupListener")
        CometChat.removeSyncListener(SYNC_LISTENER_ID)
    }
}
