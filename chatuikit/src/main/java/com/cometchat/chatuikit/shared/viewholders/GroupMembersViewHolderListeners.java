package com.cometchat.chatuikit.shared.viewholders;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.GroupMember;
import com.cometchat.chatuikit.databinding.CometchatGroupMemberListItemBinding;
import com.cometchat.chatuikit.shared.interfaces.ViewHolderCallBack;

import java.util.List;

public abstract class GroupMembersViewHolderListeners implements ViewHolderCallBack {
    private static final String TAG = GroupMembersViewHolderListeners.class.getSimpleName();

    public abstract View createView(Context context, CometchatGroupMemberListItemBinding listItem);

    public abstract void bindView(Context context, View createdView, GroupMember groupMember, Group group, RecyclerView.ViewHolder holder, List<GroupMember> groupMemberList, int position);
}
