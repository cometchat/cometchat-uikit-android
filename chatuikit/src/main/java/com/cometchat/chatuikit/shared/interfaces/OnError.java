package com.cometchat.chatuikit.shared.interfaces;

import com.cometchat.chat.exceptions.CometChatException;

public interface OnError {
    void onError(CometChatException cometchatException);
}
