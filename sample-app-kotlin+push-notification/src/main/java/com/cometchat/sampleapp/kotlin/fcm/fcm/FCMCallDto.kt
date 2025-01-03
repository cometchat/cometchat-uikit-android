package com.cometchat.sampleapp.kotlin.fcm.fcm

import com.google.gson.annotations.SerializedName

class FCMCallDto {
    // Getters and Setters
    @SerializedName("conversationId")
    var conversationId: String? = null

    @SerializedName("sender")
    var sender: String? = null

    @SerializedName("receiver")
    var receiver: String? = null

    @SerializedName("receiverName")
    var receiverName: String? = null

    @SerializedName("receiverType")
    var receiverType: String? = null

    @SerializedName("receiverAvatar")
    var receiverAvatar: String? = null

    @SerializedName("callType")
    var callType: String? = null

    @SerializedName("tag")
    var tag: String? = null

    @SerializedName("body")
    var body: String? = null

    @SerializedName("type")
    var type: String? = null

    @SerializedName("title")
    var title: String? = null

    @SerializedName("senderAvatar")
    var senderAvatar: String? = null

    @SerializedName("senderName")
    var senderName: String? = null

    @SerializedName("callAction")
    var callAction: String? = null

    @SerializedName("sessionId")
    var sessionId: String? = null

    @SerializedName("sentAt")
    var sentAt: Long? = null
}
