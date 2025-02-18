package com.cometchat.chatuikit.messageinformation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.BaseMessage;
import com.cometchat.chat.models.MessageReceipt;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.databinding.CometchatMessageInformationItemsBinding;
import com.cometchat.chatuikit.shared.resources.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MessageInformationAdapter extends RecyclerView.Adapter<MessageInformationAdapter.MessageInformtionViewHolder> {
    private static final String TAG = MessageInformationAdapter.class.getSimpleName();

    private final BaseMessage message;
    private List<MessageReceipt> messageReceipts;

    private @StyleRes int itemNameTextAppearance;
    private @ColorInt int itemNameTextColor;
    private @StyleRes int itemReadTextAppearance;
    private @ColorInt int itemReadTextColor;
    private @StyleRes int itemReadDateTextAppearance;
    private @ColorInt int itemReadDateTextColor;
    private @StyleRes int itemDeliveredTextAppearance;
    private @ColorInt int itemDeliveredTextColor;
    private @StyleRes int itemDeliveredDateTextAppearance;
    private @ColorInt int itemDeliveredDateTextColor;
    private @StyleRes int itemAvatarStyle;

    public MessageInformationAdapter(Context context, BaseMessage baseMessage) {
        this.messageReceipts = new ArrayList<>();
        this.message = baseMessage;
    }

    @NonNull
    @Override
    public MessageInformtionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CometchatMessageInformationItemsBinding binding = CometchatMessageInformationItemsBinding.inflate(LayoutInflater.from(parent.getContext()),
                                                                                                          parent,
                                                                                                          false);
        return new MessageInformtionViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull MessageInformtionViewHolder holder, int position) {
        MessageReceipt messageReceipt = messageReceipts.get(position);
        User user = messageReceipt.getSender();
        // Set the user avatar, name and style
        holder.binding.messageInformationAvatar.setAvatar(user.getName(), user.getAvatar());
        holder.binding.messageInformationAvatar.setStyle(itemAvatarStyle);
        // Set the user name, text appearance and color
        holder.binding.messageInformationName.setText(user.getName());
        holder.binding.messageInformationName.setTextAppearance(itemNameTextAppearance);
        holder.binding.messageInformationName.setTextColor(itemNameTextColor);
        // Set the read receipt text appearance and color
        if (messageReceipt.getReadAt() != 0) {
            holder.binding.messageInformationReadLayout.setVisibility(View.VISIBLE);
            holder.binding.messageInformationRead.setTextAppearance(itemReadTextAppearance);
            holder.binding.messageInformationRead.setTextColor(itemReadTextColor);
            holder.binding.messageInformationReadTimeStamp.setTextAppearance(itemReadDateTextAppearance);
            holder.binding.messageInformationReadTimeStamp.setTextColor(itemReadDateTextColor);
            holder.binding.messageInformationReadTimeStamp.setText(Utils.getDateTimeMessageInformation(messageReceipt.getReadAt() * 1000));
        } else {
            holder.binding.messageInformationReadLayout.setVisibility(View.GONE);
        }
        // Set the deliver receipt text appearance and color
        if (messageReceipt.getDeliveredAt() != 0) {
            holder.binding.messageInformationDeliveredLayout.setVisibility(View.VISIBLE);
            holder.binding.messageInformationDelivered.setTextAppearance(itemDeliveredTextAppearance);
            holder.binding.messageInformationDelivered.setTextColor(itemDeliveredTextColor);
            holder.binding.messageInformationDeliveredTimeStamp.setTextAppearance(itemDeliveredDateTextAppearance);
            holder.binding.messageInformationDeliveredTimeStamp.setTextColor(itemDeliveredDateTextColor);
            holder.binding.messageInformationDeliveredTimeStamp.setText(Utils.getDateTimeMessageInformation(messageReceipt.getDeliveredAt() * 1000));
        } else {
            holder.binding.messageInformationDeliveredLayout.setVisibility(View.GONE);
        }
        holder.itemView.setTag(R.string.cometchat_message_receipt, messageReceipt);
    }

    @Override
    public int getItemCount() {
        return messageReceipts.size();
    }

    public BaseMessage getMessage() {
        return message;
    }

    public List<MessageReceipt> getMessageReceipts() {
        return messageReceipts;
    }

    public void setMessageReceipts(List<MessageReceipt> messageReceipts) {
        this.messageReceipts = messageReceipts;
        notifyDataSetChanged();
    }

    public @StyleRes int getItemNameTextAppearance() {
        return itemNameTextAppearance;
    }

    public void setItemNameTextAppearance(@StyleRes int itemNameTextAppearance) {
        this.itemNameTextAppearance = itemNameTextAppearance;
        notifyDataSetChanged();
    }

    public @ColorInt int getItemNameTextColor() {
        return itemNameTextColor;
    }

    public void setItemNameTextColor(@ColorInt int itemNameTextColor) {
        this.itemNameTextColor = itemNameTextColor;
        notifyDataSetChanged();
    }

    public @StyleRes int getItemReadTextAppearance() {
        return itemReadTextAppearance;
    }

    public void setItemReadTextAppearance(@StyleRes int itemReadTextAppearance) {
        this.itemReadTextAppearance = itemReadTextAppearance;
        notifyDataSetChanged();
    }

    public @ColorInt int getItemReadTextColor() {
        return itemReadTextColor;
    }

    public void setItemReadTextColor(@ColorInt int itemReadTextColor) {
        this.itemReadTextColor = itemReadTextColor;
        notifyDataSetChanged();
    }

    public @StyleRes int getItemReadDateTextAppearance() {
        return itemReadDateTextAppearance;
    }

    public void setItemReadDateTextAppearance(@StyleRes int itemReadDateTextAppearance) {
        this.itemReadDateTextAppearance = itemReadDateTextAppearance;
        notifyDataSetChanged();
    }

    public @ColorInt int getItemReadDateTextColor() {
        return itemReadDateTextColor;
    }

    public void setItemReadDateTextColor(@ColorInt int itemReadDateTextColor) {
        this.itemReadDateTextColor = itemReadDateTextColor;
        notifyDataSetChanged();
    }

    public @StyleRes int getItemDeliveredTextAppearance() {
        return itemDeliveredTextAppearance;
    }

    public void setItemDeliveredTextAppearance(@StyleRes int itemDeliveredTextAppearance) {
        this.itemDeliveredTextAppearance = itemDeliveredTextAppearance;
        notifyDataSetChanged();
    }

    public @ColorInt int getItemDeliveredTextColor() {
        return itemDeliveredTextColor;
    }

    public void setItemDeliveredTextColor(@ColorInt int itemDeliveredTextColor) {
        this.itemDeliveredTextColor = itemDeliveredTextColor;
        notifyDataSetChanged();
    }

    public @StyleRes int getItemDeliveredDateTextAppearance() {
        return itemDeliveredDateTextAppearance;
    }

    public void setItemDeliveredDateTextAppearance(@StyleRes int itemDeliveredDateTextAppearance) {
        this.itemDeliveredDateTextAppearance = itemDeliveredDateTextAppearance;
        notifyDataSetChanged();
    }

    public @ColorInt int getItemDeliveredDateTextColor() {
        return itemDeliveredDateTextColor;
    }

    public void setItemDeliveredDateTextColor(@ColorInt int itemDeliveredDateTextColor) {
        this.itemDeliveredDateTextColor = itemDeliveredDateTextColor;
        notifyDataSetChanged();
    }

    public @StyleRes int getItemAvatarStyle() {
        return itemAvatarStyle;
    }

    public void setItemAvatarStyle(@StyleRes int itemAvatarStyle) {
        this.itemAvatarStyle = itemAvatarStyle;
        notifyDataSetChanged();
    }

    public static class MessageInformtionViewHolder extends RecyclerView.ViewHolder {
        private final CometchatMessageInformationItemsBinding binding;

        public MessageInformtionViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = CometchatMessageInformationItemsBinding.bind(itemView);
        }
    }
}
