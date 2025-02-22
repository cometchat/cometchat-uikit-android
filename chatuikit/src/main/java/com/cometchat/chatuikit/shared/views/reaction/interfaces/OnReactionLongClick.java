package com.cometchat.chatuikit.shared.views.reaction.interfaces;

import com.cometchat.chat.models.BaseMessage;

public interface OnReactionLongClick {
    void onReactionLongClick(String emoji, BaseMessage baseMessage);
}
