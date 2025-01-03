package com.cometchat.sampleapp.kotlin.fcm.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.CallUser
import com.cometchat.sampleapp.kotlin.fcm.databinding.FragmentCallDetailsTabBinding
import com.cometchat.sampleapp.kotlin.fcm.ui.adapters.CallDetailsTabParticipantsAdapter
import com.google.gson.Gson

class CallDetailsTabParticipantsFragment : Fragment() {

    private lateinit var binding: FragmentCallDetailsTabBinding
    private lateinit var callLog: CallLog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            callLog = Gson().fromJson(
                requireArguments().getString("callLog"), CallLog::class.java
            )
            callLog.initiator = Gson().fromJson(
                requireArguments().getString("initiator"), CallUser::class.java
            )
            callLog.receiver = Gson().fromJson(
                requireArguments().getString("receiver"), CallUser::class.java
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCallDetailsTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvCallDetails.layoutManager = LinearLayoutManager(
            context
        )
        binding.rvCallDetails.adapter = CallDetailsTabParticipantsAdapter(
            callLog
        )
    }
}
