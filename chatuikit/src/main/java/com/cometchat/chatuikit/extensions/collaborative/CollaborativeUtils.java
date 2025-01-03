package com.cometchat.chatuikit.extensions.collaborative;

import android.content.Context;
import android.view.View;

import androidx.annotation.StyleRes;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.extensions.Extensions;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.models.AdditionParameter;
import com.cometchat.chatuikit.shared.utils.MessageBubbleUtils;
import com.cometchat.chatuikit.shared.views.deletebubble.CometChatDeleteBubble;

public class CollaborativeUtils {
    private static final String TAG = CollaborativeUtils.class.getSimpleName();

    public static View getCollaborativeBubbleView(Context context, CollaborativeBoardBubbleConfiguration configuration, String title, String subTitle, String buttonText) {
        View view = View.inflate(context, R.layout.cometchat_collaborative_bubble_layout_container, null);
        CometChatCollaborativeBubble bubble = view.findViewById(R.id.cometchat_collaborative_board_bubble);
        bubble.setTitle(title);
        bubble.setSubTitle(subTitle);
        bubble.setButtonText(buttonText);
        if (configuration != null) {
            bubble.setButtonText(configuration.getButtonText());
            bubble.setTitle(configuration.getTitle());
            bubble.setSubTitle(configuration.getSubtitle());
            bubble.setStyle(configuration.getStyle());
        }
        MessageBubbleUtils.setDeletedMessageBubble(context, view);

        return view;
    }

    public static void bindWriteBordCollaborativeBubble(Context context, View view, @StyleRes int collaborativeBubbleStyle, BaseMessage message, AdditionParameter additionParameter) {
        CometChatCollaborativeBubble bubble = view.findViewById(R.id.cometchat_collaborative_board_bubble);
        CometChatDeleteBubble deletedBubble = view.findViewById(R.id.cometchat_delete_text_bubble);
        if (message.getDeletedAt() == 0) {
            bubble.setStyle(collaborativeBubbleStyle);
            deletedBubble.setVisibility(View.GONE);
            bubble.setVisibility(View.VISIBLE);
            bubble.setBoardUrl(Extensions.getWriteBoardUrl(message));
        } else {
            bubble.setVisibility(View.GONE);
            deletedBubble.setVisibility(View.VISIBLE);
            deletedBubble.setStyle(CometChatUIKit.getLoggedInUser().getUid().equals(message.getSender().getUid()) ? additionParameter.getOutgoingDeleteBubbleStyle() : additionParameter.getIncomingDeleteBubbleStyle());
        }
    }

    public static void bindWhiteBordCollaborativeBubble(Context context, View view, @StyleRes int collaborativeBubbleStyle, BaseMessage message, AdditionParameter additionParameter) {
        CometChatCollaborativeBubble bubble = view.findViewById(R.id.cometchat_collaborative_board_bubble);
        CometChatDeleteBubble deletedBubble = view.findViewById(R.id.cometchat_delete_text_bubble);
        if (message.getDeletedAt() == 0) {
            bubble.setStyle(collaborativeBubbleStyle);
            deletedBubble.setVisibility(View.GONE);
            bubble.setVisibility(View.VISIBLE);
            bubble.setBoardUrl(Extensions.getWhiteBoardUrl(message));
        } else {
            bubble.setVisibility(View.GONE);
            deletedBubble.setVisibility(View.VISIBLE);
            deletedBubble.setStyle(CometChatUIKit.getLoggedInUser().getUid().equals(message.getSender().getUid()) ? additionParameter.getOutgoingDeleteBubbleStyle() : additionParameter.getIncomingDeleteBubbleStyle());
        }
    }
}
