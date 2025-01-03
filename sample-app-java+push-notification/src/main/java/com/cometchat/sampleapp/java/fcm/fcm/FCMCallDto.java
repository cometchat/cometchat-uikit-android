package com.cometchat.sampleapp.java.fcm.fcm;

import com.google.gson.annotations.SerializedName;

public class FCMCallDto {

    @SerializedName("conversationId")
    private String conversationId;

    @SerializedName("sender")
    private String sender;

    @SerializedName("receiver")
    private String receiver;

    @SerializedName("receiverName")
    private String receiverName;

    @SerializedName("receiverType")
    private String receiverType;

    @SerializedName("receiverAvatar")
    private String receiverAvatar;

    @SerializedName("callType")
    private String callType;

    @SerializedName("tag")
    private String tag;

    @SerializedName("body")
    private String body;

    @SerializedName("type")
    private String type;

    @SerializedName("title")
    private String title;

    @SerializedName("senderAvatar")
    private String senderAvatar;

    @SerializedName("senderName")
    private String senderName;

    @SerializedName("callAction")
    private String callAction;

    @SerializedName("sessionId")
    private String sessionId;

    @SerializedName("sentAt")
    private Long sentAt;

    // Getters and Setters
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }

    public String getReceiverAvatar() {
        return receiverAvatar;
    }

    public void setReceiverAvatar(String receiverAvatar) {
        this.receiverAvatar = receiverAvatar;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getCallAction() {
        return callAction;
    }

    public void setCallAction(String callAction) {
        this.callAction = callAction;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getSentAt() {
        return sentAt;
    }

    public void setSentAt(Long sentAt) {
        this.sentAt = sentAt;
    }
}
