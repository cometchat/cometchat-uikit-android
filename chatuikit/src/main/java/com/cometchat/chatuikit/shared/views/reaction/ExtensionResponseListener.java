package com.cometchat.chatuikit.shared.views.reaction;

import com.cometchat.chat.exceptions.CometChatException;

public abstract class ExtensionResponseListener<T> {
    private static final String TAG = ExtensionResponseListener.class.getSimpleName();


    public abstract void OnResponseSuccess(T var);

    public abstract void OnResponseFailed(CometChatException e);
}
