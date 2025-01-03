package com.cometchat.chatuikit.shimmer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CometChatShimmerAdapter extends RecyclerView.Adapter<CometChatShimmerAdapter.ViewHolder> {
    private static final String TAG = CometChatShimmerAdapter.class.getSimpleName();
    private final int itemCount;
    private final @LayoutRes int layoutRes;

    public CometChatShimmerAdapter(int itemCount, @LayoutRes int itemLayout) {
        this.itemCount = itemCount;
        this.layoutRes = itemLayout;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
