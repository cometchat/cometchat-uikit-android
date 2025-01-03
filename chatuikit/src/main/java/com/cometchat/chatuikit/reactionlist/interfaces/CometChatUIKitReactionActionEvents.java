package com.cometchat.chatuikit.reactionlist.interfaces;

import android.content.Context;

import com.cometchat.chat.models.BaseMessage;

public interface CometChatUIKitReactionActionEvents {
    void onReactionAdded(Context context, BaseMessage baseMessage, String emoji);

    void onReactionRemoved(Context context, BaseMessage baseMessage, String emoji);

    void onReactionLongClick(Context context, BaseMessage baseMessage, String emoji);

    void onOpenMoreReactions(Context context, BaseMessage baseMessage);
}
