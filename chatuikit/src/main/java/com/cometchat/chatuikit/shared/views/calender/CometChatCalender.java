package com.cometchat.chatuikit.shared.views.calender;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

public class CometChatCalender extends MaterialCardView {
    private static final String TAG = CometChatCalender.class.getSimpleName();

    private TextView title;
    private CalendarView calender;
    private CalenderStyle style;

    public CometChatCalender(Context context) {
        super(context);
        init();
    }

    public CometChatCalender(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CometChatCalender(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Utils.initMaterialCard(this);
        View view = View.inflate(getContext(), R.layout.cometchat_calender_layout, null);
        title = view.findViewById(R.id.tv_select_day);
        calender = view.findViewById(R.id.view_calendar);
        calender.setSelectedWeekBackgroundColor(getResources().getColor(R.color.cometchat_color_info, getContext().getTheme()));
        addView(view);
    }

    public void setTitle(String title) {
        if (title != null && !title.isEmpty()) {
            this.title.setText(title);
        }
    }

    public void setTitleTextColor(int color) {
        if (color != 0) this.title.setTextColor(color);
    }

    public void setTitleTextAppearance(int resId) {
        if (resId != 0) this.title.setTextAppearance(resId);
    }

    public void setOnDateChangeListener(CalendarView.OnDateChangeListener onDateChangeListener) {
        if (onDateChangeListener != null) {
            this.calender.setOnDateChangeListener(onDateChangeListener);
        }
    }

    public void setStyle(CalenderStyle style) {
        if (style != null) {
            this.style = style;
            setTitleTextColor(style.getTitleTextColor());
            setTitleTextAppearance(style.getTitleTextAppearance());
            if (style.getDrawableBackground() != null)
                this.setBackground(style.getDrawableBackground());
            else if (style.getBackground() != 0) this.setCardBackgroundColor(style.getBackground());
            if (style.getStrokeWidth() >= 0) this.setStrokeWidth(style.getStrokeWidth());
            if (style.getCornerRadius() >= 0) this.setRadius(style.getCornerRadius());
            if (style.getStrokeColor() != 0) this.setStrokeColor(style.getStrokeColor());
        }
    }

    public void setMinDate(long minDate) {
        this.calender.setMinDate(minDate);
    }

    public void setMaxDate(long maxDate) {
        this.calender.setMaxDate(maxDate);
    }

    public CalendarView getCalender() {
        return calender;
    }

    public TextView getTitle() {
        return title;
    }

    public CalenderStyle getStyle() {
        return style;
    }
}
