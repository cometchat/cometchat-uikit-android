package com.cometchat.chatuikit.extensions.smartreplies.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.resources.utils.FontUtils;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Purpose - UserListAdapter is a subclass of RecyclerView Adapter which is used
 * to display the list of users. It helps to organize the users in recyclerView.
 *
 * <p>
 * Created on - 20th December 2019
 *
 * <p>
 * Modified on - 23rd March 2020
 */
public class SmartRepliesAdapter extends RecyclerView.Adapter<SmartRepliesAdapter.SmartReplyViewHolder> {
    private static final String TAG = SmartRepliesAdapter.class.getSimpleName();
    private final Context context;

    private List<String> replyArrayList = new ArrayList<>();

    private final FontUtils fontUtils;

    /**
     * It is a constructor which is used to initialize wherever we needed.
     *
     * @param context is a object of Context.
     */
    public SmartRepliesAdapter(Context context) {
        this.context = context;
        fontUtils = FontUtils.getInstance();
    }

    /**
     * It is constructor which takes userArrayList as parameter and bind it with
     * userArrayList in adapter.
     *
     * @param context        is a object of Context.
     * @param replyArrayList is a list of users used in this adapter.
     */
    public SmartRepliesAdapter(Context context, List<String> replyArrayList) {
        this.replyArrayList = replyArrayList;
        this.context = context;
        fontUtils = FontUtils.getInstance();
    }

    @NonNull
    @Override
    public SmartReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cometchat_smartreply_row, parent, false);

        return new SmartReplyViewHolder(view);
    }

    /**
     * This method is used to bind the ReactionViewHolder contents with user at
     * given position. It set username userAvatar in respective ReactionViewHolder
     * content.
     *
     * @param smartReplyViewHolder is a object of ReactionViewHolder.
     * @param i                    is a position of item in recyclerView.
     * @see User
     */
    @Override
    public void onBindViewHolder(@NonNull SmartReplyViewHolder smartReplyViewHolder, int i) {
        smartReplyViewHolder.cardView.setCardElevation(2);
        final String reply = replyArrayList.get(i);
        smartReplyViewHolder.cReply.setText(reply);
        smartReplyViewHolder.itemView.setTag(R.string.cometchat_reply_lowercase, reply);
    }

    @Override
    public int getItemCount() {
        return replyArrayList.size();
    }

    public void updateList(List<String> replies) {
        this.replyArrayList = replies;
        notifyDataSetChanged();
    }

    public static class SmartReplyViewHolder extends RecyclerView.ViewHolder {
        private final TextView cReply;
        private final MaterialCardView cardView;

        SmartReplyViewHolder(View view) {
            super(view);
            cReply = view.findViewById(R.id.replyText);
            cardView = view.findViewById(R.id.card);
            Utils.initMaterialCard(cardView);
        }
    }
}
