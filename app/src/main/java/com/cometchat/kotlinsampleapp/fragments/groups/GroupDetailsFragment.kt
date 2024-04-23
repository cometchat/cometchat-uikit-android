package com.cometchat.kotlinsampleapp.fragments.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.details.CometChatDetails
import com.cometchat.kotlinsampleapp.AppUtils
import com.cometchat.kotlinsampleapp.R

class GroupDetailsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_group_details, container, false)
        val details = view.findViewById<CometChatDetails>(R.id.group_details)
        details.setGroup(AppUtils.defaultGroup)
        return view
    }
}