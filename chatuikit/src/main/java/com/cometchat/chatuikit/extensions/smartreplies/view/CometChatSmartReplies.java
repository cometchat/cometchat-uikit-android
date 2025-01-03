package com.cometchat.chatuikit.extensions.smartreplies.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chat.core.CometChat;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.cometchatuikit.CometChatUIKit;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.resources.utils.recycler_touch.ClickListener;
import com.cometchat.chatuikit.shared.resources.utils.recycler_touch.RecyclerTouchListener;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * Purpose - SmartReply class is a subclass of recyclerview and used as
 * component by developer to display Smart Reply in his message list. Developer
 * just need to pass the list of reply at their end received at their end. It
 * helps user show smart reply at their end easily.
 *
 * <p>
 * Created on - 23th January 2020
 *
 * <p>
 * Modified on - 28th february 2023
 */
public class CometChatSmartReplies extends MaterialCardView {
    private static final String TAG = CometChatSmartReplies.class.getSimpleName();
    private ImageView closeIcon;
    private SmartRepliesAdapter smartReplyListAdapter;
    private onClose onClose;
    private onClick onClick;

    public CometChatSmartReplies(@NonNull Context context) {
        super(context);
        initComponent(context, null, 0);
    }

    public CometChatSmartReplies(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initComponent(context, attrs, 0);
    }

    public CometChatSmartReplies(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initComponent(context, attrs, defStyleAttr);
    }

    private void initComponent(Context context, AttributeSet attrs, int defStyleAttr) {
        Utils.initMaterialCard(this);
        View view = View.inflate(context, R.layout.cometchat_smart_reply_layout, null);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        closeIcon = view.findViewById(R.id.close);

        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        smartReplyListAdapter = new SmartRepliesAdapter(context);
        recyclerView.setAdapter(smartReplyListAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(context, recyclerView, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                String reply = (String) view.getTag(R.string.cometchat_reply_lowercase);
                if (onClick != null) onClick.onClick(reply, position);
            }

            @Override
            public void onLongClick(View view, int position) {
                String reply = (String) view.getTag(R.string.cometchat_reply_lowercase);
                if (onClick != null) onClick.onLongClick(reply, position);
            }
        }));

        closeIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onClose != null) {
                    onClose.onClose(view);
                }
            }
        });
        addView(view);
    }

    /**
     * This method is used to set list of replies in SmartReplyComponent.
     *
     * @param replyList is object of {@code List<String>} . It is list of smart replies.
     */
    public void setSmartReplyList(List<String> replyList) {
        if (smartReplyListAdapter != null && !replyList.isEmpty()) {
            smartReplyListAdapter.updateList(replyList);
            smartReplyListAdapter.notifyDataSetChanged();
        }
    }

    public void setCloseIcon(@DrawableRes int icon) {
        if (icon != 0) closeIcon.setImageResource(icon);
    }

    public void setCloseIcon(@NonNull Drawable icon) {
        closeIcon.setImageDrawable(icon);
    }

    public void setOnClick(CometChatSmartReplies.onClick onClick) {
        this.onClick = onClick;
    }

    public void setOnClose(CometChatSmartReplies.onClose onClose) {
        this.onClose = onClose;
    }

    public interface onClose {
        void onClose(View view);
    }

    public interface onClick {
        void onClick(String text, int pos);

        void onLongClick(String text, int pos);
    }
}
