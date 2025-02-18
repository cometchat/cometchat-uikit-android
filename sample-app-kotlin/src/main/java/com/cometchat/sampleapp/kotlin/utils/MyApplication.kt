package com.cometchat.sampleapp.kotlin.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.PopupWindow
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.CometChatTheme
import com.cometchat.chatuikit.calls.CallingExtension
import com.cometchat.chatuikit.calls.CometChatCallActivity
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKitHelper
import com.cometchat.chatuikit.shared.resources.soundmanager.CometChatSoundManager
import com.cometchat.chatuikit.shared.resources.soundmanager.Sound
import com.cometchat.chatuikit.shared.resources.utils.Utils
import com.cometchat.sampleapp.kotlin.R
import com.cometchat.sampleapp.kotlin.data.repository.Repository
import com.cometchat.sampleapp.kotlin.databinding.CustomCallNotificationBinding
import com.cometchat.sampleapp.kotlin.ui.activity.OngoingCallActivity
import com.cometchat.sampleapp.kotlin.viewmodels.SplashViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

/**
 * This is a custom Application class for managing call notifications and
 * handling CometChat events throughout the app lifecycle. It also registers
 * lifecycle callbacks to keep track of the currently active activity.
 */
class MyApplication : Application() {
    private var currentActivity: Activity? = null
    private var snackBar: Snackbar? = null
    private lateinit var soundManager: CometChatSoundManager
    private var tempCall: Call? = null

