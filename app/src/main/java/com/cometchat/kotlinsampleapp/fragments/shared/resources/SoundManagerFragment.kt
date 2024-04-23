package com.cometchat.kotlinsampleapp.fragments.shared.resources

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.shared.resources.soundManager.CometChatSoundManager
import com.cometchat.chatuikit.shared.resources.soundManager.Sound
import com.cometchat.kotlinsampleapp.AppUtils
import com.cometchat.kotlinsampleapp.R

class SoundManagerFragment : Fragment() {
    private var parentView: LinearLayout? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_sound_manager, container, false)
        parentView = view.findViewById(R.id.parent_view)
        setUpUI(view)
        val soundManager = CometChatSoundManager(activity)
        view.findViewById<View>(R.id.playIncoming).setOnClickListener {
            soundManager.play(Sound.incomingMessage) //To play incoming Messages sound
        }
        view.findViewById<View>(R.id.playOutgoing).setOnClickListener {
            soundManager.play(Sound.outgoingMessage) //To play outgoing Messages sound
        }
        return view
    }

    private fun setUpUI(view: View) {
        if (AppUtils.isNightMode(requireContext())) {
            AppUtils.changeTextColorToWhite(context, view.findViewById(R.id.soundManager_title))
            AppUtils.changeTextColorToWhite(
                context,
                view.findViewById(R.id.soundManager_description)
            )
            AppUtils.changeTextColorToWhite(context, view.findViewById(R.id.incoming_message_text))
            AppUtils.changeTextColorToWhite(context, view.findViewById(R.id.outgoing_message_text))
            parentView!!.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.app_background_dark
                    )
            )
        } else {
            AppUtils.changeTextColorToBlack(context, view.findViewById(R.id.soundManager_title))
            AppUtils.changeTextColorToBlack(
                context,
                view.findViewById(R.id.soundManager_description)
            )
            AppUtils.changeTextColorToBlack(context, view.findViewById(R.id.incoming_message_text))
            AppUtils.changeTextColorToBlack(context, view.findViewById(R.id.outgoing_message_text))
            parentView!!.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.app_background
                    )
            )
        }
    }
}