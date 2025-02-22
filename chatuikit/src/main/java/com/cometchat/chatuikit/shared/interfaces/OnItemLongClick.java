package com.cometchat.chatuikit.shared.interfaces;

import android.view.View;

public interface OnItemLongClick<T> {
    /**
     * Called when an item is clicked.
     *
     * @param view     the view that was clicked.
     * @param position the position of the long clicked item in the adapter.
     * @param t        the object associated with the clicked item.
     */
    void longClick(View view, int position, T t);
}
