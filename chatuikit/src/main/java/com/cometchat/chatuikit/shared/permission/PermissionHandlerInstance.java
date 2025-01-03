package com.cometchat.chatuikit.shared.permission;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResult;
import androidx.core.content.ContextCompat;

import com.cometchat.chatuikit.shared.permission.listener.ActivityResultListener;
import com.cometchat.chatuikit.shared.permission.listener.BaseActivityResultListener;
import com.cometchat.chatuikit.shared.permission.listener.BasePermissionResultListener;
import com.cometchat.chatuikit.shared.permission.listener.PermissionResultListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class PermissionHandlerInstance {
    private static final String TAG = PermissionHandlerInstance.class.getSimpleName();


    private WeakReference<Context> context;
    private static final ActivityResultListener EMPTY_ACTIVITY_RESULT_LISTENER = new BaseActivityResultListener();
    private static final PermissionResultListener EMPTY_PERMISSION_RESULT_LISTENER = new BasePermissionResultListener();
    private final IntentProvider intentProvider;
    public static final String INTENT_STRING = "intentString";
    public static final String PERMISSION_STRING = "permissionString";
    private CometChatPermissionActivity activity;
    private ActivityResultListener activityResultListener = EMPTY_ACTIVITY_RESULT_LISTENER;
    private PermissionResultListener permissionResultListener = EMPTY_PERMISSION_RESULT_LISTENER;

    PermissionHandlerInstance(Context context, IntentProvider intentProvider) {
        this.intentProvider = intentProvider;
        setContext(context);
    }

    void setContext(Context context) {
        this.context = new WeakReference<>(context);
    }

    void checkPermissions(PermissionResultListener listener, String[] permissions) {
        checkMultiplePermissions(listener, permissions);
    }

    void handleIntent(Intent intent, ActivityResultListener activityResultListener) {
        Context context = this.context.get();
        if (context == null) {
            return;
        }
        this.activityResultListener = activityResultListener;
        Intent activityIntent = intentProvider.get(context, CometChatPermissionActivity.class);
        activityIntent.putExtra(INTENT_STRING, intent);
        if (!(context instanceof Activity)) {
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(activityIntent);
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(0, 0);
        }
    }

    void onActivityReady(CometChatPermissionActivity activity) {
        this.activity = activity;
    }

    void onActivityDestroyed(Activity oldActivity) {
        if (activity == oldActivity) {
            activity = null;
        }
    }

    void onActivityResultCompleted(ActivityResult intent) {
        updateActivityResultGranted(intent);
    }

    private void updateActivityResultGranted(ActivityResult intent) {
        if (activity != null) {
            activity.finish();
        }
        ActivityResultListener currentListener = activityResultListener;
        activityResultListener = EMPTY_ACTIVITY_RESULT_LISTENER;
        currentListener.onResult(intent);
    }

    private void checkMultiplePermissions(final PermissionResultListener listener, final String[] permissions) {

        if (context.get() == null) {
            return;
        }
        if (activity != null && activity.isFinishing()) {
            onActivityDestroyed(activity);
        }

        this.permissionResultListener = listener;
        if (isEveryPermissionGranted(permissions, context.get())) {
            updatedPermissionResults(Arrays.asList(permissions), new ArrayList<>());
        } else {
            startActivityForPermission(permissions);
        }
    }

    private void startActivityForPermission(final String[] permissions) {
        Context context = this.context.get();
        if (context == null) {
            return;
        }
        Intent intent = intentProvider.get(context, CometChatPermissionActivity.class);
        intent.putExtra(PERMISSION_STRING, permissions);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).overridePendingTransition(0, 0);
        }
    }

    public void updatedPermissionResults(List<String> granted, List<String> denied) {
        if (activity != null) {
            activity.finish();
        }
        PermissionResultListener currentListener = permissionResultListener;
        permissionResultListener = EMPTY_PERMISSION_RESULT_LISTENER;
        currentListener.permissionResult(granted, denied);
    }

    private boolean isEveryPermissionGranted(String[] permissions, Context context) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                // This permission is not granted
                return false;
            }
        }
        // All permissions are granted
        return true;
    }
}
