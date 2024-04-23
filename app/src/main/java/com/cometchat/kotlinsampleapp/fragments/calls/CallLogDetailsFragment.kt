package com.cometchat.kotlinsampleapp.fragments.calls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.calls.constants.CometChatCallsConstants
import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.CallUser
import com.cometchat.calls.model.Participant
import com.cometchat.calls.model.Recording
import com.cometchat.chatuikit.calls.calldetails.CometChatCallLogDetails
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit
import com.cometchat.kotlinsampleapp.R

class CallLogDetailsFragment : Fragment() {
    private var cometChatCallLogDetails: CometChatCallLogDetails? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_call_details, container, false)
        cometChatCallLogDetails = view.findViewById(R.id.call_logs_details)
        createCallLog()
        return view
    }

    private fun createCallLog() {
        if (CometChatUIKit.getLoggedInUser() != null) {
            val initiator = CallUser()
            initiator.avatar = CometChatUIKit.getLoggedInUser().avatar
            initiator.uid = CometChatUIKit.getLoggedInUser().uid
            initiator.name = CometChatUIKit.getLoggedInUser().name
            val receiver = CallUser()
            receiver.uid = "UID233"
            receiver.name = "Kevin"
            receiver.avatar = "https://data-us.cometchat.io/assets/images/avatars/spiderman.png"
            val participants: MutableList<Participant> = ArrayList()
            val participant = Participant()
            participant.uid = CometChatUIKit.getLoggedInUser().uid
            participant.name = CometChatUIKit.getLoggedInUser().name
            participant.avatar = CometChatUIKit.getLoggedInUser().avatar
            participant.setJoinedAt(1327349241)
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
            val recordings: MutableList<Recording> = ArrayList()
            val recording = Recording()
            recording.recordingURL =
                "https://recordings-us.cometchat.io/236497dcc2cd529b/2023-12-15/v1.us.236497dcc2cd529b.170264141733632a2e3171d8a5dcb1f82b743fbc2730422263_2023-12-15-11-57-16.mp4"
            recording.duration = 1327349241.0
            recording.rid = "RID2023"
            val recording1 = Recording()
            recording1.recordingURL =
                "https://recordings-us.cometchat.io/236497dcc2cd529b/2023-12-15/v1.us.236497dcc2cd529b.170264141733632a2e3171d8a5dcb1f82b743fbc2730422263_2023-12-15-11-57-16.mp4"
            recording1.duration = 1327349241.0
            recording1.rid = "RID2023-1"
            recordings.add(recording)
            recordings.add(recording1)
            val callLog = CallLog()
            callLog.initiatedAt = 1327349241
            callLog.initiator = initiator
            callLog.receiver = receiver
            callLog.status = CometChatCallsConstants.CALL_STATUS_BUSY
            callLog.type = CometChatCallsConstants.CALL_TYPE_AUDIO
            callLog.participants = participants
            callLog.recordings = recordings
            callLog.receiverType = CometChatCallsConstants.RECEIVER_TYPE_USER
            cometChatCallLogDetails!!.setCall(callLog)
        }
    }
}