package com.cometchat.sampleapp.kotlin.fcm.fcm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.lifecycle.ViewModelProvider
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.TextMessage
import com.cometchat.chatuikit.logger.CometChatLogger
import com.cometchat.sampleapp.kotlin.fcm.ui.activity.SplashActivity
import com.cometchat.sampleapp.kotlin.fcm.utils.AppConstants
import com.cometchat.sampleapp.kotlin.fcm.viewmodels.SplashViewModel
import com.google.gson.Gson

class FCMMessageBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {
        if (intent != null) {
            val remoteInput = RemoteInput.getResultsFromIntent(intent)
            if (remoteInput != null) {
                val replyText = remoteInput.getCharSequence(AppConstants.FCMConstants.REPLY_FROM_NOTIFICATION)

                val notificationID = intent.getIntExtra(AppConstants.FCMConstants.KEY_CLICKED_NOTIFICATION_ID, -1)
                val data = intent.getStringExtra(AppConstants.FCMConstants.KEY_DATA)

                val fcmMessageDTO = Gson().fromJson(
                    data, FCMMessageDTO::class.java
                )
                if (replyText != null) {
                    initCometChatSDK(context, replyText.toString(), fcmMessageDTO, notificationID)
                }
            }
        }
    }

    private fun initCometChatSDK(
        context: Context,
        message: String,
        fcmMessageDTO: FCMMessageDTO,
        notificationID: Int
    ) {

        val viewModel = ViewModelProvider
            .NewInstanceFactory()
            .create(
                SplashViewModel::class.java
            )
        viewModel.initUIKit(context, object : CometChat.CallbackListener<String>() {
            override fun onSuccess(s: String) {
                sendMessage(context, message, fcmMessageDTO, notificationID)
            }

            override fun onError(e: CometChatException) {
                Toast
                    .makeText(context, "Unable to initialize CometChat SDK!", Toast.LENGTH_SHORT)
                    .show()
                CometChatLogger.e(TAG, "CometChat SDK initialization failed: " + e.message)
            }
        })
    }

    private fun sendMessage(
        context: Context,
        message: String,
        fcmMessageDTO: FCMMessageDTO,
        notificationID: Int
    ) {
        val textMessage =
            (if (fcmMessageDTO.receiverType == CometChatConstants.RECEIVER_TYPE_USER) fcmMessageDTO.sender else fcmMessageDTO.receiver)?.let {
                TextMessage(
                    it,
                    message,
                    if (fcmMessageDTO.receiverType == CometChatConstants.RECEIVER_TYPE_USER) CometChatConstants.RECEIVER_TYPE_USER else CometChatConstants.RECEIVER_TYPE_GROUP
                )
            }

        textMessage?.let {
            CometChat.sendMessage(it, object : CometChat.CallbackListener<TextMessage>() {
                override fun onSuccess(textMessage: TextMessage) {
                    if (fcmMessageDTO.receiverType == CometChatConstants.RECEIVER_TYPE_USER) {
                        val sentMsg = "You: " + textMessage.text
                        fcmMessageDTO.text = sentMsg
                    } else {
                        fcmMessageDTO.senderName = "You"
                        fcmMessageDTO.text = textMessage.text
                    }
                    val onNotificationClickIntent = Intent(context, SplashActivity::class.java)
                    FCMMessageNotificationUtils.showNotification(
                        context,
                        fcmMessageDTO,
                        onNotificationClickIntent,
                        AppConstants.FCMConstants.NOTIFICATION_KEY_REPLY_ACTION,
                        NotificationCompat.CATEGORY_MESSAGE
                    )
                }

                override fun onError(e: CometChatException) {
                    Toast
                        .makeText(context, "Unable to send message!", Toast.LENGTH_SHORT)
                        .show()
                    CometChatLogger.e(TAG, "Error sending message: " + e.message)
                }
            })
        }
    }

    companion object {
        private const val TAG = "FCMMessageBroadcastReceiver"
    }
}
