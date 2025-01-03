package com.cometchat.chatuikit.shared.interfaces;

import android.content.Context;

import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.models.CometChatDetailsOption;

public interface OnDetailOptionClick {
    void onClick(User user, Group group, String templateId, CometChatDetailsOption option, Context context);
}
