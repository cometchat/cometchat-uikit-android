package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.views.CometChatFileBubble.CometChatFileBubble
import com.cometchat.chatuikit.shared.views.CometChatFileBubble.FileBubbleStyle
import com.cometchat.kotlinsampleapp.R

class FileBubbleFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_file_bubble, container, false)
        val fileBubble = view.findViewById<CometChatFileBubble>(R.id.file_bubble)
        fileBubble.setFileUrl(
            "https://data-us.cometchat.io/2379614bd4db65dd/media/1682517934_233027292_069741a92a2f641eb428ba6d12ccb9af.pdf",
            "Sample",
            "pdf"
        )
        fileBubble.setStyle(
            FileBubbleStyle().setBackground(CometChatTheme.getInstance().palette.getAccent100(context))
                .setCornerRadius(18f)
        )
        fileBubble.title.setPadding(20, 20, 20, 10)
        fileBubble.subtitle.setPadding(20, 10, 2, 10)
        return view
    }
}