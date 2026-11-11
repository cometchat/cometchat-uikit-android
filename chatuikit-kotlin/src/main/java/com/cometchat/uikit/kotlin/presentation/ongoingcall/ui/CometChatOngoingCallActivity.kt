package com.cometchat.uikit.kotlin.presentation.ongoingcall.ui

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cometchat.calls.core.CallSession
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.chat.core.Call
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.constants.UIKitConstants.CallWorkFlow
import com.cometchat.uikit.core.models.OngoingCallEvent
import com.cometchat.uikit.core.models.OngoingCallUIState
import com.cometchat.uikit.core.utils.CallingState
import com.cometchat.uikit.core.utils.CallsUtils
import com.cometchat.uikit.core.utils.CometChatLogger
import com.cometchat.uikit.kotlin.presentation.ongoingcall.style.CometChatOngoingCallStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * CometChatOngoingCallActivity is an Activity that hosts the CometChatOngoingCall component.
 *
 * This activity:
 * - Displays the ongoing call interface with proper screen management
 * - Supports Picture-in-Picture (PIP) mode for multitasking during calls
 * - Shows when locked and turns screen on for incoming calls
 * - Handles call lifecycle and cleanup
 * - Hosts an answered incoming call: the app's Push SDK answer activity calls
 *   `launchOngoingCallActivity(context, call)` and the session the Push SDK prepared is
 *   joined here
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
public class CometChatOngoingCallActivity : AppCompatActivity() {

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

    // ==================== Views ====================
    private lateinit var container: FrameLayout
    private var ongoingCallView: CometChatOngoingCall? = null

    // ==================== State ====================
    private var sessionId: String? = null
    private var callType: String? = null
    private var answeredCall: Call? = null
    private var callWorkFlow: CallWorkFlow = CallWorkFlow.DEFAULT
    private var isInPipMode: Boolean = false
    
    // ==================== Coroutine Scope ====================
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var eventsJob: Job? = null

    // ==================== Lifecycle Methods ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set screen management flags (Requirements 8.1, 8.2, 8.3)
        setupScreenManagement()
        
        // Extract intent extras
        sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        callType = intent.getStringExtra(EXTRA_CALL_TYPE)
        callWorkFlow = intent.getStringExtra(EXTRA_CALL_WORKFLOW)?.let {
            try { CallWorkFlow.valueOf(it) } catch (e: Exception) { CallWorkFlow.DEFAULT }
        } ?: CallWorkFlow.DEFAULT

        // Opened for an answered incoming call: the call travels in the intent so hang-up can
        // report it (CometChatCallEvent.CallEnded).
        answeredCall = answeredCallFrom(intent)
        
        // Create container layout
        container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(container)
        
        // Apply window insets (Requirement 8.4)
        applyWindowInsets()
        
        // Setup ongoing call view
        setupOngoingCall()
        
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
     * A second call answered while this screen is up (End & accept on a call that rang over the
     * live one): the launch intent arrives here because it carries SINGLE_TOP. The Push SDK has
     * already left the live call, so open a fresh screen for the new call and close this one.
     * Reusing this screen would share its activity-scoped ViewModel with the call that ended.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val next = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        if (next == sessionId) return
        CometChatLogger.d(TAG, "switching from $sessionId to $next")
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

    /**
     * Applies window insets for proper system bar handling.
     *
     * **Validates: Requirement 8.4**
     */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Sets up the CometChatOngoingCall view.
     *
     * **Validates: Requirements 1.1, 1.2, 2.1, 2.2, 3.1**
     */
    private fun setupOngoingCall() {
        val sid = sessionId ?: return
        val type = callType ?: return
        // Use provided builder, or UIKit's configured builder, or create default
        val builder = onGoingSessionSettingsBuilder 
            ?: CometChatUIKit.getSessionSettingsBuilder() 
            ?: CometChatCalls.SessionSettingsBuilder()

        ongoingCallView = CometChatOngoingCall(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            
            // Apply style if provided
            ongoingCallStyle?.let { setStyle(it) }
            
            // Configure the call
            setSessionId(sid)
            setCallType(type)
            setCallWorkFlow(callWorkFlow)
            setSessionSettingsBuilder(builder)
            
            // Set error callback
            setOnError { exception ->
                CometChatLogger.e(TAG, "Ongoing call error: ${exception.message}")
            }
        }

        container.addView(ongoingCallView)
        answeredCall?.let { ongoingCallView?.getViewModel()?.setCall(it) }
        
        // Observe events for call ended and user joined
        observeEvents()
        
        // Start the call
        ongoingCallView?.startCall()
    }

