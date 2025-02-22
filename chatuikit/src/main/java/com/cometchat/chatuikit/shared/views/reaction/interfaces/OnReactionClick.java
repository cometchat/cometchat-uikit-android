package com.cometchat.chatuikit.shared.views.reaction.interfaces;

import com.cometchat.chat.models.BaseMessage;

public interface OnReactionClick {
    void onClick(String emoji, BaseMessage baseMessage);
}
