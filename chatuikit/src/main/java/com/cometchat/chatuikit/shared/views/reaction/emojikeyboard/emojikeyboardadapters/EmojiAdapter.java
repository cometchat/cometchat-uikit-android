package com.cometchat.chatuikit.shared.views.reaction.emojikeyboard.emojikeyboardadapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.views.reaction.emojikeyboard.EmojiKeyBoardView;
import com.cometchat.chatuikit.shared.views.reaction.emojikeyboard.model.EmojiCategory;

import java.util.ArrayList;
import java.util.List;

public class EmojiAdapter extends RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder> {
    private static final String TAG = EmojiAdapter.class.getSimpleName();


    private final Context context;
    private final List<EmojiCategory> emojiCategories;
    private final Handler handler;
    private EmojiKeyBoardView.OnClick onClick;
    private @StyleRes int categoryTextAppearance;
    private @ColorInt int categoryTextColor;

    public EmojiAdapter(Context context, List<EmojiCategory> emojiCategories) {
        this.context = context;
        if (emojiCategories != null) this.emojiCategories = emojiCategories;
        else this.emojiCategories = new ArrayList<>();
        handler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    @Override
    public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EmojiViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cometchat_emoji_container, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
        EmojiCategory emojiCategory = emojiCategories.get(position);
        holder.bind(emojiCategory);
    }

    @Override
    public int getItemCount() {
        return emojiCategories.size();
    }

    public void setOnClick(EmojiKeyBoardView.OnClick onClick) {
        if (onClick != null) {
            this.onClick = onClick;
            notifyDataSetChanged();
        }
    }

    public void setCategoryTextAppearance(@StyleRes int categoryTextAppearance) {
        this.categoryTextAppearance = categoryTextAppearance;
        notifyDataSetChanged();
    }

    public void setCategoryTextColor(@ColorInt int categoryTextColor) {
        this.categoryTextColor = categoryTextColor;
        notifyDataSetChanged();
    }

    public class EmojiViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryName;
        private final EmojiItemAdapter emojiItemAdapter;

        public EmojiViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.category_name);
            RecyclerView emojiListRecyclerView = itemView.findViewById(R.id.emoji_list_view);
            emojiItemAdapter = new EmojiItemAdapter(context, onClick);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(context, 8);
            emojiListRecyclerView.setLayoutManager(gridLayoutManager);
            emojiListRecyclerView.setAdapter(emojiItemAdapter);
        }

        public void bind(EmojiCategory emojiCategory) {
            if (categoryTextAppearance != 0) {
                categoryName.setTextAppearance(categoryTextAppearance);
            }
            if (categoryTextColor != 0) {
                categoryName.setTextColor(categoryTextColor);
            }

            categoryName.setText(emojiCategory.getName());
            new Thread(() -> handler.post(() -> {
                emojiItemAdapter.setEmojiList(emojiCategory.getEmojis());
                emojiItemAdapter.notifyDataSetChanged();
            })).start();

            categoryName.setText(emojiCategory.getName());
        }
    }
}
