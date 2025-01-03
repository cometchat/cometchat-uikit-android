package com.cometchat.sampleapp.kotlin.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.sampleapp.kotlin.AppCredentials
import com.cometchat.sampleapp.kotlin.R
import com.cometchat.sampleapp.kotlin.databinding.ActivitySplashBinding
import com.cometchat.sampleapp.kotlin.utils.AppUtils
import com.cometchat.sampleapp.kotlin.viewmodels.SplashViewModel

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
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
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
}
