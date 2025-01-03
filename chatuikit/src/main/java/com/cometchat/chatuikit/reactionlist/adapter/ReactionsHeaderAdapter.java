package com.cometchat.chatuikit.reactionlist.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.ReactionCount;
import com.cometchat.chatuikit.databinding.ItemEmojiBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter class for displaying reaction headers in a RecyclerView. This adapter
 * handles the display and interaction of reaction headers.
 */
public class ReactionsHeaderAdapter extends RecyclerView.Adapter<ReactionsHeaderAdapter.EmojiViewHolder> {
    private static final String TAG = ReactionsHeaderAdapter.class.getSimpleName();

    /**
     * Interface for handling events in the EmojiAdapter.
     */
    public interface EmojiAdapterEventListener {
        /**
         * Method to get the list of users who reacted with a specific emoji.
         *
         * @param emoji The emoji for which to get the reacted users list.
         */
        void getReactedUsersList(String emoji);
    }

    private String reactionFilter;
    private int activeTab = 0;

    private final List<ReactionCount> reactionHeaderList;
    private EmojiAdapterEventListener emojiAdapterEventListener;

    private @ColorInt int textColor;
    private @StyleRes int textAppearance;
    private @ColorInt int textActiveColor;
    private @ColorInt int tabActiveIndicatorColor;

    /**
     * Constructor for ReactionsHeaderAdapter.
     *
     * @param context The context in which the adapter is used.
     */
    public ReactionsHeaderAdapter(Context context) {
        reactionHeaderList = new ArrayList<>();
    }

    @NonNull
    @Override
    public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEmojiBinding binding = ItemEmojiBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new EmojiViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
        final int currentIndex = position;
        String text = reactionHeaderList.get(currentIndex).getReaction() + " " + reactionHeaderList.get(currentIndex).getCount();
        holder.binding.tvText.setText(text);
        holder.binding.tvText.setTextAppearance(textAppearance);
        holder.binding.tvText.setTextColor(currentIndex == activeTab ? textActiveColor : textColor);
        holder.binding.tabIndicator.setBackgroundColor(tabActiveIndicatorColor);
        if (currentIndex == activeTab) {
            holder.binding.tabIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tabIndicator.setVisibility(View.GONE);
        }
        holder.binding.layoutReactionTab.setOnClickListener(v -> {
            notifyItemChanged(activeTab);
            activeTab = currentIndex;
            if (emojiAdapterEventListener != null) {
                emojiAdapterEventListener.getReactedUsersList(reactionHeaderList.get(currentIndex).getReaction());
            }
            notifyItemChanged(activeTab);
        });
    }

    @Override
    public int getItemCount() {
        return reactionHeaderList.size();
    }

    /**
     * Method to set the active tab.
     *
     * @param activeTab The index of the tab to set as active.
     */
    public void setActiveTab(int activeTab) {
        this.activeTab = activeTab;
    }

    /**
     * Method to get the active tab.
     *
     * @return The index of the active tab.
     */
    public int getActiveTab() {
        return this.activeTab;
    }

    /**
     * Method to notify the adapter that the data set has changed.
     */
    public @ColorInt int getTextColor() {
        return textColor;
    }

    /**
     * Method to set the text color for the reaction headers.
     *
     * @param textColor The color to set for the text.
     */
    public void setTextColor(@ColorInt int textColor) {
        this.textColor = textColor;
    }

    /**
     * Method to get the text style for the reaction headers.
     *
     * @return The text style for the reaction headers.
     */
    public @StyleRes int getTextAppearance() {
        return textAppearance;
    }

    /**
     * Method to set the text style for the reaction headers.
     *
     * @param textAppearance The style to set for the text.
     */
    public void setTextAppearance(@StyleRes int textAppearance) {
        this.textAppearance = textAppearance;
    }

    /**
     * Method to get the active text color for the reaction headers.
     *
     * @return The active text color for the reaction headers.
     */
    public @ColorInt int getTextActiveColor() {
        return textActiveColor;
    }

    /**
     * Method to set the active text color for the reaction headers.
     *
     * @param textActiveColor The color to set for the active text.
     */
    public void setTextActiveColor(@ColorInt int textActiveColor) {
        this.textActiveColor = textActiveColor;
    }

    /**
     * Method to get the color of the active tab indicator.
     *
     * @return The color of the active tab indicator.
     */
    public @ColorInt int getTabActiveIndicatorColor() {
        return tabActiveIndicatorColor;
    }

    /**
     * Method to set the color of the active tab indicator.
     *
     * @param tabActiveIndicatorColor The color to set for the active tab indicator.
     */
    public void setTabActiveIndicatorColor(@ColorInt int tabActiveIndicatorColor) {
        this.tabActiveIndicatorColor = tabActiveIndicatorColor;
    }

    /**
     * Method to get the EmojiAdapterEventListener.
     *
     * @return The EmojiAdapterEventListener.
     */
    public EmojiAdapterEventListener getEmojiAdapterEventListener() {
        return emojiAdapterEventListener;
    }

    /**
     * Method to set the EmojiAdapterEventListener.
     *
     * @param emojiAdapterEventListener The EmojiAdapterEventListener to set.
     */
    public void setEmojiAdapterEventListener(EmojiAdapterEventListener emojiAdapterEventListener) {
        this.emojiAdapterEventListener = emojiAdapterEventListener;
    }

    /**
     * Method to get the reaction filter.
     *
     * @return The reaction filter.
     */
    public String getReactionFilter() {
        return this.reactionFilter;
    }

    /**
     * Method to set the reaction filter.
     *
     * @param reactionFilter The reaction filter to set.
     */
    public void setReactionFilter(String reactionFilter) {
        this.reactionFilter = reactionFilter;
        if (!reactionHeaderList.isEmpty()) {
            activeTab = getReactionIndexByReactionFilter(reactionFilter);
        }
    }

    /**
     * Method to get the list of reaction headers.
     *
     * @return The list of reaction headers.
     */
    public List<ReactionCount> getReactionHeaderList() {
        return reactionHeaderList;
    }

    /**
     * Method to update the list of reaction headers.
     *
     * @param reactionHeaderList The list of reaction headers to set.
     */
    public void updateReactionHeaderList(@NonNull List<ReactionCount> reactionHeaderList) {
        this.reactionHeaderList.clear();
        this.reactionHeaderList.addAll(reactionHeaderList);
        notifyDataSetChanged();
    }

    /**
     * Method to get the index of a reaction in the reaction header list.
     *
     * @param reactionFilter The reaction to find in the list.
     * @return The index of the reaction in the list.
     */
    private int getReactionIndexByReactionFilter(String reactionFilter) {
        int foundIndex = -1;
        for (int i = 0; i < reactionHeaderList.size(); i++) {
            if (reactionHeaderList.get(i).getReaction().equals(reactionFilter)) {
                foundIndex = i;
                break;
            }
        }
        return foundIndex;
    }

    /**
     * ViewHolder class for the ReactionsHeaderAdapter.
     */
    public static class EmojiViewHolder extends RecyclerView.ViewHolder {
        private final ItemEmojiBinding binding;

        /**
         * Constructor for EmojiViewHolder.
         *
         * @param itemView The view to bind.
         */
        public EmojiViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemEmojiBinding.bind(itemView);
        }
    }
}
