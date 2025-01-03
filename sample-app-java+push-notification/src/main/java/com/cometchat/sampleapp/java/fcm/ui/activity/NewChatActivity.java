package com.cometchat.sampleapp.java.fcm.ui.activity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cometchat.chat.core.GroupsRequest;
import com.cometchat.chat.core.UsersRequest;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.CometChatTheme;
import com.cometchat.chatuikit.groups.CometChatGroups;
import com.cometchat.chatuikit.shared.resources.utils.itemclicklistener.OnItemClickListener;
import com.cometchat.chatuikit.users.CometChatUsers;
import com.cometchat.sampleapp.java.fcm.R;
import com.cometchat.sampleapp.java.fcm.databinding.ActivityNewChatBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import java.util.Objects;

public class NewChatActivity extends AppCompatActivity {
    private ActivityNewChatBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.users.hideToolbar(true);
        binding.groups.hideToolbar(true);
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
        binding.users.setItemClickListener(new OnItemClickListener<User>() {
            @Override
            public void OnItemClick(User user, int position) {
                Intent intent = new Intent(NewChatActivity.this, MessagesActivity.class);
                intent.putExtra(getString(R.string.app_user), new Gson().toJson(user));
                startActivity(intent);
                finish();
            }
        });

        binding.groups.setGroupsRequestBuilder(new GroupsRequest.GroupsRequestBuilder().joinedOnly(true).setLimit(30));
        binding.groups.setItemClickListener(new OnItemClickListener<com.cometchat.chat.models.Group>() {
            @Override
            public void OnItemClick(com.cometchat.chat.models.Group group, int position) {
                Intent intent = new Intent(NewChatActivity.this, MessagesActivity.class);
                intent.putExtra(getString(R.string.app_group), new Gson().toJson(group));
                startActivity(intent);
                finish();
            }
        });

        binding.ivBack.setOnClickListener(v -> finish());
    }
}