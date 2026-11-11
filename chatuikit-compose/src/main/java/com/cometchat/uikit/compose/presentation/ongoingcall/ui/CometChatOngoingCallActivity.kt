package com.cometchat.uikit.compose.presentation.ongoingcall.ui

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cometchat.calls.core.CallSession
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.chat.core.Call
import com.cometchat.uikit.compose.presentation.ongoingcall.style.CometChatOngoingCallStyle
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.constants.UIKitConstants.CallWorkFlow
import com.cometchat.uikit.core.models.OngoingCallEvent
import com.cometchat.uikit.core.models.OngoingCallUIState
import com.cometchat.uikit.core.utils.CallRingingUtils
import com.cometchat.uikit.core.utils.CometChatLogger
import com.cometchat.uikit.core.utils.CallingState
import com.cometchat.uikit.core.utils.CallsUtils
import com.cometchat.uikit.core.viewmodel.CometChatOngoingCallViewModel

/**
 * CometChatOngoingCallActivity is a Compose Activity that hosts the CometChatOngoingCall composable.
 *
 * This activity:
 * - Displays the ongoing call interface with proper screen management
 * - Supports Picture-in-Picture (PIP) mode for multitasking during calls
 * - Shows when locked and turns screen on for incoming calls
 * - Handles call lifecycle and cleanup
 *
 * **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3, 9.4, 9.5, 10.1, 10.2, 10.3, 10.4, 11.1, 11.2, 12.1, 12.2, 12.3, 12.4**
 *
 * Usage:
 * ```kotlin
 * CometChatOngoingCallActivity.launchOngoingCallActivity(
 *     context = context,
 *     sessionId = "session-id",
 *     callType = "video",
 *     callSettingsBuilder = builder
 * )
 * ```
 */
public class CometChatOngoingCallActivity : ComponentActivity() {

