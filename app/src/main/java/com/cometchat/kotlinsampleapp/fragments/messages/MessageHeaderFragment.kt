package com.cometchat.kotlinsampleapp.fragments.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.messageheader.CometChatMessageHeader
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.kotlinsampleapp.R

class MessageHeaderFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_message_header, container, false)
        val messagesHeader = view.findViewById<CometChatMessageHeader>(R.id.messageHeader)

        //set user Object to the MessageHeader
        messagesHeader.setUser(CometChatUIKit.getLoggedInUser())
        return view
    }
}