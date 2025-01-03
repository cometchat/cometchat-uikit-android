package com.cometchat.sampleapp.java.fcm.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.calls.model.CallLog;
import com.cometchat.calls.model.Participant;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.sampleapp.java.fcm.databinding.CallDetailsParticipantsItemsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CallDetailsTabParticipantsAdapter extends RecyclerView.Adapter<CallDetailsTabParticipantsAdapter.MyViewHolder> {
    private final CallLog callLog;
    private final List<Participant> participants;

    public CallDetailsTabParticipantsAdapter(CallLog callLog) {
        this.callLog = callLog;
        this.participants = callLog.getParticipants() != null ? callLog.getParticipants() : new ArrayList<>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CallDetailsParticipantsItemsBinding binding = CallDetailsParticipantsItemsBinding.inflate(LayoutInflater.from(parent.getContext()),
                                                                                                  parent,
                                                                                                  false
        );
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Participant participant = participants.get(position);
        holder.binding.avatar.setAvatar(participant.getName(), participant.getAvatar());
        holder.binding.tvTitle.setText(participant.getName());
        holder.binding.dateTime.setDateText(Utils.callLogsTimeStamp(callLog.getInitiatedAt(), null));
        holder.binding.tvCallDuration.setText(setCallDuration(participant.getTotalDurationInMinutes()));
    }

    private String setCallDuration(double totalMinutes) {
        int hours = (int) totalMinutes / 60; // Get whole hours
        int remainingMinutes = (int) totalMinutes % 60; // Get remaining minutes
        int seconds = (int) ((totalMinutes - (int) totalMinutes) * 60); // Calculate seconds

        if (hours > 0) {
            if (remainingMinutes == 0 && seconds == 0) {
                return String.format(Locale.US, "%d hr", hours);
            } else if (seconds == 0) {
                return String.format(Locale.US, "%d hr %d min", hours, remainingMinutes);
            } else {
                return String.format(Locale.US, "%d hr %d min %d sec", hours, remainingMinutes, seconds);
            }
        } else {
            if (remainingMinutes == 0 && seconds == 0) {
                return "0 min";
            } else if (seconds == 0) {
                return String.format(Locale.US, "%d min", remainingMinutes);
            } else {
                return String.format(Locale.US, "%d min %d sec", remainingMinutes, seconds);
            }
        }
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        CallDetailsParticipantsItemsBinding binding;

        public MyViewHolder(@NonNull CallDetailsParticipantsItemsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
