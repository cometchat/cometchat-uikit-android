package com.cometchat.chatuikit.shared.formatters;

import android.content.Context;
import android.text.SpannableStringBuilder;

import androidx.lifecycle.MutableLiveData;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.views.suggestionlist.SuggestionItem;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An abstract base class to format text in the CometChat module. It provides
 * fundamental methods to handle, format, and track changes of the text. All the
 * concrete text formatter classes should extend this class and implement its
 * abstract methods.
 */
public abstract class CometChatTextFormatter implements Formatter {
    private static final String TAG = CometChatTextFormatter.class.getSimpleName();
    private final char trackingCharacter;
    private MutableLiveData<List<SuggestionItem>> suggestionItemList;
    private MutableLiveData<String> tagInfoMessage;
    private MutableLiveData<Boolean> tagInfoVisible;
    private List<SuggestionItem> selectedSuggestionItemList;
    private MutableLiveData<Boolean> showLoadingIndicator;
    private boolean disableSuggestions;
    private User user;
    private Group group;

    /**
     * Constructor to initialize the tracking character for this formatter.
     *
     * @param trackingCharacter the character for which suggestion search will be triggered.
     */
    public CometChatTextFormatter(char trackingCharacter) {
        this.trackingCharacter = trackingCharacter;
        init();
    }

    private void init() {
        suggestionItemList = new MutableLiveData<>();
        tagInfoMessage = new MutableLiveData<>();
        tagInfoVisible = new MutableLiveData<>();
        showLoadingIndicator = new MutableLiveData<>();
        selectedSuggestionItemList = new ArrayList<>();
    }

    /**
     * To set the flag if the suggestions should be disabled or not.
     *
     * @param disableSuggestions flag to indicate if suggestions should be disabled.
     */
    protected final void setDisableSuggestions(boolean disableSuggestions) {
        this.disableSuggestions = disableSuggestions;
    }

    /**
     * To set the list of suggestion items.
     *
     * @param suggestionItemList list of suggestion items.
     */
    public final void setSuggestionItemList(List<SuggestionItem> suggestionItemList) {
        Utils.runOnMainThread(() -> this.suggestionItemList.setValue(suggestionItemList));
    }

    /**
     * To show or hide the loading indicator.
     *
     * @param show flag to show or hide the loading indicator.
     */
    public final void setShowLoadingIndicator(boolean show) {
        Utils.runOnMainThread(() -> this.showLoadingIndicator.setValue(show));
    }

    /**
     * This method is used to search for suggestions based on the passed
     * queryString.
     *
     * @param context     the context in which this method is called.
     * @param queryString the query string for searching suggestions.
     */
    public abstract void search(@Nonnull Context context, @Nullable String queryString);

    /**
     * This method is called when an item on the suggestion list is clicked.
     *
     * @param context        the context in which this method is called.
     * @param suggestionItem the item that was clicked on the suggestion list.
     * @param user           the user who clicked the item. Will be null if a group clicked the
     *                       item.
     * @param group          the group that clicked the item. Will be null if a user clicked
     *                       the item.
     */
    public void onItemClick(@Nonnull Context context, @Nonnull SuggestionItem suggestionItem, @Nullable User user, @Nullable Group group) {
    }

    /**
     * Method called before the message is sent. Useful for any setup needed before
     * the message is sent.
     *
     * @param context     the context in which this method is called.
     * @param baseMessage the message to be sent.
     */
    public void handlePreMessageSend(@Nonnull Context context, @Nonnull BaseMessage baseMessage) {
    }

    /**
     * This method is called whenever the user scrolls to the bottom of the
     * suggestion list.
     */
    public abstract void onScrollToBottom();

    /**
     * Method to prepare the text of the message.
     *
     * @param context                The context in which this method is called.
     * @param baseMessage            The message to be formatted.
     * @param spannable              The SpannableStringBuilder containing the text to be formatted.
     * @param messageBubbleAlignment The alignment of the message bubble.
     * @param formattingType         The type of formatting to be applied.
     * @return The SpannableStringBuilder containing the formatted text.
     */
    public final @Nullable SpannableStringBuilder prepareMessageString(@Nonnull Context context, @Nonnull BaseMessage baseMessage, SpannableStringBuilder spannable, UIKitConstants.MessageBubbleAlignment messageBubbleAlignment, UIKitConstants.FormattingType formattingType) {
        if (UIKitConstants.FormattingType.MESSAGE_BUBBLE.equals(formattingType)) {
            if (UIKitConstants.MessageBubbleAlignment.RIGHT.equals(messageBubbleAlignment)) {
                return prepareRightMessageBubbleSpan(context, baseMessage, spannable);
            } else {
                return prepareLeftMessageBubbleSpan(context, baseMessage, spannable);
            }
        } else if (UIKitConstants.FormattingType.CONVERSATIONS.equals(formattingType)) {
            return prepareConversationSpan(context, baseMessage, spannable);
        } else {
            return prepareComposerSpan(context, baseMessage, spannable);
        }
    }

