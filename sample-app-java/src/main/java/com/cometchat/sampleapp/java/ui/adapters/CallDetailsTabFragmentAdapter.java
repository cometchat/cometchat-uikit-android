package com.cometchat.sampleapp.java.ui.adapters;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.cometchat.calls.model.CallLog;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class CallDetailsTabFragmentAdapter extends FragmentStateAdapter {
    private final List<Fragment> fragmentList = new ArrayList<>();
    private final List<CallLog> callLogList = new ArrayList<>();
    private final List<String> tabTitleList = new ArrayList<>();

    public CallDetailsTabFragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void addFragment(Fragment fragment, String tabTitle, CallLog callLog) {
        fragmentList.add(fragment);
        tabTitleList.add(tabTitle);
        callLogList.add(callLog);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = fragmentList.get(position);
        CallLog callLog = callLogList.get(position);

        // Convert callLog to JSON and set as an argument
        Bundle args = new Bundle();
        args.putString("callLog", new Gson().toJson(callLog));
        args.putString("initiator", new Gson().toJson(callLog.getInitiator()));
        args.putString("receiver", new Gson().toJson(callLog.getReceiver()));

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }

    public CallLog getCallLog(int position) {
        return callLogList.get(position);
    }

    public String getTabTitle(int position) {
        return tabTitleList.get(position);
    }
}
