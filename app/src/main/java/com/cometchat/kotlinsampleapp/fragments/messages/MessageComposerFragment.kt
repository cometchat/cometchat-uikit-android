package com.cometchat.kotlinsampleapp.fragments.messages

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.messagecomposer.CometChatMessageComposer
import com.cometchat.kotlinsampleapp.AppUtils.Companion.defaultUser
import com.cometchat.kotlinsampleapp.AppUtils.Companion.isNightMode
import com.cometchat.kotlinsampleapp.R

class MessageComposerFragment : Fragment() {
    private var parentView: RelativeLayout? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_message_composer, container, false)
        parentView = view.findViewById(R.id.parent_view)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val cometChatMessageComposer = view.findViewById<CometChatMessageComposer>(R.id.composer)
        cometChatMessageComposer.user = defaultUser
        setUpUI(view)
        return view
    }

    private fun setUpUI(view: View) {
        if (isNightMode(requireContext())) {
            parentView!!.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.app_background_dark
                    )
            )
        } else {
            parentView!!.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.app_background))
        }
    }
}