package com.cometchat.kotlinsampleapp.fragments.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.bannedmembers.CometChatBannedMembers
import com.cometchat.kotlinsampleapp.AppUtils.Companion.defaultGroup
import com.cometchat.kotlinsampleapp.R

class BannedMembersFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_banned_members, container, false)
        val bannedMembers = view.findViewById<CometChatBannedMembers>(R.id.banned_members)
        bannedMembers.setGroup(defaultGroup)
        return view
    }
}