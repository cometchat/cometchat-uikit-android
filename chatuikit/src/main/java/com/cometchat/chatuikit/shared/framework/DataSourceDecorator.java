package com.cometchat.chatuikit.shared.framework;

import android.content.Context;
import android.text.SpannableString;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Conversation;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.MediaMessage;
import com.cometchat.chat.models.TextMessage;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.ai.AIOptionsStyle;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.formatters.CometChatTextFormatter;
import com.cometchat.chatuikit.shared.models.AdditionParameter;
import com.cometchat.chatuikit.shared.models.CometChatMessageComposerAction;
import com.cometchat.chatuikit.shared.models.CometChatMessageOption;
import com.cometchat.chatuikit.shared.models.CometChatMessageTemplate;
import com.cometchat.chatuikit.shared.models.interactivemessage.CardMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.FormMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.SchedulerMessage;
import com.cometchat.chatuikit.shared.views.messagebubble.CometChatMessageBubble;

import java.util.HashMap;
import java.util.List;

public abstract class DataSourceDecorator implements DataSource {
    private static final String TAG = DataSourceDecorator.class.getSimpleName();
    private final DataSource dataSource;

    public DataSourceDecorator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns a list of message options for a text message.
     *
     * @param context     The context of the application.
     * @param baseMessage The base message object.
     * @param group       The group object associated with the message.
     * @return A list of CometChatMessageOption objects representing the available
     * options for the text message.
     */
    @Override
    public List<CometChatMessageOption> getTextMessageOptions(Context context, BaseMessage baseMessage, Group group) {
        return dataSource.getTextMessageOptions(context, baseMessage, group);
    }

    /**
     * Returns a list of message options for an image message.
     *
     * @param context     The context of the application.
     * @param baseMessage The base message object.
     * @param group       The group object associated with the message.
     * @return A list of CometChatMessageOption objects representing the available
     * options for the image message.
     */
    @Override
    public List<CometChatMessageOption> getImageMessageOptions(Context context, BaseMessage baseMessage, Group group) {
        return dataSource.getImageMessageOptions(context, baseMessage, group);
    }

    /**
     * Returns a list of message options for a video message.
     *
     * @param context     The context of the application.
     * @param baseMessage The base message object.
     * @param group       The group object associated with the message.
     * @return A list of CometChatMessageOption objects representing the available
     * options for the video message.
     */
    @Override
    public List<CometChatMessageOption> getVideoMessageOptions(Context context, BaseMessage baseMessage, Group group) {
        return dataSource.getVideoMessageOptions(context, baseMessage, group);
    }

    /**
     * Returns a list of message options for an audio message.
     *
     * @param context     The context of the application.
     * @param baseMessage The base message object.
     * @param group       The group object associated with the message.
     * @return A list of CometChatMessageOption objects representing the available
     * options for the audio message.
     */
    @Override
    public List<CometChatMessageOption> getAudioMessageOptions(Context context, BaseMessage baseMessage, Group group) {
        return dataSource.getAudioMessageOptions(context, baseMessage, group);
    }

    /**
     * Returns a list of message options for a file message.
     *
     * @param context     The context of the application.
     * @param baseMessage The base message object.
     * @param group       The group object associated with the message.
     * @return A list of CometChatMessageOption objects representing the available
     * options for the file message.
     */
    @Override
    public List<CometChatMessageOption> getFileMessageOptions(Context context, BaseMessage baseMessage, Group group) {
        return dataSource.getFileMessageOptions(context, baseMessage, group);
    }

