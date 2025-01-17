package com.cometchat.chatuikit.extensions.polls.bubble.chipviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.views.avatar.CometChatAvatar;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("NotifyDataSetChanged")
public class ImageAndCountAdapter extends RecyclerView.Adapter<ImageAndCountAdapter.MyViewHolder> {
    private static final String TAG = ImageAndCountAdapter.class.getSimpleName();


    private final Context context;
    private List<ImageTextPoJo> images;
    private @StyleRes int avatarStyle;

    public ImageAndCountAdapter(Context context) {
        this.context = context;
        images = new ArrayList<>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cometchat_image_and_count_row_layout, parent, false);
        int overlap = 20;
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.setMargins(0, 0, viewType == 0 ? 0 : -overlap, 0);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        ImageTextPoJo imageTextPoJo = images.get(position);
        holder.bindView(imageTextPoJo, position);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void setList(List<ImageTextPoJo> images) {
        if (images != null && !images.isEmpty()) {
            this.images = images;
            notifyDataSetChanged();
        }
    }

    public void setAvatarStyle(@StyleRes int style) {
        this.avatarStyle = style;
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final CometChatAvatar avatar;
        private TextView emoji;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.cometchatAvatar);
            avatar.setRadius(context.getResources().getDimension(R.dimen.cometchat_radius_max));
        }

        public void bindView(ImageTextPoJo imageTextPoJo, int position) {
            avatar.setAvatar(imageTextPoJo.getText(), imageTextPoJo.getImageUrl());
            avatar.setStyle(avatarStyle);
        }
    }
}
