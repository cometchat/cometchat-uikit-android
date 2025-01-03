package com.cometchat.sampleapp.kotlin.fcm.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.sampleapp.kotlin.fcm.AppCredentials
import com.cometchat.sampleapp.kotlin.fcm.R
import com.cometchat.sampleapp.kotlin.fcm.databinding.ActivitySplashBinding
import com.cometchat.sampleapp.kotlin.fcm.utils.AppConstants
import com.cometchat.sampleapp.kotlin.fcm.utils.AppUtils
import com.cometchat.sampleapp.kotlin.fcm.viewmodels.SplashViewModel

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkAppCredentials()
    }

    private fun checkAppCredentials() {
        val appId = AppUtils.getDataFromSharedPref(this, String::class.java, R.string.app_cred_id, AppCredentials.APP_ID)

        if (appId.isNullOrEmpty()) {
            startActivity(Intent(this, AppCredentialsActivity::class.java))
            finish()
        } else {
            initViewModel()
        }
    }

    private fun initViewModel() { // Initialize ViewModel
        val viewModel: SplashViewModel = ViewModelProvider(this)[SplashViewModel::class.java] // Initialize CometChat UIKit
        if (!CometChatUIKit.isSDKInitialized()) {
            viewModel.initUIKit(this)
        } else {
            viewModel.checkUserIsNotLoggedIn()
        } // Observe login status
        viewModel.getLoginStatus().observe(this) { isLoggedIn ->
            if (isLoggedIn != null) {
                if (isLoggedIn) {
                    handleDeepLinking()
                } else {
                    val intent = Intent(
                        this@SplashActivity, LoginActivity::class.java
                    )
                    Toast.makeText(
                        this@SplashActivity, R.string.app_not_logged_in, Toast.LENGTH_SHORT
                    ).show()
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun handleDeepLinking() {
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(AppConstants.FCMConstants.NOTIFICATION_GROUP_SUMMARY_ID)
        val notificationType: String? = intent.getStringExtra(AppConstants.FCMConstants.NOTIFICATION_TYPE)
        val notificationPayload: String? = intent.getStringExtra(AppConstants.FCMConstants.NOTIFICATION_PAYLOAD)
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra(AppConstants.FCMConstants.NOTIFICATION_TYPE, notificationType)
        intent.putExtra(AppConstants.FCMConstants.NOTIFICATION_PAYLOAD, notificationPayload)
        startActivity(intent)
        finish()
    }
}
