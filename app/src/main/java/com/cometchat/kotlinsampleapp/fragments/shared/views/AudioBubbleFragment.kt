package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.resources.utils.AudioPlayer
import com.cometchat.chatuikit.shared.views.CometChatAudioBubble.AudioBubbleStyle
import com.cometchat.chatuikit.shared.views.CometChatAudioBubble.CometChatAudioBubble
import com.cometchat.kotlinsampleapp.R

class AudioBubbleFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View =
            inflater.inflate(R.layout.fragment_audio_bubble, container, false)
        val cometChatAudioBubble = view.findViewById<CometChatAudioBubble>(R.id.audio_bubble)
        cometChatAudioBubble.setAudioUrl(
            "https://data-us.cometchat.io/2379614bd4db65dd/media/1682517916_1406731591_130612180fb2e657699814eb52817574.mp3",
            "SoundHelix",
            "Song"
        )
        cometChatAudioBubble.setStyle(
            AudioBubbleStyle().setBackground(
                CometChatTheme.getInstance().palette.getAccent100(context)
            ).setCornerRadius(18f)
        )
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioPlayer.getAudioPlayer().stopPlayer()
    }
}