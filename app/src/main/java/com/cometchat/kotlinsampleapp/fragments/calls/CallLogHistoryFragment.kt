package com.cometchat.kotlinsampleapp.fragments.calls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.chatuikit.calls.callhistory.CometChatCallLogHistory
import com.cometchat.kotlinsampleapp.R

class CallLogHistoryFragment : Fragment() {
    private var cometChatCallLogHistory: CometChatCallLogHistory? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_call_log_history, container, false)
        cometChatCallLogHistory = view.findViewById(R.id.call_logs_history)
        return view
    }
}