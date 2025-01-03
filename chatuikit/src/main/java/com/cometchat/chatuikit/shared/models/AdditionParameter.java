package com.cometchat.chatuikit.shared.models;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.formatters.CometChatTextFormatter;

import java.util.List;

public class AdditionParameter {
    private static final String TAG = AdditionParameter.class.getSimpleName();


    private List<CometChatTextFormatter> textFormatters;

    private @StyleRes int incomingDeleteBubbleStyle;
    private @StyleRes int outgoingDeleteBubbleStyle;

    private @StyleRes int incomingTextBubbleStyle;
    private @StyleRes int outgoingTextBubbleStyle;

    private @StyleRes int incomingImageBubbleStyle;
    private @StyleRes int outgoingImageBubbleStyle;

    private @StyleRes int incomingFileBubbleStyle;
    private @StyleRes int outgoingFileBubbleStyle;

    private @StyleRes int incomingAudioBubbleStyle;
    private @StyleRes int outgoingAudioBubbleStyle;

    private @StyleRes int incomingVideoBubbleStyle;
    private @StyleRes int outgoingVideoBubbleStyle;

    private @StyleRes int incomingCollaborativeBubbleStyle;
    private @StyleRes int outgoingCollaborativeBubbleStyle;

    private @StyleRes int incomingFormBubbleStyle;
    private @StyleRes int outgoingFormBubbleStyle;

    private @StyleRes int incomingPollBubbleStyle;
    private @StyleRes int outgoingPollBubbleStyle;

    private @StyleRes int incomingSchedulerBubbleStyle;
    private @StyleRes int outgoingSchedulerBubbleStyle;

    private @StyleRes int incomingCardBubbleStyle;
    private @StyleRes int outgoingCardBubbleStyle;

    private @StyleRes int incomingMeetCallBubbleStyle;
    private @StyleRes int outgoingMeetCallBubbleStyle;
    private @StyleRes int callButtonStyle;

    private @StyleRes int actionBubbleStyle;
    private @StyleRes int callActionBubbleStyle;

    private @StyleRes int aiConversationStarterStyle;
    private @StyleRes int aiSmartRepliesStyle;
    private @StyleRes int aiConversationSummaryStyle;

    private Drawable inactiveStickerIcon;
    private @ColorInt int inactiveAuxiliaryIconTint;
    private Drawable activeStickerIcon;
    private @ColorInt int activeAuxiliaryIconTint;

    public void setCallButtonStyle(@StyleRes int callButtonStyle) {
        this.callButtonStyle = callButtonStyle;
    }

    public void setAiConversationStarterStyle(@StyleRes int aiConversationStarterStyle) {
        this.aiConversationStarterStyle = aiConversationStarterStyle;
    }

    public void setAiSmartRepliesStyle(@StyleRes int aiSmartRepliesStyle) {
        this.aiSmartRepliesStyle = aiSmartRepliesStyle;
    }

    public void setAiConversationSummaryStyle(@StyleRes int aiConversationSummaryStyle) {
        this.aiConversationSummaryStyle = aiConversationSummaryStyle;
    }

    public void setTextFormatters(List<CometChatTextFormatter> textFormatters) {
        this.textFormatters = textFormatters;
    }

    public void setInactiveStickerIcon(Drawable inactiveStickerIcon) {
        this.inactiveStickerIcon = inactiveStickerIcon;
    }

    public void setInactiveAuxiliaryIconTint(@ColorInt int inactiveAuxiliaryIconTint) {
        this.inactiveAuxiliaryIconTint = inactiveAuxiliaryIconTint;
    }

    public void setActiveStickerIcon(Drawable activeStickerIcon) {
        this.activeStickerIcon = activeStickerIcon;
    }

    public void setActiveAuxiliaryIconTint(@ColorInt int activeAuxiliaryIconTint) {
        this.activeAuxiliaryIconTint = activeAuxiliaryIconTint;
    }

    public void setCallActionBubbleStyle(@StyleRes int callActionBubbleStyle) {
        this.callActionBubbleStyle = callActionBubbleStyle;
    }

    public void setIncomingMeetCallBubbleStyle(@StyleRes int incomingMeetCallBubbleStyle) {
        this.incomingMeetCallBubbleStyle = incomingMeetCallBubbleStyle;
    }

