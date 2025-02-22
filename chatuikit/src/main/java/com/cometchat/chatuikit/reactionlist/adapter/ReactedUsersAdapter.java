package com.cometchat.chatuikit.reactionlist.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.Reaction;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.databinding.ItemUserBinding;
import com.cometchat.chatuikit.reactionlist.OnReactionListItemClick;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.resources.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Purpose - ReactedUsersAdapter is a subclass of RecyclerView Adapter which is
 * used to display the list of users who reacted to a particular message. It
 * helps to set data to reactedUsersList and inflates the user item view to
 * display the user who reacted to the message. Created on - 17 September 2024
 * Modified on -
 */
public class ReactedUsersAdapter extends RecyclerView.Adapter<ReactedUsersAdapter.ReactedUsersViewHolder> {
    private static final String TAG = ReactedUsersAdapter.class.getSimpleName();
    private final Context context;
    private BaseMessage baseMessage;
    private List<Reaction> reactedUsersList = new ArrayList<>();
    private ReactedUsersAdapter.ReactedUsersAdapterEventListener reactedUsersAdapterEventListener;
    private OnReactionListItemClick onReactionListItemClick;
    private @StyleRes int avatarStyle;
    private @StyleRes int titleTextAppearance;
    private @ColorInt int titleTextColor;
    private @StyleRes int subTitleTextAppearance;
    private @ColorInt int subTitleTextColor;
    private @StyleRes int tailViewTextAppearance;
    /**
     * Constructor for ReactedUsersAdapter.
     *
     * @param context The context in which the adapter is used.
     */
    public ReactedUsersAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ReactedUsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ReactedUsersViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull ReactedUsersViewHolder holder, int position) {
        final int currentPosition = position;

        holder.binding.avatar.setStyle(getAvatarStyle());
        holder.binding.avatar.setAvatar(reactedUsersList.get(currentPosition).getReactedBy().getName(),
                                        reactedUsersList.get(currentPosition).getReactedBy().getAvatar());

        if (reactedUsersList.get(currentPosition).getUid().equals(CometChatUIKit.getLoggedInUser().getUid())) {
            holder.binding.tvTitle.setText(context.getString(R.string.cometchat_you));
            holder.binding.tvTitle.setTextAppearance(getTitleTextAppearance());
            holder.binding.tvTitle.setTextColor(getTitleTextColor());

            holder.binding.tvSubtitle.setVisibility(View.VISIBLE);
            holder.binding.tvSubtitle.setText(context.getString(R.string.cometchat_tap_to_remove));
            holder.binding.tvSubtitle.setTextAppearance(getSubTitleTextAppearance());
            holder.binding.tvSubtitle.setTextColor(getSubTitleTextColor());
        } else {
            holder.binding.tvTitle.setText(reactedUsersList.get(currentPosition).getReactedBy().getName());
            holder.binding.tvTitle.setTextAppearance(getTitleTextAppearance());
            holder.binding.tvTitle.setTextColor(getTitleTextColor());

            holder.binding.tvSubtitle.setVisibility(View.GONE);
        }

        TextView reactionText = new TextView(context);
        reactionText.setText(reactedUsersList.get(currentPosition).getReaction());
        reactionText.setTextAppearance(getTailViewTextAppearance());
        reactionText.setTextColor(getTitleTextColor());
        Utils.handleView(holder.binding.tailView, reactionText, true);

        holder.binding.parentLayout.setOnClickListener(v -> {
            if (reactedUsersAdapterEventListener != null) {
                reactedUsersAdapterEventListener.itemClicked(baseMessage, reactedUsersList, currentPosition);
            }
            if (onReactionListItemClick != null)
                onReactionListItemClick.onItemClick(reactedUsersList.get(currentPosition), baseMessage);
        });
    }

    @Override
    public int getItemCount() {
        return reactedUsersList.size();
    }

    /**
     * Gets the avatar style resource.
     *
     * @return The avatar style resource.
     */
    public @StyleRes int getAvatarStyle() {
        return avatarStyle;
    }

    /**
     * Sets the avatar style resource.
     *
     * @param avatarStyle The avatar style resource to set.
     */
    public void setAvatarStyle(@StyleRes int avatarStyle) {
        this.avatarStyle = avatarStyle;
    }

    /**
     * Gets the title text style resource.
     *
     * @return The title text style resource.
     */
    public @StyleRes int getTitleTextAppearance() {
        return titleTextAppearance;
    }

    /**
     * Sets the title text style resource.
     *
     * @param titleTextAppearance The title text style resource to set.
     */
    public void setTitleTextAppearance(@StyleRes int titleTextAppearance) {
        this.titleTextAppearance = titleTextAppearance;
    }

    /**
     * Gets the title text color.
     *
     * @return The title text color.
     */
    public @ColorInt int getTitleTextColor() {
        return titleTextColor;
    }

    /**
     * Sets the title text color.
     *
     * @param titleTextColor The title text color to set.
     */
    public void setTitleTextColor(@ColorInt int titleTextColor) {
        this.titleTextColor = titleTextColor;
    }

    /**
     * Gets the subtitle text style resource.
     *
     * @return The subtitle text style resource.
     */
    public @StyleRes int getSubTitleTextAppearance() {
        return subTitleTextAppearance;
    }

    /**
     * Sets the subtitle text style resource.
     *
     * @param subTitleTextAppearance The subtitle text style resource to set.
     */
    public void setSubTitleTextAppearance(@StyleRes int subTitleTextAppearance) {
        this.subTitleTextAppearance = subTitleTextAppearance;
    }

    /**
     * Gets the subtitle text color.
     *
     * @return The subtitle text color.
     */
    public @ColorInt int getSubTitleTextColor() {
        return subTitleTextColor;
    }

    /**
     * Sets the subtitle text color.
     *
     * @param subTitleTextColor The subtitle text color to set.
     */
    public void setSubTitleTextColor(@ColorInt int subTitleTextColor) {
        this.subTitleTextColor = subTitleTextColor;
    }

    /**
     * Gets the tail view text style resource.
     *
     * @return The tail view text style resource.
     */
    public @StyleRes int getTailViewTextAppearance() {
        return tailViewTextAppearance;
    }

    /**
     * Sets the tail view text style resource.
     *
     * @param tailViewTextAppearance The tail view text style resource to set.
     */
    public void setTailViewTextAppearance(@StyleRes int tailViewTextAppearance) {
        this.tailViewTextAppearance = tailViewTextAppearance;
    }

    /**
     * Gets the base message associated with the reactions.
     *
     * @return The base message.
     */
    public BaseMessage getBaseMessage() {
        return baseMessage;
    }

    /**
     * Sets the base message associated with the reactions.
     *
     * @param baseMessage The base message to set.
     */
    public void setBaseMessage(BaseMessage baseMessage) {
        this.baseMessage = baseMessage;
    }

    /**
     * Gets the event listener for the ReactedUsersAdapter.
     *
     * @return The event listener for the ReactedUsersAdapter.
     */
    public ReactedUsersAdapterEventListener getReactedUsersAdapterEventListener() {
        return reactedUsersAdapterEventListener;
    }

    /**
     * Sets the event listener for the ReactedUsersAdapter.
     *
     * @param reactedUsersAdapterEventListener The event listener to set.
     */
    public void setReactedUsersAdapterEventListener(ReactedUsersAdapterEventListener reactedUsersAdapterEventListener) {
        this.reactedUsersAdapterEventListener = reactedUsersAdapterEventListener;
    }

    /**
     * Gets the item click listener for the ReactedUsersAdapter.
     *
     * @return The item click listener for the ReactedUsersAdapter.
     */
    public OnReactionListItemClick getOnReactionListItemClick() {
        return onReactionListItemClick;
    }

    /**
     * Sets the item click listener for the ReactedUsersAdapter.
     *
     * @param onReactionListItemClick The item click listener to set.
     */
    public void setOnReactionListItemClick(OnReactionListItemClick onReactionListItemClick) {
        this.onReactionListItemClick = onReactionListItemClick;
    }

    /**
     * Gets the list of users who reacted.
     *
     * @return The list of users who reacted.
     */
    public List<Reaction> getReactedUsersList() {
        return reactedUsersList;
    }

    /**
     * Sets the list of users who reacted.
     *
     * @param reactedUsersList The list of users who reacted to set.
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setReactedUsersList(List<Reaction> reactedUsersList) {
        this.reactedUsersList = reactedUsersList;
        notifyDataSetChanged();
    }

    /**
     * Interface for handling item click events in the ReactedUsersAdapter.
     */
    public interface ReactedUsersAdapterEventListener {
        /**
         * Called when an item in the list is clicked.
         *
         * @param baseMessage  The base message associated with the reaction.
         * @param reactionList The list of reactions.
         * @param position     The position of the clicked item.
         */
        void itemClicked(BaseMessage baseMessage, List<Reaction> reactionList, int position);
    }

    /**
     * ViewHolder class for ReactedUsersAdapter.
     */
    public static class ReactedUsersViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserBinding binding;

        /**
         * Constructor for ReactedUsersViewHolder.
         *
         * @param view The view to bind.
         */
        public ReactedUsersViewHolder(@NonNull View view) {
            super(view);
            binding = ItemUserBinding.bind(itemView);
        }
    }
}