    /**
     * @param context
     * @param messageBubble
     * @param alignment
     * @return
     */
    @Override
    public View getBottomView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
        return dataSource.getBottomView(context, messageBubble, alignment);
    }

    /**
     * @param context
     * @param createdView
     * @param message
     * @param alignment
     * @param holder
     * @param messageList
     * @param position
     * @param additionParameter
     */
    @Override
    public void bindBottomView(Context context,
                               View createdView,
                               BaseMessage message,
                               UIKitConstants.MessageBubbleAlignment alignment,
                               RecyclerView.ViewHolder holder,
                               List<BaseMessage> messageList,
                               int position,
                               AdditionParameter additionParameter) {
        dataSource.bindBottomView(context, createdView, message, alignment, holder, messageList, position, additionParameter);
    }

    /**
     * @param context
     * @param messageBubble
     * @param alignment
     * @return
     */
    @Override
    public View getTextBubbleContentView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
        return dataSource.getTextBubbleContentView(context, messageBubble, alignment);
    }

    /**
     * @param context
     * @param createdView
     * @param message
     * @param textBubbleStyle
     * @param alignment
     * @param holder
     * @param messageList
     * @param position
     * @param additionParameter
     */
    @Override
    public void bindTextBubbleContentView(Context context,
                                          View createdView,
                                          TextMessage message,
                                          int textBubbleStyle,
                                          UIKitConstants.MessageBubbleAlignment alignment,
                                          RecyclerView.ViewHolder holder,
                                          List<BaseMessage> messageList,
                                          int position,
                                          AdditionParameter additionParameter) {
        dataSource.bindTextBubbleContentView(context,
                                             createdView,
                                             message,
                                             textBubbleStyle,
                                             alignment,
                                             holder,
                                             messageList,
                                             position,
                                             additionParameter);
    }

    /**
     * @param context
     * @param messageBubble
     * @param alignment
     * @return
     */
    @Override
    public View getFormBubbleContentView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
        return dataSource.getFormBubbleContentView(context, messageBubble, alignment);
    }

    /**
     * @param context
     * @param createdView
     * @param message
     * @param formBubbleStyle
     * @param alignment
     * @param holder
     * @param messageList
     * @param position
     * @param additionParameter
     */
    @Override
    public void bindFormBubbleContentView(Context context,
                                          View createdView,
                                          FormMessage message,
                                          int formBubbleStyle,
                                          UIKitConstants.MessageBubbleAlignment alignment,
                                          RecyclerView.ViewHolder holder,
                                          List<BaseMessage> messageList,
                                          int position,
                                          AdditionParameter additionParameter) {
        dataSource.bindFormBubbleContentView(context,
                                             createdView,
                                             message,
                                             formBubbleStyle,
                                             alignment,
                                             holder,
                                             messageList,
                                             position,
                                             additionParameter);
    }

    /**
     * @param context
     * @param messageBubble
     * @param alignment
     * @return
     */
    @Override
    public View getSchedulerBubbleContentView(Context context,
                                              CometChatMessageBubble messageBubble,
                                              UIKitConstants.MessageBubbleAlignment alignment) {
        return dataSource.getSchedulerBubbleContentView(context, messageBubble, alignment);
    }

    /**
     * @param context
     * @param createdView
     * @param message
     * @param schedulerBubbleStyle
     * @param alignment
     * @param holder
     * @param messageList
     * @param position
     * @param additionParameter
     */
    @Override
    public void bindSchedulerBubbleContentView(Context context,
                                               View createdView,
                                               SchedulerMessage message,
                                               int schedulerBubbleStyle,
                                               UIKitConstants.MessageBubbleAlignment alignment,
                                               RecyclerView.ViewHolder holder,
                                               List<BaseMessage> messageList,
                                               int position,
                                               AdditionParameter additionParameter) {
        dataSource.bindSchedulerBubbleContentView(context,
                                                  createdView,
                                                  message,
                                                  schedulerBubbleStyle,
                                                  alignment,
                                                  holder,
                                                  messageList,
                                                  position,
                                                  additionParameter);
    }

    /**
     * @param context
     * @param messageBubble
     * @param alignment
     * @return
     */
    @Override
    public View getCardBubbleContentView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
        return dataSource.getCardBubbleContentView(context, messageBubble, alignment);
    }

    /**
     * @param context
     * @param createdView
     * @param message
     * @param cardBubbleStyle
     * @param alignment
     * @param holder
     * @param messageList
     * @param position
     * @param additionParameter
     */
    @Override
    public void bindCardBubbleContentView(Context context,
                                          View createdView,
                                          CardMessage message,
                                          int cardBubbleStyle,
                                          UIKitConstants.MessageBubbleAlignment alignment,
                                          RecyclerView.ViewHolder holder,
                                          List<BaseMessage> messageList,
                                          int position,
                                          AdditionParameter additionParameter) {
        dataSource.bindCardBubbleContentView(context,
                                             createdView,
                                             message,
                                             cardBubbleStyle,
                                             alignment,
                                             holder,
                                             messageList,
                                             position,
                                             additionParameter);
    }

    /**
     * @param context
     * @param messageBubble
     * @param alignment
     * @return
     */
    @Override
    public View getImageBubbleContentView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
        return dataSource.getImageBubbleContentView(context, messageBubble, alignment);
    }

    /**
     * @param context
     * @param createdView
     * @param imageUrl
     * @param message
     * @param imageBubbleStyle
     * @param alignment
     * @param holder
     * @param messageList
     * @param position
     * @param additionParameter
     */
    @Override
    public void bindImageBubbleContentView(Context context,
                                           View createdView,
                                           String imageUrl,
                                           MediaMessage message,
                                           int imageBubbleStyle,
                                           UIKitConstants.MessageBubbleAlignment alignment,
                                           RecyclerView.ViewHolder holder,
                                           List<BaseMessage> messageList,
                                           int position,
                                           AdditionParameter additionParameter) {
        dataSource.bindImageBubbleContentView(context,
                                              createdView,
                                              imageUrl,
                                              message,
                                              imageBubbleStyle,
                                              alignment,
                                              holder,
                                              messageList,
                                              position,
                                              additionParameter);
    }

    /**
     * @param context
     * @param messageBubble
     * @param alignment
     * @return
     */
    @Override
    public View getVideoBubbleContentView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
        return dataSource.getVideoBubbleContentView(context, messageBubble, alignment);
    }

    /**
     * @param context
     * @param createdView
     * @param thumbnailUrl
     * @param message
     * @param videoBubbleStyle
     * @param alignment
     * @param holder
     * @param messageList
     * @param position
     * @param additionParameter
     */
    @Override
    public void bindVideoBubbleContentView(Context context,
                                           View createdView,
                                           String thumbnailUrl,
                                           MediaMessage message,
                                           int videoBubbleStyle,
                                           UIKitConstants.MessageBubbleAlignment alignment,
                                           RecyclerView.ViewHolder holder,
                                           List<BaseMessage> messageList,
                                           int position,
                                           AdditionParameter additionParameter) {
        dataSource.bindVideoBubbleContentView(context,
                                              createdView,
                                              thumbnailUrl,
                                              message,
                                              videoBubbleStyle,
                                              alignment,
                                              holder,
                                              messageList,
                                              position,
                                              additionParameter);
    }

    /**
     * @param context
     * @param messageBubble
     * @param alignment
     * @return
     */
    @Override
    public View getFileBubbleContentView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
        return dataSource.getFileBubbleContentView(context, messageBubble, alignment);
    }

    /**
     * @param context
     * @param createdView
     * @param message
     * @param fileBubbleStyle
     * @param alignment
     * @param holder
     * @param messageList
     * @param position
     * @param additionParameter
     */
    @Override
    public void bindFileBubbleContentView(Context context,
                                          View createdView,
                                          MediaMessage message,
                                          int fileBubbleStyle,
                                          UIKitConstants.MessageBubbleAlignment alignment,
                                          RecyclerView.ViewHolder holder,
                                          List<BaseMessage> messageList,
                                          int position,
                                          AdditionParameter additionParameter) {
        dataSource.bindFileBubbleContentView(context,
                                             createdView,
                                             message,
                                             fileBubbleStyle,
                                             alignment,
                                             holder,
                                             messageList,
                                             position,
                                             additionParameter);
    }

    /**
     * @param context
     * @param messageBubble
     * @param alignment
     * @return
     */
    @Override
    public View getAudioBubbleContentView(Context context, CometChatMessageBubble messageBubble, UIKitConstants.MessageBubbleAlignment alignment) {
        return dataSource.getAudioBubbleContentView(context, messageBubble, alignment);
    }

    /**
     * @param context
     * @param createdView
     * @param message
     * @param audioBubbleStyle
     * @param alignment
     * @param holder
     * @param messageList
     * @param position
     * @param additionParameter
     */
    @Override
    public void bindAudioBubbleContentView(Context context,
                                           View createdView,
                                           MediaMessage message,
                                           int audioBubbleStyle,
                                           UIKitConstants.MessageBubbleAlignment alignment,
                                           RecyclerView.ViewHolder holder,
                                           List<BaseMessage> messageList,
                                           int position,
                                           AdditionParameter additionParameter) {
        dataSource.bindAudioBubbleContentView(context,
                                              createdView,
                                              message,
                                              audioBubbleStyle,
                                              alignment,
                                              holder,
                                              messageList,
                                              position,
                                              additionParameter);
    }

    /**
     * @param additionParameter
     * @return
     */
    @Override
    public CometChatMessageTemplate getAudioTemplate(AdditionParameter additionParameter) {
        return dataSource.getAudioTemplate(additionParameter);
    }

    /**
     * @param additionParameter
     * @return
     */
    @Override
    public CometChatMessageTemplate getVideoTemplate(AdditionParameter additionParameter) {
        return dataSource.getVideoTemplate(additionParameter);
    }

    /**
     * @param additionParameter
     * @return
     */
    @Override
    public CometChatMessageTemplate getImageTemplate(AdditionParameter additionParameter) {
        return dataSource.getImageTemplate(additionParameter);
    }

    /**
     * @param additionParameter
     * @return
     */
    @Override
    public CometChatMessageTemplate getGroupActionsTemplate(AdditionParameter additionParameter) {
        return dataSource.getGroupActionsTemplate(additionParameter);
    }

    /**
     * @param additionParameter
     * @return
     */
    @Override
    public CometChatMessageTemplate getFileTemplate(AdditionParameter additionParameter) {
        return dataSource.getFileTemplate(additionParameter);
    }

    /**
     * @param additionParameter
     * @return
     */
    @Override
    public CometChatMessageTemplate getTextTemplate(AdditionParameter additionParameter) {
        return dataSource.getTextTemplate(additionParameter);
    }

    /**
     * @param additionParameter
     * @return
     */
    @Override
    public CometChatMessageTemplate getFormTemplate(AdditionParameter additionParameter) {
        return dataSource.getFormTemplate(additionParameter);
    }

    /**
     * @param additionParameter
     * @return
     */
    @Override
    public CometChatMessageTemplate getSchedulerTemplate(AdditionParameter additionParameter) {
        return dataSource.getSchedulerTemplate(additionParameter);
    }

    /**
     * @param additionParameter
     * @return
     */
    @Override
    public CometChatMessageTemplate getCardTemplate(AdditionParameter additionParameter) {
        return dataSource.getCardTemplate(additionParameter);
    }

    /**
     * @param additionParameter
     * @return
     */
    @Override
    public List<CometChatMessageTemplate> getMessageTemplates(AdditionParameter additionParameter) {
        return dataSource.getMessageTemplates(additionParameter);
    }

    /**
     * Returns the message template for the specified category and type.
     *
     * @param category The category of the message.
     * @param type     The type of the message.
     * @return The CometChatMessageTemplate object representing the specified
     * message template.
     */
    @Override
    public CometChatMessageTemplate getMessageTemplate(String category, String type) {
        return dataSource.getMessageTemplate(category, type);
    }

    /**
     * Returns a list of message options for a message.
     *
     * @param context     The context of the application.
     * @param baseMessage The base message object.
     * @param group       The group object associated with the message.
     * @return A list of CometChatMessageOption objects representing the available
     * options for the message.
     */
    @Override
    public List<CometChatMessageOption> getMessageOptions(Context context, BaseMessage baseMessage, Group group) {
        return dataSource.getMessageOptions(context, baseMessage, group);
    }

    /**
     * Returns a list of attachment options for the message composer.
     *
     * @param context The context of the application.
     * @param user    The user object associated with the composer.
     * @param group   The group object associated with the composer.
     * @param idMap   The map of attachment IDs.
     * @return A list of CometChatMessageComposerAction objects representing the
     * available attachment options.
     */
    @Override
    public List<CometChatMessageComposerAction> getAttachmentOptions(Context context, User user, Group group, HashMap<String, String> idMap) {
        return dataSource.getAttachmentOptions(context, user, group, idMap);
    }

    /**
     * Returns a list of AI options for the message composer.
     *
     * @param context        The context of the application.
     * @param user           The user object associated with the composer.
     * @param group          The group object associated with the composer.
     * @param idMap          The map of attachment IDs.
     * @param aiOptionsStyle
     * @return A list of CometChatMessageComposerAction objects representing the
     * available AI options.
     */
    @Override
    public List<CometChatMessageComposerAction> getAIOptions(Context context,
                                                             User user,
                                                             Group group,
                                                             HashMap<String, String> idMap,
                                                             AIOptionsStyle aiOptionsStyle,
                                                             AdditionParameter additionParameter) {
        return dataSource.getAIOptions(context, user, group, idMap, aiOptionsStyle, additionParameter);
    }

    /**
     * Returns the auxiliary option view for the message composer.
     *
     * @param context The context of the application.
     * @param user    The user object associated with the composer.
     * @param group   The group object associated with the composer.
     * @param id      The ID of the auxiliary option.
     * @return The auxiliary option view.
     */
    @Override
    public View getAuxiliaryOption(Context context, User user, Group group, HashMap<String, String> id, AdditionParameter additionParameter) {
        return dataSource.getAuxiliaryOption(context, user, group, id, additionParameter);
    }

    /**
     * Returns a list of common options for the message.
     *
     * @param context     The context of the application.
     * @param baseMessage The base message object.
     * @param group       The group object associated with the message.
     * @return A list of CometChatMessageOption objects representing the common
     * options for the message.
     */
    @Override
    public List<CometChatMessageOption> getCommonOptions(Context context, BaseMessage baseMessage, Group group) {
        return dataSource.getCommonOptions(context, baseMessage, group);
    }

    /**
     * Returns a list of default message types.
     *
     * @return A list of default message types.
     */
    @Override
    public List<String> getDefaultMessageTypes() {
        return dataSource.getDefaultMessageTypes();
    }

    /**
     * Returns a list of default message categories.
     *
     * @return A list of default message categories.
     */
    @Override
    public List<String> getDefaultMessageCategories() {
        return dataSource.getDefaultMessageCategories();
    }

    /**
     * @param context
     * @param conversation
     * @param additionParameter
     * @return
     */
    @Override
    public SpannableString getLastConversationMessage(Context context, Conversation conversation, AdditionParameter additionParameter) {
        return dataSource.getLastConversationMessage(context, conversation, additionParameter);
    }

    /**
     * Returns the auxiliary header menu view for the user or group.
     *
     * @param context The context of the application.
     * @param user    The user object associated with the header menu.
     * @param group   The group object associated with the header menu.
     * @return The auxiliary header menu view.
     */
    @Override
    public View getAuxiliaryHeaderMenu(Context context, User user, Group group, AdditionParameter additionParameter) {
        return dataSource.getAuxiliaryHeaderMenu(context, user, group, additionParameter);
    }

    /**
     * @param context
     * @return
     */
    @Override
    public List<CometChatTextFormatter> getTextFormatters(Context context) {
        return dataSource.getTextFormatters(context);
    }
}
