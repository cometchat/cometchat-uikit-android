package com.cometchat.sampleapp.java.fcm.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.resources.utils.itemclicklistener.OnItemClickListener;
import com.cometchat.sampleapp.java.fcm.R;
import com.cometchat.sampleapp.java.fcm.databinding.FragmentUsersBinding;
import com.cometchat.sampleapp.java.fcm.ui.activity.MessagesActivity;
import com.google.gson.Gson;

public class UsersFragment extends Fragment {
    private FragmentUsersBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.users.setItemClickListener(new OnItemClickListener<User>() {
            @Override
            public void OnItemClick(User user, int position) {
                Intent intent = new Intent(requireActivity(), MessagesActivity.class);
                intent.putExtra(getString(R.string.app_user), new Gson().toJson(user));
                startActivity(intent);
            }
        });
    }
}
