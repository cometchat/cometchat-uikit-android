package com.cometchat.chatuikit.shared.views.timeslotitem;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

public class CometChatTimeSlotItem extends MaterialCardView {
    private static final String TAG = CometChatTimeSlotItem.class.getSimpleName();
    private TextView timeSlot;
    private LinearLayout timeScheduleLayout;

    public CometChatTimeSlotItem(Context context) {
        super(context);
        init();
    }

    public CometChatTimeSlotItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CometChatTimeSlotItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Utils.initMaterialCard(this);
        View view = View.inflate(getContext(), R.layout.cometchat_time_schedule_row, null);
        timeSlot = view.findViewById(R.id.time_slot);
        timeScheduleLayout = view.findViewById(R.id.parent_layout);
        addView(view);
    }

    public void setTime(String time) {
        if (time != null && !time.isEmpty()) {
            timeSlot.setText(time);
        }
    }

    public void setTimeTextColor(@ColorInt int color) {
        if (color != 0) timeSlot.setTextColor(color);
    }

    public void setPadding(int left, int top, int right, int bottom) {
        timeScheduleLayout.setPadding(left, top, right, bottom);
    }

    public void setTimeTextSize(int size) {
        if (size != 0) timeSlot.setTextSize(size);
    }

    public void timeTextAppearance(@StyleRes int style) {
        if (style != 0) timeSlot.setTextAppearance(style);
    }

    public void setStyle(TimeSlotItemStyle style) {
        if (style != null) {
            setTimeTextColor(style.getTimeColor());
            setTimeTextSize(style.getTimeTextAppearance());
            timeTextAppearance(style.getTimeTextAppearance());
            if (style.getDrawableBackground() != null)
                this.setBackground(style.getDrawableBackground());
            else if (style.getBackground() != 0) this.setCardBackgroundColor(style.getBackground());
            if (style.getStrokeWidth() >= 0) this.setStrokeWidth(style.getStrokeWidth());
            if (style.getCornerRadius() >= 0) this.setRadius(style.getCornerRadius());
            if (style.getStrokeColor() != 0) this.setStrokeColor(style.getStrokeColor());
        }
    }

    public TextView getTimeSlot() {
        return timeSlot;
    }
}
