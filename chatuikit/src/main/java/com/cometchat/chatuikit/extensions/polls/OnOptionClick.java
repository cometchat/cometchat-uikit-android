package com.cometchat.chatuikit.extensions.polls;

import com.cometchat.chat.models.CustomMessage;

public interface OnOptionClick {
    void onClick(CustomMessage baseMessage, String selectedOption, int position);
}
