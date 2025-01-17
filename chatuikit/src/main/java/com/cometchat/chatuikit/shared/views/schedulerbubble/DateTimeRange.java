package com.cometchat.chatuikit.shared.views.schedulerbubble;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateTimeRange implements Comparable<DateTimeRange> {
    private static final String TAG = DateTimeRange.class.getSimpleName();
    public Calendar from;
    public Calendar to;

    public DateTimeRange(Calendar from, Calendar to) {
        this.from = from;
        this.to = to;
    }

    public Calendar getFrom() {
        return from;
    }

    public Calendar getTo() {
        return to;
    }

    @Override
    public int compareTo(DateTimeRange other) {
        if (!from.equals(other.from)) {
            return from.compareTo(other.from);
        } else {
            return to.compareTo(other.to);
        }
    }

    // check if the given two ranges collide somewhere
    public boolean isColliding(DateTimeRange other) {
        if (from.compareTo(other.from) >= 0 && from.compareTo(other.to) <= 0) {
            return true;
        } else if (to.compareTo(other.from) >= 0 && to.compareTo(other.to) <= 0) {
            return true;
        } else return from.compareTo(other.from) <= 0 && to.compareTo(other.to) >= 0;
    }

    @NonNull
    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return "TimeRange{from: " + dateFormat.format(from.getTime()) + ", to: " + dateFormat.format(to.getTime()) + '}';
    }
}
