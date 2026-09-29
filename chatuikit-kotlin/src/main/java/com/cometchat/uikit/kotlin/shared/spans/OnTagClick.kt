package com.cometchat.uikit.kotlin.shared.spans

import android.content.Context

/**
 * Interface for handling tag click events.
 */
public fun interface OnTagClick<T> {
    public fun onClick(context: Context, item: T)
}
