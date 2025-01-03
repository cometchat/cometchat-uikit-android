package com.cometchat.sampleapp.java.fcm.ui.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.calls.constants.CometChatCallsConstants;
import com.cometchat.calls.model.CallLog;
import com.cometchat.calls.model.CallUser;
import com.cometchat.chatuikit.CometChatTheme;
import com.cometchat.chatuikit.calls.utils.CallUtils;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.sampleapp.java.fcm.R;
import com.cometchat.sampleapp.java.fcm.databinding.CallDetailsHistoryItemsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CallDetailsTabHistoryAdapter extends RecyclerView.Adapter<CallDetailsTabHistoryAdapter.MyViewHolder> {
    private final Context context;
    private List<CallLog> callLogs;

    public CallDetailsTabHistoryAdapter(Context context) {
        this.context = context;
        callLogs = new ArrayList<>();
    }

    public void setCallLogs(@Nullable List<CallLog> list) {
        if (list != null) {
            this.callLogs = list;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CallDetailsHistoryItemsBinding binding = CallDetailsHistoryItemsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        CallLog callLog = callLogs.get(position);
        boolean isLoggedInUser = CallUtils.isLoggedInUser((CallUser) callLog.getInitiator());
        boolean isMissedOrUnanswered = callLog.getStatus().equals(CometChatCallsConstants.CALL_STATUS_UNANSWERED) || callLog.getStatus().equals(
            CometChatCallsConstants.CALL_STATUS_MISSED);
        holder.binding.tvInfoDate.setDateText(Utils.callLogsTimeStamp(callLog.getInitiatedAt(), null));
        if (callLog.getType().equals(CometChatCallsConstants.CALL_TYPE_AUDIO) || callLog.getType()
                                                                                        .equals(CometChatCallsConstants.CALL_TYPE_VIDEO) || callLog
            .getType()
            .equals(CometChatCallsConstants.CALL_TYPE_AUDIO_VIDEO)) {
            if (isLoggedInUser) {
                holder.binding.tvInfoTitle.setText(R.string.app_call_outgoing);
                holder.binding.tvInfoTitle.setTextAppearance(CometChatTheme.getTextAppearanceHeading4Medium(context));
                holder.binding.tvInfoTitle.setTextColor(CometChatTheme.getTextColorPrimary(context));
                setupCallIcon(
                    holder.binding.ivInfoIcon,
                    AppCompatResources.getDrawable(context, com.cometchat.chatuikit.R.drawable.cometchat_ic_outgoing_call),
                    CometChatTheme.getSuccessColor(context)
                );
            } else if (isMissedOrUnanswered) {
                holder.binding.tvInfoTitle.setText(R.string.app_call_missed);
                holder.binding.tvInfoTitle.setTextAppearance(CometChatTheme.getTextAppearanceHeading4Medium(context));
                holder.binding.tvInfoTitle.setTextColor(CometChatTheme.getErrorColor(context));
                setupCallIcon(
                    holder.binding.ivInfoIcon,
                    AppCompatResources.getDrawable(context, com.cometchat.chatuikit.R.drawable.cometchat_ic_missed_call),
                    CometChatTheme.getErrorColor(context)
                );
            } else {
                holder.binding.tvInfoTitle.setText(R.string.app_call_incoming);
                holder.binding.tvInfoTitle.setTextAppearance(CometChatTheme.getTextAppearanceHeading4Medium(context));
                holder.binding.tvInfoTitle.setTextColor(CometChatTheme.getTextColorPrimary(context));
                setupCallIcon(
                    holder.binding.ivInfoIcon,
                    AppCompatResources.getDrawable(context, com.cometchat.chatuikit.R.drawable.cometchat_ic_incoming_call),
                    CometChatTheme.getSuccessColor(context)
                );
            }
        }

        holder.binding.tvInfoCallDuration.setText(getCallDuration(callLog.getTotalDurationInMinutes()));
    }

    private void setupCallIcon(ImageView imageView, Drawable icon, @ColorInt int iconTint) {
        imageView.setBackground(icon);
        imageView.setBackgroundTintList(ColorStateList.valueOf(iconTint));
    }

    private String getCallDuration(double totalDurationInMinutes) {
        int minutes, seconds;
        minutes = (int) totalDurationInMinutes;
        seconds = (int) ((totalDurationInMinutes - minutes) * 60);
        return String.format(Locale.US, "%dm %ds", minutes, seconds);
    }

    @Override
    public int getItemCount() {
        return callLogs.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        CallDetailsHistoryItemsBinding binding;

        public MyViewHolder(@NonNull CallDetailsHistoryItemsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
