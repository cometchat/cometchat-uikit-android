package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.views.CometChatTextBubble.CometChatTextBubble
import com.cometchat.chatuikit.shared.views.CometChatTextBubble.TextBubbleStyle
import com.cometchat.kotlinsampleapp.R

class TextBubbleFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_text_bubble, container, false)
        val cometChatTheme = CometChatTheme.getInstance()
        val receiverBubble = view.findViewById<CometChatTextBubble>(R.id.receiver_bubble)
        receiverBubble.setText("Hi John, How are you?")
        receiverBubble.setStyle(
            TextBubbleStyle().setBackground(
                cometChatTheme.palette.getAccent100(
                    context
                )
            ).setTextColor(cometChatTheme.palette.getAccent(context)).setCornerRadius(18f)
        )
        val senderBubble = view.findViewById<CometChatTextBubble>(R.id.sender_bubble)
        senderBubble.setText("Hey Jack,I am fine. How about you?")
        senderBubble.setStyle(
            TextBubbleStyle().setBackground(
                cometChatTheme.palette.getPrimary(
                    context
                )
            ).setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                .setCornerRadius(18f)
        )
        return view
    }
}