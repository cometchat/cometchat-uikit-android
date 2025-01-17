package com.cometchat.chatuikit.shared.views.timeslotselector;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.views.schedulerbubble.DateTimeRange;
import com.cometchat.chatuikit.shared.views.schedulerbubble.SchedulerUtils;
import com.cometchat.chatuikit.shared.views.timeslotitem.TimeSlotItemStyle;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class CometChatTimeSlotSelector extends MaterialCardView {
    private static final String TAG = CometChatTimeSlotSelector.class.getSimpleName();


    private RecyclerView recyclerView;
    private MyAdapter adapter;
    private List<DateTimeRange> items;
    private TextView selectedDateTextView, selectDateTextView, separator;
    private LinearLayout noTimeSlotView;
    private TextView emptyTimeSlotText;
    private ImageView emptyTimeSlotIcon, calenderImageView;

    public CometChatTimeSlotSelector(Context context) {
        super(context);
        init();
    }

    private void init() {
        Utils.initMaterialCard(this);
        View view = View.inflate(getContext(), R.layout.cometchat_time_slot, null);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        items = new ArrayList<>();
        adapter = new MyAdapter(getContext(), items);
        recyclerView.setAdapter(adapter);
        selectedDateTextView = view.findViewById(R.id.tv_selected_date);
        calenderImageView = view.findViewById(R.id.calender_img);
        separator = view.findViewById(R.id.separator);
        noTimeSlotView = view.findViewById(R.id.no_time_slot_layout);
        emptyTimeSlotText = view.findViewById(R.id.no_time_slot_text);
        emptyTimeSlotIcon = view.findViewById(R.id.no_time_slot_image);
        selectDateTextView = view.findViewById(R.id.select_time);
        addView(view);
    }

    public CometChatTimeSlotSelector(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CometChatTimeSlotSelector(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setSelectedDate(String date) {
        if (date != null && !date.isEmpty()) selectedDateTextView.setText(date);
    }

    public void setSelectDate(String date) {
        if (date != null && !date.isEmpty()) selectDateTextView.setText(date);
    }

    public void setSelectDateVisibility(int visibility) {
        selectDateTextView.setVisibility(visibility);
    }

    public void setSelectedDateVisibility(int visibility) {
        selectedDateTextView.setVisibility(visibility);
    }

    public void setEmptyTimeSlotText(String text) {
        if (text != null && !text.isEmpty()) emptyTimeSlotText.setText(text);
    }

    public void setEmptyTimeSlotIcon(int resId) {
        if (resId != 0) emptyTimeSlotIcon.setImageResource(resId);
    }

    public void setEmptyTimeSlotVisibility(int visibility) {
        noTimeSlotView.setVisibility(visibility);
    }

    public void setEmptyTimeSlotIconVisibility(int visibility) {
        emptyTimeSlotIcon.setVisibility(visibility);
    }

    public void setTime(List<DateTimeRange> availability, List<DateTimeRange> otherMeetings, int duration, int bufferTime) {
        items.clear();
        items.addAll(SchedulerUtils.getFreeTimeSlots(availability, otherMeetings, duration, bufferTime));
        if (!items.isEmpty()) {
            noTimeSlotView.setVisibility(GONE);
            adapter.setItems(items);
            recyclerView.setVisibility(VISIBLE);
        } else {
            noTimeSlotView.setVisibility(VISIBLE);
            recyclerView.setVisibility(GONE);
        }
    }

    public void setStyle(TimeSlotSelectorStyle style) {
        if (style != null) {
            setEmptyTimeSlotIconTint(style.getEmptyTimeSlotIconTint());
            setEmptyTimeSlotTextColor(style.getEmptyTimeSlotTextColor());
            setEmptyTimeSlotTextAppearance(style.getEmptyTimeSlotTextAppearance());
            setSelectedDateTextAppearance(style.getChosenDateTextAppearance());
            setSelectedDateTextColor(style.getChosenDateTextColor());
            setSelectDateTextAppearance(style.getTitleTextAppearance());
            setSelectDateTextColor(style.getTitleColor());
            setSeparatorColor(style.getSeparatorColor());
            setCalenderImageTint(style.getCalenderImageTint());
            if (style.getDrawableBackground() != null)
                this.setBackground(style.getDrawableBackground());
            else if (style.getBackground() != 0) this.setCardBackgroundColor(style.getBackground());
            if (style.getStrokeWidth() >= 0) this.setStrokeWidth(style.getStrokeWidth());
            if (style.getCornerRadius() >= 0) this.setRadius(style.getCornerRadius());
            if (style.getStrokeColor() != 0) this.setStrokeColor(style.getStrokeColor());
        }
    }

    public void setEmptyTimeSlotIconTint(int color) {
        if (color != 0) emptyTimeSlotIcon.setImageTintList(ColorStateList.valueOf(color));
    }

    public void setEmptyTimeSlotTextColor(int color) {
        if (color != 0) emptyTimeSlotText.setTextColor(color);
    }

    public void setEmptyTimeSlotTextAppearance(int resId) {
        if (resId != 0) emptyTimeSlotText.setTextAppearance(resId);
    }

    public void setSelectedDateTextAppearance(int resId) {
        if (resId != 0) selectedDateTextView.setTextAppearance(resId);
    }

    public void setSelectedDateTextColor(int color) {
        if (color != 0) selectedDateTextView.setTextColor(color);
    }

    public void setSelectDateTextAppearance(int resId) {
        if (resId != 0) selectDateTextView.setTextAppearance(resId);
    }

    public void setSelectDateTextColor(int color) {
        if (color != 0) selectDateTextView.setTextColor(color);
    }

    public void setSeparatorColor(@ColorInt int separatorColor) {
        if (separatorColor != 0) {
            separator.setBackgroundColor(separatorColor);
        }
    }

    public void setCalenderImageTint(int color) {
        if (color != 0) {
            calenderImageView.setImageTintList(ColorStateList.valueOf(color));
        }
    }

    public void setSlotStyle(TimeSlotItemStyle slotStyle) {
        if (slotStyle != null) {
            adapter.setSlotStyle(slotStyle);
        }
    }

    public void setSelectedSlotStyle(TimeSlotItemStyle selectedSlotStyle) {
        if (selectedSlotStyle != null) {
            adapter.setSelectedSlotStyle(selectedSlotStyle);
        }
    }

    public void setOnSelectionListener(OnSelectionListener onSelectionListener) {
        if (onSelectionListener != null) {
            adapter.setOnSelectionListener(onSelectionListener);
        }
    }

    public interface OnSelectionListener {
        void onSelected(Context context, View view, DateTimeRange dateTimeRange, int position);
    }
}
