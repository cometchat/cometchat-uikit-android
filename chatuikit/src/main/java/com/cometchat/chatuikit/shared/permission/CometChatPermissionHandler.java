package com.cometchat.chatuikit.shared.permission;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.cometchat.chatuikit.shared.permission.builder.ActivityResultHandlerBuilder;
import com.cometchat.chatuikit.shared.permission.builder.PermissionHandlerBuilder;
import com.cometchat.chatuikit.shared.permission.listener.ActivityResultListener;
import com.cometchat.chatuikit.shared.permission.listener.BaseActivityResultListener;
import com.cometchat.chatuikit.shared.permission.listener.BasePermissionResultListener;
import com.cometchat.chatuikit.shared.permission.listener.PermissionResultListener;

import java.util.List;

public final class CometChatPermissionHandler implements PermissionHandlerBuilder, ActivityResultHandlerBuilder {
    private static final String TAG = CometChatPermissionHandler.class.getSimpleName();


    private static PermissionHandlerInstance instance;
    private String[] permissions;
    private Intent requestIntent;
    private ActivityResultListener resultListener = new BaseActivityResultListener();
    private PermissionResultListener permissionResultListener = new BasePermissionResultListener();

    private CometChatPermissionHandler(Context context) {
        initialize(context);
    }

    public static CometChatPermissionHandler withContext(Context context) {
        return new CometChatPermissionHandler(context);
    }

    @Override
    public PermissionHandlerBuilder withPermissions(String[] permissions) {
        this.permissions = permissions;
        return this;
    }

    @Override
    public PermissionHandlerBuilder withListener(PermissionResultListener listener) {
        this.permissionResultListener = listener;
        return this;
    }

    @Override
    public void check() {
        if (permissions == null) return;
        instance.checkPermissions(permissionResultListener, permissions);
    }

    private static void initialize(Context context) {
        if (instance == null) {
            IntentProvider intentProvider = new IntentProvider();
            instance = new PermissionHandlerInstance(context, intentProvider);
        } else {
            instance.setContext(context);
        }
    }

    /**
     * Method called whenever the CometChatPermissionActivity has been created or
     * recreated and is ready to be used.
     */
    static void onActivityReady(CometChatPermissionActivity activity) {
        if (instance != null) {
            instance.onActivityReady(activity);
        }
    }

    static void onActivityDestroyed(CometChatPermissionActivity oldActivity) {
        if (instance != null) {
            instance.onActivityDestroyed(oldActivity);
        }
    }

    static void onPermissionsRequested(List<String> grantedPermissions, List<String> deniedPermissions) {
        if (instance != null) {
            instance.updatedPermissionResults(grantedPermissions, deniedPermissions);
        }
    }

    static void onActivityResultCompleted(androidx.activity.result.ActivityResult activityResult) {
        if (instance != null) {
            instance.onActivityResultCompleted(activityResult);
        }
    }

    @Override
    public ActivityResultHandlerBuilder withIntent(Intent intent) {
        requestIntent = intent;
        return this;
    }

    @Override
    public void launch() {
        if (requestIntent == null) return;
        instance.handleIntent(requestIntent, resultListener);
    }

    @NonNull
    @Override
    public ActivityResultHandlerBuilder registerListener(ActivityResultListener listener) {
        this.resultListener = listener;
        return this;
    }
}
