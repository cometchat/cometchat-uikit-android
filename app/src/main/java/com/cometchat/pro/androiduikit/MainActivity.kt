package com.cometchat.pro.androiduikit

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.cometchat.pro.androiduikit.constants.AppConfig
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.CometChat.CallbackListener
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.User
import com.cometchat.pro.uikit.ui_resources.utils.ErrorMessagesUtils
import com.cometchat.pro.uikit.ui_resources.utils.Utils
import com.google.android.material.card.MaterialCardView
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private lateinit var user1: MaterialCardView

    private lateinit var user2: MaterialCardView

    private lateinit var user3: MaterialCardView

    private lateinit var user4: MaterialCardView

    private lateinit var ivLogo: AppCompatImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCometChat: AppCompatTextView
    private lateinit var parentView: LinearLayout
    private lateinit var gridLayoutContainer: LinearLayout
    private lateinit var stateMessage: TextView
    private lateinit var stateLayout: LinearLayout
    private lateinit var user1Name: TextView
    private lateinit var user2Name: TextView
    private lateinit var user3Name: TextView
    private lateinit var user4Name: TextView
    private lateinit var user1Avatar: ImageView
    private lateinit var user2Avatar: ImageView
    private lateinit var user3Avatar: ImageView
    private lateinit var user4Avatar: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        parentView = findViewById(R.id.parent_view)
        progressBar = findViewById(R.id.progress_bar)
        stateMessage = findViewById(R.id.state_message)
        stateLayout = findViewById(R.id.state_layout)
        gridLayoutContainer = findViewById(R.id.grid_layout_container)
        user1 = findViewById(R.id.user1)
        user2 = findViewById(R.id.user2)
        user3 = findViewById(R.id.user3)
        user4 = findViewById(R.id.user4)
        ivLogo = findViewById(R.id.ivLogo)
        tvCometChat = findViewById(R.id.tvComet)
        user1Name = findViewById(R.id.user1_name)
        user2Name = findViewById(R.id.user2_name)
        user3Name = findViewById(R.id.user3_name)
        user4Name = findViewById(R.id.user4_name)
        user1Avatar = findViewById(R.id.user1_avatar_image)
        user2Avatar = findViewById(R.id.user2_avatar_image)
        user3Avatar = findViewById(R.id.user3_avatar_image)
        user4Avatar = findViewById(R.id.user4_avatar_image)

        user1.visibility = View.GONE
        user2.visibility = View.GONE
        user3.visibility = View.GONE
        user4.visibility = View.GONE

        gridLayoutContainer.visibility = View.INVISIBLE
        stateMessage.text = getString(R.string.please_wait)
        progressBar.visibility = View.VISIBLE

        findViewById<View>(R.id.login).setOnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    LoginActivity::class.java
                )
            )
        }

        AppUtils.fetchSampleUsers(object : CometChat.CallbackListener<List<User>>() {
            override fun onSuccess(users: List<User>) {
                if (users.isNotEmpty()) {
                    setUsers(users)
                } else {
                    stateLayout.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    stateMessage.text = getString(R.string.no_sample_users_available)
                }
            }

            override fun onError(e: CometChatException) {
                setUsers(AppUtils.processSampleUserList(AppUtils.loadJSONFromAsset(this@MainActivity)))
            }
        })

        user1.setOnClickListener {
            findViewById<View>(R.id.user1Progressbar).visibility = View.VISIBLE
            login(user1.tag.toString())
        }
        user2.setOnClickListener {
            findViewById<View>(R.id.user2Progressbar).visibility = View.VISIBLE
            login(user2.tag.toString())
        }
        user3.setOnClickListener {
            findViewById<View>(R.id.user3Progressbar).visibility = View.VISIBLE
            login(user3.tag.toString())
        }
        user4.setOnClickListener {
            findViewById<View>(R.id.user4Progressbar).visibility = View.VISIBLE
            login(user4.tag.toString())
        }

        if (Utils.isDarkMode(this)) {
            ivLogo.imageTintList =
                ColorStateList.valueOf(resources.getColor(R.color.textColorWhite))
        } else {
            ivLogo.imageTintList =
                ColorStateList.valueOf(resources.getColor(R.color.black))
        }
        setUpUI()

    }

    private fun setUsers(users: List<User>) {
        progressBar.visibility = View.GONE
        stateLayout.visibility = View.GONE
        gridLayoutContainer.visibility = View.VISIBLE
        for (i in users.indices) {
            if (i == 0) {
                user1Name.text = users[i].name
                Glide.with(this).load(users[i].avatar).error(R.drawable.ironman)
                    .into(user1Avatar)
                user1.tag = users[i].uid
                user1.visibility = View.VISIBLE
            } else if (i == 1) {
                user2Name.text = users[i].name
                Glide.with(this).load(users[i].avatar).error(R.drawable.captainamerica)
                    .into(user2Avatar)
                user2.tag = users[i].uid
                user2.visibility = View.VISIBLE
            } else if (i == 2) {
                user3Name.text = users[i].name
                Glide.with(this).load(users[i].avatar).error(R.drawable.spiderman)
                    .into(user3Avatar)
                user3.tag = users[i].uid
                user3.visibility = View.VISIBLE
            } else if (i == 3) {
                user4Name.text = users[i].name
                Glide.with(this).load(users[i].avatar).error(R.drawable.wolverine)
                    .into(user4Avatar)
                user4.tag = users[i].uid
                user4.visibility = View.VISIBLE
            }
        }
    }

    private fun setUpUI() {
        if (AppUtils.isNightMode(this)) {
            parentView.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.app_background_dark))
            tvCometChat.setTextColor(resources.getColor(R.color.app_background))
        } else {
            parentView.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.app_background))
            tvCometChat.setTextColor(resources.getColor(R.color.app_background_dark))
        }
    }

    private fun login(uid: String) {
        CometChat.login(uid, AppConfig.AppDetails.AUTH_KEY, object : CallbackListener<User?>() {
            override fun onSuccess(user: User?) {
                startActivity(Intent(this@MainActivity, SelectActivity::class.java))
                finish()
            }

            override fun onError(e: CometChatException) {
                val str = uid + "_progressbar"
                val id = resources.getIdentifier(str, "id", packageName)
                findViewById<View>(id).visibility = View.GONE
                ErrorMessagesUtils.cometChatErrorMessage(this@MainActivity, e.code)
            }
        })
    }

    fun createUser(view: View?) {
        startActivity(Intent(this, CreateUserActivity::class.java))
    }

    override fun onBackPressed() {
        super.onBackPressed()
        exitProcess(0)
    }
}