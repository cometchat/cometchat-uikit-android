package com.cometchat.chatuikit.shared.interfaces;

import android.view.View;

public interface OnItemClick<T> {
    /**
     * Called when an item is clicked.
     *
     * @param view     the view that was clicked.
     * @param position the position of the clicked item in the adapter.
     * @param t        the object associated with the clicked item.
     */
    void click(View view, int position, T t);
}
