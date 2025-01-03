package com.cometchat.chatuikit.shared.resources.utils.sticker_header;

import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

public interface StickyHeaderAdapter<T extends RecyclerView.ViewHolder> {

    long getHeaderId(int var1);

    T onCreateHeaderViewHolder(ViewGroup var1);

    void onBindHeaderViewHolder(T var1, int var2, long var3);
}
