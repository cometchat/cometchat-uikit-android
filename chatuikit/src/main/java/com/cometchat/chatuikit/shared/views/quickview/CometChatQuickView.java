package com.cometchat.chatuikit.shared.views.quickview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

public class CometChatQuickView extends MaterialCardView {
    private static final String TAG = CometChatQuickView.class.getSimpleName();
    private ImageView closeImage;
    private TextView title;
    private TextView subTitle;
    private MaterialCardView verticalBar, quickViewCard;

    public CometChatQuickView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        Utils.initMaterialCard(this);
        View view = View.inflate(context, R.layout.cometchat_quick_view, null);
        closeImage = view.findViewById(R.id.iv_message_close);
        title = view.findViewById(R.id.tv_message_layout_title);
        subTitle = view.findViewById(R.id.tv_message_layout_subtitle);
        verticalBar = view.findViewById(R.id.vertical_bar);
        quickViewCard = view.findViewById(R.id.quick_card);
        addView(view);
    }

    public CometChatQuickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CometChatQuickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setCloseClickListener(OnClickListener clickListener) {
        if (clickListener != null) closeImage.setOnClickListener(clickListener);
    }

    public void setCloseIcon(int icon) {
        if (icon != 0) closeImage.setImageResource(icon);
    }

    public void setCloseIconVisibility(int visibility) {
        closeImage.setVisibility(visibility);
    }

    public void setVerticalBarVisibility(int visibility) {
        verticalBar.setVisibility(visibility);
    }

    public ImageView getCloseImage() {
        return closeImage;
    }

    public TextView getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title != null && !title.isEmpty()) {
            setTitleVisibility(VISIBLE);
            this.title.setText(title);
        } else {
            setTitleVisibility(GONE);
        }
    }

    public void setTitleVisibility(int visibility) {
        title.setVisibility(visibility);
    }

    public TextView getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        if (subTitle != null && !subTitle.isEmpty()) {
            setSubTitleVisibility(VISIBLE);
            this.subTitle.setText(subTitle);
        } else {
            setSubTitleVisibility(GONE);
        }
    }

    public void setSubTitleVisibility(int visibility) {
        subTitle.setVisibility(visibility);
    }

    public MaterialCardView getVerticalBar() {
        return verticalBar;
    }

    public void setStyle(QuickViewStyle style) {
        if (style != null) {
            setTitleTextColor(style.getTitleColor());
            setSubtitleTextColor(style.getSubtitleColor());
            setTitleTextAppearance(style.getTitleAppearance());
            setSubtitleTextAppearance(style.getSubtitleAppearance());
            setCloseIconTint(style.getCloseIconTint());
            setCloseIconTint(style.getCloseIconTint());
            setLeadingBarTint(style.getLeadingBarTint());

            if (style.getDrawableBackground() != null)
                quickViewCard.setBackground(style.getDrawableBackground());
            else if (style.getBackground() != 0)
                quickViewCard.setCardBackgroundColor(style.getBackground());
            if (style.getStrokeWidth() >= 0) quickViewCard.setStrokeWidth(style.getStrokeWidth());
            if (style.getCornerRadius() >= 0) quickViewCard.setRadius(style.getCornerRadius());
            if (style.getStrokeColor() != 0) quickViewCard.setStrokeColor(style.getStrokeColor());
        }
    }

    public void setTitleTextColor(int color) {
        if (color != 0) title.setTextColor(color);
    }

    public void setSubtitleTextColor(int color) {
        if (color != 0) subTitle.setTextColor(color);
    }

    public void setTitleTextAppearance(int style) {
        if (style != 0) title.setTextAppearance(style);
    }

    public void setSubtitleTextAppearance(int style) {
        if (style != 0) subTitle.setTextAppearance(style);
    }

    public void setCloseIconTint(int color) {
        if (color != 0) closeImage.setColorFilter(color);
    }

    public void setLeadingBarTint(int color) {
        if (color != 0) verticalBar.setCardBackgroundColor(color);
    }
}
