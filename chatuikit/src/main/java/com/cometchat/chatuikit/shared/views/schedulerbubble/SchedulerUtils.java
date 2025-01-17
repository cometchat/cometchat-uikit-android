package com.cometchat.chatuikit.shared.views.schedulerbubble;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactivemessage.SchedulerMessage;
import com.cometchat.chatuikit.shared.models.interactivemessage.TimeRange;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SchedulerUtils {
    private static final String TAG = SchedulerUtils.class.getSimpleName();

    @Nullable
    public static String convertTimeToSystemTimeZone(String time, String sourceTimeZoneId) {
        SimpleDateFormat sourceFormat = new SimpleDateFormat("HHmm", Locale.US);
        sourceFormat.setTimeZone(TimeZone.getTimeZone(sourceTimeZoneId));

        SimpleDateFormat targetFormat = new SimpleDateFormat("h:mm a", Locale.US);
        targetFormat.setTimeZone(TimeZone.getDefault());

        try {
            Date sourceTime = sourceFormat.parse(time);
            return targetFormat.format(sourceTime);
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
            return null;
        }
    }

    public static void readIcsFile(String fileUrl, Callback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HashMap<String, List<DateTimeRange>> meetings = new HashMap<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
            SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US); // for HashMap key

            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                StringBuilder fileContents = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        fileContents.append(line).append("\n");
                    }
                }

                connection.disconnect();

                // Parse the .ics file
                Pattern eventPattern = Pattern.compile("BEGIN:VEVENT.*?END:VEVENT", Pattern.DOTALL);
                Matcher eventMatcher = eventPattern.matcher(fileContents.toString());
                String timeZoneId = matchLine(fileContents.toString(), "X-WR-TIMEZONE");

                if (timeZoneId == null) {
                    timeZoneId = "UTC";
                }

                dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneId));

                while (eventMatcher.find()) {
                    String eventBlock = eventMatcher.group();
                    String start = matchLine(eventBlock, "DTSTART");
                    String end = matchLine(eventBlock, "DTEND");

                    if (start != null && end != null) {
                        Date startDate = dateFormat.parse(start);
                        Date endDate = dateFormat.parse(end);

                        Calendar startCalendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));
                        startCalendar.setTime(startDate);

                        Calendar endCalendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId));
                        endCalendar.setTime(endDate);

                        String dateKeyStart = dateKeyFormat.format(startCalendar.getTime());
                        String dateKeyEnd = dateKeyFormat.format(endCalendar.getTime());

                        DateTimeRange dateTimeRange = new DateTimeRange(convertToSystemTimeZone(startCalendar), convertToSystemTimeZone(endCalendar));

                        // Check if the meeting spans multiple days
                        if (!dateKeyStart.equals(dateKeyEnd)) {
                            // Split the meeting into parts for each day
                            splitMeeting(meetings, dateTimeRange, dateKeyStart, dateKeyFormat);
                        } else {
                            // Add dateTimeRange to the meetings map for a single day
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                meetings.computeIfAbsent(dateKeyStart, k -> new ArrayList<>()).add(dateTimeRange);
                            }
                        }
                    }
                }
                handler.post(() -> callback.onResult(meetings));
            } catch (Exception e) {
                CometChatLogger.e(TAG, e.toString());
            }
        });
    }

    private static String matchLine(String input, String keyword) {
        Pattern linePattern = Pattern.compile(keyword + ":([^:]*)");
        Matcher lineMatcher = linePattern.matcher(input);
        if (lineMatcher.find()) {
            return lineMatcher.group(1);
        } else {
            return null;
        }
    }

    private static Calendar convertToSystemTimeZone(Calendar originalTime) {
        String systemTimeZoneId = TimeZone.getDefault().getID();
        TimeZone systemTimeZone = TimeZone.getTimeZone(systemTimeZoneId);
        Calendar systemTime = Calendar.getInstance(systemTimeZone);
        systemTime.setTimeInMillis(originalTime.getTimeInMillis());
        return systemTime;
    }

    private static void splitMeeting(HashMap<String, List<DateTimeRange>> meetings,
                                     DateTimeRange dateTimeRange,
                                     String dateKeyStart,
                                     SimpleDateFormat dateKeyFormat) {
        Calendar startCalendar = dateTimeRange.getFrom();
        Calendar endCalendar = dateTimeRange.getTo();

        String currentDayKey = dateKeyStart;

        while (!isSameDay(startCalendar, endCalendar)) {
            Calendar midnightCalendar = (Calendar) startCalendar.clone();
            midnightCalendar.add(Calendar.DAY_OF_YEAR, 1);
            midnightCalendar.set(Calendar.HOUR_OF_DAY, 0);
            midnightCalendar.set(Calendar.MINUTE, 0);
            midnightCalendar.set(Calendar.SECOND, 0);
            midnightCalendar.set(Calendar.MILLISECOND, 0);

            DateTimeRange currentDayRange = new DateTimeRange(convertToSystemTimeZone(startCalendar), convertToSystemTimeZone(midnightCalendar));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                meetings.computeIfAbsent(currentDayKey, k -> new ArrayList<>()).add(currentDayRange);
            }

            startCalendar = midnightCalendar;
            currentDayKey = dateKeyFormat.format(startCalendar.getTime());
        }

        DateTimeRange remainingRange = new DateTimeRange(convertToSystemTimeZone(startCalendar), convertToSystemTimeZone(endCalendar));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            meetings.computeIfAbsent(currentDayKey, k -> new ArrayList<>()).add(remainingRange);
        }
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private static Calendar convertToUTCTimeZone(Calendar originalTime) {
        Calendar utcTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcTime.setTimeInMillis(originalTime.getTimeInMillis());
        return utcTime;
    }

    public static Calendar convertToTimeZone(Calendar originalTime, @NonNull TimeZone timeZone) {
        Calendar initialCalendar = (Calendar) originalTime.clone();

        // Set milliseconds to 0 in the initial calendar
        initialCalendar.set(Calendar.MILLISECOND, 0);

        // Convert the date and time to the target time zone
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        dateFormat.setTimeZone(timeZone);

        String date = dateFormat.format(initialCalendar.getTime());

        // Parse the date string back to Date
        Date convertedDate;
        try {
            convertedDate = dateFormat.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        // Create a new Calendar with the converted date in the target time zone
        Calendar convertedCalendar = Calendar.getInstance(timeZone);
        convertedCalendar.setTime(convertedDate);

        // Set milliseconds to 0 in the converted calendar
        convertedCalendar.set(Calendar.MILLISECOND, 0);

        return convertedCalendar;
    }

    private static TimeZone extractTimeZone(String line) {
        TimeZone timeZone = null;
        if (line.contains("TZID")) {
            int equalsIndex = line.indexOf("=");
            int colonIndex = line.indexOf(":");
            if (equalsIndex != -1 && colonIndex != -1 && equalsIndex < colonIndex) {
                String timeZoneId = line.substring(equalsIndex + 1, colonIndex);
                timeZone = TimeZone.getTimeZone(timeZoneId);
            }
        }
        return timeZone;
    }

    public static int[] getHrsAndMin(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return new int[]{hours, minutes};
    }

    public static long getDateFromString(String dateString, String TimeZone) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone(TimeZone));
        try {
            Date date = sdf.parse(dateString);
            Calendar calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone(TimeZone));
            calendar.setTime(date);
            Calendar targetCalendar = convertToSystemTimeZone(calendar);
            return targetCalendar.getTimeInMillis();
        } catch (ParseException e) {
            CometChatLogger.e(TAG, e.toString());
            return 0;
        }
    }

    public static void processMeetingAsync(SchedulerMessage schedulerMessage,
                                           HashMap<String, List<DateTimeRange>> occupiedMeetings,
                                           Callback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            List<DateTimeRange> dateTimeRanges = processMeetingLogic(schedulerMessage, occupiedMeetings);
            HashMap<String, List<DateTimeRange>> hashMap = new HashMap<>();
            hashMap.put(UIKitConstants.SchedulerConstants.AVAILABLE, dateTimeRanges);
            handler.post(() -> callback.onResult(hashMap));
        });
    }

    private static List<DateTimeRange> processMeetingLogic(SchedulerMessage schedulerMessage, HashMap<String, List<DateTimeRange>> occupiedMeetings) {
        List<DateTimeRange> dateTimeRanges = new ArrayList<>();

        HashMap<String, List<DateTimeRange>> clonedHashMap = new HashMap<>();
        for (Map.Entry<String, List<DateTimeRange>> entry : occupiedMeetings.entrySet()) {
            String key = entry.getKey();
            List<DateTimeRange> value = entry.getValue();
            List<DateTimeRange> clonedList = new ArrayList<>();
            for (DateTimeRange dateTimeRange : value) {
                Calendar from = (Calendar) dateTimeRange.getFrom().clone();
                Calendar to = (Calendar) dateTimeRange.getTo().clone();
                clonedList.add(new DateTimeRange(from, to));
            }
            clonedHashMap.put(key, clonedList);
        }

        boolean breakLoop = false;
        Calendar meetingStartDate = Calendar.getInstance();
        meetingStartDate.setTimeInMillis(SchedulerUtils.getDateFromString(schedulerMessage.getDateRangeStart(),
                                                                          "0000",
                                                                          schedulerMessage.getTimezoneCode()));
        Calendar meetingEndDate = Calendar.getInstance();
        meetingEndDate.setTimeInMillis(SchedulerUtils.getDateFromString(schedulerMessage.getDateRangeEnd(),
                                                                        "2359",
                                                                        schedulerMessage.getTimezoneCode()));

        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US); // for HashMap key

        Calendar tempCalender = new GregorianCalendar();
        tempCalender.setTimeInMillis(System.currentTimeMillis());
        tempCalender.set(Calendar.HOUR_OF_DAY, 0);
        tempCalender.set(Calendar.MINUTE, 0);
        tempCalender.set(Calendar.SECOND, 0);
        Calendar tempEndMeeting = Calendar.getInstance();
        String dateKey = dateKeyFormat.format(tempCalender.getTime());

        if (clonedHashMap.containsKey(dateKey)) {
            List<DateTimeRange> existingList = clonedHashMap.get(dateKey);
            if (existingList != null) {
                existingList.add(new DateTimeRange(tempCalender, tempEndMeeting));
            } else {
                clonedHashMap.put(dateKey, new ArrayList<>(Collections.singletonList(new DateTimeRange(tempCalender, tempEndMeeting))));
            }
        } else {
            clonedHashMap.put(dateKey, new ArrayList<>(Collections.singletonList(new DateTimeRange(tempCalender, tempEndMeeting))));
        }

        Calendar fromMeetingDate = Calendar.getInstance();
        Calendar todayDateCalendar = Calendar.getInstance();
        if (meetingStartDate.after(todayDateCalendar)) {
            fromMeetingDate.setTimeInMillis(SchedulerUtils.getDateFromString(schedulerMessage.getDateRangeStart(),
                                                                             "0000",
                                                                             schedulerMessage.getTimezoneCode()));
        } else if (meetingEndDate.after(todayDateCalendar) && meetingStartDate.before(todayDateCalendar)) {
            fromMeetingDate.setTimeInMillis(System.currentTimeMillis());
        } else {
            return dateTimeRanges;
        }

        while (!breakLoop) {
            String dayOfWeek = fromMeetingDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
            if (dayOfWeek != null) {
                dayOfWeek = dayOfWeek.toLowerCase();
            } else {
                dayOfWeek = "";
            }
            if (schedulerMessage.getAvailability() != null && schedulerMessage.getAvailability().containsKey(dayOfWeek)) {
                HashMap<String, List<DateTimeRange>> range = SchedulerUtils.getMeetingRange(fromMeetingDate,
                                                                                            schedulerMessage.getTimezoneCode(),
                                                                                            schedulerMessage.getAvailability(),
                                                                                            clonedHashMap,
                                                                                            schedulerMessage.getDuration());
                if (!range.isEmpty()) {
                    dateTimeRanges.addAll(SchedulerUtils.getFreeTimeSlots(range.get(UIKitConstants.SchedulerConstants.AVAILABLE),
                                                                          range.get(UIKitConstants.SchedulerConstants.OCCUPIED),
                                                                          schedulerMessage.getDuration(),
                                                                          schedulerMessage.getBufferTime()));
                    if (dateTimeRanges.size() >= 3) {
                        breakLoop = true;
                    }
                }
            }
            fromMeetingDate.add(Calendar.DAY_OF_MONTH, 1);
            if (fromMeetingDate.after(meetingEndDate)) {
                breakLoop = true;
            }
        }
        return dateTimeRanges;
    }

    public static long getDateFromString(String date, String time, String timeZoneCode) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneCode));

        try {
            Calendar sourceCalendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneCode));
            sourceCalendar.setTime(dateFormat.parse(date));
            sourceCalendar.set(Calendar.HOUR_OF_DAY, SchedulerUtils.getHoursFromString(time));
            sourceCalendar.set(Calendar.MINUTE, SchedulerUtils.getMinutesFromString(time));
            sourceCalendar.set(Calendar.SECOND, 0);
            sourceCalendar.set(Calendar.MILLISECOND, 0);
            Calendar targetCalendar = convertToSystemTimeZone(sourceCalendar);
            return targetCalendar.getTimeInMillis();
        } catch (ParseException e) {
            CometChatLogger.e(TAG, e.toString());
            return 0;
        }
    }

    public static HashMap<String, List<DateTimeRange>> getMeetingRange(Calendar selectedDate,
                                                                       String timeZoneCode,
                                                                       HashMap<String, List<TimeRange>> availability,
                                                                       HashMap<String, List<DateTimeRange>> occupiedMeets,
                                                                       int duration) {
        String dayOfWeek = selectedDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
        if (dayOfWeek != null) {
            dayOfWeek = dayOfWeek.toLowerCase();
        }
        HashMap<String, List<DateTimeRange>> hashMap = new HashMap<>();
        SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateKey = "", futureDateKey = "";
        Calendar toCalender = Calendar.getInstance();

        List<DateTimeRange> dateTimeRanges = new ArrayList<>();
        List<DateTimeRange> occupiedMeetings = new ArrayList<>();

        if (!timeZoneCode.equals(TimeZone.getDefault().getID())) {
            selectedDate.set(Calendar.HOUR, 0);
            selectedDate.set(Calendar.MINUTE, 0);
            selectedDate.set(Calendar.SECOND, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);

            toCalender.setTimeInMillis(selectedDate.getTimeInMillis());
            toCalender.set(Calendar.HOUR, 23);
            toCalender.set(Calendar.MINUTE, 59);
            toCalender.set(Calendar.SECOND, 60);
            toCalender.set(Calendar.MILLISECOND, 0);
        }

        Calendar fromCalenderMeeting, toCalenderMeeting;

        if (!timeZoneCode.equals(TimeZone.getDefault().getID())) {
            fromCalenderMeeting = new GregorianCalendar(TimeZone.getTimeZone(timeZoneCode));
            toCalenderMeeting = new GregorianCalendar(TimeZone.getTimeZone(timeZoneCode));
            fromCalenderMeeting.setTimeInMillis(selectedDate.getTimeInMillis());
            toCalenderMeeting.setTimeInMillis(toCalender.getTimeInMillis());

            dateKey = dateKeyFormat.format(selectedDate.getTime());

            String day1 = fromCalenderMeeting.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
            String day2 = toCalenderMeeting.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
            day1 = (day1 != null) ? day1.toLowerCase() : "";
            day2 = (day2 != null) ? day2.toLowerCase() : "";

            List<DateTimeRange> tempDateTimeRanges = new ArrayList<>();
            if (day1.equals(day2)) {
                if (availability.containsKey(day1)) {
                    List<TimeRange> value = availability.get(day1);
                    if (value != null) {
                        tempDateTimeRanges.addAll(getMeetingRangeForAvailability(fromCalenderMeeting, timeZoneCode, value));
                    }
                }
            } else {
                if (availability.containsKey(day1)) {
                    List<TimeRange> value = availability.get(day1);
                    if (value != null) {
                        tempDateTimeRanges.addAll(getMeetingRangeForAvailability(fromCalenderMeeting, timeZoneCode, value));
                    }
                }
                if (availability.containsKey(day2)) {
                    List<TimeRange> value = availability.get(day2);
                    if (value != null) {
                        tempDateTimeRanges.addAll(getMeetingRangeForAvailability(toCalenderMeeting, timeZoneCode, value));
                    }
                }
            }

            for (DateTimeRange dateTimeRange : tempDateTimeRanges) {
                Calendar fromCalendar = dateTimeRange.getFrom();
                Calendar toCalendar = dateTimeRange.getTo();

                if (fromCalendar.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)) {
                    if (toCalendar.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)) {
                        dateTimeRanges.add(dateTimeRange);
                    } else {
                        Calendar tempCalender = (Calendar) fromCalendar.clone();
                        tempCalender.set(Calendar.HOUR_OF_DAY, 23);
                        tempCalender.set(Calendar.MINUTE, 59);
                        tempCalender.set(Calendar.SECOND, 59);
                        tempCalender.set(Calendar.MILLISECOND, 999);
                        dateTimeRanges.add(new DateTimeRange(fromCalendar, tempCalender));
                    }
                } else {
                    if (toCalendar.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)) {
                        Calendar tempCalender = (Calendar) toCalendar.clone();
                        tempCalender.set(Calendar.HOUR_OF_DAY, 0);
                        tempCalender.set(Calendar.MINUTE, 0);
                        tempCalender.set(Calendar.SECOND, 0);
                        tempCalender.set(Calendar.MILLISECOND, 0);
                        dateTimeRanges.add(new DateTimeRange(tempCalender, toCalendar));
                    }
                }
            }

            if (!dateTimeRanges.isEmpty()) {
                futureDateKey = dateKeyFormat.format(dateTimeRanges.get(dateTimeRanges.size() - 1).getTo().getTime());

                Calendar futureFromCalender = (Calendar) dateTimeRanges.get(dateTimeRanges.size() - 1).getTo().clone();

                if (futureFromCalender.get(Calendar.HOUR) == 0 && futureFromCalender.get(Calendar.MINUTE) == 0 && futureFromCalender.get(Calendar.SECOND) == 0) {
                    futureFromCalender.set(Calendar.DAY_OF_YEAR, selectedDate.get(Calendar.DAY_OF_YEAR) + 1);
                    dateKeyFormat.setTimeZone(TimeZone.getTimeZone(timeZoneCode));
                    futureDateKey = dateKeyFormat.format(futureFromCalender.getTime());
                    String futureDayOfWeek = futureFromCalender.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
                    if (futureDayOfWeek != null) {
                        futureDayOfWeek = futureDayOfWeek.toLowerCase();
                    } else {
                        futureDayOfWeek = "";
                    }
                    if (availability.containsKey(futureDayOfWeek)) {
                        TimeRange futureTimeRange;
                        List<TimeRange> timeRangesForDay = availability.get(futureDayOfWeek);
                        if (timeRangesForDay != null && !timeRangesForDay.isEmpty()) {
                            futureTimeRange = timeRangesForDay.get(0);
                        } else {
                            futureTimeRange = null;
                        }
                        if (futureTimeRange != null) {
                            futureFromCalender.set(Calendar.HOUR_OF_DAY, SchedulerUtils.getHoursFromString(futureTimeRange.getFrom()));
                            futureFromCalender.set(Calendar.MINUTE, SchedulerUtils.getMinutesFromString(futureTimeRange.getFrom()));
                        }
                        futureFromCalender.set(Calendar.SECOND, 0);
                        futureFromCalender.set(Calendar.MILLISECOND, 0);
                        if (toCalender.equals(futureFromCalender)) {
                            toCalenderMeeting.set(Calendar.MINUTE, duration - 1);
                        }
                    }
                    dateTimeRanges.set(dateTimeRanges.size() - 1,
                                       new DateTimeRange(dateTimeRanges.get(dateTimeRanges.size() - 1).getFrom(), toCalenderMeeting));
                }
            }

        } else {
            if (availability != null && availability.containsKey(dayOfWeek)) {
                List<TimeRange> timeRangeList = availability.get(dayOfWeek);
                if (timeRangeList != null && !timeRangeList.isEmpty()) {
                    for (TimeRange timeRange : timeRangeList) {
                        selectedDate.set(Calendar.HOUR_OF_DAY, SchedulerUtils.getHoursFromString(timeRange.getFrom()));
                        selectedDate.set(Calendar.MINUTE, SchedulerUtils.getMinutesFromString(timeRange.getFrom()));
                        selectedDate.set(Calendar.SECOND, 0);
                        selectedDate.set(Calendar.MILLISECOND, 0);

                        toCalender.setTime(selectedDate.getTime());
                        toCalender.set(Calendar.HOUR_OF_DAY, SchedulerUtils.getHoursFromString(timeRange.getTo()));
                        toCalender.set(Calendar.MINUTE, SchedulerUtils.getMinutesFromString(timeRange.getTo()));
                        toCalender.set(Calendar.SECOND, 60);

                        fromCalenderMeeting = Calendar.getInstance(TimeZone.getDefault());
                        toCalenderMeeting = Calendar.getInstance(TimeZone.getDefault());
                        fromCalenderMeeting.setTime(selectedDate.getTime());
                        toCalenderMeeting.setTime(toCalender.getTime());

                        dateKey = dateKeyFormat.format(selectedDate.getTime());

                        Calendar futureFromCalender = (Calendar) selectedDate.clone();
                        futureFromCalender.setTime(selectedDate.getTime());
                        futureFromCalender.set(Calendar.DAY_OF_YEAR, selectedDate.get(Calendar.DAY_OF_YEAR) + 1);
                        futureDateKey = dateKeyFormat.format(futureFromCalender.getTime());
                        String futureDayOfWeek = futureFromCalender.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.ENGLISH);
                        if (futureDayOfWeek != null) {
                            futureDayOfWeek = futureDayOfWeek.toLowerCase();
                        } else {
                            futureDayOfWeek = "";
                        }
                        if (availability.containsKey(futureDayOfWeek)) {
                            TimeRange futureTimeRange;
                            List<TimeRange> timeRangesForDay = availability.get(futureDayOfWeek);
                            if (timeRangesForDay != null && !timeRangesForDay.isEmpty()) {
                                futureTimeRange = timeRangesForDay.get(0);
                            } else {
                                futureTimeRange = null;
                            }
                            if (futureTimeRange != null) {
                                futureFromCalender.set(Calendar.HOUR_OF_DAY, SchedulerUtils.getHoursFromString(futureTimeRange.getFrom()));
                                futureFromCalender.set(Calendar.MINUTE, SchedulerUtils.getMinutesFromString(futureTimeRange.getFrom()));
                            }
                            futureFromCalender.set(Calendar.SECOND, 0);
                            futureFromCalender.set(Calendar.MILLISECOND, 0);
                            if (toCalender.equals(futureFromCalender)) {
                                toCalenderMeeting.set(Calendar.MINUTE, duration - 1);
                            }
                        }
                        dateTimeRanges.add(new DateTimeRange(fromCalenderMeeting, toCalenderMeeting));
                    }
                }
            }
        }
        if (occupiedMeets.containsKey(dateKey)) {
            occupiedMeetings.addAll(occupiedMeets.get(dateKey));
        }
        if (occupiedMeets.containsKey(futureDateKey)) {
            occupiedMeetings.addAll(occupiedMeets.get(futureDateKey));
        }

        hashMap.put(UIKitConstants.SchedulerConstants.AVAILABLE, dateTimeRanges);
        hashMap.put(UIKitConstants.SchedulerConstants.OCCUPIED, mergeOverlapping(occupiedMeetings));
        return hashMap;
    }

    public static List<DateTimeRange> getFreeTimeSlots(List<DateTimeRange> availability,
                                                       List<DateTimeRange> otherMeetings,
                                                       int duration,
                                                       int bufferTime) {
        List<DateTimeRange> freeIntervals = new ArrayList<>();
        if (availability != null && !availability.isEmpty()) {
            for (DateTimeRange dateTimeRange : availability) {
                freeIntervals.addAll(getFreeIntervals(dateTimeRange.getFrom(), dateTimeRange.getTo(), otherMeetings, duration, bufferTime));
            }
        }
        return freeIntervals;
    }

    public static int getHoursFromString(@Nullable String time) {
        if (time != null && time.length() == 4) {
            String hours = time.substring(0, 2);
            return Integer.parseInt(hours);
        } else {
            Log.e(SchedulerUtils.class.getSimpleName(), "getHoursFromString: Invalid time format");
            return 0;
        }
    }

    public static int getMinutesFromString(String time) {
        if (time != null && time.length() == 4) {
            String minutes = time.substring(2, 4);
            return Integer.parseInt(minutes);
        } else {
            Log.e(SchedulerUtils.class.getSimpleName(), "getMinutesFromString: Invalid time format");
            return 0;
        }
    }

    public static List<DateTimeRange> getMeetingRangeForAvailability(Calendar selectedDate, String timeZoneCode, List<TimeRange> availability) {

        List<DateTimeRange> dateTimeRanges = new ArrayList<>();
        for (TimeRange timeRange : availability) {
            Calendar fromCalenderMeeting = new GregorianCalendar(TimeZone.getTimeZone(timeZoneCode));
            Calendar toCalenderMeeting = new GregorianCalendar(TimeZone.getTimeZone(timeZoneCode));
            fromCalenderMeeting.setTimeInMillis(selectedDate.getTimeInMillis());

            fromCalenderMeeting.set(Calendar.HOUR_OF_DAY, SchedulerUtils.getHoursFromString(timeRange.getFrom()));
            fromCalenderMeeting.set(Calendar.MINUTE, SchedulerUtils.getMinutesFromString(timeRange.getFrom()));
            fromCalenderMeeting.set(Calendar.SECOND, 0);
            fromCalenderMeeting.set(Calendar.MILLISECOND, 0);

            toCalenderMeeting.setTimeInMillis(fromCalenderMeeting.getTimeInMillis());
            toCalenderMeeting.set(Calendar.HOUR_OF_DAY, SchedulerUtils.getHoursFromString(timeRange.getTo()));
            toCalenderMeeting.set(Calendar.MINUTE, SchedulerUtils.getMinutesFromString(timeRange.getTo()));
            toCalenderMeeting.set(Calendar.SECOND, 60);
            toCalenderMeeting.set(Calendar.MILLISECOND, 0);
            dateTimeRanges.add(new DateTimeRange(convertToSystemTimeZone(fromCalenderMeeting), convertToSystemTimeZone(toCalenderMeeting)));
        }

        return dateTimeRanges;
    }

    public static List<DateTimeRange> mergeOverlapping(List<DateTimeRange> dateTimeRanges) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dateTimeRanges.sort(Comparator.comparing(DateTimeRange::getFrom));
        }
        LinkedList<DateTimeRange> merged = new LinkedList<>();
        for (DateTimeRange dtRange : dateTimeRanges) {
            if (merged.isEmpty() || !dtRange.isColliding(merged.getLast())) {
                merged.add(dtRange);
            } else {
                DateTimeRange lastRange = merged.getLast();
                Calendar maxEndTime = (dtRange.getTo().compareTo(lastRange.getTo()) > 0) ? dtRange.getTo() : lastRange.getTo();
                lastRange.to = maxEndTime;
            }
        }

        return new ArrayList<>(merged);
    }

    @NonNull
    public static List<DateTimeRange> getFreeIntervals(Calendar from, Calendar to, List<DateTimeRange> otherMeetings, int duration, int bufferTime) {
        List<DateTimeRange> freeIntervals = new ArrayList<>();
        Calendar currentTimeSlot = (Calendar) from.clone();

        // Sort otherMeetings by start time to ensure proper order
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Collections.sort(otherMeetings, Comparator.comparing(DateTimeRange::getFrom));
        }

        for (DateTimeRange meeting : otherMeetings) {
            // Ignore meetings that are not within 'from' and 'to'
            if (meeting.getTo().before(from) || meeting.getFrom().after(to)) {
                continue;
            }

            while (currentTimeSlot.before(meeting.getFrom())) {
                Calendar endOfSlot = getEndOfSlot(currentTimeSlot, duration); // Use the original duration
                if (endOfSlot.before(meeting.getFrom()) && !endOfSlot.after(to)) {

                    // Check if the slot end does not fall during another meeting
                    boolean isFree = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Calendar finalCurrentTimeSlot = currentTimeSlot;
                        isFree = otherMeetings
                            .stream()
                            .noneMatch(m -> (finalCurrentTimeSlot.before(m.getTo()) && endOfSlot.after(m.getFrom())) // Slot
                                       // overlaps with a meeting
                            );
                    }

                    if (isFree) {
                        freeIntervals.add(new DateTimeRange(currentTimeSlot, endOfSlot));
                    }

                    currentTimeSlot = (Calendar) endOfSlot.clone();
                } else {
                    break;
                }
            }

            // Always set the currentTimeSlot to the end of the meeting plus buffer time
            currentTimeSlot = (Calendar) meeting.getTo().clone();
            currentTimeSlot.add(Calendar.MINUTE, bufferTime);
        }

        // Add remaining time after the last meeting, if any
        while (currentTimeSlot.before(to)) {
            Calendar endOfSlot = getEndOfSlot(currentTimeSlot, duration); // Use the original duration
            if (!endOfSlot.after(to)) {
                boolean isFree = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    isFree = otherMeetings
                        .stream()
                        .noneMatch(m -> (endOfSlot.after(m.getFrom()) || endOfSlot.equals(m.getFrom())) && endOfSlot.before(m.getTo()));
                }

                if (isFree) {
                    freeIntervals.add(new DateTimeRange(currentTimeSlot, endOfSlot));
                }

                currentTimeSlot = (Calendar) endOfSlot.clone();
            } else {
                break;
            }
        }

        return freeIntervals;
    }

    // Helper method to find the end of a time slot
    public static Calendar getEndOfSlot(Calendar start, int duration) {
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MINUTE, duration);
        return end;
    }

    public static boolean checkInsideOrEquals(List<DateTimeRange> dateTimeRanges, DateTimeRange target) {
        for (DateTimeRange dateTimeRange : dateTimeRanges) {
            if (insideOrEquals(target, dateTimeRange)) {
                return true;
            }
        }
        return false;
    }

    public static boolean insideOrEquals(DateTimeRange to, DateTimeRange other) {
        return to.getFrom().compareTo(other.getFrom()) >= 0 && to.getTo().compareTo(other.getTo()) <= 0;
    }

    public interface Callback {
        void onResult(HashMap<String, List<DateTimeRange>> result);
    }
}
