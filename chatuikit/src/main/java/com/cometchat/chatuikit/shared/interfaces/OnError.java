package com.cometchat.chatuikit.shared.interfaces;

import android.content.Context;

import com.cometchat.chat.exceptions.CometChatException;

public interface OnError {
    void onError(Context context, CometChatException cometchatException);
}
