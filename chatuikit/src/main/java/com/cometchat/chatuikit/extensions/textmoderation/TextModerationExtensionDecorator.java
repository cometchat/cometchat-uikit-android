package com.cometchat.chatuikit.extensions.textmoderation;

import android.content.Context;
import android.text.SpannableString;
import android.view.View;

import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Conversation;
import com.cometchat.chat.models.TextMessage;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.extensions.Extensions;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.formatters.FormatterUtils;
import com.cometchat.chatuikit.shared.framework.DataSource;
import com.cometchat.chatuikit.shared.framework.DataSourceDecorator;
import com.cometchat.chatuikit.shared.models.AdditionParameter;
import com.cometchat.chatuikit.shared.resources.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class TextModerationExtensionDecorator extends DataSourceDecorator {
    private static final String TAG = TextModerationExtensionDecorator.class.getSimpleName();

    public TextModerationExtensionDecorator(DataSource dataSource) {
        super(dataSource);
    }

    public TextModerationExtensionDecorator(DataSource dataSource, @StyleRes int textBubbleStyle) {
        super(dataSource);
    }

    @Override
    public void bindTextBubbleContentView(Context context, View createdView, TextMessage message, @StyleRes int textBubbleStyle, UIKitConstants.MessageBubbleAlignment alignment, RecyclerView.ViewHolder holder, List<BaseMessage> messageList, int position, AdditionParameter additionParameter) {
        message.setText(getContentText(context, message));
        super.bindTextBubbleContentView(context, createdView, message, textBubbleStyle, alignment, holder, messageList, position, additionParameter);
    }

    public String getContentText(Context context, TextMessage textMessage) {
        String text = null;
        if (textMessage != null) {
            text = Extensions.checkProfanityMessage(context, textMessage);
            if (text != null && text.equalsIgnoreCase(textMessage.getText())) {
                text = Extensions.checkDataMasking(context, textMessage);
            }
        }
        return text == null ? "" : text;
    }

    @Override
    public SpannableString getLastConversationMessage(Context context, Conversation conversation, AdditionParameter additionParameter) {
        return getLastConversationMessage_(context, conversation, additionParameter);
    }

    public SpannableString getLastConversationMessage_(Context context, Conversation conversation, AdditionParameter additionParameter) {
        SpannableString lastMessageText;
        BaseMessage baseMessage = conversation.getLastMessage();
        if (baseMessage != null) {
            SpannableString message = getLastMessage(context, baseMessage);
            if (message != null) {
                lastMessageText = message;
            } else
                lastMessageText = super.getLastConversationMessage(context, conversation, additionParameter);
            if (baseMessage.getDeletedAt() > 0) {
                lastMessageText = SpannableString.valueOf(context.getString(R.string.cometchat_this_message_deleted));
            }
        } else {
            lastMessageText = SpannableString.valueOf(context.getResources().getString(R.string.cometchat_start_conv_hint));
        }
        return SpannableString.valueOf(FormatterUtils.getFormattedText(context, conversation.getLastMessage(), UIKitConstants.FormattingType.CONVERSATIONS, null, String.valueOf(lastMessageText), additionParameter != null && additionParameter.getTextFormatters() != null ? additionParameter.getTextFormatters() : new ArrayList<>()));
    }

    public SpannableString getLastMessage(Context context, BaseMessage lastMessage) {
        String message = null;
        if (UIKitConstants.MessageCategory.MESSAGE.equals(lastMessage.getCategory()) && UIKitConstants.MessageType.TEXT.equalsIgnoreCase(lastMessage.getType()))
            message = Utils.getMessagePrefix(lastMessage, context) + getContentText(context, (TextMessage) lastMessage);
        return message == null ? null : SpannableString.valueOf(message);
    }

    @Override
    public String getId() {
        return TextModerationExtensionDecorator.class.getSimpleName();
    }
}
