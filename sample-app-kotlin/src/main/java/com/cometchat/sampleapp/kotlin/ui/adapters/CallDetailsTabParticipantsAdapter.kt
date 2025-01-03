package com.cometchat.sampleapp.kotlin.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.Participant
import com.cometchat.chatuikit.shared.resources.utils.Utils
import com.cometchat.sampleapp.kotlin.databinding.CallDetailsParticipantsItemsBinding
import java.util.Locale

class CallDetailsTabParticipantsAdapter
    (private val callLog: CallLog) : RecyclerView.Adapter<CallDetailsTabParticipantsAdapter.MyViewHolder>() {
    private val participants: List<Participant> = if (callLog.participants != null) callLog.participants else ArrayList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding = CallDetailsParticipantsItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val participant = participants[position]
        holder.binding.avatar.setAvatar(participant.name, participant.avatar)
        holder.binding.tvTitle.text = participant.name
        holder.binding.dateTime.dateText = Utils.callLogsTimeStamp(
            callLog.initiatedAt, null
        )
        holder.binding.tvCallDuration.text = setCallDuration(participant.totalDurationInMinutes)
    }

    private fun setCallDuration(totalMinutes: Double): String {
        val hours = totalMinutes.toInt() / 60 // Get whole hours
        val remainingMinutes = totalMinutes.toInt() % 60 // Get remaining minutes
        val seconds = ((totalMinutes - totalMinutes.toInt()) * 60).toInt() // Calculate seconds

        return if (hours > 0) {
            if (remainingMinutes == 0 && seconds == 0) {
                String.format(Locale.US, "%d hr", hours)
            } else if (seconds == 0) {
                String.format(Locale.US, "%d hr %d min", hours, remainingMinutes)
            } else {
                String.format(
                    Locale.US, "%d hr %d min %d sec", hours, remainingMinutes, seconds
                )
            }
        } else {
            if (remainingMinutes == 0 && seconds == 0) {
                "0 min"
            } else if (seconds == 0) {
                String.format(Locale.US, "%d min", remainingMinutes)
            } else {
                String.format(
                    Locale.US, "%d min %d sec", remainingMinutes, seconds
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return participants.size
    }

    class MyViewHolder(var binding: CallDetailsParticipantsItemsBinding) : RecyclerView.ViewHolder(binding.root)
}