    public companion object {
        private const val TAG = "CometChatOngoingCallActivity"

        // Intent extras
        private const val EXTRA_SESSION_ID = "extra_session_id"
        private const val EXTRA_CALL_TYPE = "extra_call_type"
        private const val EXTRA_CALL_WORKFLOW = "extra_call_workflow"
        private const val EXTRA_CALL_JSON = "extra_call_json"

        // Static storage for SessionSettingsBuilder (cannot be passed via Intent)
        // **Validates: Requirement 12.4**
        @Volatile
        private var onGoingSessionSettingsBuilder: CometChatCalls.SessionSettingsBuilder? = null

        // Static storage for style
        @Volatile
        private var ongoingCallStyle: CometChatOngoingCallStyle? = null

        /**
         * Launches the ongoing call activity.
         *
         * **Validates: Requirements 12.1, 12.2, 12.3, 12.4**
         *
         * @param context The context to start the activity from
         * @param sessionId The unique identifier for the call session
         * @param callType The type of call ("audio" or "video")
         * @param callWorkFlow The workflow type (DEFAULT for 1:1, MEETING for group)
         * @param callSettingsBuilder Optional call settings builder for custom configuration
         * @param style Optional style configuration for the ongoing call screen
         */
        @JvmStatic
        public fun launchOngoingCallActivity(
            context: Context,
            sessionId: String,
            callType: String,
            callWorkFlow: CallWorkFlow = CallWorkFlow.DEFAULT,
            callSettingsBuilder: CometChatCalls.SessionSettingsBuilder? = null,
            style: CometChatOngoingCallStyle? = null
        ) {
            // Store SessionSettingsBuilder in static variable (Requirement 12.4)
            onGoingSessionSettingsBuilder = callSettingsBuilder
            ongoingCallStyle = style

            val intent = Intent(context, CometChatOngoingCallActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_CALL_TYPE, callType)
                putExtra(EXTRA_CALL_WORKFLOW, callWorkFlow.name)
                // Add FLAG_ACTIVITY_NEW_TASK (Requirement 12.3)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        /**
         * Launches the ongoing call activity for an answered incoming [call], as the final step
         * of the app's Push SDK answer activity (`PNConfiguration.setAnswerActivity`). The app
         * reads the Push SDK's `IncomingCall` from its intent with `CallBundles.getCall`,
         * builds the [Call] with `CallRingingUtils.buildIncomingCall`, then calls this. The
         * screen joins the session the Push SDK prepared and emits
         * `CometChatCallEvent.CallEnded(call)` on hang-up.
         *
         * A different call answered while this screen is already up (End & accept on a call
         * that rang over the live one) opens a fresh screen and closes the old one.
         *
         * @param context The context to start the activity from
         * @param call The answered call; `sessionId` and `type` must be set
         * @param callSettingsBuilder Optional call settings builder for custom configuration
         * @param style Optional style configuration for the ongoing call screen
         */
        @JvmStatic
        @JvmOverloads
        public fun launchOngoingCallActivity(
            context: Context,
            call: Call,
            callSettingsBuilder: CometChatCalls.SessionSettingsBuilder? = null,
            style: CometChatOngoingCallStyle? = null
        ) {
            val sessionId = call.sessionId ?: return
            val callType = call.type ?: return
            onGoingSessionSettingsBuilder = callSettingsBuilder
            ongoingCallStyle = style

            val intent = Intent(context, CometChatOngoingCallActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_CALL_TYPE, callType)
                putExtra(EXTRA_CALL_WORKFLOW, CallWorkFlow.DEFAULT.name)
                call.rawMessage?.let { putExtra(EXTRA_CALL_JSON, it.toString()) }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        }
    }

    // ==================== State ====================
    private var sessionId: String? = null
    private var callType: String? = null
    private var answeredCall: Call? = null
    private var callWorkFlow: CallWorkFlow = CallWorkFlow.DEFAULT
    private var isInPipMode: Boolean = false
    
    // Track if we're transitioning to PIP (to avoid premature exit handling)
    private var isEnteringPipMode: Boolean = false
    
    // ==================== ViewModel Reference ====================
    private var ongoingCallViewModel: CometChatOngoingCallViewModel? = null

    // ==================== Lifecycle Methods ====================

    /**
     * A second call answered while this screen is up (End & accept on a call that rang over the
     * live one): the launch intent arrives here because it carries SINGLE_TOP. The Push SDK has
     * already left the live call, so open a fresh screen for the new call and close this one.
     * Reusing this screen would share its activity-scoped ViewModel with the call that ended.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val next = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        if (next == sessionId) return
        startActivity(
            Intent(intent).setClass(this, CometChatOngoingCallActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    /** The answered call the app's answer activity put in [intent], if any. */
    private fun answeredCallFrom(intent: Intent?): Call? {
        val json = intent?.getStringExtra(EXTRA_CALL_JSON) ?: return null
        return try {
            Call.fromJson(json)
        } catch (e: Exception) {
            CometChatLogger.e(TAG, "answered call could not be read: ${e.message}")
            null
        }
    }

    /**
     * Requests the microphone, plus the camera for a video call only (ENG-40552).
     */
    private fun requestCallPermissions(callType: String?) {
        val missing = CallRingingUtils.permissionsForCallType(callType).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 101)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set screen management flags (Requirements 8.1, 8.2, 8.3)
        setupScreenManagement()

        // Extract intent extras
        sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        callType = intent.getStringExtra(EXTRA_CALL_TYPE)

        // Same as v5: ask for permissions when the call screen is created, now matched to the
        // call type. The answer is not awaited; the SDK joins either way.
        requestCallPermissions(callType)
        callWorkFlow = intent.getStringExtra(EXTRA_CALL_WORKFLOW)?.let {
            try { CallWorkFlow.valueOf(it) } catch (e: Exception) { CallWorkFlow.DEFAULT }
        } ?: CallWorkFlow.DEFAULT

        // Opened for an answered incoming call: the call travels in the intent so hang-up can
        // report it (CometChatCallEvent.CallEnded).
        answeredCall = answeredCallFrom(intent)

        // Apply window insets for edge-to-edge (Requirement 8.4)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set Compose content
        setContent {
            val sid = sessionId
            val type = callType
            // Use provided builder, or UIKit's configured builder, or create default
            val builder = onGoingSessionSettingsBuilder 
                ?: CometChatUIKit.getSessionSettingsBuilder() 
                ?: CometChatCalls.SessionSettingsBuilder()

            if (sid != null && type != null) {
                val viewModel: CometChatOngoingCallViewModel = viewModel()
                val style = ongoingCallStyle ?: CometChatOngoingCallStyle.default()
                
                // Store ViewModel reference for PIP handling
                ongoingCallViewModel = viewModel
                answeredCall?.let { viewModel.setCall(it) }

                // Observe events for call ended and user joined
                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is OngoingCallEvent.CallEnded,
                            is OngoingCallEvent.SessionTimeout -> {
                                finish()
                            }
                            is OngoingCallEvent.UserJoined -> {
                                // Handle PIP mode entry for current user (Requirement 18.1)
                                if (event.isCurrentUser && isInPipMode) {
                                    CallSession.getInstance()?.enablePictureInPictureLayout()
                                }
                            }
                            else -> { /* handled by composable */ }
                        }
                    }
                }

                // Observe UI state for Ended state
                LaunchedEffect(Unit) {
                    viewModel.uiState.collect { state ->
                        if (state is OngoingCallUIState.Ended) {
                            finish()
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    CometChatOngoingCall(
                        sessionId = sid,
                        callType = type,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        viewModel = viewModel,
                        callWorkFlow = callWorkFlow,
                        callSettingsBuilder = builder,
                        style = style,
                        onCallEnded = { finish() },
                        onError = { /* Error handled by composable */ }
                    )
                }
            }
        }
        
