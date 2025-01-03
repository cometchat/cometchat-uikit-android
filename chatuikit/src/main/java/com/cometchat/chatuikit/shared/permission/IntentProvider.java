package com.cometchat.chatuikit.shared.permission;

import android.content.Context;
import android.content.Intent;

class IntentProvider {
    private static final String TAG = IntentProvider.class.getSimpleName();


    public Intent get(Context context, Class<?> clazz) {
        return new Intent(context, clazz);
    }
}
