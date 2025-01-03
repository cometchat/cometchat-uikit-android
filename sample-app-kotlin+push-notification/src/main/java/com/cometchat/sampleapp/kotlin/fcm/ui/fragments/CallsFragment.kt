package com.cometchat.sampleapp.kotlin.fcm.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.cometchat.calls.constants.CometChatCallsConstants
import com.cometchat.calls.model.CallLog
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chatuikit.CometChatTheme
import com.cometchat.chatuikit.calls.CometChatCallActivity
import com.cometchat.chatuikit.calls.calllogs.CallLogsAdapter.CallLogsViewHolder
import com.cometchat.chatuikit.shared.interfaces.CallLogsClickListener
import com.cometchat.sampleapp.kotlin.fcm.databinding.FragmentCallsBinding
import com.cometchat.sampleapp.kotlin.fcm.ui.activity.CallDetailsActivity
import com.cometchat.sampleapp.kotlin.fcm.utils.AppUtils.getProgressBar
import com.cometchat.sampleapp.kotlin.fcm.viewmodels.CallsFragmentViewModel
import com.google.gson.Gson

class CallsFragment : Fragment() {

    private lateinit var binding: FragmentCallsBinding
    private lateinit var viewModel: CallsFragmentViewModel
    private var isCallActive = false
    private var enableAutoRefresh = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCallsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()

        initClickListeners()
    }

    override fun onResume() {
        super.onResume()
        if (enableAutoRefresh) {
            enableAutoRefresh = false
            isCallActive = false
            binding.callLog.refreshCallLogs()
        }
    }

    override fun onPause() {
        super.onPause()
        enableAutoRefresh = true
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider.NewInstanceFactory().create(
            CallsFragmentViewModel::class.java
        )
        viewModel.onCallStart().observe(viewLifecycleOwner, onCallStart())
        viewModel.onError().observe(viewLifecycleOwner, onError())
    }

    private fun onCallStart(): Observer<Call> {
        return Observer { call: Call? ->
            CometChatCallActivity.launchOutgoingCallScreen(
                requireContext(), call!!, null
            )
        }
    }

    private fun onError(): Observer<CometChatException> {
        return Observer { e: CometChatException ->
            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun initClickListeners() {
        binding.callLog.setClickListener(object : CallLogsClickListener {
            override fun setOnItemClickListener(
                context: Context, holder: CallLogsViewHolder, position: Int, callLog: CallLog
            ) {
                val intent = Intent(context, CallDetailsActivity::class.java)
                intent.putExtra("callLog", Gson().toJson(callLog))
                intent.putExtra("initiator", Gson().toJson(callLog.initiator))
                intent.putExtra("receiver", Gson().toJson(callLog.receiver))
                startActivity(intent)
            }

            override fun setOnItemLongClickListener(
                context: Context, holder: CallLogsViewHolder, position: Int, callLog: CallLog
            ) {
            }

            override fun setOnItemCallIconClickListener(
                context: Context, holder: CallLogsViewHolder, position: Int, callLog: CallLog
            ) {
                if (!isCallActive) {
                    isCallActive = true
                    holder.binding.tailView.removeAllViews()
                    holder.binding.tailView.addView(
                        getProgressBar(
                            requireContext(),
                            requireContext().resources.getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_30dp),
                            CometChatTheme.getTextColorPrimary(requireContext())
                        )
                    )
                    val listener: CometChat.CallbackListener<Void> = object : CometChat.CallbackListener<Void>() {
                        override fun onSuccess(unused: Void?) {
                            isCallActive = false
                        }

                        override fun onError(e: CometChatException) {
                            isCallActive = false
                        }
                    }
                    if (callLog.type == CometChatCallsConstants.CALL_TYPE_AUDIO) {
                        viewModel.startCall(CometChatConstants.CALL_TYPE_AUDIO, callLog, listener)
                    } else if (callLog.type == CometChatCallsConstants.CALL_TYPE_VIDEO) {
                        viewModel.startCall(CometChatConstants.CALL_TYPE_VIDEO, callLog, listener)
                    }
                }
            }
        })
    }

    companion object {

        private val TAG: String = CallsFragment::class.java.simpleName
    }
}
