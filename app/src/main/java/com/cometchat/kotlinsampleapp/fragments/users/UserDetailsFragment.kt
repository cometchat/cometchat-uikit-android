package com.cometchat.kotlinsampleapp.fragments.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.details.CometChatDetails
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.kotlinsampleapp.R

class UserDetailsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_user_details, container, false)
        val cometChatDetails = view.findViewById<CometChatDetails>(R.id.users_details)
        cometChatDetails.setUser(CometChatUIKit.getLoggedInUser())
        return view
    }
}