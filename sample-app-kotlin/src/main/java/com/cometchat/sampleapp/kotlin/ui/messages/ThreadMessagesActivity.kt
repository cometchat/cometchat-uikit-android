package com.cometchat.sampleapp.kotlin.ui.messages

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.google.gson.Gson
import com.cometchat.sampleapp.kotlin.databinding.ActivityThreadMessagesBinding
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatThreadEvent
import com.cometchat.uikit.core.utils.CometChatThreadSubscription
import kotlinx.coroutines.launch

/**
 * Activity for displaying thread replies to a message.
 *
 * This activity displays thread replies using CometChat UI Kit components:
 * - CometChatThreadHeader: Shows the parent message
 * - CometChatMessageList: Displays thread replies
 * - CometChatMessageComposer: Allows composing thread replies
 *
 * ## Features:
 * - Display parent message context
 * - Real-time thread reply updates
 * - Support for all message types in replies
 *
 * ## Usage:
 * ```kotlin
 * ThreadMessagesActivity.start(context, parentMessage)
 * ```
 *
 * @see com.cometchat.uikit.kotlin.presentation.threadheader.ui.CometChatThreadHeader
 * @see com.cometchat.uikit.kotlin.presentation.messagelist.ui.CometChatMessageList
 * @see com.cometchat.uikit.kotlin.presentation.messagecomposer.ui.CometChatMessageComposer
 *
 * Validates: Requirements 6.9
 */
class ThreadMessagesActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_PARENT_MESSAGE_ID = "extra_parent_message_id"
        private const val EXTRA_RECEIVER_ID = "extra_receiver_id"
        private const val EXTRA_RECEIVER_TYPE = "extra_receiver_type"
        private const val EXTRA_GO_TO_MESSAGE_ID = "extra_go_to_message_id"
        // The parent message and the user/group objects themselves, so the thread opens without a
        // server lookup (offline too).
        private const val EXTRA_PARENT_MESSAGE = "extra_parent_message"
        private const val EXTRA_USER_JSON = "extra_user_json"
        private const val EXTRA_GROUP_JSON = "extra_group_json"

        /**
         * Starts ThreadMessagesActivity with a parent message.
         *
         * @param context The context to start the activity from
         * @param parentMessage The parent message to show thread for
         * @param goToMessageId Optional message ID to scroll to within the thread
         * @param user The 1:1 chat's user, when the caller holds it
         * @param group The group, when the caller holds it
         */
        fun start(
            context: Context,
            parentMessage: BaseMessage,
            goToMessageId: Long = 0,
            user: User? = null,
            group: Group? = null
        ) {
            val intent = Intent(context, ThreadMessagesActivity::class.java).apply {
                putExtra(EXTRA_PARENT_MESSAGE, parentMessage)
                user?.let { putExtra(EXTRA_USER_JSON, it.toJson().toString()) }
                group?.let { putExtra(EXTRA_GROUP_JSON, Gson().toJson(it)) }
                putExtra(EXTRA_PARENT_MESSAGE_ID, parentMessage.id.toLong())
                putExtra(EXTRA_RECEIVER_ID, parentMessage.receiverUid)
                putExtra(EXTRA_RECEIVER_TYPE, parentMessage.receiverType)
                if (goToMessageId > 0) putExtra(EXTRA_GO_TO_MESSAGE_ID, goToMessageId)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityThreadMessagesBinding
    private var parentMessageId: Long = -1
    private var receiverId: String? = null
    private var receiverType: String? = null
    private var goToMessageId: Long = 0
    private var user: User? = null
    private var group: Group? = null
    private var parentMessage: BaseMessage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityThreadMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        extractIntentData()
        setupToolbar()
        loadData()
    }

    /**
     * Applies system window insets padding to avoid overlap with system bars.
     */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Extracts data from intent extras.
     */
    private fun extractIntentData() {
        parentMessageId = intent.getLongExtra(EXTRA_PARENT_MESSAGE_ID, -1)
        receiverId = intent.getStringExtra(EXTRA_RECEIVER_ID)
        receiverType = intent.getStringExtra(EXTRA_RECEIVER_TYPE)
        goToMessageId = intent.getLongExtra(EXTRA_GO_TO_MESSAGE_ID, 0)
    }

    /**
     * Sets up the toolbar with back navigation.
     */
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * Loads the parent message and receiver data.
     */
    private fun loadData() {
        if (parentMessageId == -1L || receiverId == null || receiverType == null) {
            finish()
            return
        }

        // The user/group the caller passed, when there is one: the thread opens on it at once, offline
        // too (CometChat.getUser/getGroup read only the server). The id lookup is the fallback.
        val passedUser = intent.getStringExtra(EXTRA_USER_JSON)?.let { runCatching { User.fromJson(it) }.getOrNull() }
        val passedGroup = intent.getStringExtra(EXTRA_GROUP_JSON)?.let { runCatching { Gson().fromJson(it, Group::class.java) }.getOrNull() }
        if (passedUser != null || passedGroup != null) {
            user = passedUser
            group = passedGroup
            loadParentMessage()
            return
        }

        // Load receiver (user or group)
        when (receiverType) {
            CometChatConstants.RECEIVER_TYPE_USER -> loadUser(receiverId!!)
            CometChatConstants.RECEIVER_TYPE_GROUP -> loadGroup(receiverId!!)
            else -> finish()
        }
    }

    /**
     * Loads user data from CometChat.
     */
    private fun loadUser(userId: String) {
        CometChat.getUser(userId, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(fetchedUser: User) {
                user = fetchedUser
                loadParentMessage()
            }

            override fun onError(e: CometChatException) {
                finish()
            }
        })
    }

    /**
     * Loads group data from CometChat.
     */
    private fun loadGroup(groupId: String) {
        CometChat.getGroup(groupId, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(fetchedGroup: Group) {
                group = fetchedGroup
                loadParentMessage()
            }

            override fun onError(e: CometChatException) {
                finish()
            }
        })
    }

    /**
     * Loads the parent message from CometChat.
     */
    private fun loadParentMessage() {
        // The parent the caller passed opens the thread as it was shown; else it is read below.
        val passedParent = IntentCompat.getParcelableExtra(intent, EXTRA_PARENT_MESSAGE, BaseMessage::class.java)
        if (passedParent != null) {
            parentMessage = passedParent
            setupThreadUI()
            return
        }
        // Two-phase read (BEH-READ-35): open the thread from whichever phase lands first — the cached
        // parent when there is one, else the server's — and only once. A later phase or an offline
        // error after the cached phase is ignored.
        CometChat.getMessageDetails(parentMessageId, object : CometChat.MessageDetailsListener() {
            private var opened = false

            override fun onCachedMessage(message: BaseMessage?) {
                message?.let { openThread(it) }
            }

            override fun onServerMessage(message: BaseMessage?) {
                if (message != null) {
                    openThread(message)
                } else {
                    onError(CometChatException("ERR_NO_VALUE", "The server returned no message"))
                }
            }

            private fun openThread(message: BaseMessage) {
                if (opened) return
                opened = true
                if (isFinishing || isDestroyed) return
                parentMessage = message
                setupThreadUI()
            }

            override fun onError(e: CometChatException) {
                if (opened) return
                finish()
            }
        })
    }

    /**
     * Sets up the thread UI components after data is loaded.
     */
    private fun setupThreadUI() {
        setupThreadHeader()
        setupMessageList()
        setupMessageComposer()
        parentMessage?.let { setupThreadSubscriptionBell(it) }
    }

    /**
     * Sets up the thread header component.
     */
    private fun setupThreadHeader() {
        binding.threadHeader.apply {
            parentMessage?.let { setParentMessage(it) }
            // Thread subscription bell lives in the title bar (Figma / Flutter parity); hide the
            // kit header's own control so only one bell shows.
            setThreadSubscriptionVisibility(View.GONE)
        }
    }

    /**
     * Sets up the message list component for thread replies.
     */
    private fun setupMessageList() {
        binding.messageList.apply {
            // Set the full parent message for thread context (must be before setUser/setGroup)
            parentMessage?.let { setParentMessage(it) }

            // Navigate to specific message if provided (e.g., from search)
            if (goToMessageId > 0) {
                gotoMessage(goToMessageId)
            }

            // Set user or group
            user?.let { setUser(it) }
            group?.let { setGroup(it) }

            // Enable real-time updates
            setScrollToBottomOnNewMessage(true)
        }
    }

    /**
     * Sets up the message composer component for thread replies.
     */
    private fun setupMessageComposer() {
        binding.messageComposer.apply {
            // Set user or group
            user?.let { setUser(it) }
            group?.let { setGroup(it) }

            // Set parent message ID for thread context
            setParentMessageId(parentMessageId)

            // Enable all attachment types. The poll and collaborative options default to
            // hidden, so they have to be turned on explicitly -- setHideAttachmentButton
            // only controls the button itself.
            setHideAttachmentButton(false)
            setHideVoiceRecordingButton(false)
            setPollOptionVisibility(View.VISIBLE)
            setCollaborativeDocumentOptionVisibility(View.VISIBLE)
            setCollaborativeWhiteboardOptionVisibility(View.VISIBLE)

            // Rich-text formatting, matching this app's MessagesActivity.
            setEnableRichTextFormatting(true)
            setRichTextToolbarVisibility(View.VISIBLE)
        }
    }

    /**
     * Wires the thread-subscription (mute/unmute) bell in the title bar — the Figma /
     * cross-platform (Flutter) placement. Renders only when the feature gate is on and the
     * thread has a valid root. Optimistic flip on tap with a single in-flight guard; reverts
     * with a toast on error; stays in sync with changes from any surface via the kit event bus.
     */
    private fun setupThreadSubscriptionBell(parentMessage: BaseMessage) {
        val bell = binding.ivThreadSubscription
        val rootId = parentMessage.id
        if (!CometChatThreadSubscription.isAvailableForThread(parentMessage)) {
            bell.visibility = View.GONE
            return
        }
        bell.visibility = View.VISIBLE
        // State is read off the parent message — the server's per-viewer flag — never from a cache.
        renderThreadSubscriptionBell(parentMessage.isThreadSubscribed())

        bell.setOnClickListener {
            // The controller owns the debounce, in-flight lock, optimistic publish and revert;
            // the optimistic flip reaches this bell through the bus collector below.
            CometChatThreadSubscription.toggle(parentMessage, parentMessage.isThreadSubscribed()) { result ->
                val toast = when (result) {
                    is CometChatThreadSubscription.ToggleResult.Success -> getString(
                        if (result.subscribed) com.cometchat.uikit.kotlin.R.string.cometchat_thread_subscribed_toast
                        else com.cometchat.uikit.kotlin.R.string.cometchat_thread_unsubscribed_toast
                    )

                    is CometChatThreadSubscription.ToggleResult.Failure ->
                        getString(com.cometchat.uikit.kotlin.R.string.cometchat_thread_subscription_failed)
                }
                Toast.makeText(this@ThreadMessagesActivity, toast, Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            CometChatEvents.threadEvents.collect { event ->
                if (event is CometChatThreadEvent.SubscriptionChanged &&
                    event.parentMessageId == rootId
                ) {
                    // Stamp the held parent too, so a direct read stays coherent.
                    parentMessage.setThreadSubscribed(event.subscribed)
                    renderThreadSubscriptionBell(event.subscribed)
                }
            }
        }
    }

    private fun renderThreadSubscriptionBell(subscribed: Boolean) {
        binding.ivThreadSubscription.setImageResource(
            if (subscribed) com.cometchat.uikit.core.R.drawable.cometchat_ic_notifications
            else com.cometchat.uikit.core.R.drawable.cometchat_ic_notifications_off
        )
        binding.ivThreadSubscription.contentDescription = getString(
            if (subscribed) com.cometchat.uikit.kotlin.R.string.cometchat_thread_mute
            else com.cometchat.uikit.kotlin.R.string.cometchat_thread_unmute
        )
    }
}
