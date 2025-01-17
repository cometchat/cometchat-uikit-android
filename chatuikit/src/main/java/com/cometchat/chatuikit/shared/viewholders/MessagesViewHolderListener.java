package com.cometchat.chatuikit.shared.viewholders;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.interfaces.ViewHolderCallBack;
import com.cometchat.chatuikit.shared.views.messagebubble.CometChatMessageBubble;

import java.util.List;

public abstract class MessagesViewHolderListener implements ViewHolderCallBack {
    private static final String TAG = MessagesViewHolderListener.class.getSimpleName();

    public abstract View createView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment);

    public abstract void bindView(Context context,
                                  View createdView,
                                  BaseMessage message,
                                  UIKitConstants.MessageBubbleAlignment alignment,
                                  RecyclerView.ViewHolder holder,
                                  List<BaseMessage> messageList,
                                  int position);
}
