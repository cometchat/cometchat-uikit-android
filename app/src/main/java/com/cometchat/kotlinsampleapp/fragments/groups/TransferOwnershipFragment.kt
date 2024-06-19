package com.cometchat.kotlinsampleapp.fragments.groups

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.transferownership.CometChatTransferOwnership
import com.cometchat.kotlinsampleapp.AppUtils.Companion.defaultGroup
import com.cometchat.kotlinsampleapp.R

class TransferOwnershipFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_transfer_ownership, container, false)
        val transferOwnership =
            view.findViewById<CometChatTransferOwnership>(R.id.transfer_ownership)
        transferOwnership.setGroup(defaultGroup)
        return view
    }
}