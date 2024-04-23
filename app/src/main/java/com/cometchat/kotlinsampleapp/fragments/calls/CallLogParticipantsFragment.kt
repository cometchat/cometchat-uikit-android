package com.cometchat.kotlinsampleapp.fragments.calls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.calls.model.Participant
import com.cometchat.chatuikit.calls.callparticipants.CometChatCallLogParticipants
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.kotlinsampleapp.R

class CallLogParticipantsFragment : Fragment() {
    private var cometChatCallLogParticipants: CometChatCallLogParticipants? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_call_log_participants, container, false)
        cometChatCallLogParticipants =
            view.findViewById(R.id.call_logs_participants)
        createCallParticipants()
        return view
    }

    private fun createCallParticipants() {
        val participants: MutableList<Participant> = ArrayList()
        val participant = Participant()
        participant.uid = CometChatUIKit.getLoggedInUser().uid
        participant.name = CometChatUIKit.getLoggedInUser().name
        participant.avatar = CometChatUIKit.getLoggedInUser().avatar
        participant.setJoinedAt(1327349241)
        participant.isHasJoined = true
        participant.totalDurationInMinutes = 1327349241.0
        val participant1 = Participant()
        participant1.uid = "UID233"
        participant1.name = "Kevin"
        participant1.avatar = "https://data-us.cometchat.io/assets/images/avatars/spiderman.png"
        participant1.setJoinedAt(1327349241)
        participant1.isHasJoined = true
        participant1.totalDurationInMinutes = 1327349241.0
        participants.add(participant)
        participants.add(participant1)
        cometChatCallLogParticipants!!.setParticipantList(participants)
    }
}