package com.cometchat.sampleapp.compose.app

import android.app.Application
import android.util.Log
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.sampleapp.compose.utils.AppPreferences
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.UIKitSettings

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
 * - **No calling** - This sample is chat only: no Calls tab, call buttons or incoming-call
 *   UI, and `enableCalling` stays off. In Ringing v2 an incoming call reaches an app only
 *   through the Push SDK, so calling lives in sample-app-compose-push.
 *
 * @see CometChatUIKit
 * @see UIKitSettings
 */
class SampleApplication : Application() {

    companion object {
        private const val TAG = "SampleApplication"
    }

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
        Log.d(TAG, "SampleApplication created")
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

        // Persist credentials so warm-start paths can read them.
        AppPreferences(this).saveCredentials(appId = appId, region = region, authKey = authKey)

        // Build UIKit settings with the provided credentials
        val uiKitSettings = UIKitSettings.UIKitSettingsBuilder()
            .setAppId(appId)
            .setRegion(region)
            .setAuthKey(authKey)
            .subscribePresenceForAllUsers()
            .setAutoEstablishSocketConnection(true)
            // Offline + encryption, always on (D86, D92).
            .setOfflineConfig(AppPreferences(this).offlineConfig())
            .build()

        // Initialize the CometChat UIKit
        CometChatUIKit.init(
            context = this,
            authSettings = uiKitSettings,
            callbackListener = object : CometChat.CallbackListener<String>() {
                override fun onSuccess(result: String?) {
                    Log.d(TAG, "CometChat SDK initialized successfully: $result")
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

}
