package com.cometchat.chatuikit.shared.views.schedulerbubble;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.views.avatar.CometChatAvatar;
import com.google.android.material.card.MaterialCardView;

public class SchedulerBubbleHeader extends MaterialCardView {
    private static final String TAG = SchedulerBubbleHeader.class.getSimpleName();


    private ImageView ivBackArrow;
    private CometChatAvatar cometchatAvatar;
    private TextView tvTitle, tvSubTitle;
    private LinearLayout parentView, schedulerDetails;
    private ImageView subtitleImage;

    public SchedulerBubbleHeader(Context context) {
        super(context);
        init();
    }

    private void init() {
        Utils.initMaterialCard(this);
        View view = View.inflate(getContext(), R.layout.cometchat_meeting_bubble_header, null);
        ivBackArrow = view.findViewById(R.id.back_arrow);
        cometchatAvatar = view.findViewById(R.id.meeting_avatar);
        tvTitle = view.findViewById(R.id.scheduler_name);
        tvSubTitle = view.findViewById(R.id.schedule_duration);
        parentView = view.findViewById(R.id.parent);
        schedulerDetails = view.findViewById(R.id.scheduler_details);
        subtitleImage = view.findViewById(R.id.subtitle_image);
        cometchatAvatar.setRadius(1000);
        addView(view);
    }

    public SchedulerBubbleHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SchedulerBubbleHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setAvatar(String url, String name) {
        cometchatAvatar.setAvatar(name, url);
    }

    public void setSubtitleImageTint(@ColorInt int color) {
        if (color != 0) subtitleImage.setImageTintList(ColorStateList.valueOf(color));
    }

    public void setSubtitleImageSize(int size) {
        if (size > 0) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) subtitleImage.getLayoutParams();
            layoutParams.width = Utils.convertDpToPx(getContext(), size);
            layoutParams.height = Utils.convertDpToPx(getContext(), size);
            subtitleImage.setLayoutParams(layoutParams);
        }
    }

    public void setTitle(String name) {
        if (name != null && !name.isEmpty()) {
            tvTitle.setText(name);
        } else {
            tvTitle.setVisibility(View.GONE);
        }
    }

    public void setSubTitle(String subTitle) {
        if (subTitle != null && !subTitle.isEmpty()) {
            tvSubTitle.setText(subTitle);
        } else {
            tvSubTitle.setVisibility(View.GONE);
        }
    }

    public void setBackArrowClickListener(OnClickListener listener) {
        ivBackArrow.setOnClickListener(listener);
    }

    public void setAvatarClickListener(OnClickListener listener) {
        cometchatAvatar.setOnClickListener(listener);
    }

    public void setSubtitleTextColor(int color) {
        tvSubTitle.setTextColor(color);
    }

    public void setTitleTextColor(int color) {
        tvTitle.setTextColor(color);
    }

    public void setBackArrowTint(int color) {
        ivBackArrow.setImageTintList(ColorStateList.valueOf(color));
    }

    public void setTextAppearance(int style) {
        tvTitle.setTypeface(null, style);
    }

    public void setAvatarStyle(@StyleRes int avatarStyle) {
        cometchatAvatar.setStyle(avatarStyle);
    }

    public void setTitleTextAppearance(int resId) {
        if (resId != 0) tvTitle.setTextAppearance(resId);
    }

    public void setSubTitleTextAppearance(int resId) {
        if (resId != 0) tvSubTitle.setTextAppearance(resId);
    }

    public void setLayoutOrientation(int orientation) {
        if (orientation == LinearLayout.VERTICAL) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) schedulerDetails.getLayoutParams();
            params.setMarginStart(0);
            params.topMargin = Utils.convertDpToPx(getContext(), 8);
            schedulerDetails.setLayoutParams(params);
        } else if (orientation == LinearLayout.HORIZONTAL) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) schedulerDetails.getLayoutParams();
            params.setMarginStart(Utils.convertDpToPx(getContext(), 5));
            params.topMargin = 0;
            schedulerDetails.setLayoutParams(params);
        }
        parentView.setOrientation(orientation);
    }

    private void showOnlyTitleAndArrow() {
        ivBackArrow.setVisibility(VISIBLE);
        cometchatAvatar.setVisibility(View.GONE);
        tvSubTitle.setVisibility(View.GONE);
        subtitleImage.setVisibility(View.GONE);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) schedulerDetails.getLayoutParams();
        params.setMarginStart(0);
        params.topMargin = Utils.convertDpToPx(getContext(), 0);
        setTextAlignment(TEXT_ALIGNMENT_TEXT_START);
        schedulerDetails.setLayoutParams(params);
        parentView.setOrientation(LinearLayout.HORIZONTAL);
    }

    public void setTextAlignment(int alignment) {
        tvTitle.setTextAlignment(alignment);
        tvSubTitle.setTextAlignment(alignment);
    }

    public void showInVerticalMode() {
        cometchatAvatar.setVisibility(View.VISIBLE);
        tvSubTitle.setVisibility(View.GONE);
        subtitleImage.setVisibility(View.GONE);
        ivBackArrow.setVisibility(View.GONE);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) schedulerDetails.getLayoutParams();
        params.setMarginStart(0);
        params.topMargin = Utils.convertDpToPx(getContext(), 8);
        schedulerDetails.setLayoutParams(params);

        setGravity(Gravity.CENTER);
        setTextAlignment(TEXT_ALIGNMENT_CENTER);
        parentView.setOrientation(LinearLayout.VERTICAL);
    }

    public void setGravity(int gravity) {
        parentView.setGravity(gravity);
    }

    public void showInHorizontalMode() {
        cometchatAvatar.setVisibility(View.VISIBLE);
        tvSubTitle.setVisibility(View.VISIBLE);
        subtitleImage.setVisibility(View.VISIBLE);
        ivBackArrow.setVisibility(VISIBLE);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) schedulerDetails.getLayoutParams();
        params.setMarginStart(Utils.convertDpToPx(getContext(), 5));
        params.topMargin = Utils.convertDpToPx(getContext(), 0);
        schedulerDetails.setLayoutParams(params);
        setTextAlignment(TEXT_ALIGNMENT_TEXT_START);
        parentView.setOrientation(LinearLayout.HORIZONTAL);
    }

    public CometChatAvatar getCometChatAvatar() {
        return cometchatAvatar;
    }

    public TextView getTitleView() {
        return tvTitle;
    }

    public TextView getSubTitleView() {
        return tvSubTitle;
    }

    public ImageView getBackArrowView() {
        return ivBackArrow;
    }

    public LinearLayout getSchedulerDetails() {
        return schedulerDetails;
    }

    public ImageView getSubtitleImage() {
        return subtitleImage;
    }

    public void setSubtitleImage(@DrawableRes int res) {
        if (res != 0) {
            subtitleImage.setImageResource(res);
        }
    }

    public LinearLayout getParentView() {
        return parentView;
    }
}
