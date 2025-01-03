package com.cometchat.chatuikit.shared.formatters;

import android.content.Context;
import android.text.SpannableStringBuilder;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;

import java.util.List;

public class FormatterUtils {
    private static final String TAG = FormatterUtils.class.getSimpleName();


    public static SpannableStringBuilder getFormattedText(Context context, BaseMessage baseMessage, UIKitConstants.FormattingType formattingType, UIKitConstants.MessageBubbleAlignment alignment, String text, List<CometChatTextFormatter> formatters) {
        SpannableStringBuilder spannableString = new SpannableStringBuilder(text);
        for (CometChatTextFormatter textFormatter : formatters) {
            spannableString = textFormatter.prepareMessageString(context, baseMessage, spannableString, alignment, formattingType);
        }
        return spannableString;
    }
}
