package com.cometchat.sampleapp.java.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.calls.model.Recording;
import com.cometchat.chatuikit.shared.resources.utils.MediaUtils;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.sampleapp.java.databinding.CallDetailsRecordingsItemsBinding;

import java.util.List;

public class CallDetailsTabRecordingsAdapter extends RecyclerView.Adapter<CallDetailsTabRecordingsAdapter.MyViewHolder> {

    private final Context context;
    private final List<Recording> recordings;

    public CallDetailsTabRecordingsAdapter(@NonNull Context context, @NonNull List<Recording> recordings) {
        this.context = context;
        this.recordings = recordings;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CallDetailsRecordingsItemsBinding binding = CallDetailsRecordingsItemsBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Recording recording = recordings.get(position);
        holder.binding.tvTitle.setText(recording.getRid());
        holder.binding.dateTime.setDateText(Utils.callLogsTimeStamp(recording.getStartTime(), null));

        holder.binding.ivPlayRecording.setOnClickListener(v -> {
            MediaUtils.openMediaInPlayer(context, recording.getRecordingURL(), "video/*");
        });

        holder.binding.ivDownloadRecording.setOnClickListener(v -> {
            MediaUtils.downloadFile(context, recording.getRecordingURL(), recording.getRid(), ".mp4");
        });
    }

    @Override
    public int getItemCount() {
        return recordings.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        CallDetailsRecordingsItemsBinding binding;

        public MyViewHolder(@NonNull CallDetailsRecordingsItemsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
