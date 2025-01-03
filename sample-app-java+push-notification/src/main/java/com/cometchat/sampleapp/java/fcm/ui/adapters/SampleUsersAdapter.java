package com.cometchat.sampleapp.java.fcm.ui.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.CometChatTheme;
import com.cometchat.sampleapp.java.fcm.data.interfaces.OnSampleUserSelected;
import com.cometchat.sampleapp.java.fcm.databinding.SampleUserItemsBinding;

import java.util.List;

public class SampleUsersAdapter extends RecyclerView.Adapter<SampleUsersAdapter.UserViewHolder> {

    private final List<User> userList;
    private final OnSampleUserSelected onSampleUserSelected;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public SampleUsersAdapter(List<User> userList, OnSampleUserSelected onSampleUserSelected) {
        this.userList = userList;
        this.onSampleUserSelected = onSampleUserSelected;
    }

    public void updateList(List<User> users) {
        userList.clear();
        userList.addAll(users);
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SampleUserItemsBinding binding = SampleUserItemsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(binding.getRoot());
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.binding.avatar.setAvatar(user.getName(), user.getAvatar());
        holder.binding.tvUserName.setText(user.getName());
        holder.binding.tvUserUid.setText(user.getUid());

        if (position == selectedPosition) {
            holder.binding.ivSelected.setVisibility(View.VISIBLE);
            holder.binding.cardUser.setStrokeColor(CometChatTheme.getStrokeColorHighlight(holder.itemView.getContext()));
            holder.binding.cardUser.setCardBackgroundColor(CometChatTheme.getExtendedPrimaryColor50(holder.itemView.getContext()));
        } else {
            holder.binding.ivSelected.setVisibility(View.GONE);
            holder.binding.cardUser.setStrokeColor(CometChatTheme.getStrokeColorLight(holder.itemView.getContext()));
            holder.binding.cardUser.setCardBackgroundColor(CometChatTheme.getBackgroundColor1(holder.itemView.getContext()));
        }

        holder.itemView.setOnClickListener(v -> {
            onSampleUserSelected.onSelect(user);
            selectedPosition = (selectedPosition == position) ? RecyclerView.NO_POSITION : position;
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        SampleUserItemsBinding binding;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = SampleUserItemsBinding.bind(itemView);
        }
    }
}