    /**
     * Initializes the application by setting up Firebase, adding the CometChat call
     * listener, and registering activity lifecycle callbacks.
     */
    override fun onCreate() {
        super.onCreate()

        soundManager = CometChatSoundManager(this)

        addCallListener()

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var isActivityChangingConfigurations = false
            private var activityReferences = 0

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                currentActivity = activity
                if (currentActivity is CometChatCallActivity) {
                    Utils.setStatusBarColor(
                        currentActivity, CometChatTheme.getBackgroundColor3(activity)
                    )
                } else {
                    Utils.setStatusBarColor(
                        currentActivity, CometChatTheme.getBackgroundColor1(activity)
                    )
                }
            }

            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    isAppInForeground = true
                }
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
                if (snackBar != null && snackBar!!.isShown && tempCall != null) {
                    showTopSnackBar(tempCall)
                } else
                    dismissTopSnackBar()
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    isAppInForeground = false
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivity === activity) {
                    currentActivity = null
                }
            }
        })
    }

    /**
     * Adds a call listener using CometChat to handle incoming and outgoing call
     * events.
     */
    private fun addCallListener() {
        CometChat.addCallListener(LISTENER_ID, object : CometChat.CallListener() {
            override fun onIncomingCallReceived(call: Call) {
                launchIncomingCallPopup(call)
            }

            override fun onOutgoingCallAccepted(call: Call) {
                dismissTopSnackBar()
            }

            override fun onOutgoingCallRejected(call: Call) {
                dismissTopSnackBar()
            }

            override fun onIncomingCallCancelled(call: Call) {
                dismissTopSnackBar()
            }

            override fun onCallEndedMessageReceived(call: Call) {
                dismissTopSnackBar()
            }
        })
    }

    /**
     * Displays a custom SnackBar notification at the top of the screen for incoming
     * calls.
     *
     * @param call The call object representing the incoming call.
     */
    @SuppressLint("RestrictedApi")
    fun showTopSnackBar(call: Call?) {
        if (currentActivity == null && call == null) return
        tempCall = call // Get the root view of the current activity
        val rootView: View = currentActivity!!.findViewById(android.R.id.content)
        val customView = View.inflate(currentActivity, R.layout.custom_call_notification, null)
        val binding = CustomCallNotificationBinding.bind(customView)
        val callUser = call!!.callInitiator as User
        binding.callerName.text = callUser.name
        binding.callType.text = String.format(Locale.US, getString(R.string.app_incoming_s_call), call.type)
        binding.callerAvatar.setAvatar(callUser.name, callUser.avatar)
        binding.callTypeIcon.setImageResource(if (call.type == CometChatConstants.CALL_TYPE_AUDIO) com.cometchat.chatuikit.R.drawable.cometchat_ic_call_voice else com.cometchat.chatuikit.R.drawable.cometchat_ic_call_video)

        binding.rejectButton.setOnClickListener {
            rejectCall(
                call
            )
        }
        binding.acceptButton.setOnClickListener {
            acceptCall(
                call
            )
        }

        snackBar = Snackbar.make(rootView, " ", Snackbar.LENGTH_INDEFINITE)
        val layout: Snackbar.SnackbarLayout = snackBar?.view as Snackbar.SnackbarLayout
        val params: FrameLayout.LayoutParams = layout.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        layout.setLayoutParams(params)
        layout.setBackgroundColor(
            currentActivity!!.resources.getColor(android.R.color.transparent, null)
        )

        layout.addView(binding.root, 0)
        for (popupWindow in popupWindows) {
            if (popupWindow.isShowing) popupWindow.dismiss()
        }
        popupWindows.clear()
        snackBar!!.show()
    }

    /**
     * Dismisses the current top SnackBar if it's being shown and resets any related
     * call state.
     */
    fun dismissTopSnackBar() {
        if (snackBar != null && snackBar!!.isShown) {
            snackBar!!.dismiss()
            snackBar = null
            tempCall = null
            CallingExtension.setActiveCall(null)
            pauseSound()
        }
    }

    /**
     * Launches an incoming call popup when an incoming call is received.
     *
     * @param call The call object representing the incoming call.
     */
    fun launchIncomingCallPopup(call: Call) {
        if (call.callInitiator is User) {
            val callInitiator = call.callInitiator as User
            if (CometChatUIKit.getLoggedInUser().uid.equals(callInitiator.uid, ignoreCase = true)) return
        }

        if (CometChat.getActiveCall() == null && CallingExtension.getActiveCall() == null && !CallingExtension.isActiveMeeting()) {
            CallingExtension.setActiveCall(call)
            playSound()
            showTopSnackBar(call)
        } else {
            rejectCallWithBusyStatus(call)
        }
    }

    /**
     * Rejects an incoming call with a "busy" status if another call is active.
     *
     * @param call The call object representing the call to be rejected.
     */
    private fun rejectCallWithBusyStatus(call: Call) {
        Thread {
            try {
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            Repository.rejectCallWithBusyStatus(call, object : CometChat.CallbackListener<Call>() {
                override fun onSuccess(call: Call) {
                    CometChatUIKitHelper.onCallRejected(call)
                }

                override fun onError(e: CometChatException) {
                }
            })
        }.start()
    }

    /**
     * Rejects an incoming call and dismisses the notification.
     *
     * @param call The call object representing the call to be rejected.
     */
    private fun rejectCall(call: Call) {
        Repository.rejectCall(call, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(call: Call) {
                dismissTopSnackBar()
            }

            override fun onError(e: CometChatException) {
                dismissTopSnackBar()
            }
        })
    }

    /**
     * Accepts an incoming call and starts the ongoing call activity.
     *
     * @param call The call object representing the call to be accepted.
     */
    private fun acceptCall(call: Call) {
        Repository.acceptCall(call, object : CometChat.CallbackListener<Call>() {
            override fun onSuccess(call: Call) {
                startOngoingCallActivity(call)
            }

            override fun onError(e: CometChatException) {
                dismissTopSnackBar()
            }
        })
    }

    /**
     * Starts the OngoingCallActivity to manage an accepted call.
     *
     * @param call The call object representing the accepted call.
     */
    private fun startOngoingCallActivity(call: Call) {
        dismissTopSnackBar()
        val intent = Intent(this, OngoingCallActivity::class.java)
        intent.putExtra(getString(R.string.app_session_id), call.sessionId)
        intent.putExtra(getString(R.string.app_call_type), call.type)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * Plays the sound for an incoming call notification.
     */
    private fun playSound() {
        soundManager.play(Sound.incomingCall)
    }

    /**
     * Silently pauses any currently playing sound.
     */
    fun pauseSound() {
        soundManager.pauseSilently()
    }

    companion object {
        var popupWindows = ArrayList<PopupWindow>()
        private var LISTENER_ID: String = System.currentTimeMillis().toString()
        var currentOpenChatId: String? = null
        var isAppInForeground: Boolean = false
    }
}
