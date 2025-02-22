package com.cometchat.chatuikit.shared.views.reaction.emojikeyboard.emojikeyboardadapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.views.reaction.emojikeyboard.EmojiKeyBoardView;
import com.cometchat.chatuikit.shared.views.reaction.emojikeyboard.model.Emoji;

import java.util.ArrayList;
import java.util.List;

public class EmojiItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = EmojiItemAdapter.class.getSimpleName();
    private final EmojiKeyBoardView.OnClick onClick;
    private List<Emoji> emojiList;

    public EmojiItemAdapter(Context context, EmojiKeyBoardView.OnClick onClick) {
        emojiList = new ArrayList<>();
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EmojiItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cometchat_emoji_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((EmojiItemViewHolder) holder).bind(emojiList.get(position));
    }

    @Override
    public int getItemCount() {
        return emojiList.size();
    }

    public void setEmojiList(List<Emoji> emojiList) {
        this.emojiList = emojiList;
    }

    public class EmojiItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView cometchatEmojiItemText;

        public EmojiItemViewHolder(@NonNull View itemView) {
            super(itemView);
            cometchatEmojiItemText = itemView.findViewById(R.id.cometchat_emoji_item_text);
        }

        public void bind(Emoji emoji) {
            cometchatEmojiItemText.setText(emoji.getEmoji());
            cometchatEmojiItemText.setOnClickListener(v -> {
                if (onClick != null) {
                    onClick.onClick(emoji.getEmoji());
                }
            });

            cometchatEmojiItemText.setOnLongClickListener(v -> {
                if (onClick != null) {
                    onClick.onLongClick(emoji.getEmoji());
                }
                return true;
            });
        }
    }
}
