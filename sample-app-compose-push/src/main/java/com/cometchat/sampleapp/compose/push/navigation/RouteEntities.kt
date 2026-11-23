package com.cometchat.sampleapp.compose.push.navigation

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User

/**
 * How a destination turns its route's user or group id back into the object (ENG-41524,
 * ENG-41553):
 * open at once on the copy the calling screen held (put in [EntityCache] before navigating), then
 * refresh it from the server. Online the refresh brings the current scope, block state and member
 * count; offline (`CometChat.getUser` / `getGroup` read only the server) it fails and the held copy
 * stays, so the destination opens instead of popping back to the chat list. With nothing held
 * (a notification deep link) the server alone answers, and its error reaches the route as before.
 */
internal object RouteEntities {

    /**
     * The held user for [uid] answers [listener] at once and the server's copy then goes to
     * [onRefreshed]; a failed refresh keeps the held copy. With none held, the server answers
     * [listener], error included.
     */
    fun getUser(
        uid: String,
        listener: CometChat.CallbackListener<User>,
        onRefreshed: (User) -> Unit,
        held: (String) -> User? = EntityCache::getUser,
        fetch: (String, CometChat.CallbackListener<User>) -> Unit = { id, l -> CometChat.getUser(id, l) }
    ) {
        val user = held(uid)
        if (user == null) {
            fetch(uid, listener)
            return
        }
        listener.onSuccess(user)
        refreshUser(uid, onRefreshed, fetch)
    }

    /**
     * The held group for [guid] answers [listener] at once and the server's copy then goes to
     * [onRefreshed]; a failed refresh keeps the held copy. With none held, the server answers
     * [listener], error included.
     */
    fun getGroup(
        guid: String,
        listener: CometChat.CallbackListener<Group>,
        onRefreshed: (Group) -> Unit,
        held: (String) -> Group? = EntityCache::getGroup,
        fetch: (String, CometChat.CallbackListener<Group>) -> Unit = { id, l -> CometChat.getGroup(id, l) }
    ) {
        val group = held(guid)
        if (group == null) {
            fetch(guid, listener)
            return
        }
        listener.onSuccess(group)
        refreshGroup(guid, onRefreshed, fetch)
    }

    /** The server's copy of [uid] to [onRefreshed]; on error nothing happens, so the held copy stays. */
    fun refreshUser(
        uid: String,
        onRefreshed: (User) -> Unit,
        fetch: (String, CometChat.CallbackListener<User>) -> Unit = { id, l -> CometChat.getUser(id, l) }
    ) = fetch(uid, refresh(onRefreshed))

    /** The server's copy of [guid] to [onRefreshed]; on error nothing happens, so the held copy stays. */
    fun refreshGroup(
        guid: String,
        onRefreshed: (Group) -> Unit,
        fetch: (String, CometChat.CallbackListener<Group>) -> Unit = { id, l -> CometChat.getGroup(id, l) }
    ) = fetch(guid, refresh(onRefreshed))

    /**
     * The user/group a search hit should open on (ENG-41559 item 4): the copy already held, else the
     * one the message carries (the peer of a 1:1 message, or the group it was sent to). Held for the
     * destination, which then refreshes it from the server.
     */
    fun holdForMessage(message: BaseMessage, loggedInUid: String?) {
        when (message.receiverType) {
            CometChatConstants.RECEIVER_TYPE_USER -> {
                val peer = if (message.sender?.uid == loggedInUid) message.receiver as? User else message.sender
                if (peer != null && EntityCache.getUser(peer.uid) == null) hold(peer, null)
            }
            CometChatConstants.RECEIVER_TYPE_GROUP -> {
                val group = message.receiver as? Group
                if (group != null && EntityCache.getGroup(group.guid) == null) hold(null, group)
            }
        }
    }

    /** Puts the objects a screen holds where the next destination resolves them from. */
    fun hold(user: User?, group: Group?) {
        user?.let { EntityCache.putUser(it.uid, it) }
        group?.let { EntityCache.putGroup(it.guid, it) }
    }

    private fun <T> refresh(onRefreshed: (T) -> Unit) = object : CometChat.CallbackListener<T>() {
        override fun onSuccess(fresh: T) {
            if (fresh != null) onRefreshed(fresh)
        }

        override fun onError(e: CometChatException?) {
            // Offline, or the server failed: the held copy stays on screen.
        }
    }
}
