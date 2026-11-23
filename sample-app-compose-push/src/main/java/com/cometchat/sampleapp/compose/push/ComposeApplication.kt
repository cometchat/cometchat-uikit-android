package com.cometchat.sampleapp.compose.push

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.pushnotification.CometChatPushNotifications
import com.cometchat.pushnotification.PNConfiguration
import com.cometchat.sampleapp.compose.push.calls.toCometChatPNIncomingCallTheme
import com.cometchat.uikit.compose.theme.incomingCallTheme
import com.cometchat.sampleapp.compose.push.ui.darkScheme
import com.cometchat.sampleapp.compose.push.ui.lightScheme
import com.cometchat.pushnotification.listeners.NotificationTapListener
import com.cometchat.sampleapp.compose.push.calls.CallAnswerActivity
import com.cometchat.sampleapp.compose.push.fcm.AppFCMService
import com.cometchat.sampleapp.compose.push.utils.AppConstants
import com.google.firebase.FirebaseApp
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.UIKitSettings
import com.cometchat.sampleapp.compose.push.shared.AppCredentials
import com.cometchat.sampleapp.compose.push.shared.AppPreferences
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Application class for managing call notifications and handling CometChat events.
 * Exposes incoming call state via StateFlow for Compose UI to observe.
 *
 * SDK initialization is NOT performed here - it is handled by SplashScreen or AppCredentialsScreen.
 * Call listeners are registered via onSDKInitialized() callback after SDK is initialized elsewhere.
 */
class ComposeApplication : Application() {
    private val TAG: String = "ComposeApplication"
    private var currentActivityInstance: Activity? = null
    private val isConnectedToWebSockets = AtomicBoolean(false)

    // Track if SDK has been initialized and listeners registered
    private var isSDKInitialized = false

    
    companion object {
        
        // Singleton instance for accessing from Compose
        private var instance: ComposeApplication? = null

        private var isAppInForeground: Boolean = false
        var currentActivity: Activity? = null

        fun getInstance(): ComposeApplication? = instance

        fun isAppInForeground(): Boolean = isAppInForeground
    }

