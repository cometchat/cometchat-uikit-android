package com.cometchat.chatuikit.shared.views.timeslotselector;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.views.schedulerbubble.DateTimeRange;
import com.cometchat.chatuikit.shared.views.timeslotitem.CometChatTimeSlotItem;
import com.cometchat.chatuikit.shared.views.timeslotitem.TimeSlotItemStyle;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private static final String TAG = MyAdapter.class.getSimpleName();
    private final Context context;
    private final int radius = 25;
    private List<DateTimeRange> items;
    private UIKitConstants.TimeFormat timeFormat;
    private SimpleDateFormat simpleDateFormat;
    private CometChatTimeSlotSelector.OnSelectionListener onSelectionListener;
    private TimeSlotItemStyle style;
    private TimeSlotItemStyle selectedStyle;
    private HashMap<String, Integer> selectedItems;

    public MyAdapter(Context context, List<DateTimeRange> items) {
        this.context = context;
        this.items = items;
        timeFormat = UIKitConstants.TimeFormat.TWELVE_HOUR;
        selectedItems = new HashMap<>();
        setSlotStyle(new TimeSlotItemStyle().setCornerRadius(radius).setTimeColor(Color.BLACK).setBackgroundColor(Color.WHITE));
        setSelectedSlotStyle(new TimeSlotItemStyle().setCornerRadius(radius).setTimeColor(Color.WHITE).setBackgroundColor(Color.BLUE));
        simpleDateFormat = new SimpleDateFormat("hh:mm a", Locale.US);
    }

    public void setSlotStyle(TimeSlotItemStyle style) {
        if (style != null) {
            this.style = style;
            notifyDataSetChanged();
        }
    }

    public void setSelectedSlotStyle(TimeSlotItemStyle selectedStyle) {
        if (selectedStyle != null) {
            this.selectedStyle = selectedStyle;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cometchat_time_slot_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyAdapter.MyViewHolder holder, int position) {
        DateTimeRange dateTimeRange = items.get(position);

        // Set time format
        if (UIKitConstants.TimeFormat.TWENTY_FOUR_HOUR.equals(timeFormat)) {
            simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.US);
        }

        // Format and set time
        String lastMessageTime = simpleDateFormat.format(dateTimeRange.getFrom().getTime());
        holder.timeSchedulerItem.setTime(lastMessageTime);
        holder.timeSchedulerItem.setTimeTextSize(11);

        // Set onClickListener
        holder.timeSchedulerItem.setOnClickListener(view -> {
            // Handle selected items safely
            Integer tempPos = selectedItems.get(UIKitConstants.SchedulerConstants.AVAILABLE);
            selectedItems.put(UIKitConstants.SchedulerConstants.AVAILABLE, position);
            if (tempPos != null) {
                notifyItemChanged(tempPos);
            }

            // Apply style and radius
            holder.timeSchedulerItem.setStyle(selectedStyle);
            holder.timeSchedulerItem.setRadius(radius);

            // Notify selection listener
            if (onSelectionListener != null) {
                onSelectionListener.onSelected(context, view, dateTimeRange, position);
            }
        });

        // Apply style and radius for all items
        holder.timeSchedulerItem.setStyle(style);
        holder.timeSchedulerItem.setRadius(radius);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setTimeFormat(UIKitConstants.TimeFormat timeFormat) {
        if (timeFormat != null) {
            this.timeFormat = timeFormat;
            notifyDataSetChanged();
        }
    }

    public List<DateTimeRange> getItems() {
        return items;
    }

    public void setItems(List<DateTimeRange> items) {
        if (items != null) {
            this.items = items;
            notifyDataSetChanged();
        }
    }

    public void add(DateTimeRange dateTimeRange) {
        items.add(dateTimeRange);
        notifyDataSetChanged();
    }

    public void addAll(List<DateTimeRange> dateTimeRanges) {
        items.addAll(dateTimeRanges);
        notifyDataSetChanged();
    }

    public void remove(DateTimeRange dateTimeRange) {
        items.remove(dateTimeRange);
        notifyDataSetChanged();
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public DateTimeRange getItem(int position) {
        return items.get(position);
    }

    public void insert(DateTimeRange dateTimeRange, int position) {
        items.add(position, dateTimeRange);
        notifyDataSetChanged();
    }

    public void set(int position, DateTimeRange dateTimeRange) {
        items.set(position, dateTimeRange);
        notifyDataSetChanged();
    }

    public void update(DateTimeRange dateTimeRange) {
        int index = items.indexOf(dateTimeRange);
        if (index != -1) {
            items.set(index, dateTimeRange);
            notifyDataSetChanged();
        }
    }

    public void setOnSelectionListener(CometChatTimeSlotSelector.OnSelectionListener onSelectionListener) {
        if (onSelectionListener != null) {
            this.onSelectionListener = onSelectionListener;
        }
    }

    public HashMap<String, Integer> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(HashMap<String, Integer> selectedItems) {
        this.selectedItems = selectedItems;
        notifyDataSetChanged();
    }

    public TimeSlotItemStyle getSelectedStyle() {
        return selectedStyle;
    }

    public TimeSlotItemStyle getStyle() {
        return style;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public CometChatTimeSlotItem timeSchedulerItem;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            timeSchedulerItem = itemView.findViewById(R.id.time_scheduler_item);
        }
    }
}
