package com.cometchat.sampleapp.java.fcm.ui.fragments;

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
import com.cometchat.sampleapp.java.fcm.databinding.FragmentCallDetailsTabBinding;
import com.cometchat.sampleapp.java.fcm.ui.adapters.CallDetailsTabParticipantsAdapter;
import com.google.gson.Gson;

public class CallDetailsTabParticipantsFragment extends Fragment {

    private FragmentCallDetailsTabBinding binding;
    private CallLog callLog;

    public CallDetailsTabParticipantsFragment() {
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
        binding.rvCallDetails.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvCallDetails.setAdapter(new CallDetailsTabParticipantsAdapter(callLog));
    }
}