    override fun onCreate() {
        super.onCreate()
        instance = this


        // ── CometChat Push Notifications ──
        FirebaseApp.initializeApp(this)
        setupPushNotificationListeners()
        // Ensure the CometChat SDK (and then PN/Calls) is initialized here in Application.onCreate.
        // Required for KILLED-STATE pushes: FCM starts the process and Application.onCreate runs
        // before AppFCMService.onMessageReceived, but no Splash/UI runs to initialize the SDK.
        ensureSdkInitializedForPush()

        // Register activity lifecycle callbacks to track current activity and foreground state
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var isActivityChangingConfigurations = false
            private var activityReferences = 0

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                currentActivityInstance = activity
                currentActivity = activity
            }
            override fun onActivityStarted(activity: Activity) {
                currentActivityInstance = activity
                currentActivity = activity

                // Connect to WebSocket when app comes to foreground (only if SDK is initialized)
                if (isSDKInitialized && isConnectedToWebSockets.compareAndSet(false, true)) {
                    CometChat.connect(object : CometChat.CallbackListener<String?>() {
                        override fun onSuccess(s: String?) {
                            isConnectedToWebSockets.set(true)
                        }
                        override fun onError(e: CometChatException) {
                            isConnectedToWebSockets.set(false)
                        }
                    })
                }

                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    isAppInForeground = true
                    Log.d(TAG, "App is now in FOREGROUND")
                }
            }
            override fun onActivityResumed(activity: Activity) {
                currentActivityInstance = activity
                currentActivity = activity
                // Re-show incoming call UI if there's a pending call
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    isAppInForeground = false
                    Log.d(TAG, "App is now in BACKGROUND")

                    // Disconnect WebSocket when app goes to background (only if SDK is initialized)
                    if (isSDKInitialized) {
                        CometChat.disconnect(object : CometChat.CallbackListener<String?>() {
                            override fun onSuccess(s: String?) {
                                isConnectedToWebSockets.set(false)
                            }
                            override fun onError(e: CometChatException) {}
                        })
                    }
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivityInstance === activity) {
                    currentActivityInstance = null
                    currentActivity = null
                }
            }
        })
    }

    /**
     * Called by SplashScreen or AppCredentialsScreen after SDK initialization succeeds.
     * Initializes push notifications; the Calls SDK is initialised by CometChatUIKit.init.
     *
     * This method is idempotent - calling it multiple times has no effect after the first call.
     */
    fun onSDKInitialized() {
        if (!isSDKInitialized) {
            isSDKInitialized = true
            Log.d(TAG, "onSDKInitialized called, registering call listeners")

            // The Calls SDK is initialised by CometChatUIKit.init (setEnableCalling, with the
            // same hosts). A second init here was redundant, and the Calls SDK keeps the
            // hosts of its first init.


            // Initialize Push Notifications now that credentials are available
            initPushNotifications()
        } else {
            Log.d(TAG, "onSDKInitialized called but already initialized, skipping")
        }
    }

    /**
     * Initializes the CometChat Push Notifications SDK using credentials from SharedPreferences.
     * Safe to call multiple times; no-ops when credentials are not yet available (first launch
     * before AppCredentials are entered). VoIP is enabled to match the in-app Calls setup.
     */
    /**
     * Initializes the CometChat SDK from persisted credentials when it hasn't been initialized yet
     * (e.g. a killed-state push that started the process with no Splash/UI). On success it runs
     * onSDKInitialized(), which initializes Calls and Push Notifications. When the SDK is already
     * initialized (warm start), it goes straight to onSDKInitialized().
     */
    private fun ensureSdkInitializedForPush() {
        if (CometChatUIKit.isSDKInitialized()) {
            onSDKInitialized()
            return
        }
        val appId = AppPreferences.getAppId(this)
        val region = AppPreferences.getRegion(this)
        val authKey = AppPreferences.getAuthKey(this)
        if (appId.isNullOrEmpty() || region.isNullOrEmpty() || authKey.isNullOrEmpty()) {
            Log.d(TAG, "No stored credentials — deferring SDK init to the onboarding flow")
            return
        }
        // NOTE: keep these settings identical to SplashViewModel.initUIKit() — in particular
        // setEnableCalling(true), or the UIKit will hide call buttons when the SDK is initialized here first.
        val settings = UIKitSettings.UIKitSettingsBuilder()
            .setAutoEstablishSocketConnection(true)
            .setAppId(appId)
            .setRegion(region)
            .setAuthKey(authKey)
            .subscribePresenceForAllUsers()
            .setEnableCalling(true)
            // Offline + encryption, always on (D86, D92).
            .setOfflineConfig(AppPreferences.offlineConfig())
            .build()
        CometChatUIKit.init(this, settings, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(result: String?) {
                Log.d(TAG, "CometChat SDK initialized in Application (killed-state ready)")
                onSDKInitialized()
            }
            override fun onError(e: CometChatException) {
                Log.e(TAG, "Application SDK init failed: ${e.message}")
            }
        })
    }

    private fun initPushNotifications() {
        // The CometChat SDK must be initialized before the PN SDK (which connects to CometChat).
        // The SDK is initialized in the Splash/AppCredentials flow, which then calls
        // onSDKInitialized() — so this is a no-op on the early Application.onCreate() call.
        if (!CometChatUIKit.isSDKInitialized()) {
            Log.d(TAG, "Skipping Push Notifications init — CometChat SDK not initialized yet")
            return
        }
        val appId = AppPreferences.getAppId(this)
        val region = AppPreferences.getRegion(this)
        if (appId.isNullOrEmpty() || region.isNullOrEmpty()) {
            Log.d(TAG, "Skipping Push Notifications init — credentials not set yet")
            return
        }
        // Ringing v2, Push SDK 2.0 in AUTO mode: the SDK rings (CallStyle notification,
        // full-screen intent, ringtone, expiry, durable decline) and launches the app's
        // CallAnswerActivity on answer, which opens the UIKit's call screen. The UIKit itself
        // does not depend on the Push SDK.
        val config = PNConfiguration.Builder(appId, region)
            .setProviderId(AppCredentials.PROVIDER_ID)
            .setAnswerActivity(CallAnswerActivity::class.java)
            .setNotificationSmallIcon(R.drawable.ic_cometchat_notification)
            .setInlineReplyEnabled(true)
            // The incoming-call screen in the same colour schemes the app gives CometChatTheme.
            .setIncomingCallTheme(incomingCallTheme(lightScheme, darkScheme).toCometChatPNIncomingCallTheme())
            .apply {
                // Same host as the Chat and Calls SDKs, or the token registers on the wrong app.
                CometChatUIKit.getAuthSettings()?.overrideClientHost?.let { overrideClientHost(it) }
            }
            .build()
        CometChatPushNotifications.init(this, config)
        Log.d(TAG, "CometChat Push Notifications initialized")
    }

    /**
     * Registers the CallAnsweredHandler (accept call from a push) and the NotificationTapListener.
     * The tap listener launches the single host MainActivity with the app's existing FCM extras so
     * MainActivity.handleNotificationIntent routes via the NavHost. (Routing to the exact
     * conversation is a known follow-up — see integration notes.)
     */
    private fun setupPushNotificationListeners() {

        CometChatPushNotifications.setOnNotificationTapListener(object : NotificationTapListener {
            override fun onNotificationTapped(
                context: Context,
                user: User?,
                group: Group?,
                message: BaseMessage?
            ) {
                // Launch the host Activity with the tapped conversation; MainActivity hands the
                // deep link to the NavHost, which opens the conversation — or the thread, when the
                // tapped message is a reply and so does not live in the main list.
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra(
                        AppConstants.FCMConstants.NOTIFICATION_TYPE,
                        AppConstants.FCMConstants.NOTIFICATION_TYPE_MESSAGE
                    )
                    user?.uid?.let { putExtra(AppConstants.FCMConstants.KEY_UID, it) }
                    group?.guid?.let { putExtra(AppConstants.FCMConstants.KEY_GUID, it) }
                    message?.let { tapped ->
                        putExtra(AppConstants.FCMConstants.KEY_MESSAGE_ID, tapped.id.toLong())
                        putExtra(
                            AppConstants.FCMConstants.KEY_PARENT_MESSAGE_ID,
                            tapped.parentMessageId.toLong()
                        )
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
            }
        })
    }

    
    

    
    
    
    
}