    /**
     * Observes ViewModel events for call lifecycle handling.
     */
    private fun observeEvents() {
        val viewModel = ongoingCallView?.getViewModel() ?: return
        
        eventsJob = activityScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is OngoingCallEvent.CallEnded -> {
                        finish()
                    }
                    is OngoingCallEvent.SessionTimeout -> {
                        finish()
                    }
                    is OngoingCallEvent.UserJoined -> {
                        // Handle PIP mode entry for current user (Requirement 18.1)
                        if (event.isCurrentUser && isInPipMode) {
                            CallSession.getInstance()?.enablePictureInPictureLayout()
                        }
                    }
                    is OngoingCallEvent.UserLeft -> {
                        // No action needed
                    }
                    is OngoingCallEvent.Error -> {
                        // Error handled by onError callback
                    }
                }
            }
        }
        
        // Also observe UI state for Ended state
        activityScope.launch {
            viewModel.uiState.collect { state ->
                if (state is OngoingCallUIState.Ended) {
                    finish()
                }
            }
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
        isInPipMode = isInPictureInPictureMode
        
        if (isInPictureInPictureMode) {
            // Entering PIP mode (Requirement 9.3)
            CallSession.getInstance()?.enablePictureInPictureLayout()
        } else {
            // Exiting PIP mode (Requirement 9.4)
            CallSession.getInstance()?.disablePictureInPictureLayout()
        }
    }

    /**
     * Called when the activity is stopped.
     * Handles PIP exit if in PIP mode.
     *
     * **Validates: Requirements 10.1, 10.2, 10.3, 10.4**
     * 
     * IMPORTANT: We must check isInPictureInPictureMode directly (not the cached isInPipMode variable)
     * because when user dismisses PIP, onPictureInPictureModeChanged(false) is called BEFORE onStop(),
     * which sets isInPipMode to false. But isInPictureInPictureMode() still returns true during onStop().
     * This matches the Java v5 implementation behavior.
     */
    override fun onStop() {
        super.onStop()
        
        // Handle PIP exit when in PIP mode (Requirement 10.1)
        // Use isInPictureInPictureMode directly, not the cached variable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) {
            handlePiPExit()
        }
    }

    /**
     * Handles PIP exit by ending the call appropriately based on workflow.
     *
     * **Validates: Requirements 10.2, 10.3, 10.4**
     */
    private fun handlePiPExit() {
        // Use the view's handlePiPExit method which handles workflow-dependent cleanup
        ongoingCallView?.handlePiPExit(callWorkFlow)
        
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
        
        // Cancel coroutine scope
        eventsJob?.cancel()
        activityScope.cancel()
        
        // Ensure the call session is fully torn down to stop WebRTC rendering — but only this
        // screen's own session. After End & accept the next call's screen may already have
        // joined, and leaving "whatever is active" here would drop that call.
        val active = CallsUtils.activeSessionId()
        if (active == null || active == sessionId) {
            CallSession.getInstance()?.leave()
        }
        
        // Clear static builder (Requirement 11.1)
        onGoingSessionSettingsBuilder = null
        ongoingCallStyle = null
        
        // Clear active call (Requirement 11.2), unless the next call's screen already set its own
        if (CallingState.getActiveCall()?.sessionId.let { it == null || it == sessionId }) {
            CallingState.setActiveCall(null)
        }
    }
}
