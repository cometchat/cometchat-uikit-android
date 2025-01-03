package com.cometchat.chatuikit.shared.models.interactivemessage;

public class TimeRange {
    private static final String TAG = TimeRange.class.getSimpleName();


    private String from;
    private String to;

    // Constructor
    public TimeRange(String from, String to) {
        this.from = from;
        this.to = to;
    }

    // Getters and Setters
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
