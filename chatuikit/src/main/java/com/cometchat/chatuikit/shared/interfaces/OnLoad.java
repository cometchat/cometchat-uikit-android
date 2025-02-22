package com.cometchat.chatuikit.shared.interfaces;

import java.util.List;

public interface OnLoad<T> {
    /**
     * Called when a list is loaded.
     *
     * @param list the list that was loaded.
     */
    void onLoad(List<T> list);
}
