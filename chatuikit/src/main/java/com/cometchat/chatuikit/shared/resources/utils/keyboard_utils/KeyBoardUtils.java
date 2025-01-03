package com.cometchat.chatuikit.shared.resources.utils.keyboard_utils;

import android.app.Activity;
import android.view.View;
import android.view.ViewTreeObserver;

public class KeyBoardUtils {
    private static final String TAG = KeyBoardUtils.class.getSimpleName();
    static int mAppHeight;
    static int currentOrientation = -1;

    public static void setKeyboardVisibilityListener(final Activity activity, final View contentView, final KeyboardVisibilityListener keyboardVisibilityListener) {
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int mPreviousHeight;

            @Override
            public void onGlobalLayout() {
                int newHeight = contentView.getHeight();
                if (newHeight == mPreviousHeight) {
                    return;
                }
                mPreviousHeight = newHeight;
                if (activity.getResources().getConfiguration().orientation != currentOrientation) {
                    currentOrientation = activity.getResources().getConfiguration().orientation;
                    mAppHeight = 0;
                }

                if (newHeight >= mAppHeight) {
                    mAppHeight = newHeight;
                }
                if (newHeight != 0) {
                    boolean isKeyboardVisible = mAppHeight > newHeight;
                    keyboardVisibilityListener.onKeyboardVisibilityChanged(isKeyboardVisible);
                    keyboardVisibilityListener.onKeyboardVisibilityChanged(mAppHeight > newHeight);
                    if (!isKeyboardVisible) {
                        View currentFocused = activity.getCurrentFocus();
                        if (currentFocused != null) {
                            currentFocused.clearFocus();
                        }
                    }
                }
            }
        });
    }
}
