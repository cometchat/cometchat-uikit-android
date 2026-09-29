package com.cometchat.uikit.compose.presentation.shared.interfaces

public interface DateTimeFormatterCallback {
    public fun time(timestamp: Long): String? {
        return null
    }

    public fun today(timestamp: Long): String? {
        return null
    }

    public fun yesterday(timestamp: Long): String? {
        return null
    }

    public fun lastWeek(timestamp: Long): String? {
        return null
    }

    public fun otherDays(timestamp: Long): String? {
        return null
    }

    public fun minute(timestamp: Long): String? {
        return null
    }

    public fun minutes(diffInMinutesFromNow: Long, timestamp: Long): String? {
        return null
    }

    public fun hour(timestamp: Long): String? {
        return null
    }

    public fun hours(diffInHourFromNow: Long, timestamp: Long): String? {
        return null
    }
}
