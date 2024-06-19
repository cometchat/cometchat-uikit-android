package com.cometchat.kotlinsampleapp.fragments.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.messages.CometChatMessages
import com.cometchat.kotlinsampleapp.AppUtils.Companion.defaultUser
import com.cometchat.kotlinsampleapp.R

class MessagesFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_messages, container, false)
        val messages = view.findViewById<CometChatMessages>(R.id.messages)
        messages.setUser(defaultUser)
        return view
    }
}