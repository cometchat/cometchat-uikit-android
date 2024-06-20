package com.cometchat.pro.androiduikit

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.cometchat.pro.androiduikit.constants.StringConstants
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.User

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class AppUtils {
    companion object {
        private var userList: MutableList<User> = ArrayList()

        fun fetchSampleUsers(listener: CometChat.CallbackListener<List<User>>) {
            if (userList.isEmpty()) {
                val request: Request =
                    Request.Builder().url(StringConstants.SAMPLE_APP_USERS_URL)
                        .method("GET", null).build()
                val client = OkHttpClient()
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnMainThread {
                            listener.onError(
                                CometChatException("11", e.message)
                            )
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful && response.body != null) {
                            try {
                                userList = processSampleUserList(response.body!!.string())
                            } catch (e: IOException) {
                                runOnMainThread {
                                    listener.onError(
                                        CometChatException("10", e.message)
                                    )
                                }
                            }
                            runOnMainThread {
                                listener.onSuccess(
                                    userList
                                )
                            }
                        } else {
                            runOnMainThread {
                                listener.onError(
                                    CometChatException(
                                        "Unexpected code ",
                                        response.code.toString()
                                    )
                                )
                            }
                        }
                    }
                })
            } else {
                runOnMainThread {
                    listener.onSuccess(
                        userList
                    )
                }
            }
        }

        fun runOnMainThread(runnable: Runnable?) {
            val mainThread = Handler(Looper.getMainLooper())
            mainThread.post(runnable!!)
        }
        fun processSampleUserList(jsonString: String?): MutableList<User> {
            val users: MutableList<User> = java.util.ArrayList()
            try {
                val jsonObject = JSONObject(jsonString)
                val jsonArray = jsonObject.getJSONArray(StringConstants.KEY_USER)
                for (i in 0 until jsonArray.length()) {
                    val userJson = jsonArray.getJSONObject(i)
                    val user = User()
                    user.uid = userJson.getString(StringConstants.UID)
                    user.name = userJson.getString(StringConstants.NAME)
                    user.avatar = userJson.getString(StringConstants.AVATAR)
                    users.add(user)
                }
            } catch (ignore: java.lang.Exception) {
            }
            return users
        }
        fun loadJSONFromAsset(context: Context): String? {
            var json: String? = null
            try {
                val `is` = context.assets.open("SampleUsers.json")
                val size = `is`.available()
                val buffer = ByteArray(size)
                `is`.read(buffer)
                `is`.close()
                json = String(buffer, charset("UTF-8"))
            } catch (ex: IOException) {
                ex.printStackTrace()
                return null
            }
            return json
        }
        val defaultUserList: List<User?>
            get() = userList

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
            imageView.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context!!,
                    R.color.textColorWhite
                )
            )
        }

        fun changeIconTintToBlack(context: Context?, imageView: ImageView) {
            imageView.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context!!,
                    R.color.black
                )
            )
        }

        fun changeTextColorToWhite(context: Context?, textView: TextView) {
            textView.setTextColor(ContextCompat.getColor(context!!, R.color.textColorWhite))
        }

        fun changeTextColorToBlack(context: Context?, textView: TextView) {
            textView.setTextColor(ContextCompat.getColor(context!!, R.color.black))
        }
    }
}
