package com.cometchat.ai.sampleapp.compose.navigation

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds the agents (users) a screen already has, so the chat screen can open on them instead of
 * re-fetching by uid.
 *
 * Routes carry only the uid. `CometChat.getUser` reads only the server, so offline a by-uid
 * re-fetch fails and the chat cannot open. A screen puts the user it holds here before navigating
 * ([hold]); the chat resolves it with [getUser], which answers at once from the held copy and then
 * refreshes it from the server. Offline the refresh fails and the held copy stays. With nothing
 * held the server alone answers, error included.
 *
 * Cleared on logout and login ([clear]), so the next user never opens on this user's objects.
 */
internal object RouteEntities {

    private val users = ConcurrentHashMap<String, User>()

    /** Puts the user a screen holds where the chat screen resolves it from. */
    fun hold(user: User?) {
        user?.let { users[it.uid] = it }
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

    /** Drops every held user. Called on logout and login (a user switch). */
    fun clear() {
        users.clear()
    }
}
