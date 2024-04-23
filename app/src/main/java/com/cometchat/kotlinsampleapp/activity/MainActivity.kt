package com.cometchat.kotlinsampleapp.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.chatuikit.shared.cometchatuikit.UIKitSettings.UIKitSettingsBuilder
import com.cometchat.chatuikit.shared.resources.utils.Utils
import com.cometchat.kotlinsampleapp.AppConstants
import com.cometchat.kotlinsampleapp.AppUtils.fetchDefaultObjects
import com.cometchat.kotlinsampleapp.AppUtils.isNightMode
import com.cometchat.kotlinsampleapp.BuildConfig
import com.cometchat.kotlinsampleapp.R
import com.cometchat.kotlinsampleapp.databinding.ActivityMainBinding
import com.google.android.material.card.MaterialCardView
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var superhero1: MaterialCardView
    private lateinit var superhero2: MaterialCardView
    private lateinit var superhero3: MaterialCardView
    private lateinit var superhero4: MaterialCardView
    private lateinit var ivLogo: AppCompatImageView
    private lateinit var tvCometChat: AppCompatTextView
    private lateinit var parentView: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        parentView = binding.parentView
        Utils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.white))
        val uiKitSettings =
            UIKitSettingsBuilder().setRegion(AppConstants.REGION).setAppId(AppConstants.APP_ID)
                .setAuthKey(AppConstants.AUTH_KEY).subscribePresenceForAllUsers().build()
        CometChatUIKit.init(this, uiKitSettings, object : CometChat.CallbackListener<String?>() {
            override fun onSuccess(s: String?) {
                CometChat.setDemoMetaInfo(appMetadata)
                if (CometChatUIKit.getLoggedInUser() != null) {
                    fetchDefaultObjects()
                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    finish()
                }
            }

            override fun onError(e: CometChatException) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        })
        superhero1 = binding.superhero1
        superhero2 = binding.superhero2
        superhero3 = binding.superhero3
        superhero4 = binding.superhero4
        ivLogo = binding.ivLogo
        tvCometChat = binding.tvComet

        binding.login.setOnClickListener {
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        }

        superhero1.setOnClickListener {
            binding.superhero1Progressbar.visibility = View.VISIBLE
            login("superhero1")
        }

        superhero2.setOnClickListener {
            binding.superhero2Progressbar.visibility = View.VISIBLE
            login("superhero2")
        }

        superhero3.setOnClickListener {
            binding.superhero3Progressbar.visibility = View.VISIBLE
            login("superhero3")
        }

        superhero4.setOnClickListener {
            binding.superhero4Progressbar.visibility = View.VISIBLE
            login("superhero4")
        }
        if (Utils.isDarkMode(this)) {
            ivLogo.setImageTintList(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        R.color.white
                    )
                )
            )
        } else {
            ivLogo.setImageTintList(
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        com.cometchat.chatuikit.R.color.cometchat_primary_text_color
                    )
                )
            )
        }
        setUpUI()
    }

    private fun login(uid: String) {
        CometChatUIKit.login(uid, object : CometChat.CallbackListener<User?>() {
            override fun onSuccess(user: User?) {
                fetchDefaultObjects()
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                finish()
            }

            override fun onError(e: CometChatException) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setUpUI() {
        if (isNightMode(this)) {
            Utils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.app_background_dark))
            parentView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.app_background_dark
                )
            )
            tvCometChat.setTextColor(ContextCompat.getColor(this, R.color.app_background))
        } else {
            Utils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.app_background))
            parentView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.app_background
                )
            )
            tvCometChat.setTextColor(ContextCompat.getColor(this, R.color.app_background_dark))
        }
    }

    private val appMetadata: JSONObject
        get() {
            val jsonObject = JSONObject()
            try {
                jsonObject.put("name", getResources().getString(R.string.app_name))
                jsonObject.put("type", "sample")
                jsonObject.put("version", BuildConfig.VERSION_NAME)
                jsonObject.put("bundle", BuildConfig.APPLICATION_ID)
                jsonObject.put("platform", "android")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return jsonObject
        }

    fun createUser(view: View?) {
        startActivity(Intent(this, CreateUserActivity::class.java))
    }
}