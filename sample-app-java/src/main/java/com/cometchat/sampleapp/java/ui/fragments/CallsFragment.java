package com.cometchat.sampleapp.java.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.cometchat.calls.constants.CometChatCallsConstants;
import com.cometchat.calls.model.CallLog;
import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.core.Call;
import com.cometchat.chat.core.CometChat;
import com.cometchat.chat.exceptions.CometChatException;
import com.cometchat.chatuikit.CometChatTheme;
import com.cometchat.chatuikit.calls.CometChatCallActivity;
import com.cometchat.chatuikit.calls.calllogs.CallLogsAdapter;
import com.cometchat.chatuikit.shared.interfaces.CallLogsClickListener;
import com.cometchat.sampleapp.java.databinding.FragmentCallsBinding;
import com.cometchat.sampleapp.java.ui.activity.CallDetailsActivity;
import com.cometchat.sampleapp.java.utils.AppUtils;
import com.cometchat.sampleapp.java.viewmodels.CallsFragmentViewModel;
import com.google.gson.Gson;

public class CallsFragment extends Fragment {
    private static final String TAG = CallsFragment.class.getSimpleName();

    private CallsFragmentViewModel viewModel;
    private FragmentCallsBinding binding;
    private boolean isCallActive = false;
    private boolean enableAutoRefresh = false;

    public CallsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCallsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViewModel();

        initClickListeners();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (enableAutoRefresh) {
            enableAutoRefresh = false;
            isCallActive = false;
            binding.callLog.refreshCallLogs();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        enableAutoRefresh = true;
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider.NewInstanceFactory().create(CallsFragmentViewModel.class);
        viewModel.onCallStart().observe(getViewLifecycleOwner(), onCallStart());
        viewModel.onError().observe(getViewLifecycleOwner(), onError());
    }

    private Observer<Call> onCallStart() {
        return call -> {
            CometChatCallActivity.launchOutgoingCallScreen(requireContext(), call, null);
        };
    }

    private Observer<CometChatException> onError() {
        return e -> {
            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        };
    }

    private void initClickListeners() {
        binding.callLog.setClickListener(new CallLogsClickListener() {
            @Override
            public void setOnItemClickListener(Context context, CallLogsAdapter.CallLogsViewHolder holder, int position, CallLog callLog) {
                Intent intent = new Intent(context, CallDetailsActivity.class);
                intent.putExtra("callLog", new Gson().toJson(callLog));
                intent.putExtra("initiator", new Gson().toJson(callLog.getInitiator()));
                intent.putExtra("receiver", new Gson().toJson(callLog.getReceiver()));
                startActivity(intent);
            }

            @Override
            public void setOnItemLongClickListener(Context context, CallLogsAdapter.CallLogsViewHolder holder, int position, CallLog callLog) {}

            @Override
            public void setOnItemCallIconClickListener(Context context, CallLogsAdapter.CallLogsViewHolder holder, int position, CallLog callLog) {
                if (!isCallActive) {
                    isCallActive = true;
                    holder.getBinding().tailView.removeAllViews();
                    holder.getBinding().tailView.addView(
                            AppUtils.getProgressBar(
                                    requireContext(),
                                    requireContext().getResources().getDimensionPixelSize(com.cometchat.chatuikit.R.dimen.cometchat_30dp),
                                    CometChatTheme.getTextColorPrimary(requireContext())
                            )
                    );
                    CometChat.CallbackListener<Void> listener = new CometChat.CallbackListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            isCallActive = false;
                        }

                        @Override
                        public void onError(CometChatException e) {
                            isCallActive = false;
                        }
                    };
                    if (callLog.getType().equals(CometChatCallsConstants.CALL_TYPE_AUDIO)) {
                        viewModel.startCall(CometChatConstants.CALL_TYPE_AUDIO, callLog, listener);
                    } else if (callLog.getType().equals(CometChatCallsConstants.CALL_TYPE_VIDEO)) {
                        viewModel.startCall(CometChatConstants.CALL_TYPE_VIDEO, callLog, listener);
                    }
                }
            }
        });
    }
}
