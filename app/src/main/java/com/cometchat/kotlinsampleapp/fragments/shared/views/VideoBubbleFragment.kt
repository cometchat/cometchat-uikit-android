package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.views.CometChatVideoBubble.CometChatVideoBubble
import com.cometchat.chatuikit.shared.views.CometChatVideoBubble.VideoBubbleStyle
import com.cometchat.kotlinsampleapp.R

class VideoBubbleFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_video_bubble, container, false)
        val videoBubble = view.findViewById<CometChatVideoBubble>(R.id.video_bubble)
        videoBubble.setStyle(
            VideoBubbleStyle().setCornerRadius(18f).setBackground(
                ContextCompat.getColor(
                    requireContext(),
                    com.cometchat.chatuikit.R.color.cometchat_accent100
                )
            )
        )
        videoBubble.setVideoUrl(
            "https://data-us.cometchat.io/2379614bd4db65dd/media/1682517886_527585446_3e8e02fc506fa535eecfe0965e1a2024.mp4",
            R.drawable.ic_launcher_background
        )
        return view
    }
}