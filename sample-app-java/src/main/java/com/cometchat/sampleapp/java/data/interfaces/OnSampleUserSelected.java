package com.cometchat.sampleapp.java.data.interfaces;

import com.cometchat.chat.models.User;

/** Interface to handle back press events. */
public interface OnSampleUserSelected {
    /** Called when the back button is pressed. */
    void onSelect(User user);
}
