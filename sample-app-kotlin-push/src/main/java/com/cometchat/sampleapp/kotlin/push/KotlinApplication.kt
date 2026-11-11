package com.cometchat.sampleapp.kotlin.push

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.helpers.Logger
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.pushnotification.CometChatPushNotifications
import com.cometchat.pushnotification.PNConfiguration
import com.cometchat.sampleapp.kotlin.push.calls.toCometChatPNIncomingCallTheme
import com.cometchat.uikit.kotlin.theme.CometChatTheme
import com.cometchat.pushnotification.listeners.NotificationTapListener
import com.cometchat.sampleapp.kotlin.push.calls.CallAnswerActivity
import com.cometchat.sampleapp.kotlin.push.appflow.MessagesActivity
import com.cometchat.sampleapp.kotlin.push.fcm.AppFCMService
import com.google.firebase.FirebaseApp
import com.google.gson.Gson
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.UIKitSettings
import com.cometchat.sampleapp.kotlin.push.shared.AppCredentials
import com.cometchat.sampleapp.kotlin.push.shared.AppPreferences
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Application class for managing call notifications and handling CometChat events.
 * Uses a Snackbar to display incoming call UI at the top of the screen.
 *
 * SDK initialization is handled by SplashActivity/AppCredentialsActivity.
 * Call listeners are registered via onSDKInitialized() callback.
 */
class KotlinApplication : Application() {
    private val TAG: String = "Application"
    private var currentActivityInstance: Activity? = null
    private val isConnectedToWebSockets = AtomicBoolean(false)

    /** Flag to track if SDK has been initialized and listeners registered */
    private var isSDKInitialized = false

    companion object {

        private var isAppInForeground: Boolean = false
        var currentActivity: Activity? = null

        fun isAppInForeground(): Boolean = isAppInForeground
    }

    override fun onCreate() {
        super.onCreate()

        // ── CometChat Push Notifications ──
        // Initialize Firebase and set the push listeners (listeners are credential-independent).
        FirebaseApp.initializeApp(this)
        setupPushNotificationListeners()
        // Ensure the CometChat SDK (and then PN/Calls) is initialized here in Application.onCreate.
        // This is required for KILLED-STATE pushes: FCM starts the process and Application.onCreate
        // runs before AppFCMService.onMessageReceived, but no Splash/UI runs to initialize the SDK.
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

                // Connect to WebSocket when app comes to foreground.
                // ENG-39442: guard on SDK init: before credentials are entered the SDK is
                // not initialized, and calling connect() then crashes with the SDK's
                // "Please call the CometChat.init() method..." error (getLoggedInUser hits an
                // uninitialized SQLiteManager). Skipping here lets the app route to the credentials
                // screen; connect still runs after init + login and on subsequent foregrounds.
                if (CometChatUIKit.isSDKInitialized() && isConnectedToWebSockets.compareAndSet(false, true)) {
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
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    isAppInForeground = false
                    Log.d(TAG, "App is now in BACKGROUND")

                    // Disconnect WebSocket when app goes to background (only if the SDK was
                    // initialized — otherwise disconnect hits the same uninitialized-SDK crash). ENG-39442.
                    if (CometChatUIKit.isSDKInitialized()) {
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
     * Called by SplashActivity/AppCredentialsActivity after SDK initialization succeeds.
     * Initializes push notifications; the Calls SDK is initialised by CometChatUIKit.init.
     *
     * This method is idempotent - it will only register listeners once.
     */
    fun onSDKInitialized() {
        if (!isSDKInitialized) {
            isSDKInitialized = true
            Log.d(TAG, "onSDKInitialized: Registering call listeners")

            // The Calls SDK is initialised by CometChatUIKit.init (setEnableCalling, with the
            // same hosts). A second init here was redundant, and the Calls SDK keeps the
            // hosts of its first init.


            // Initialize Push Notifications now that credentials are available
            initPushNotifications()
        }
    }

    /**
     * Initializes the CometChat Push Notifications SDK using credentials from SharedPreferences.
     * Safe to call multiple times; no-ops when credentials are not yet available (e.g. first launch
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
            .setAutoEstablishSocketConnection(false)
            .setAppId(appId)
            .setRegion(region)
            .setAuthKey(authKey)
            .subscribePresenceForAllUsers()
            .setEnableCalling(true)
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
            // The incoming-call screen in the UIKit's colours, light and dark, from the manifest theme.
            .setIncomingCallTheme(CometChatTheme.getIncomingCallTheme(this).toCometChatPNIncomingCallTheme())
            .apply {
                // Same host as the Chat and Calls SDKs, or the token registers on the wrong app.
                CometChatUIKit.getAuthSettings()?.overrideClientHost?.let { overrideClientHost(it) }
            }
            .build()
        CometChatPushNotifications.init(this, config)
        Log.d(TAG, "CometChat Push Notifications initialized")
    }

    /**
     * Registers the CallAnsweredHandler (accept call from a push) and the NotificationTapListener
     * (navigate to the tapped conversation). These do not depend on credentials, so they are set
     * once in onCreate().
     */
    private fun setupPushNotificationListeners() {

        // Fires when the user taps a chat notification — open the conversation in MessagesActivity.
        CometChatPushNotifications.setOnNotificationTapListener(object : NotificationTapListener {
            override fun onNotificationTapped(
                context: Context,
                user: User?,
                group: Group?,
                message: BaseMessage?
            ) {
                val intent = Intent(context, MessagesActivity::class.java).apply {
                    user?.let { putExtra(getString(R.string.app_user), it.toJson().toString()) }
                    group?.let { putExtra(getString(R.string.app_group), Gson().toJson(it)) }
                    message?.let { tapped ->
                        val parentId = tapped.parentMessageId.toLong()
                        if (parentId > 0) {
                            // A thread reply does not live in the main list — MessagesActivity opens
                            // the conversation, then stacks the thread and highlights the reply there.
                            putExtra(MessagesActivity.EXTRA_NOTIFICATION_PARENT_ID, parentId)
                            putExtra(MessagesActivity.EXTRA_NOTIFICATION_REPLY_ID, tapped.id.toLong())
                        } else {
                            putExtra("goToMessageId", tapped.id.toLong())
                        }
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        })
    }
    
    
    
    
    
    /**
     * Displays a custom SnackBar notification at the top of the screen for incoming calls.
     */
    
    
    
    
}
