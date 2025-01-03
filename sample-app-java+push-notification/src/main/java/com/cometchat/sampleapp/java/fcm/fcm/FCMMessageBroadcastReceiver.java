package com.cometchat.sampleapp.java.fcm.fcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.lifecycle.ViewModelProvider;

import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chat.models.TextMessage;
import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.sampleapp.java.fcm.ui.activity.SplashActivity;
import com.cometchat.sampleapp.java.fcm.utils.AppConstants;
import com.cometchat.sampleapp.java.fcm.viewmodels.SplashViewModel;
import com.google.gson.Gson;

public class FCMMessageBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "FCMMessageBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                CharSequence replyText = remoteInput.getCharSequence(AppConstants.FCMConstants.REPLY_FROM_NOTIFICATION);

                int notificationID = intent.getIntExtra(AppConstants.FCMConstants.KEY_CLICKED_NOTIFICATION_ID, -1);
                String data = intent.getStringExtra(AppConstants.FCMConstants.KEY_DATA);

                FCMMessageDTO fcmMessageDTO = new Gson().fromJson(data, FCMMessageDTO.class);
                if (replyText != null) {
                    initCometChatSDK(context, replyText.toString(), fcmMessageDTO, notificationID);
                }
            }
        }
    }

    private void initCometChatSDK(final Context context, final String message, final FCMMessageDTO fcmMessageDTO, final int notificationID) {
        SplashViewModel viewModel = new ViewModelProvider.NewInstanceFactory().create(SplashViewModel.class);
        viewModel.initUIKit(context, new CometChat.CallbackListener<String>() {
            @Override
            public void onSuccess(String s) {
                sendMessage(context, message, fcmMessageDTO, notificationID);
            }

            @Override
            public void onError(CometChatException e) {
                Toast.makeText(context, "Unable to initialize CometChat SDK!", Toast.LENGTH_SHORT).show();
                CometChatLogger.e(TAG, "CometChat SDK initialization failed: " + e.getMessage());
            }
        });
    }

    private void sendMessage(final Context context, final String message, final FCMMessageDTO fcmMessageDTO, final int notificationID) {
        TextMessage textMessage = new TextMessage(fcmMessageDTO
                                                      .getReceiverType()
                                                      .equals(CometChatConstants.RECEIVER_TYPE_USER) ? fcmMessageDTO.getSender() : fcmMessageDTO.getReceiver(),
                                                  message,
                                                  fcmMessageDTO
                                                      .getReceiverType()
                                                      .equals(CometChatConstants.RECEIVER_TYPE_USER) ? CometChatConstants.RECEIVER_TYPE_USER : CometChatConstants.RECEIVER_TYPE_GROUP);

        CometChat.sendMessage(textMessage, new CometChat.CallbackListener<TextMessage>() {
            @Override
            public void onSuccess(TextMessage textMessage) {
                if (fcmMessageDTO.getReceiverType().equals(CometChatConstants.RECEIVER_TYPE_USER)) {
                    String sentMsg = "You: " + textMessage.getText();
                    fcmMessageDTO.setText(sentMsg);
                } else {
                    fcmMessageDTO.setSenderName("You");
                    fcmMessageDTO.setText(textMessage.getText());
                }
                Intent onNotificationClickIntent = new Intent(context, SplashActivity.class);
                FCMMessageNotificationUtils.showNotification(
                    context,
                    fcmMessageDTO,
                    onNotificationClickIntent,
                    AppConstants.FCMConstants.NOTIFICATION_KEY_REPLY_ACTION,
                    NotificationCompat.CATEGORY_MESSAGE
                );
            }

            @Override
            public void onError(CometChatException e) {
                Toast.makeText(context, "Unable to send message!", Toast.LENGTH_SHORT).show();
                CometChatLogger.e(TAG, "Error sending message: " + e.getMessage());
            }
        });
    }

}
