package com.cometchat.sampleapp.java.fcm.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.sampleapp.java.fcm.AppCredentials;
import com.cometchat.sampleapp.java.fcm.R;
import com.cometchat.sampleapp.java.fcm.databinding.ActivitySplashBinding;
import com.cometchat.sampleapp.java.fcm.utils.AppConstants;
import com.cometchat.sampleapp.java.fcm.utils.AppUtils;
import com.cometchat.sampleapp.java.fcm.viewmodels.SplashViewModel;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        checkAppCredentials();

    }

    private void checkAppCredentials() {
        String appId = AppUtils.getDataFromSharedPref(this, String.class, getString(R.string.app_cred_id), AppCredentials.APP_ID);
        if (appId == null || appId.isEmpty()) {
            startActivity(new Intent(this, AppCredentialsActivity.class));
            finish();
        } else {
            initViewModel();
        }
    }

    private void initViewModel() {
        // Initialize ViewModel
        SplashViewModel viewModel = new ViewModelProvider(this).get(SplashViewModel.class);

        // Initialize CometChat UIKit
        if (!CometChatUIKit.isSDKInitialized()) {
            viewModel.initUIKit(this);
        } else {
            viewModel.checkUserIsNotLoggedIn();
        }

        // Observe login status
        viewModel.getLoginStatus().observe(this, isLoggedIn -> {
            if (isLoggedIn != null) {
                if (isLoggedIn) {
                    handleDeepLinking();
                } else {
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    Toast.makeText(SplashActivity.this, R.string.app_not_logged_in, Toast.LENGTH_SHORT).show();
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    private void handleDeepLinking() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(AppConstants.FCMConstants.NOTIFICATION_GROUP_SUMMARY_ID);
        String notificationType = getIntent().getStringExtra(AppConstants.FCMConstants.NOTIFICATION_TYPE);
        String notificationPayload = getIntent().getStringExtra(AppConstants.FCMConstants.NOTIFICATION_PAYLOAD);
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra(AppConstants.FCMConstants.NOTIFICATION_TYPE, notificationType);
        intent.putExtra(AppConstants.FCMConstants.NOTIFICATION_PAYLOAD, notificationPayload);
        startActivity(intent);
        finish();
    }

}
