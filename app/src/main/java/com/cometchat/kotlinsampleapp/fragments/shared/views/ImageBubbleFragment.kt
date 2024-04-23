package com.cometchat.kotlinsampleapp.fragments.shared.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.resources.theme.CometChatTheme
import com.cometchat.chatuikit.shared.views.CometChatImageBubble.CometChatImageBubble
import com.cometchat.chatuikit.shared.views.CometChatImageBubble.ImageBubbleStyle
import com.cometchat.kotlinsampleapp.R

class ImageBubbleFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_image_bubble, container, false)
        val imageBubble = view.findViewById<CometChatImageBubble>(R.id.image_bubble)
        val cometChatTheme = CometChatTheme.getInstance()
        imageBubble.setImageUrl(
            "https://data-us.cometchat.io/2379614bd4db65dd/media/1682517838_2050398854_08d684e835e3c003f70f2478f937ed57.jpeg",
            R.drawable.ic_launcher_background,
            false
        )
        imageBubble.setStyle(
            ImageBubbleStyle().setCornerRadius(18f)
                .setTextColor(cometChatTheme.palette.getAccent(context))
                .setBackground(
                    ContextCompat.getColor(
                        requireContext(),
                        com.cometchat.chatuikit.R.color.cometchat_accent100
                    )
                )
        )
        imageBubble.setCaption("This is a simple representation of CometChat Image Bubble")
        return view
    }
}