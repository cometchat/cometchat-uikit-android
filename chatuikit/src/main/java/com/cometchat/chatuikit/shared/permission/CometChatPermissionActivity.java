package com.cometchat.chatuikit.shared.permission;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class CometChatPermissionActivity extends AppCompatActivity {
    private static final String TAG = CometChatPermissionActivity.class.getSimpleName();


    private ActivityResultLauncher<Intent> launchActivityForResult;
    private ActivityResultLauncher<String[]> launchActivityForPermission;
    private String[] permissions;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            if (getIntent().hasExtra(PermissionHandlerInstance.PERMISSION_STRING)) {
                permissions = getIntent().getStringArrayExtra(PermissionHandlerInstance.PERMISSION_STRING);
            } else if (getIntent().hasExtra(PermissionHandlerInstance.INTENT_STRING)) {
                intent = getIntent().getParcelableExtra(PermissionHandlerInstance.INTENT_STRING);
            }
            CometChatPermissionHandler.onActivityReady(this);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        launchActivityForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            CometChatPermissionHandler.onActivityResultCompleted(result);
            finish();
        });

        launchActivityForPermission = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissionGrantedHashMap -> {
            List<String> grantedPermissions = new LinkedList<>();
            List<String> deniedPermissions = new LinkedList<>();
            for (Map.Entry<String, Boolean> entry : permissionGrantedHashMap.entrySet()) {
                String key = entry.getKey();
                Boolean value = entry.getValue();
                if (Arrays.asList(permissions).contains(key) && !value) {
                    deniedPermissions.add(key);
                } else {
                    grantedPermissions.add(key);
                }
            }
            CometChatPermissionHandler.onPermissionsRequested(grantedPermissions, deniedPermissions);
            finish();
        });

        if (permissions != null) launchActivityForPermission.launch(permissions);
        else if (intent != null) launchActivityForResult.launch(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        launchActivityForResult.unregister();
        launchActivityForPermission.unregister();
        CometChatPermissionHandler.onActivityDestroyed(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        CometChatPermissionHandler.onActivityReady(this);
    }
}
