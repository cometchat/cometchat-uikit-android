package com.cometchat.chatuikit.shared.resources.utils.itemclicklistener;

public abstract class OnItemClickListener<T> {
    private static final String TAG = OnItemClickListener.class.getSimpleName();

    public abstract void OnItemClick(T var, int position);

    public void OnItemLongClick(T var, int position) {
    }
}
