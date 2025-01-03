package com.cometchat.sampleapp.kotlin.fcm.ui.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.calls.constants.CometChatCallsConstants
import com.cometchat.calls.model.CallLog
import com.cometchat.calls.model.CallUser
import com.cometchat.chatuikit.CometChatTheme
import com.cometchat.chatuikit.calls.utils.CallUtils
import com.cometchat.chatuikit.shared.resources.utils.Utils
import com.cometchat.sampleapp.kotlin.fcm.R
import com.cometchat.sampleapp.kotlin.fcm.databinding.CallDetailsHistoryItemsBinding
import java.util.Locale

class CallDetailsTabHistoryAdapter(private val context: Context) : RecyclerView.Adapter<CallDetailsTabHistoryAdapter.MyViewHolder>() {
    private var callLogs: List<CallLog>

    init {
        callLogs = ArrayList()
    }

    fun setCallLogs(list: List<CallLog>) {
        this.callLogs = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding = CallDetailsHistoryItemsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val callLog = callLogs[position]
        val isLoggedInUser = CallUtils.isLoggedInUser(callLog.initiator as CallUser)
        val isMissedOrUnanswered =
            callLog.status == CometChatCallsConstants.CALL_STATUS_UNANSWERED || callLog.status == CometChatCallsConstants.CALL_STATUS_MISSED
        holder.binding.tvInfoDate.dateText = Utils.callLogsTimeStamp(
            callLog.initiatedAt, null
        )
        if (callLog.type == CometChatCallsConstants.CALL_TYPE_AUDIO || callLog.type == CometChatCallsConstants.CALL_TYPE_VIDEO || callLog.type == CometChatCallsConstants.CALL_TYPE_AUDIO_VIDEO) {
            if (isLoggedInUser) {
                holder.binding.tvInfoTitle.setText(R.string.app_call_outgoing)
                holder.binding.tvInfoTitle.setTextAppearance(
                    CometChatTheme.getTextAppearanceHeading4Medium(
                        context
                    )
                )
                holder.binding.tvInfoTitle.setTextColor(CometChatTheme.getTextColorPrimary(context))
                setupCallIcon(
                    holder.binding.ivInfoIcon, AppCompatResources.getDrawable(
                        context, com.cometchat.chatuikit.R.drawable.cometchat_ic_outgoing_call
                    ), CometChatTheme.getSuccessColor(
                        context
                    )
                )
            } else if (isMissedOrUnanswered) {
                holder.binding.tvInfoTitle.setText(R.string.app_call_missed)
                holder.binding.tvInfoTitle.setTextAppearance(
                    CometChatTheme.getTextAppearanceHeading4Medium(
                        context
                    )
                )
                holder.binding.tvInfoTitle.setTextColor(CometChatTheme.getErrorColor(context))
                setupCallIcon(
                    holder.binding.ivInfoIcon, AppCompatResources.getDrawable(
                        context, com.cometchat.chatuikit.R.drawable.cometchat_ic_missed_call
                    ), CometChatTheme.getErrorColor(
                        context
                    )
                )
            } else {
                holder.binding.tvInfoTitle.setText(R.string.app_call_incoming)
                holder.binding.tvInfoTitle.setTextAppearance(
                    CometChatTheme.getTextAppearanceHeading4Medium(
                        context
                    )
                )
                holder.binding.tvInfoTitle.setTextColor(CometChatTheme.getTextColorPrimary(context))
                setupCallIcon(
                    holder.binding.ivInfoIcon, AppCompatResources.getDrawable(
                        context, com.cometchat.chatuikit.R.drawable.cometchat_ic_incoming_call
                    ), CometChatTheme.getSuccessColor(
                        context
                    )
                )
            }
        }

        holder.binding.tvInfoCallDuration.text = getCallDuration(callLog.totalDurationInMinutes)
    }

    private fun setupCallIcon(
        imageView: ImageView,
        icon: Drawable?,
        @ColorInt iconTint: Int
    ) {
        imageView.background = icon
        imageView.backgroundTintList = ColorStateList.valueOf(iconTint)
    }

    private fun getCallDuration(totalDurationInMinutes: Double): String {
        val seconds: Int
        val minutes = totalDurationInMinutes.toInt()
        seconds = ((totalDurationInMinutes - minutes) * 60).toInt()
        return String.format(Locale.US, "%dm %ds", minutes, seconds)
    }

    override fun getItemCount(): Int {
        return callLogs.size
    }

    class MyViewHolder(var binding: CallDetailsHistoryItemsBinding) : RecyclerView.ViewHolder(binding.root)
}
