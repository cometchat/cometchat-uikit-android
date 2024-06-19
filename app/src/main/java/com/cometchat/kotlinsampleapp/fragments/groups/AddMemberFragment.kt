package com.cometchat.kotlinsampleapp.fragments.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.addmembers.CometChatAddMembers
import com.cometchat.kotlinsampleapp.AppUtils.Companion.defaultGroup
import com.cometchat.kotlinsampleapp.R

class AddMemberFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_add_member, container, false)
        val addMembers = view.findViewById<CometChatAddMembers>(R.id.add_members)
        addMembers.setGroup(defaultGroup)
        return view
    }
}