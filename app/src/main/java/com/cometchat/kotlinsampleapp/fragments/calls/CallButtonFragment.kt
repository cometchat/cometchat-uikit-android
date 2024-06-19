package com.cometchat.kotlinsampleapp.fragments.calls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.calls.callbutton.CometChatCallButtons
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.resources.utils.Utils
import com.cometchat.chatuikit.shared.views.button.ButtonStyle
import com.cometchat.kotlinsampleapp.AppUtils.Companion.defaultUser
import com.cometchat.kotlinsampleapp.R

class CallButtonFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_call_button, container, false)
        val cometChatCallButton = view.findViewById<CometChatCallButtons>(R.id.call_button)
        cometChatCallButton.videoCallButton.hideButtonBackground(true)
        cometChatCallButton.voiceCallButton.hideButtonBackground(true)
        cometChatCallButton.setVideoCallIcon(com.cometchat.chatuikit.R.drawable.cometchat_video_icon)
        cometChatCallButton.setVoiceCallIcon(com.cometchat.chatuikit.R.drawable.cometchat_call_icon)
        cometChatCallButton.hideButtonText(false)
        cometChatCallButton.setVideoButtonText("video call")
        cometChatCallButton.setVoiceButtonText("voice call")
        cometChatCallButton.setMarginForButtons(
            Utils.convertDpToPx(
                context, 1
            )
        )
        cometChatCallButton.setButtonStyle(
            ButtonStyle().setButtonSize(
                Utils.convertDpToPx(
                    context, 25
                ), Utils.convertDpToPx(context, 25)
            ).setButtonIconTint(
                CometChatTheme.getInstance().palette.getPrimary(context)
            )
        )
        cometChatCallButton.setUser(defaultUser)
        return view
    }
}