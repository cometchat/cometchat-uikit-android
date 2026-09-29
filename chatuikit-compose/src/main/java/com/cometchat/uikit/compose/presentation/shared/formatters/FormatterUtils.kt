package com.cometchat.uikit.compose.presentation.shared.formatters

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.utils.CometChatLogger

/**
 * Utility class for formatting text with multiple formatters.
 * Applies a list of formatters sequentially to a given text.
 */
public object FormatterUtils {
    private const val TAG = "FormatterUtils"

    /**
     * Gets formatted text by applying a list of formatters to the input text.
     * Returns an AnnotatedString with styling applied for mentions and other formatted elements.
     *
     * @param context The Android context
     * @param baseMessage The base message to format
     * @param formattingType The type of formatting to apply
     * @param alignment The message bubble alignment
     * @param text The original text to format
     * @param formatters The list of formatters to apply sequentially
     * @return The formatted AnnotatedString after applying all formatters
     */
    public fun getFormattedText(
        context: Context,
        baseMessage: BaseMessage,
        formattingType: UIKitConstants.FormattingType,
        alignment: UIKitConstants.MessageBubbleAlignment,
        text: String,
        formatters: List<CometChatTextFormatter>
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString(text)

        CometChatLogger.d(TAG, "getFormattedText: mentionedUsers=${baseMessage.mentionedUsers?.map { it.uid }}")
        CometChatLogger.d(TAG, "getFormattedText: formatters count=${formatters.size}")

        var formattedText: AnnotatedString = AnnotatedString(text)
        for (textFormatter in formatters) {
            CometChatLogger.d(TAG, "getFormattedText: applying formatter ${textFormatter.javaClass.simpleName}")
            formattedText = textFormatter.prepareMessageString(
                context,
                baseMessage,
                formattedText,
                alignment,
                formattingType
            )
        }

        return formattedText
    }
}