        // on API 31+ enable auto-enter so Home and recents send the
        // call to PiP. Auto-enter does NOT fire on the Back gesture, though, so Back must be
        // intercepted explicitly on every API level - otherwise Back finishes this activity
        // and the call ends (reproduced on API 31+ where the interceptor used to be skipped).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val pipMetrics = resources.displayMetrics
            setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(pipMetrics.widthPixels, pipMetrics.heightPixels))
                    .setAutoEnterEnabled(true)
                    .build()
            )
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                enterPipMode()
            }
        })
    }

    /**
     * Sets up screen management for showing when locked and turning screen on.
     *
     * **Validates: Requirements 8.1, 8.2, 8.3**
     */
    private fun setupScreenManagement() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // API 27+ (Requirement 8.1, 8.2)
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            // API < 27 (Requirement 8.3)
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }
    
    // ==================== PIP Mode Methods ====================

    /**
     * Enters Picture-in-Picture mode.
     *
     * **Validates: Requirements 9.1, 9.2, 9.5**
     */
    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val metrics = resources.displayMetrics
            // Use screen aspect ratio for PIP dimensions (Requirement 9.5)
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(metrics.widthPixels, metrics.heightPixels))
                    .build()
            )
        }
    }

    /**
     * Called when the user leaves the activity without explicitly finishing it.
     * Enters PIP mode.
     *
     * **Validates: Requirement 9.2**
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Set flag to indicate we're entering PIP mode (not exiting)
        isEnteringPipMode = true
        enterPipMode()
    }

    /**
     * Called when PIP mode changes.
     * Calls CallSession.enablePictureInPictureLayout/disablePictureInPictureLayout accordingly.
     *
     * **Validates: Requirements 9.3, 9.4**
     */
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        
        val wasInPipMode = isInPipMode
        isInPipMode = isInPictureInPictureMode

        if (isInPictureInPictureMode) {
            // Entering PIP mode (Requirement 9.3)
            CallSession.getInstance()?.enablePictureInPictureLayout()
            // Reset the entering flag after we've entered
            isEnteringPipMode = false
        } else {
            // Exiting PIP mode (Requirement 9.4)
            CallSession.getInstance()?.disablePictureInPictureLayout()
            
            // Only call handlePiPExit if the activity is finishing (user dismissed PIP)
            // If the activity is NOT finishing, user clicked PIP to return to full screen
            if (wasInPipMode && isFinishing) {
                handlePiPExit()
            }
        }
    }

    /**
     * Called when the activity is stopped.
     * 
     * IMPORTANT: We should NOT call handlePiPExit() here when entering PIP mode.
     * The onStop() is called both when:
     * 1. Entering PIP mode (activity goes to background) - call should continue
     * 2. Activity is being destroyed - cleanup is handled in onDestroy or onPictureInPictureModeChanged
     * 
     * The handlePiPExit() is now called from onPictureInPictureModeChanged() when
     * the user dismisses the PIP window (isInPictureInPictureMode changes from true to false).
     *
     * **Validates: Requirements 10.1, 10.2, 10.3, 10.4**
     */
    override fun onStop() {
        super.onStop()
        // No action needed - PIP exit handled in onPictureInPictureModeChanged
    }

    /**
     * Handles PIP exit by ending the call appropriately based on workflow.
     *
     * **Validates: Requirements 10.2, 10.3, 10.4**
     */
    private fun handlePiPExit() {
        // Remove listeners
        ongoingCallViewModel?.removeListeners()

        // Workflow-dependent cleanup (Requirements 10.2, 10.3)
        if (callWorkFlow != CallWorkFlow.MEETING) {
            // DEFAULT workflow: end call and clear active call
            ongoingCallViewModel?.endCall()
        } else {
            // MEETING workflow: leave session only
            CallSession.getInstance()?.leave()
        }

        CallingState.setIsActiveMeeting(false)

        // Finish activity after cleanup (Requirement 10.4)
        finish()
    }

    /**
     * Called when the activity is destroyed.
     * Clears static builder and active call.
     *
     * **Validates: Requirements 11.1, 11.2**
     */
    override fun onDestroy() {
        super.onDestroy()

        // Only this screen's own session and call. After End & accept the next call's screen may
        // already be up, and tearing down "whatever is active" here would drop that call.
        val active = CallsUtils.activeSessionId()
        if (active == null || active == sessionId) {
            // Ensure the call session is fully torn down to stop WebRTC rendering
            CallSession.getInstance()?.leave()
        }

        // Clear static builder (Requirement 11.1)
        onGoingSessionSettingsBuilder = null
        ongoingCallStyle = null

        // Clear active call (Requirement 11.2)
        if (CallingState.getActiveCall()?.sessionId.let { it == null || it == sessionId }) {
            CallingState.setActiveCall(null)
        }
    }
}
