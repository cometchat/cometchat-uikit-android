package com.cometchat.chatuikit.shared.spans;

import android.content.Context;

public interface OnTagClick<T> {
    void onClick(Context context, T t);
}
