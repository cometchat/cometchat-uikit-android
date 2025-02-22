package com.cometchat.chatuikit.reactionlist;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Reaction;

public interface OnReactionListItemClick {
    void onItemClick(Reaction reaction, BaseMessage message);
}
