package com.cometchat.sampleapp.java.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cometchat.calls.model.CallLog;
import com.cometchat.calls.model.CallUser;
import com.cometchat.sampleapp.java.databinding.FragmentCallDetailsTabBinding;
import com.cometchat.sampleapp.java.ui.adapters.CallDetailsTabRecordingsAdapter;
import com.google.gson.Gson;

public class CallDetailsTabRecordingFragment extends Fragment {

    private FragmentCallDetailsTabBinding binding;

    private CallLog callLog;
    private CallDetailsTabRecordingsAdapter adapter;
    private LinearLayoutManager layoutManager;

    public CallDetailsTabRecordingFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            callLog = new Gson().fromJson(getArguments().getString("callLog"), CallLog.class);
            callLog.setInitiator(new Gson().fromJson(getArguments().getString("initiator"), CallUser.class));
            callLog.setReceiver(new Gson().fromJson(getArguments().getString("receiver"), CallUser.class));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCallDetailsTabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initRecyclerView();
    }

    private void initRecyclerView() {
        layoutManager = new LinearLayoutManager(getContext());
        binding.rvCallDetails.setLayoutManager(layoutManager);

        if (callLog.isHasRecording()) {
            binding.rvCallDetails.setVisibility(View.VISIBLE);
            binding.emptyStateView.setVisibility(View.GONE);
            adapter = new CallDetailsTabRecordingsAdapter(requireContext(), callLog.getRecordings());
            binding.rvCallDetails.setAdapter(adapter);
        } else {
            binding.rvCallDetails.setVisibility(View.GONE);
            binding.emptyStateView.setVisibility(View.VISIBLE);
        }
    }

    public FragmentCallDetailsTabBinding getBinding() {
        return binding;
    }

    public CallLog getCallLog() {
        return callLog;
    }

    public CallDetailsTabRecordingsAdapter getAdapter() {
        return adapter;
    }

    public LinearLayoutManager getLayoutManager() {
        return layoutManager;
    }
}
