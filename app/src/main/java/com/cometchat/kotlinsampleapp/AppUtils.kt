package com.cometchat.kotlinsampleapp

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User

object AppUtils {
    private var group: Group? = null
    private var user: User? = null
    fun fetchDefaultObjects() {
        CometChat.getGroup("supergroup", object : CometChat.CallbackListener<Group?>() {
            override fun onSuccess(group_: Group?) {
                group = group_
            }

            override fun onError(e: CometChatException?) {}
        })
        CometChat.getUser("superhero5", object : CometChat.CallbackListener<User?>() {
            override fun onSuccess(user_: User?) {
                user = user_
            }

            override fun onError(e: CometChatException?) {}
        })
    }

    val defaultGroup: Group?
        get() = group

    val defaultUser: User?
        get() = user

    fun switchLightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun switchDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    fun isNightMode(context: Context): Boolean {
        val currentNightMode =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    fun changeIconTintToWhite(context: Context?, imageView: ImageView) {
        imageView.setImageTintList(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context!!,
                    R.color.white
                )
            )
        )
    }

    fun changeIconTintToBlack(context: Context?, imageView: ImageView) {
        imageView.setImageTintList(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context!!,
                    R.color.black
                )
            )
        )
    }

    fun changeTextColorToWhite(context: Context?, textView: TextView) {
        textView.setTextColor(ContextCompat.getColor(context!!, R.color.white))
    }

    fun changeTextColorToBlack(context: Context?, textView: TextView) {
        textView.setTextColor(ContextCompat.getColor(context!!, R.color.black))
    }
}
