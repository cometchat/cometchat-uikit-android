package com.cometchat.chatuikit.shared.interfaces;

import java.util.List;

public interface OnSelection<T> {
    /**
     * Called when an item is selected.
     *
     * @param t the object associated with the selected item.
     */
    void onSelection(List<T> t);
}