    /**
     * Method to prepare the text to be shown in the left message bubble.
     *
     * @param context     The context in which this method is called.
     * @param baseMessage The message to be formatted.
     * @param spannable   The SpannableStringBuilder containing the text to be formatted.
     * @return The SpannableStringBuilder containing the formatted text.
     */
    public @Nullable SpannableStringBuilder prepareLeftMessageBubbleSpan(@Nonnull Context context, @Nonnull BaseMessage baseMessage, SpannableStringBuilder spannable) {
        return spannable;
    }

    /**
     * Method to prepare the text to be shown in the right message bubble.
     *
     * @param context     The context in which this method is called.
     * @param baseMessage The message to be formatted.
     * @param spannable   The SpannableStringBuilder containing the text to be formatted.
     * @return The SpannableStringBuilder containing the formatted text.
     */
    public @Nullable SpannableStringBuilder prepareRightMessageBubbleSpan(@Nonnull Context context, @Nonnull BaseMessage baseMessage, SpannableStringBuilder spannable) {
        return spannable;
    }

    /**
     * Method to prepare the text to be shown in the composer.
     *
     * @param context     The context in which this method is called.
     * @param baseMessage The message to be formatted.
     * @param spannable   The SpannableStringBuilder containing the text to be formatted.
     * @return The SpannableStringBuilder containing the formatted text.
     */
    public @Nullable SpannableStringBuilder prepareComposerSpan(@Nonnull Context context, @Nonnull BaseMessage baseMessage, SpannableStringBuilder spannable) {
        return spannable;
    }

    /**
     * Method to prepare the text to be shown in the conversation view.
     *
     * @param context     The context in which this method is called.
     * @param baseMessage The message to be formatted.
     * @param spannable   The SpannableStringBuilder containing the text to be formatted.
     * @return The SpannableStringBuilder containing the formatted text.
     */
    public @Nullable SpannableStringBuilder prepareConversationSpan(@Nonnull Context context, @Nonnull BaseMessage baseMessage, SpannableStringBuilder spannable) {
        return spannable;
    }

    public void observeSelectionList(@Nonnull Context context, @Nonnull List<SuggestionItem> selectedSuggestionItemList) {
    }

    /**
     * To set the selected list of suggestions.
     *
     * @param selectedSuggestionItemList the selected list of suggestions.
     */
    public final void setSelectedList(@Nonnull Context context, @Nonnull List<SuggestionItem> selectedSuggestionItemList) {
        this.selectedSuggestionItemList = selectedSuggestionItemList;
        observeSelectionList(context, selectedSuggestionItemList);
    }

    /**
     * To set the tag information message.
     *
     * @param tagInfoMessage the tag information message.
     */
    public final void setInfoText(@Nonnull String tagInfoMessage) {
        Utils.runOnMainThread(() -> this.tagInfoMessage.postValue(tagInfoMessage));
    }

    /**
     * To show or hide the tag information visibility.
     *
     * @param tagInfoVisible flag to show or hide the tag information.
     */
    public final void setInfoVisibility(boolean tagInfoVisible) {
        Utils.runOnMainThread(() -> this.tagInfoVisible.setValue(tagInfoVisible));
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Nonnull
    public List<SuggestionItem> getSelectedList() {
        return selectedSuggestionItemList;
    }

    public final MutableLiveData<List<SuggestionItem>> getSuggestionItemList() {
        return suggestionItemList;
    }

    public final MutableLiveData<String> getTagInfoMessage() {
        return tagInfoMessage;
    }

    public final MutableLiveData<Boolean> getTagInfoVisibility() {
        return tagInfoVisible;
    }

    public final MutableLiveData<Boolean> getShowLoadingIndicator() {
        return showLoadingIndicator;
    }

    public boolean getDisableSuggestions() {
        return disableSuggestions;
    }

    public final char getTrackingCharacter() {
        return trackingCharacter;
    }

    public final char getId() {
        return trackingCharacter;
    }

    public User getUser() {
        return user;
    }

    public Group getGroup() {
        return group;
    }
}
