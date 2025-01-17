package com.cometchat.chatuikit.calls.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.interfaces.OnClick;
import com.cometchat.chatuikit.shared.views.date.CometChatDate;
import com.google.android.material.card.MaterialCardView;

public class CallTailView extends MaterialCardView {
    private static final String TAG = CallTailView.class.getSimpleName();
    private CometChatDate chatDate;
    private ImageView infoButton;
    private OnClick onClick;

    public CallTailView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setStrokeWidth(0);
        View view = View.inflate(context, R.layout.cometchat_call_tail_view, null);
        chatDate = view.findViewById(R.id.call_time);
        infoButton = view.findViewById(R.id.conversations_item_container);
        infoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onClick != null) onClick.onClick();
            }
        });
        setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
        addView(view);
    }

    public CallTailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CallTailView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CometChatDate getChatDate() {
        return chatDate;
    }

    public void setIcon(@DrawableRes int icon) {
        if (icon != 0) {
            infoButton.setImageResource(icon);
        }
    }

    public void setIconTint(@ColorInt int color) {
        if (color != 0) {
            infoButton.setImageTintList(ColorStateList.valueOf(color));
        }
    }

    public void setOnClick(OnClick onClick) {
        this.onClick = onClick;
    }

    public ImageView getInfoButton() {
        return infoButton;
    }
}
