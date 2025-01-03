package com.cometchat.chatuikit.shared.interfaces;

import android.content.Context;

import com.cometchat.calls.exceptions.CometChatException;

public interface OnCallError {
    void onError(Context context, CometChatException cometchatException);
}
