package com.cometchat.sampleapp.kotlin.push.appflow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.sampleapp.kotlin.push.R
import com.cometchat.sampleapp.kotlin.push.databinding.ActivitySearchBinding
import com.google.gson.Gson
import org.json.JSONObject

/**
 * Activity for global and contextual search across conversations and messages.
 *
 * Integrates the CometChatSearch component from chatuikit-kotlin to provide
 * search functionality across conversations and messages.
 *
 * ## Features:
 * - Global search across all conversations and messages
 * - Contextual search within a specific user or group conversation
 * - Filter chips for Photos, Videos, Documents, Links, Audio, Groups, Unread
 * - Debounced search with pagination support
 *
 * ## Usage:
 * ```kotlin
 * // Global search (from conversations)
 * val intent = Intent(context, SearchActivity::class.java)
 * startActivity(intent)
 *
 * // Contextual search (from messages)
 * val intent = Intent(context, SearchActivity::class.java)
 * intent.putExtra("user", user.toJson().toString())
 * startActivityForResult(intent, REQUEST_CODE)
 * ```
 */
class SearchActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SearchActivity"
    }

    private lateinit var binding: ActivitySearchBinding

    private var userId: String? = null
    private var groupId: String? = null
    private var user: User? = null
    private var group: Group? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractIntentData()
        applyWindowInsets()
        setupSearchComponent()
    }

    /**
     * Extracts user/group data from intent extras.
     * Supports both JSON string extras (from MessagesActivity) and direct ID extras.
     */
    private fun extractIntentData() {
        // Try JSON string extras first (passed from MessagesActivity)
        val userJson = intent.getStringExtra(getString(R.string.app_user))
        val groupJson = intent.getStringExtra(getString(R.string.app_group))

        try {
            if (userJson != null) {
                user = User.fromJson(JSONObject(userJson).toString())
                userId = user?.uid
            }
            if (groupJson != null) {
                group = Gson().fromJson(groupJson, Group::class.java)
                groupId = group?.guid
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing intent data: ${e.message}")
        }

        Log.d(TAG, "extractIntentData: userId=$userId, groupId=$groupId")
    }

    /**
     * Applies system window insets padding to avoid overlap with system bars.
     */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Sets up the CometChatSearch component with configuration and callbacks.
     */
    private fun setupSearchComponent() {
        binding.searchComponent.apply {
            // Configure contextual search if UID or GUID is provided
            userId?.let { setUid(it) }
            groupId?.let { setGuid(it) }

            // Back button
            setOnBackPress {
                finish()
            }

            // Conversation click — navigate to messages
            setOnConversationClick { conversation ->
                navigateToMessages(conversation)
            }

            // Message click — navigate to message context
            setOnMessageClick { message ->
                navigateToMessageContext(message)
            }

            // Error callback
            setOnError { exception ->
                Log.e(TAG, "Search error: ${exception.message}")
            }
        }
    }

    /**
     * Navigates to MessagesActivity for the selected conversation.
     */
    private fun navigateToMessages(conversation: Conversation) {
        when (conversation.conversationType) {
            CometChatConstants.CONVERSATION_TYPE_USER -> {
                val conversationUser = conversation.conversationWith as? User
                conversationUser?.let {
                    navigateToChat(it, null, 0)
                }
            }
            CometChatConstants.CONVERSATION_TYPE_GROUP -> {
                val conversationGroup = conversation.conversationWith as? Group
                conversationGroup?.let {
                    navigateToChat(null, it, 0)
                }
            }
        }
    }

    /**
     * Handles message click — fetches User/Group, checks for parent message (thread),
     * then navigates to the appropriate screen.
     */
    private fun navigateToMessageContext(message: BaseMessage) {
        // A hit in the chat this search was opened from: use the user/group that chat passed in.
        // CometChat.getUser/getGroup read only the server, so offline the re-fetch below fails
        // and the tap does nothing (ENG-41524/41553 pattern).
        SearchHitChat.held(message, CometChatUIKit.getLoggedInUser()?.uid, user, group)?.let { (u, g) ->
            fetchParentAndNavigate(u, g, message)
            return
        }
        // Any other hit (a global search): the user/group the result itself carries. Not connected,
        // it opens on that copy at once; connected, the server's copy is used as before, and the
        // carried copy stands in if that lookup fails.
        val carried = SearchHitChat.carried(message, CometChatUIKit.getLoggedInUser()?.uid)
        if (carried != null && !SearchHitChat.isConnected()) {
            fetchParentAndNavigate(carried.first, carried.second, message)
            return
        }
        when (message.receiverType) {
            CometChatConstants.RECEIVER_TYPE_USER -> {
                val uid = if (message.sender.uid == CometChatUIKit.getLoggedInUser()?.uid) {
                    message.receiverUid
                } else {
                    message.sender.uid
                }
                CometChat.getUser(uid, object : CometChat.CallbackListener<User>() {
                    override fun onSuccess(fetchedUser: User) {
                        fetchParentAndNavigate(fetchedUser, null, message)
                    }
                    override fun onError(e: CometChatException) {
                        Log.e(TAG, "getUser error: ${e.message}")
                        carried?.let { fetchParentAndNavigate(it.first, it.second, message) }
                    }
                })
            }
            CometChatConstants.RECEIVER_TYPE_GROUP -> {
                CometChat.getGroup(message.receiverUid, object : CometChat.CallbackListener<Group>() {
                    override fun onSuccess(fetchedGroup: Group) {
                        fetchParentAndNavigate(null, fetchedGroup, message)
                    }
                    override fun onError(e: CometChatException) {
                        Log.e(TAG, "getGroup error: ${e.message}")
                        carried?.let { fetchParentAndNavigate(it.first, it.second, message) }
                    }
                })
            }
        }
    }

    /**
     * Fetches the parent message if the tapped message is a thread reply,
     * then navigates to the appropriate screen.
     */
    private fun fetchParentAndNavigate(user: User?, group: Group?, message: BaseMessage) {
        val parentMessageId = message.parentMessageId
        if (parentMessageId > 0) {
            // Two-phase read (BEH-READ-35): navigate on whichever phase lands first — the cached
            // parent when there is one, else the server's — and only once. A later phase or an
            // offline error after the cached phase is ignored.
            CometChat.getMessageDetails(parentMessageId, object : CometChat.MessageDetailsListener() {
                private var navigated = false

                override fun onCachedMessage(parentMessage: BaseMessage?) {
                    parentMessage?.let { toThread(it) }
                }
                override fun onServerMessage(parentMessage: BaseMessage?) {
                    // A server phase without a parent ends like an error: open the chat instead.
                    if (parentMessage != null) {
                        toThread(parentMessage)
                    } else {
                        onError(CometChatException("ERR_NO_VALUE", "The server returned no message"))
                    }
                }
                private fun toThread(parentMessage: BaseMessage) {
                    if (navigated) return
                    navigated = true
                    navigateToThread(parentMessage, user, group, message.id.toLong())
                }
                override fun onError(e: CometChatException) {
                    if (navigated) return
                    navigated = true
                    // Parent fetch failed — navigate to messages without thread
                    navigateToChat(user, group, message.id.toLong())
                }
            })
        } else {
            navigateToChat(user, group, message.id.toLong())
        }
    }

    /**
     * Navigates to ThreadMessageActivity for thread replies.
     */
    private fun navigateToThread(parentMessage: BaseMessage, user: User?, group: Group?, goToMessageId: Long) {
        // Check if navigating to a different chat than the current context
        if (isNavigatingToDifferentChat(user, group)) {
            val resultIntent = Intent().apply {
                putExtra("navigateToDifferentChat", true)
                user?.let { putExtra(getString(R.string.app_user), it.toJson().toString()) }
                group?.let { putExtra(getString(R.string.app_group), Gson().toJson(it)) }
                putExtra(getString(R.string.app_base_message), parentMessage.rawMessage.toString())
                if (goToMessageId > 0) {
                    putExtra("goToMessageId", goToMessageId)
                }
            }
            setResult(RESULT_OK, resultIntent)
        } else {
            val intent = Intent(this, ThreadMessageActivity::class.java).apply {
                putExtra(ThreadMessageActivity.EXTRA_PARENT_MESSAGE, parentMessage)
                putExtra("rawJson", parentMessage.rawMessage.toString())
                user?.let { putExtra(getString(R.string.app_user), it.toJson().toString()) }
                group?.let { putExtra(getString(R.string.app_group), Gson().toJson(it)) }
                if (goToMessageId > 0) {
                    // Pass the tapped reply's id — not the parent message — so the
                    // thread view scrolls to the actual search result
                    putExtra("goToMessageId", goToMessageId)
                }
            }
            startActivity(intent)
        }
        finish()
    }

    /**
     * Navigates to MessagesActivity with goToMessageId to scroll to the tapped message.
     */
    private fun navigateToChat(user: User?, group: Group?, goToMessageId: Long) {
        // Global search (no context) — start a new MessagesActivity directly
        if (this.user == null && this.group == null) {
            val intent = Intent(this, MessagesActivity::class.java).apply {
                user?.let { putExtra(getString(R.string.app_user), it.toJson().toString()) }
                group?.let { putExtra(getString(R.string.app_group), Gson().toJson(it)) }
                if (goToMessageId > 0) {
                    putExtra("goToMessageId", goToMessageId)
                }
            }
            startActivity(intent)
            finish()
            return
        }

        val isDifferentChat = isNavigatingToDifferentChat(user, group)
        if (isDifferentChat) {
            val resultIntent = Intent().apply {
                putExtra("navigateToDifferentChat", true)
                user?.let { putExtra(getString(R.string.app_user), it.toJson().toString()) }
                group?.let { putExtra(getString(R.string.app_group), Gson().toJson(it)) }
                if (goToMessageId > 0) {
                    putExtra("goToMessageId", goToMessageId)
                }
            }
            setResult(RESULT_OK, resultIntent)
        } else {
            // Same chat context — return goToMessageId to the calling activity
            val resultIntent = Intent().apply {
                if (goToMessageId > 0) {
                    putExtra("goToMessageId", goToMessageId)
                }
            }
            setResult(RESULT_OK, resultIntent)
        }
        finish()
    }

    /**
     * Checks if the target user/group is different from the current search context.
     * When launched from MessagesActivity with a user/group context, clicking a result
     * from a different conversation should return a result to the caller.
     */
    private fun isNavigatingToDifferentChat(targetUser: User?, targetGroup: Group?): Boolean {
        if (this.user == null && this.group == null) return false

        return when {
            targetUser != null && this.user != null -> targetUser.uid != this.user!!.uid
            targetGroup != null && this.group != null -> targetGroup.guid != this.group!!.guid
            else -> true
        }
    }
}

