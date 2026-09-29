package com.cometchat.sampleapp.compose.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.cometchat.calls.core.CallAppSettings
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.sampleapp.compose.utils.AppPreferences
import com.cometchat.uikit.compose.presentation.ongoingcall.ui.CometChatOngoingCallActivity
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.UIKitSettings
import com.cometchat.uikit.core.constants.UIKitConstants.CallWorkFlow
import com.cometchat.uikit.core.events.CometChatCallEvent
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.resources.soundmanager.CometChatSoundManager
import com.cometchat.uikit.core.resources.soundmanager.Sound
import com.cometchat.uikit.core.utils.CallManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Application class for the CometChat Sample App (Jetpack Compose).
 *
 * This class serves as the entry point for the application and provides
 * methods to initialize the CometChat SDK with configurable credentials.
 *
 * ## Key Design Decisions:
 * - SDK initialization is **deferred to the login flow** rather than being
 *   performed in [onCreate]. This allows the app to support credential
 *   switching and ensures initialization happens with valid credentials.
 * - **No Firebase/FCM initialization** - Push notifications are excluded
 *   from this sample app to maintain simplicity and focus on core chat features.
 * - **In-app calling is wired.** After the SDK is initialized, [onSDKInitialized]
 *   initializes the Calls SDK and registers a [CometChat.CallListener] so that an
 *   incoming voice/video call surfaces the [com.cometchat.uikit.compose.presentation.incomingcall.ui.CometChatIncomingCall]
 *   overlay (observed by MainActivity via [incomingCall]). This mirrors master-app-compose's
 *   foreground call chain; killed-state FCM/VoIP push is intentionally out of scope for the sample.
 *
 * @see CometChatUIKit
 * @see UIKitSettings
 */
class SampleApplication : Application() {

