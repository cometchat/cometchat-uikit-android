package com.cometchat.chatuikit.shared.interfaces;

import com.cometchat.chat.models.BaseMessage;

public interface OnReactionClickListener {
    void onClick(String reaction);

    void onClick(BaseMessage baseMessage, String reaction);
}
