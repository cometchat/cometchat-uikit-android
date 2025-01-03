package com.cometchat.sampleapp.java.fcm.fcm;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cometchat.sampleapp.java.fcm.R;
import com.cometchat.sampleapp.java.fcm.databinding.ActivityIncomingCallBinding;
import com.cometchat.sampleapp.java.fcm.ui.activity.SplashActivity;

public class IncomingCallActivity extends AppCompatActivity {
    private ActivityIncomingCallBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityIncomingCallBinding binding = ActivityIncomingCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String callerName = getIntent().getStringExtra("caller_name");
        String callerAvatar = getIntent().getStringExtra("caller_avatar");
        String callType = getIntent().getStringExtra("call_type");


        // Set caller details
        binding.callerName.setText(callerName);
        Glide.with(this).load(callerAvatar).placeholder(R.drawable.ic_user_profile).into(binding.callerAvatar);

        binding.acceptCallButton.setOnClickListener(v -> startCallActivity(callType));
        binding.rejectCallButton.setOnClickListener(v -> finish());
    }

    private void startCallActivity(String callType) {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.putExtra("call_type", callType);
        startActivity(intent);
        finish();
    }
}