package com.cometchat.sampleapp.java.fcm.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.cometchat.chat.core.GroupsRequest;
import com.cometchat.chat.core.UsersRequest;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.interfaces.OnItemClick;
import com.cometchat.sampleapp.java.fcm.R;
import com.cometchat.sampleapp.java.fcm.databinding.ActivityNewChatBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

public class NewChatActivity extends AppCompatActivity {
    private ActivityNewChatBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.users.setToolbarVisibility(View.GONE);
        binding.groups.setToolbarVisibility(View.GONE);
        binding.users.setSeparatorVisibility(View.GONE);
        binding.groups.setSeparatorVisibility(View.GONE);
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    binding.users.setVisibility(View.VISIBLE);
                    binding.groups.setVisibility(View.GONE);
                } else {
                    binding.users.setVisibility(View.GONE);
                    binding.groups.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Optional logic for reselected tab
            }
        });

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getResources().getString(R.string.app_bottom_nav_users)));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getResources().getString(R.string.app_bottom_nav_groups)));
        binding.tabLayout.getTabAt(0).select();
        binding.users.setUsersRequestBuilder(new UsersRequest.UsersRequestBuilder().hideBlockedUsers(true).setLimit(30));
        binding.users.setOnItemClick(new OnItemClick<User>() {
            @Override
            public void click(View view, int poUser, User user) {
                Intent intent = new Intent(NewChatActivity.this, MessagesActivity.class);
                intent.putExtra(getString(R.string.app_user), new Gson().toJson(user));
                startActivity(intent);
                finish();
            }
        });

        binding.groups.setGroupsRequestBuilder(new GroupsRequest.GroupsRequestBuilder().joinedOnly(true).setLimit(30));
        binding.groups.setOnItemClick((view, position, group) -> {
            Intent intent = new Intent(NewChatActivity.this, MessagesActivity.class);
            intent.putExtra(getString(R.string.app_group), new Gson().toJson(group));
            startActivity(intent);
            finish();
        });

        binding.ivBack.setOnClickListener(v -> finish());
    }
}