    companion object {
        private const val TAG = "SampleApplication"
        private val LISTENER_ID = "AppCallListener_${System.currentTimeMillis()}"

        private var instance: SampleApplication? = null
        private var tempCall: Call? = null

        /** Access the singleton so Compose UI (MainActivity) can observe incoming-call state. */
        fun getInstance(): SampleApplication? = instance
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var soundManager: CometChatSoundManager? = null
    private var currentActivityInstance: Activity? = null

    // Guards against registering the call listeners more than once.
    private var callListenersRegistered = false

    // Incoming call state exposed for Compose UI to observe.
    private val _incomingCall = MutableStateFlow<Call?>(null)
    val incomingCall: StateFlow<Call?> = _incomingCall.asStateFlow()

    /**
     * Called when the application is starting, before any activity, service,
     * or receiver objects have been created.
     *
     * Note: SDK initialization is intentionally NOT performed here.
     * It is deferred to the login flow to support credential configuration
     * and switching between different CometChat app credentials.
     */
    override fun onCreate() {
        super.onCreate()
        instance = this
        soundManager = CometChatSoundManager(this)
        Log.d(TAG, "SampleApplication created")

        // Track the current activity (needed to launch the ongoing-call screen) and re-show a
        // pending incoming call when an activity resumes.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                currentActivityInstance = activity
            }

            override fun onActivityStarted(activity: Activity) {
                currentActivityInstance = activity
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivityInstance = activity
                if (_incomingCall.value == null && tempCall != null) {
                    _incomingCall.value = tempCall
                }
            }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivityInstance === activity) currentActivityInstance = null
            }
        })
    }

    /**
     * Initializes the CometChat SDK with the provided credentials.
     *
     * This method should be called before attempting to log in users.
     * It configures the CometChat UIKit with the specified App ID, Region,
     * and Auth Key, then initializes the underlying SDK.
     *
     * ## Thread Safety:
     * This method can be called from any thread. The callbacks will be
     * invoked on the main thread.
     *
     * ## Error Handling:
     * If initialization fails, the [onError] callback will be invoked with
     * a [CometChatException] containing details about the failure. Common
     * failure reasons include:
     * - Invalid App ID
     * - Invalid Region
     * - Network connectivity issues
     *
     * @param appId The CometChat App ID obtained from the CometChat dashboard.
     *              This uniquely identifies your CometChat application.
     * @param region The region where your CometChat app is hosted (e.g., "us", "eu", "in").
     *               This should match the region selected during app creation.
     * @param authKey The Auth Key for your CometChat app, used for user authentication.
     *                This can be found in the CometChat dashboard under API & Auth Keys.
     * @param onSuccess Callback invoked when SDK initialization completes successfully.
     *                  After this callback, you can proceed to log in users.
     * @param onError Callback invoked when SDK initialization fails.
     *                The [CometChatException] parameter contains error details.
     *
     * @see CometChatUIKit.init
     * @see UIKitSettings
     */
    fun initializeCometChat(
        appId: String,
        region: String,
        authKey: String,
        onSuccess: () -> Unit,
        onError: (CometChatException) -> Unit
    ) {
        Log.d(TAG, "Initializing CometChat SDK with appId: $appId, region: $region")

        // Validate input parameters
        if (appId.isBlank()) {
            val exception = CometChatException(
                "INVALID_APP_ID",
                "App ID cannot be empty",
                "Please provide a valid CometChat App ID"
            )
            Log.e(TAG, "Initialization failed: ${exception.message}")
            onError(exception)
            return
        }

        if (region.isBlank()) {
            val exception = CometChatException(
                "INVALID_REGION",
                "Region cannot be empty",
                "Please provide a valid region (e.g., 'us', 'eu', 'in')"
            )
            Log.e(TAG, "Initialization failed: ${exception.message}")
            onError(exception)
            return
        }

        if (authKey.isBlank()) {
            val exception = CometChatException(
                "INVALID_AUTH_KEY",
                "Auth Key cannot be empty",
                "Please provide a valid CometChat Auth Key"
            )
            Log.e(TAG, "Initialization failed: ${exception.message}")
            onError(exception)
            return
        }

        // Persist credentials so the Calls SDK init (and warm-start paths) can read them.
        AppPreferences(this).saveCredentials(appId = appId, region = region, authKey = authKey)

        // Build UIKit settings with the provided credentials
        val uiKitSettings = UIKitSettings.UIKitSettingsBuilder()
            .setAppId(appId)
            .setRegion(region)
            .setAuthKey(authKey)
            .subscribePresenceForAllUsers()
            .setAutoEstablishSocketConnection(true)
            .setEnableCalling(true)
            .build()

        // Initialize the CometChat UIKit
        CometChatUIKit.init(
            context = this,
            authSettings = uiKitSettings,
            callbackListener = object : CometChat.CallbackListener<String>() {
                override fun onSuccess(result: String?) {
                    Log.d(TAG, "CometChat SDK initialized successfully: $result")
                    // Wire the Calls SDK + incoming-call listener now that the SDK is ready.
                    onSDKInitialized()
                    onSuccess()
                }

                override fun onError(exception: CometChatException?) {
                    val error = exception ?: CometChatException(
                        "UNKNOWN_ERROR",
                        "Unknown initialization error",
                        "An unexpected error occurred during SDK initialization"
                    )
                    Log.e(TAG, "CometChat SDK initialization failed: ${error.message}")
                    onError(error)
                }
            }
        )
    }

    /**
     * Checks if the CometChat SDK has been initialized.
     *
     * This can be used to determine whether [initializeCometChat] needs to
     * be called before attempting to log in users.
     *
     * @return `true` if the SDK is initialized and ready for use, `false` otherwise.
     *
     * @see CometChatUIKit.isSDKInitialized
     */
    fun isSDKInitialized(): Boolean {
        return CometChatUIKit.isSDKInitialized()
    }

    // ─── Calling ────────────────────────────────────────────────────────────────

    /**
     * Registers the Calls SDK and the incoming-call listeners after the CometChat SDK is
     * initialized. Idempotent — repeated calls (credential switch, re-login) are no-ops.
     *
     * Called from [initializeCometChat]'s success callback for the normal app flow, and directly
     * by the E2E harness after it initializes the SDK itself.
     */
    fun onSDKInitialized() {
        if (callListenersRegistered) return
        callListenersRegistered = true
        Log.d(TAG, "onSDKInitialized — registering call listeners")
        initCometChatCalls()
        addCallEventsListener()
    }

    /** Initializes the CometChatCalls SDK, then registers the incoming-call listener. */
    private fun initCometChatCalls() {
        val prefs = AppPreferences(this)
        val appId = prefs.getAppId()
        val region = prefs.getRegion()
        if (appId.isNullOrEmpty() || region.isNullOrEmpty()) {
            Log.e(TAG, "Cannot initialize CometChatCalls: missing credentials")
            return
        }
        val callAppSettings = CallAppSettings.CallAppSettingBuilder()
            .setAppId(appId)
            .setRegion(region)
            .build()
        CometChatCalls.init(this, callAppSettings, object : CometChatCalls.CallbackListener<String>() {
            override fun onSuccess(p0: String?) {
                Log.d(TAG, "CometChatCalls init onSuccess: $p0")
                addCallListener()
            }

            override fun onError(p0: com.cometchat.calls.exceptions.CometChatException?) {
                Log.e(TAG, "CometChatCalls init onError: ${p0?.message}")
            }
        })
    }

    /** Handles incoming calls via WebSocket (in-app, foreground). */
    private fun addCallListener() {
        CometChat.addCallListener(LISTENER_ID, object : CometChat.CallListener() {
            override fun onIncomingCallReceived(call: Call) {
                Log.d(TAG, "onIncomingCallReceived: ${call.sessionId}")
                playSound()
                launchIncomingCallPopup(call)
            }

            override fun onOutgoingCallAccepted(call: Call) {
                Log.d(TAG, "onOutgoingCallAccepted: ${call.sessionId}")
                dismissIncomingCall()
            }

            override fun onOutgoingCallRejected(call: Call) {
                Log.d(TAG, "onOutgoingCallRejected: ${call.sessionId}")
                dismissIncomingCall()
            }

            override fun onIncomingCallCancelled(call: Call) {
                Log.d(TAG, "onIncomingCallCancelled: ${call.sessionId}")
                dismissIncomingCall()
            }

            override fun onCallEndedMessageReceived(call: Call) {
                Log.d(TAG, "onCallEndedMessageReceived: ${call.sessionId}")
                dismissIncomingCall()
            }
        })
    }

    /** Handles call accepted/rejected raised from the incoming-call UI. */
    private fun addCallEventsListener() {
        applicationScope.launch {
            CometChatEvents.callEvents.collect { event ->
                when (event) {
                    is CometChatCallEvent.CallAccepted -> {
                        if (_incomingCall.value != null) {
                            val call = event.call
                            currentActivityInstance?.let { activity ->
                                CometChatOngoingCallActivity.launchOngoingCallActivity(
                                    activity,
                                    call.sessionId,
                                    call.type,
                                    CallWorkFlow.DEFAULT,
                                    null,
                                    null
                                )
                            }
                        }
                        dismissIncomingCall()
                    }

                    is CometChatCallEvent.CallRejected -> dismissIncomingCall()
                    else -> {}
                }
            }
        }
    }

    /** Surfaces the incoming-call overlay, guarding against self-calls and busy state. */
    private fun launchIncomingCallPopup(call: Call) {
        val callInitiator = call.callInitiator
        if (callInitiator is User) {
            val loggedInUser = CometChatUIKit.getLoggedInUser()
            if (loggedInUser != null && loggedInUser.uid.equals(callInitiator.uid, ignoreCase = true)) {
                return
            }
        }
        if (CometChat.getActiveCall() == null && CallManager.getActiveCall() == null) {
            CallManager.setActiveCall(call)
            tempCall = call
            _incomingCall.value = call
        } else {
            rejectCallWithBusyStatus(call)
        }
    }

    /** Dismisses the incoming-call overlay and clears active-call state. */
    fun dismissIncomingCall() {
        if (_incomingCall.value != null) {
            _incomingCall.value = null
            tempCall = null
            CallManager.setActiveCall(null)
        }
        pauseSound()
    }

    /** Rejects an incoming call with busy status when already in a call. */
    private fun rejectCallWithBusyStatus(call: Call) {
        CometChat.rejectCall(
            call.sessionId,
            CometChatConstants.CALL_STATUS_BUSY,
            object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(rejectedCall: Call) {
                    Log.d(TAG, "Call rejected with busy status")
                }

                override fun onError(e: CometChatException) {
                    Log.e(TAG, "Failed to reject call: ${e.message}")
                }
            }
        )
    }

    private fun playSound() {
        soundManager?.play(Sound.INCOMING_CALL, 0)
    }

    private fun pauseSound() {
        soundManager?.pauseSilently()
    }
}