    public void setOutgoingMeetCallBubbleStyle(@StyleRes int outgoingMeetCallBubbleStyle) {
        this.outgoingMeetCallBubbleStyle = outgoingMeetCallBubbleStyle;
    }

    public void setIncomingTextBubbleStyle(@StyleRes int incomingTextBubbleStyle) {
        this.incomingTextBubbleStyle = incomingTextBubbleStyle;
    }

    public void setOutgoingTextBubbleStyle(@StyleRes int outgoingTextBubbleStyle) {
        this.outgoingTextBubbleStyle = outgoingTextBubbleStyle;
    }

    public void setIncomingImageBubbleStyle(@StyleRes int incomingImageBubbleStyle) {
        this.incomingImageBubbleStyle = incomingImageBubbleStyle;
    }

    public void setOutgoingImageBubbleStyle(@StyleRes int outgoingImageBubbleStyle) {
        this.outgoingImageBubbleStyle = outgoingImageBubbleStyle;
    }

    public void setIncomingFileBubbleStyle(@StyleRes int incomingFileBubbleStyle) {
        this.incomingFileBubbleStyle = incomingFileBubbleStyle;
    }

    public void setOutgoingFileBubbleStyle(@StyleRes int outgoingFileBubbleStyle) {
        this.outgoingFileBubbleStyle = outgoingFileBubbleStyle;
    }

    public void setIncomingAudioBubbleStyle(@StyleRes int incomingAudioBubbleStyle) {
        this.incomingAudioBubbleStyle = incomingAudioBubbleStyle;
    }

    public void setOutgoingAudioBubbleStyle(@StyleRes int outgoingAudioBubbleStyle) {
        this.outgoingAudioBubbleStyle = outgoingAudioBubbleStyle;
    }

    public void setIncomingVideoBubbleStyle(@StyleRes int incomingVideoBubbleStyle) {
        this.incomingVideoBubbleStyle = incomingVideoBubbleStyle;
    }

    public void setOutgoingVideoBubbleStyle(@StyleRes int outgoingVideoBubbleStyle) {
        this.outgoingVideoBubbleStyle = outgoingVideoBubbleStyle;
    }

    public void setOutgoingCollaborativeBubbleStyle(@StyleRes int outgoingCollaborativeBubbleStyle) {
        this.outgoingCollaborativeBubbleStyle = outgoingCollaborativeBubbleStyle;
    }

    public void setIncomingFormBubbleStyle(@StyleRes int incomingFormBubbleStyle) {
        this.incomingFormBubbleStyle = incomingFormBubbleStyle;
    }

    public void setOutgoingFormBubbleStyle(@StyleRes int outgoingFormBubbleStyle) {
        this.outgoingFormBubbleStyle = outgoingFormBubbleStyle;
    }

    public void setIncomingPollBubbleStyle(@StyleRes int incomingPollBubbleStyle) {
        this.incomingPollBubbleStyle = incomingPollBubbleStyle;
    }

    public void setOutgoingPollBubbleStyle(@StyleRes int outgoingPollBubbleStyle) {
        this.outgoingPollBubbleStyle = outgoingPollBubbleStyle;
    }

    public void setIncomingSchedulerBubbleStyle(@StyleRes int incomingSchedulerBubbleStyle) {
        this.incomingSchedulerBubbleStyle = incomingSchedulerBubbleStyle;
    }

    public void setOutgoingSchedulerBubbleStyle(@StyleRes int outgoingSchedulerBubbleStyle) {
        this.outgoingSchedulerBubbleStyle = outgoingSchedulerBubbleStyle;
    }

    public void setIncomingCardBubbleStyle(@StyleRes int incomingCardBubbleStyle) {
        this.incomingCardBubbleStyle = incomingCardBubbleStyle;
    }

    public void setOutgoingCardBubbleStyle(@StyleRes int outgoingCardBubbleStyle) {
        this.outgoingCardBubbleStyle = outgoingCardBubbleStyle;
    }

    public void setIncomingCollaborativeBubbleStyle(@StyleRes int incomingCollaborativeBubbleStyle) {
        this.incomingCollaborativeBubbleStyle = incomingCollaborativeBubbleStyle;
    }

    public void setIncomingDeleteBubbleStyle(@StyleRes int incomingDeleteBubbleStyle) {
        this.incomingDeleteBubbleStyle = incomingDeleteBubbleStyle;
    }

