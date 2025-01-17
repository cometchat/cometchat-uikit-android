package com.cometchat.chatuikit.shared.views.mediarecorder;

import android.content.Context;

import java.io.File;

/**
 * Interface for handling submit click events.
 */
public interface OnSubmitClick {
    /**
     * Called when a submit action is triggered.
     *
     * @param file    the file associated with the submit action
     * @param context the context in which the submit action occurs
     */
    void onClick(File file, Context context);
}
