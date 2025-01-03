package com.cometchat.chatuikit.shared.permission.listener;

import java.util.List;

public class BasePermissionResultListener implements PermissionResultListener {
    private static final String TAG = BasePermissionResultListener.class.getSimpleName();

    @Override
    public void permissionResult(List<String> grantedPermission, List<String> deniedPermission) {
    }
}