    public void setOutgoingDeleteBubbleStyle(@StyleRes int outgoingDeleteBubbleStyle) {
        this.outgoingDeleteBubbleStyle = outgoingDeleteBubbleStyle;
    }

    public void setActionBubbleStyle(@StyleRes int actionBubbleStyle) {
        this.actionBubbleStyle = actionBubbleStyle;
    }

    public @StyleRes int getIncomingTextBubbleStyle() {
        return incomingTextBubbleStyle;
    }

    public @StyleRes int getOutgoingTextBubbleStyle() {
        return outgoingTextBubbleStyle;
    }

    public @StyleRes int getIncomingImageBubbleStyle() {
        return incomingImageBubbleStyle;
    }

    public @StyleRes int getOutgoingImageBubbleStyle() {
        return outgoingImageBubbleStyle;
    }

    public @StyleRes int getIncomingFileBubbleStyle() {
        return incomingFileBubbleStyle;
    }

    public @StyleRes int getOutgoingFileBubbleStyle() {
        return outgoingFileBubbleStyle;
    }

    public @StyleRes int getIncomingAudioBubbleStyle() {
        return incomingAudioBubbleStyle;
    }

    public @StyleRes int getOutgoingAudioBubbleStyle() {
        return outgoingAudioBubbleStyle;
    }

    public @StyleRes int getIncomingVideoBubbleStyle() {
        return incomingVideoBubbleStyle;
    }

    public @StyleRes int getOutgoingVideoBubbleStyle() {
        return outgoingVideoBubbleStyle;
    }

    public @StyleRes int getIncomingCollaborativeBubbleStyle() {
        return incomingCollaborativeBubbleStyle;
    }

    public @StyleRes int getOutgoingCollaborativeBubbleStyle() {
        return outgoingCollaborativeBubbleStyle;
    }

    public @StyleRes int getIncomingFormBubbleStyle() {
        return incomingFormBubbleStyle;
    }

    public @StyleRes int getOutgoingFormBubbleStyle() {
        return outgoingFormBubbleStyle;
    }

    public @StyleRes int getIncomingPollBubbleStyle() {
        return incomingPollBubbleStyle;
    }

    public @StyleRes int getOutgoingPollBubbleStyle() {
        return outgoingPollBubbleStyle;
    }

    public @StyleRes int getIncomingSchedulerBubbleStyle() {
        return incomingSchedulerBubbleStyle;
    }

    public @StyleRes int getOutgoingSchedulerBubbleStyle() {
        return outgoingSchedulerBubbleStyle;
    }

    public @StyleRes int getIncomingCardBubbleStyle() {
        return incomingCardBubbleStyle;
    }

    public @StyleRes int getOutgoingCardBubbleStyle() {
        return outgoingCardBubbleStyle;
    }

    public @StyleRes int getIncomingDeleteBubbleStyle() {
        return incomingDeleteBubbleStyle;
    }

    public @StyleRes int getOutgoingDeleteBubbleStyle() {
        return outgoingDeleteBubbleStyle;
    }

    public @StyleRes int getActionBubbleStyle() {
        return actionBubbleStyle;
    }

    public @StyleRes int getIncomingMeetCallBubbleStyle() {
        return incomingMeetCallBubbleStyle;
    }

    public @StyleRes int getCallButtonStyle() {
        return callButtonStyle;
    }

    public @StyleRes int getOutgoingMeetCallBubbleStyle() {
        return outgoingMeetCallBubbleStyle;
    }

    public Drawable getInactiveStickerIcon() {
        return inactiveStickerIcon;
    }

    public @ColorInt int getInactiveAuxiliaryIconTint() {
        return inactiveAuxiliaryIconTint;
    }

    public @StyleRes int getCallActionBubbleStyle() {
        return callActionBubbleStyle;
    }

    public Drawable getActiveStickerIcon() {
        return activeStickerIcon;
    }

    public @ColorInt int getActiveAuxiliaryIconTint() {
        return activeAuxiliaryIconTint;
    }

    public @StyleRes int getAiConversationStarterStyle() {
        return aiConversationStarterStyle;
    }

    public @StyleRes int getAiSmartRepliesStyle() {
        return aiSmartRepliesStyle;
    }

    public @StyleRes int getAiConversationSummaryStyle() {
        return aiConversationSummaryStyle;
    }

    public List<CometChatTextFormatter> getTextFormatters() {
        return textFormatters;
    }
}
