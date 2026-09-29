package com.cometchat.uikit.kotlin.shared.interfaces

public interface DateTimeFormatterCallback {
    public fun time(timestamp: Long): String? = null
    public fun today(timestamp: Long): String? = null
    public fun yesterday(timestamp: Long): String? = null
    public fun lastWeek(timestamp: Long): String? = null
    public fun otherDays(timestamp: Long): String? = null
    public fun minute(timestamp: Long): String? = null
    public fun minutes(diffInMinutesFromNow: Long, timestamp: Long): String? = null
    public fun hour(timestamp: Long): String? = null
    public fun hours(diffInHourFromNow: Long, timestamp: Long): String? = null
}