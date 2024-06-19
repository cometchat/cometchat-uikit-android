package com.cometchat.kotlinsampleapp.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.User
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.kotlinsampleapp.AppUtils.Companion.changeTextColorToBlack
import com.cometchat.kotlinsampleapp.AppUtils.Companion.changeTextColorToWhite
import com.cometchat.kotlinsampleapp.AppUtils.Companion.fetchDefaultObjects
import com.cometchat.kotlinsampleapp.AppUtils.Companion.isNightMode
import com.cometchat.kotlinsampleapp.R
import com.cometchat.kotlinsampleapp.databinding.ActivityLoginBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var inputLayout: TextInputLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var uid: TextInputEditText
    private lateinit var parentView: RelativeLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        uid = binding.etUID
        progressBar = binding.loginProgress
        inputLayout = binding.inputUID
        parentView = binding.parentView
        uid.setOnEditorActionListener { textView: TextView?, i: Int, keyEvent: KeyEvent? ->
            if (i == EditorInfo.IME_ACTION_DONE) {
                if (uid.getText().toString().isEmpty()) {
                    uid.error = "Enter UID"
                } else {
                    uid.error = null
                    progressBar.visibility = View.VISIBLE
                    inputLayout.isEndIconVisible = false
                    login(uid.getText().toString())
                }
            }
            true
        }
        binding.tvSignIn.setOnClickListener {
            if (uid.text.toString().isEmpty()) {
                uid.error = "Enter UID"
            } else {
                progressBar.visibility = View.VISIBLE
                inputLayout.isEndIconVisible = false
                login(uid.getText().toString())
            }
        }
        setUpUI()
    }

    private fun login(uid: String) {
        CometChatUIKit.login(uid, object : CometChat.CallbackListener<User?>() {
            override fun onSuccess(user: User?) {
                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                fetchDefaultObjects()
                finishAffinity()
            }

            override fun onError(e: CometChatException) {
                inputLayout.isEndIconVisible = true
                progressBar.visibility = View.GONE
                Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun createUser() {
        startActivity(Intent(this@LoginActivity, CreateUserActivity::class.java))
    }

    private fun setUpUI() {
        if (isNightMode(this)) {
            changeTextColorToWhite(this, binding.tvTitle)
            changeTextColorToWhite(this, binding.tvDes2)
            inputLayout.editText!!.setTextColor(ContextCompat.getColor(this, R.color.white))
            parentView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.app_background_dark
                )
            )
        } else {
            changeTextColorToBlack(this, binding.tvTitle)
            changeTextColorToBlack(this, binding.tvDes2)
            parentView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.app_background
                )
            )
        }
    }
}