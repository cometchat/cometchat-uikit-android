package com.cometchat.kotlinsampleapp.fragments.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.TextMessage
import com.cometchat.chatuikit.messageinformation.CometChatMessageInformation
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.kotlinsampleapp.AppUtils.Companion.defaultUser
import com.cometchat.kotlinsampleapp.R

class MessageInformationFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_message_information, container, false)
        val messageInformation =
            view.findViewById<CometChatMessageInformation>(R.id.message_information)
        //It is necessary to set message object and Template before loading the component
        val textMessage = TextMessage(
            CometChatUIKit.getLoggedInUser().uid,
            "Hey Jack,I am fine. How about you?",
            CometChatConstants.RECEIVER_TYPE_USER
        )
        textMessage.readAt = System.currentTimeMillis() / 100
        textMessage.receiver = defaultUser
        textMessage.sender = CometChatUIKit.getLoggedInUser()
        messageInformation.message = textMessage
        messageInformation.template = CometChatUIKit.getDataSource().textTemplate
        return view
    }
}