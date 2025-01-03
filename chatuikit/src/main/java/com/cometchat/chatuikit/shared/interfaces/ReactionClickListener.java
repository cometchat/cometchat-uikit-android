package com.cometchat.chatuikit.shared.interfaces;

import com.cometchat.chat.models.BaseMessage;

public interface ReactionClickListener {
    void onReactionClick(BaseMessage baseMessage, String reaction);
}
