package com.cometchat.chatuikit.shared.permission.builder;

import android.content.Intent;

import com.cometchat.chatuikit.shared.permission.listener.ActivityResultListener;

public interface ActivityResultHandlerBuilder {
    void launch();

    ActivityResultHandlerBuilder withIntent(Intent intent);

    ActivityResultHandlerBuilder registerListener(ActivityResultListener listener);
}
