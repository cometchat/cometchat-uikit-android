package com.cometchat.chatuikit.shared.interfaces;

import android.content.Context;

import com.cometchat.calls.model.CallLog;
import com.cometchat.chatuikit.calls.calllogs.CallLogsAdapter;

public interface CallLogsClickListener {
    void setOnItemClickListener(Context context, CallLogsAdapter.CallLogsViewHolder holder, int position, CallLog callLog);

    void setOnItemLongClickListener(Context context, CallLogsAdapter.CallLogsViewHolder holder, int position, CallLog callLog);

    void setOnItemCallIconClickListener(Context context, CallLogsAdapter.CallLogsViewHolder holder, int position, CallLog callLog);
}
