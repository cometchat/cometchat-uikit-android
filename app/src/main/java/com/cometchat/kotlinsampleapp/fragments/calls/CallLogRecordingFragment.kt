package com.cometchat.kotlinsampleapp.fragments.calls

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cometchat.calls.model.Recording
import com.cometchat.chatuikit.calls.callrecordings.CometChatCallLogRecordings
import com.cometchat.kotlinsampleapp.R


class CallLogRecordingFragment : Fragment() {
    private var cometChatCallLogRecordings: CometChatCallLogRecordings? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_call_log_recording, container, false)
        cometChatCallLogRecordings =
            view.findViewById(R.id.call_logs_recordings)
        createCallRecordings()
        return view
    }

    private fun createCallRecordings() {
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
        cometChatCallLogRecordings!!.setRecordingList(recordings)
    }
}