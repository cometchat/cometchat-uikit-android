package com.cometchat.sampleapp.compose.navigation

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds the users and groups a screen already has, so the next destination can open on them
 * instead of re-fetching by id.
 *
 * Routes carry only ids. `CometChat.getUser` / `getGroup` read only the server, so offline a
 * by-id re-fetch fails and the destination cannot open. A screen puts the objects it holds here
 * before navigating ([hold]); the destination resolves them with [getUser] / [getGroup], which
 * answer at once from the held copy and then refresh it from the server. Online the refresh brings
 * the current scope, block state and member count; offline it fails and the held copy stays. With
 * nothing held (e.g. a deep link) the server alone answers, error included.
 *
 * Cleared on logout and login ([clear]), so the next user never opens on this user's objects.
 */
internal object RouteEntities {

    private val users = ConcurrentHashMap<String, User>()
    private val groups = ConcurrentHashMap<String, Group>()

    /** Puts the objects a screen holds where the next destination resolves them from. */
    fun hold(user: User?, group: Group?) {
        user?.let { users[it.uid] = it }
        group?.let { groups[it.guid] = it }
    }

    /**
     * The user/group a message opens on: the copy already held, else the one the message carries
     * (the peer of a 1:1 message, or the group it was sent to).
     */
    fun holdForMessage(message: BaseMessage, loggedInUid: String?) {
        when (message.receiverType) {
            CometChatConstants.RECEIVER_TYPE_USER -> {
                val peer = if (message.sender?.uid == loggedInUid) message.receiver as? User else message.sender
                if (peer != null && !users.containsKey(peer.uid)) hold(peer, null)
            }
            CometChatConstants.RECEIVER_TYPE_GROUP -> {
                val group = message.receiver as? Group
                if (group != null && !groups.containsKey(group.guid)) hold(null, group)
            }
        }
    }

    /**
     * Resolves [uid]. Held: [listener] gets the held copy at once, then the server's copy if the
     * refresh succeeds (so `onSuccess` can run twice); a failed refresh keeps the held copy. Not
     * held: the server answers [listener], error included.
     */
    fun getUser(uid: String, listener: CometChat.CallbackListener<User>) {
        val held = users[uid]
        if (held != null) listener.onSuccess(held)
        CometChat.getUser(uid, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(fresh: User?) {
                if (fresh == null) return
                users[fresh.uid] = fresh
                listener.onSuccess(fresh)
            }

            override fun onError(e: CometChatException?) {
                // Offline, or the server failed: the held copy stays on screen.
                if (held == null) listener.onError(e)
            }
        })
    }

    /** As [getUser], for a group. */
    fun getGroup(guid: String, listener: CometChat.CallbackListener<Group>) {
        val held = groups[guid]
        if (held != null) listener.onSuccess(held)
        CometChat.getGroup(guid, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(fresh: Group?) {
                if (fresh == null) return
                groups[fresh.guid] = fresh
                listener.onSuccess(fresh)
            }

            override fun onError(e: CometChatException?) {
                if (held == null) listener.onError(e)
            }
        })
    }

    /** Drops every held object. Called on logout and login (a user switch). */
    fun clear() {
        users.clear()
        groups.clear()
    }
}
