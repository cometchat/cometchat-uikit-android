package com.cometchat.chatuikit.shared.permission.listener;

import java.util.List;

public interface PermissionResultListener {
    void permissionResult(List<String> grantedPermission, List<String> deniedPermission);
}
