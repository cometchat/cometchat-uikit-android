package com.cometchat.sampleapp.kotlin.ui.activity

import android.app.PictureInPictureParams
import android.os.Bundle
import android.util.Rational
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.chatuikit.shared.constants.UIKitConstants
import com.cometchat.sampleapp.kotlin.R
import com.cometchat.sampleapp.kotlin.databinding.ActivityOngoingCallBinding

/**
 * This activity handles ongoing calls, setting up the UI and managing the call
 * session. It retrieves call details from the intent and initializes the call
 * interface.
 */
class OngoingCallActivity : AppCompatActivity() {
    /**
     * Called when the activity is starting. It initializes the UI and starts the
     * call.
     *
     * @param savedInstanceState
     * If the activity is being re-initialized after previously being
     * shut down, this Bundle contains the data it most recently supplied
     * in onSaveInstanceState.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOngoingCallBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        // Retrieve the call details from the intent
        val sessionId = intent.getStringExtra(getString(R.string.app_session_id))
        val callType = intent.getStringExtra(getString(R.string.app_call_type))

        // Configure the ongoing call UI with the received Call data
        binding.cometchatOngoingCall.setCallWorkFlow(UIKitConstants.CallWorkFlow.DEFAULT)
        binding.cometchatOngoingCall.setSessionId(sessionId)
        binding.cometchatOngoingCall.setCallType(callType)

        // Start the call session
        binding.cometchatOngoingCall.startCall()

        // Disable back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startPictureInPictureMode()
            }
        })
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        startPictureInPictureMode()
    }

    private fun startPictureInPictureMode() {
        val metrics = resources.displayMetrics
        this.enterPictureInPictureMode(
            PictureInPictureParams
                .Builder()
                .setAspectRatio(Rational(metrics.widthPixels, metrics.heightPixels))
                .build()
        )
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            CometChatCalls.enterPIPMode()
        } else {
            CometChatCalls.exitPIPMode()
        }
    }
}
