package com.cometchat.kotlinsampleapp.fragments.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.groupmembers.CometChatGroupMembers
import com.cometchat.kotlinsampleapp.AppUtils.Companion.defaultGroup
import com.cometchat.kotlinsampleapp.R

class GroupMembersFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_group_members, container, false)
        val groupMembers = view.findViewById<CometChatGroupMembers>(R.id.members)
        groupMembers.setGroup(defaultGroup)
        return view
    }
}