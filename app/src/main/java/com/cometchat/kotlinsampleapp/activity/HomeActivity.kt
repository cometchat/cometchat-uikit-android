package com.cometchat.kotlinsampleapp.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.resources.theme.Palette
import com.cometchat.chatuikit.shared.resources.utils.Utils
import com.cometchat.kotlinsampleapp.AppUtils.Companion.changeIconTintToBlack
import com.cometchat.kotlinsampleapp.AppUtils.Companion.changeIconTintToWhite
import com.cometchat.kotlinsampleapp.AppUtils.Companion.isNightMode
import com.cometchat.kotlinsampleapp.AppUtils.Companion.switchDarkMode
import com.cometchat.kotlinsampleapp.AppUtils.Companion.switchLightMode
import com.cometchat.kotlinsampleapp.R
import com.cometchat.kotlinsampleapp.constants.StringConstants
import com.cometchat.kotlinsampleapp.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var darkMode: ImageView
    private lateinit var lightMode: ImageView
    private lateinit var logout: ImageView
    private lateinit var parentView: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        darkMode = binding.darkMode
        lightMode = binding.lightMode
        parentView = binding.parentView
        logout = binding.logout
        setUpUI()
        binding.chats.setOnClickListener {
            handleIntent(StringConstants.CONVERSATIONS)
        }
        binding.users.setOnClickListener {
            handleIntent(StringConstants.USERS)
        }
        binding.groups.setOnClickListener {
            handleIntent(StringConstants.GROUPS)
        }
        binding.messages.setOnClickListener {
            handleIntent(StringConstants.MESSAGES)
        }
        binding.shared.setOnClickListener {
            handleIntent(StringConstants.SHARED)
        }
        binding.calls.setOnClickListener {
            handleIntent(StringConstants.CALLS)
        }
        logout.setOnClickListener {
            CometChatUIKit.logout(object : CometChat.CallbackListener<String?>() {
                override fun onSuccess(s: String?) {
                    startActivity(
                        Intent(
                            this@HomeActivity,
                            MainActivity::class.java
                        )
                    )
                    finishAffinity()
                }

                override fun onError(e: CometChatException) {}
            })
        }
        darkMode.setOnClickListener { toggleDarkMode() }
        lightMode.setOnClickListener { toggleDarkMode() }
    }

    private fun setUpUI() {
        if (isNightMode(this)) {
            changeIconTintToWhite(this, darkMode)
            changeIconTintToWhite(this, lightMode)
            changeIconTintToWhite(this, logout)
            Utils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.app_background_dark))
            parentView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.app_background_dark
                )
            )
            darkMode.setVisibility(View.GONE)
            lightMode.setVisibility(View.VISIBLE)
        } else {
            changeIconTintToBlack(this, darkMode)
            changeIconTintToBlack(this, lightMode)
            changeIconTintToBlack(this, logout)
            Utils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.app_background))
            parentView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.app_background
                )
            )
            darkMode.setVisibility(View.VISIBLE)
            lightMode.setVisibility(View.GONE)
        }
    }

    private fun toggleDarkMode() {
        if (isNightMode(this)) {
            Palette.getInstance().mode(CometChatTheme.MODE.LIGHT)
            switchLightMode()
        } else {
            Palette.getInstance().mode(CometChatTheme.MODE.DARK)
            switchDarkMode()
        }
    }

    private fun handleIntent(module: String) {
        val intent = Intent(this, ComponentListActivity::class.java)
        intent.putExtra(StringConstants.MODULE, module)
        startActivity(intent)
    }
}