/**
 * The chat a search was opened in, when a message hit belongs to it (ENG-41524/41553): the user or
 * group the chat passed to the search, so the hit opens without a server-only re-fetch. Null for a
 * global search or a hit in another chat, which keep resolving through the server.
 */
internal object SearchHitChat {
    fun held(message: BaseMessage, loggedInUid: String?, user: User?, group: Group?): Pair<User?, Group?>? =
        when (message.receiverType) {
            CometChatConstants.RECEIVER_TYPE_USER -> {
                val peer = if (message.sender?.uid == loggedInUid) message.receiverUid else message.sender?.uid
                user?.takeIf { peer != null && it.uid == peer }?.let { Pair(it, null) }
            }
            CometChatConstants.RECEIVER_TYPE_GROUP ->
                group?.takeIf { it.guid == message.receiverUid }?.let { Pair(null, it) }
            else -> null
        }

    /**
     * The user/group a message hit carries: the peer of a 1:1 message (the receiver when the
     * logged-in user sent it, else the sender) or the group it was sent to. Null when absent.
     */
    fun carried(message: BaseMessage, loggedInUid: String?): Pair<User?, Group?>? =
        when (message.receiverType) {
            CometChatConstants.RECEIVER_TYPE_USER -> {
                val peer = if (message.sender?.uid == loggedInUid) message.receiver as? User else message.sender
                peer?.let { Pair(it, null) }
            }
            CometChatConstants.RECEIVER_TYPE_GROUP -> (message.receiver as? Group)?.let { Pair(null, it) }
            else -> null
        }

    fun isConnected(): Boolean =
        runCatching { CometChat.getConnectionStatus() == CometChatConstants.WS_STATE_CONNECTED }.getOrDefault(false)
}
