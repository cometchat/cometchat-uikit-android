package com.cometchat.chatuikit.shared.models;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;

public interface MessageTemplateCallBack {

    View createView(Context context, UIKitConstants.MessageBubbleAlignment alignment);

    void bindView(Context context, View view, RecyclerView.ViewHolder holder, BaseMessage baseMessage, UIKitConstants.MessageBubbleAlignment alignment, int position);
}
