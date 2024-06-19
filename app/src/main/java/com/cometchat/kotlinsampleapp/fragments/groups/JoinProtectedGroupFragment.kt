package com.cometchat.kotlinsampleapp.fragments.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.joinprotectedgroup.CometChatJoinProtectedGroup
import com.cometchat.kotlinsampleapp.AppUtils.Companion.defaultGroup
import com.cometchat.kotlinsampleapp.R

class JoinProtectedGroupFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_join_protected_group, container, false)
        val joinProtectedGroup = view.findViewById<CometChatJoinProtectedGroup>(R.id.join_group)
        joinProtectedGroup.setGroup(defaultGroup)
        return view
    }
}