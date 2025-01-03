package com.cometchat.chatuikit.shared.permission.builder;

import com.cometchat.chatuikit.shared.permission.listener.PermissionResultListener;

public interface PermissionHandlerBuilder {
    void check();

    PermissionHandlerBuilder withPermissions(String[] permissions);

    PermissionHandlerBuilder withListener(PermissionResultListener listener);
}
