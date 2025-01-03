package com.cometchat.chatuikit.shared.viewholders;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.Group;
import com.cometchat.chatuikit.databinding.CometchatListBaseItemsBinding;
import com.cometchat.chatuikit.shared.interfaces.ViewHolderCallBack;

import java.util.List;

public abstract class GroupsViewHolderListener implements ViewHolderCallBack {
    private static final String TAG = GroupsViewHolderListener.class.getSimpleName();

    public abstract View createView(Context context, CometchatListBaseItemsBinding listItem);

    public abstract void bindView(Context context, View createdView, Group group, RecyclerView.ViewHolder holder, List<Group> groupList, int position);
}
