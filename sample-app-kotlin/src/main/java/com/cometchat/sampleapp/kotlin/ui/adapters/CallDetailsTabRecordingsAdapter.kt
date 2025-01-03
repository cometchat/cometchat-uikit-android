package com.cometchat.sampleapp.kotlin.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.calls.model.Recording
import com.cometchat.chatuikit.shared.resources.utils.MediaUtils
import com.cometchat.chatuikit.shared.resources.utils.Utils
import com.cometchat.sampleapp.kotlin.databinding.CallDetailsRecordingsItemsBinding

class CallDetailsTabRecordingsAdapter
    (
    private val context: Context,
    private val recordings: List<Recording>
) : RecyclerView.Adapter<CallDetailsTabRecordingsAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding = CallDetailsRecordingsItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val recording = recordings[position]
        holder.binding.tvTitle.text = recording.rid
        holder.binding.dateTime.dateText = Utils.callLogsTimeStamp(recording.startTime.toLong(), null)

        holder.binding.ivPlayRecording.setOnClickListener { v: View? ->
            MediaUtils.openMediaInPlayer(
                context, recording.recordingURL, "video/*"
            )
        }

        holder.binding.ivDownloadRecording.setOnClickListener { v: View? ->
            MediaUtils.downloadFile(
                context, recording.recordingURL, recording.rid, ".mp4"
            )
        }
    }

    override fun getItemCount(): Int {
        return recordings.size
    }

    class MyViewHolder(var binding: CallDetailsRecordingsItemsBinding) : RecyclerView.ViewHolder(binding.root)
}
