package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.views.mediarecorder.CometChatMediaRecorder
import com.cometchat.chatuikit.shared.views.mediarecorder.MediaRecorderStyle
import com.cometchat.kotlinsampleapp.R

class MediaRecorderFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_media_recorder, container, false)
        val mediaRecorder = view.findViewById<CometChatMediaRecorder>(R.id.recorder)
        val cometChatTheme = CometChatTheme.getInstance()
        mediaRecorder.setStyle(
            MediaRecorderStyle()
                .setBackground(cometChatTheme.palette.getBackground(context))
                .setBackground(cometChatTheme.palette.getBackground(context))
                .setRecordedContainerColor(cometChatTheme.palette.getAccent100(context))
                .setPlayIconTint(cometChatTheme.palette.getAccent(context))
                .setPauseIconTint(cometChatTheme.palette.getAccent(context))
                .setStopIconTint(cometChatTheme.palette.getError(context))
                .setVoiceRecordingIconTint(cometChatTheme.palette.getError(context))
                .setRecordingChunkColor(cometChatTheme.palette.getPrimary(context))
                .setTimerTextColor(cometChatTheme.palette.getAccent(context))
                .setTimerTextAppearance(cometChatTheme.typography.text1)
        )
        mediaRecorder.cardElevation = 10f
        mediaRecorder.radius = 16f
        return view
    }
}