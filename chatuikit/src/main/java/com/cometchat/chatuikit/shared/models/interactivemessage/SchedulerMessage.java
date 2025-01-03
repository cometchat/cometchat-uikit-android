package com.cometchat.chatuikit.shared.models.interactivemessage;

import com.cometchat.chat.constants.CometChatConstants;
import com.cometchat.chat.models.InteractionGoal;
import com.cometchat.chat.models.InteractiveMessage;
import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactiveelements.ButtonElement;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SchedulerMessage extends InteractiveMessage {
    private static final String TAG = SchedulerMessage.class.getSimpleName();
    private String title;
    private String avatarUrl;
    private String goalCompletionText;
    private String timezoneCode;
    private int bufferTime = 0;
    private int duration = 0;
    private HashMap<String, List<TimeRange>> availability;
    private String dateRangeStart = null;
    private String dateRangeEnd = null;
    private String icsFileUrl;
    private ButtonElement scheduleElement;

    public SchedulerMessage() {
        super(null, null, UIKitConstants.MessageType.SCHEDULER, new JSONObject());
        InteractionGoal interactionGoal = new InteractionGoal(CometChatConstants.INTERACTION_TYPE_ANY, null);
        setInteractionGoal(interactionGoal);
        setAllowSenderInteraction(false);
    }

    public SchedulerMessage(String receiverId, String receiverType, String timezoneCode, int bufferTime, int duration, HashMap<String, List<TimeRange>> availability, ButtonElement submitElement) {
        super(receiverId, receiverType, UIKitConstants.MessageType.SCHEDULER, new JSONObject());
        InteractionGoal interactionGoal = new InteractionGoal(CometChatConstants.INTERACTION_TYPE_ANY, null);
        setTimezoneCode(timezoneCode);
        setBufferTime(bufferTime);
        setDuration(duration);
        setAvailability(availability);
        setScheduleElement(submitElement);
        setInteractionGoal(interactionGoal);
        setAllowSenderInteraction(false);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getGoalCompletionText() {
        return goalCompletionText;
    }

    public void setGoalCompletionText(String goalCompletionText) {
        this.goalCompletionText = goalCompletionText;
    }

    public String getTimezoneCode() {
        return timezoneCode;
    }

    public void setTimezoneCode(String timezoneCode) {
        this.timezoneCode = timezoneCode;
    }

    public int getBufferTime() {
        return bufferTime;
    }

    public void setBufferTime(int bufferTime) {
        this.bufferTime = bufferTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public HashMap<String, List<TimeRange>> getAvailability() {
        return availability;
    }

    public void setAvailability(HashMap<String, List<TimeRange>> availability) {
        this.availability = availability;
    }

    public String getDateRangeStart() {
        return dateRangeStart;
    }

    public void setDateRangeStart(String dateRangeStart) {
        this.dateRangeStart = dateRangeStart;
    }

    public String getDateRangeEnd() {
        return dateRangeEnd;
    }

    public void setDateRangeEnd(String dateRangeEnd) {
        this.dateRangeEnd = dateRangeEnd;
    }

    public String getIcsFileUrl() {
        return icsFileUrl;
    }

    public void setIcsFileUrl(String icsFileUrl) {
        this.icsFileUrl = icsFileUrl;
    }

    public ButtonElement getScheduleElement() {
        return scheduleElement;
    }

    public void setScheduleElement(ButtonElement scheduleElement) {
        this.scheduleElement = scheduleElement;
    }

    public static SchedulerMessage fromInteractive(InteractiveMessage interactiveMessage) {
        if (interactiveMessage == null) return null;
        SchedulerMessage schedulerMessage = new SchedulerMessage();
        schedulerMessage.setId(interactiveMessage.getId());
        schedulerMessage.setReceiverType(interactiveMessage.getReceiverType());
        schedulerMessage.setReceiver(interactiveMessage.getReceiver());
        schedulerMessage.setSender(interactiveMessage.getSender());
        schedulerMessage.setSentAt(interactiveMessage.getSentAt());
        schedulerMessage.setReadAt(interactiveMessage.getReadAt());
        schedulerMessage.setDeliveredAt(interactiveMessage.getDeliveredAt());
        schedulerMessage.setUpdatedAt(interactiveMessage.getUpdatedAt());
        schedulerMessage.setDeletedBy(interactiveMessage.getDeletedBy());
        schedulerMessage.setDeletedAt(interactiveMessage.getDeletedAt());
        schedulerMessage.setInteractionGoal(interactiveMessage.getInteractionGoal());
        schedulerMessage.setInteractions(interactiveMessage.getInteractions() == null ? new ArrayList<>() : interactiveMessage.getInteractions());
        schedulerMessage.setMetadata(interactiveMessage.getMetadata());
        schedulerMessage.setMuid(interactiveMessage.getMuid());
        schedulerMessage.setRawMessage(interactiveMessage.getRawMessage());
        schedulerMessage.setConversationId(interactiveMessage.getConversationId());
        schedulerMessage.setReadByMeAt(interactiveMessage.getReadByMeAt());
        schedulerMessage.setReceiverUid(interactiveMessage.getReceiverUid());
        schedulerMessage.setReplyCount(interactiveMessage.getReplyCount());
        schedulerMessage.setTags(interactiveMessage.getTags());
        schedulerMessage.setAllowSenderInteraction(interactiveMessage.isAllowSenderInteraction());

        try {
            if (interactiveMessage.getInteractiveData() != null) {
                JSONObject jsonObject = interactiveMessage.getInteractiveData();
                if (jsonObject.has(InteractiveConstants.TITLE)) {
                    schedulerMessage.setTitle(jsonObject.optString(InteractiveConstants.TITLE));
                }
                if (jsonObject.has(InteractiveConstants.AVATAR_URL)) {
                    schedulerMessage.setAvatarUrl(jsonObject.optString(InteractiveConstants.AVATAR_URL));
                }
                if (jsonObject.has(InteractiveConstants.GOAL_COMPLETION_TEXT)) {
                    schedulerMessage.setGoalCompletionText(jsonObject.optString(InteractiveConstants.GOAL_COMPLETION_TEXT));
                }
                if (jsonObject.has(InteractiveConstants.TIME_ZONE_CODE)) {
                    schedulerMessage.setTimezoneCode(jsonObject.optString(InteractiveConstants.TIME_ZONE_CODE));
                }
                if (jsonObject.has(InteractiveConstants.BUFFER_TIME)) {
                    schedulerMessage.setBufferTime(jsonObject.optInt(InteractiveConstants.BUFFER_TIME));
                }
                if (jsonObject.has(InteractiveConstants.DURATION)) {
                    schedulerMessage.setDuration(jsonObject.optInt(InteractiveConstants.DURATION));
                }
                if (jsonObject.has(InteractiveConstants.AVAILABILITY)) {
                    HashMap<String, List<TimeRange>> map = new HashMap<>();
                    JSONObject availabilityObject = jsonObject.getJSONObject(InteractiveConstants.AVAILABILITY);
                    Iterator<String> keys = availabilityObject.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        List<TimeRange> timeRanges = new ArrayList<>();
                        JSONArray timeRangeArray = availabilityObject.getJSONArray(key);
                        for (int i = 0; i < timeRangeArray.length(); i++) {
                            JSONObject timeRangeObject = timeRangeArray.getJSONObject(i);
                            String from = timeRangeObject.getString(InteractiveConstants.FROM_TIME);
                            String to = timeRangeObject.getString(InteractiveConstants.TO_TIME);
                            TimeRange timeRange = new TimeRange(from, to);
                            timeRanges.add(timeRange);
                        }
                        map.put(key, timeRanges);
                    }
                    schedulerMessage.setAvailability(map);
                }
                if (jsonObject.has(InteractiveConstants.DATE_RANGE_START)) {
                    schedulerMessage.setDateRangeStart(jsonObject.optString(InteractiveConstants.DATE_RANGE_START));
                }
                if (jsonObject.has(InteractiveConstants.DATE_RANGE_END)) {
                    schedulerMessage.setDateRangeEnd(jsonObject.optString(InteractiveConstants.DATE_RANGE_END));
                }
                if (jsonObject.has(InteractiveConstants.ICS_FILE_URL)) {
                    schedulerMessage.setIcsFileUrl(jsonObject.optString(InteractiveConstants.ICS_FILE_URL));
                }
                if (jsonObject.has(InteractiveConstants.INTERACTIVE_MESSAGE_SCHEDULE_ELEMENT)) {
                    JSONObject submitElement = jsonObject.getJSONObject(InteractiveConstants.INTERACTIVE_MESSAGE_SCHEDULE_ELEMENT);
                    schedulerMessage.setScheduleElement(ButtonElement.fromJson(submitElement));
                }
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.getMessage());
        }
        return schedulerMessage;
    }

    public InteractiveMessage toInteractiveMessage() {
        InteractiveMessage interactiveMessage = new InteractiveMessage(getReceiverUid(), getReceiverType(), UIKitConstants.MessageType.SCHEDULER, new JSONObject());
        interactiveMessage.setId(getId());
        interactiveMessage.setReceiverType(getReceiverType());
        interactiveMessage.setReceiver(getReceiver());
        interactiveMessage.setSender(getSender());
        interactiveMessage.setSentAt(getSentAt());
        interactiveMessage.setReadAt(getReadAt());
        interactiveMessage.setDeletedAt(getDeletedAt());
        interactiveMessage.setDeliveredAt(getDeliveredAt());
        interactiveMessage.setUpdatedAt(getUpdatedAt());
        interactiveMessage.setDeletedBy(getDeletedBy());
        interactiveMessage.setInteractionGoal(getInteractionGoal());
        interactiveMessage.setInteractions(getInteractions() == null ? new ArrayList<>() : getInteractions());
        interactiveMessage.setMetadata(getMetadata());
        interactiveMessage.setMuid(getMuid());
        interactiveMessage.setRawMessage(getRawMessage());
        interactiveMessage.setConversationId(getConversationId());
        interactiveMessage.setReadByMeAt(getReadByMeAt());
        interactiveMessage.setReceiverUid(getReceiverUid());
        interactiveMessage.setReplyCount(getReplyCount());
        interactiveMessage.setTags(getTags());
        interactiveMessage.setAllowSenderInteraction(isAllowSenderInteraction());

        JSONObject jsonObject = new JSONObject();
        try {
            if (getTitle() != null && !getTitle().isEmpty()) {
                jsonObject.put(InteractiveConstants.TITLE, getTitle());
            }
            if (getAvatarUrl() != null && !getAvatarUrl().isEmpty()) {
                jsonObject.put(InteractiveConstants.AVATAR_URL, getAvatarUrl());
            }
            if (getGoalCompletionText() != null && !getGoalCompletionText().isEmpty()) {
                jsonObject.put(InteractiveConstants.GOAL_COMPLETION_TEXT, getGoalCompletionText());
            }
            if (getTimezoneCode() != null && !getTimezoneCode().isEmpty()) {
                jsonObject.put(InteractiveConstants.TIME_ZONE_CODE, getTimezoneCode());
            }
            if (getBufferTime() > 0) {
                jsonObject.put(InteractiveConstants.BUFFER_TIME, getBufferTime());
            }
            if (getDuration() > 0) {
                jsonObject.put(InteractiveConstants.DURATION, getDuration());
            }
            if (getAvailability() != null && !getAvailability().isEmpty()) {
                JSONObject availabilityJsonObject = new JSONObject();
                try {
                    for (Map.Entry<String, List<TimeRange>> entry : getAvailability().entrySet()) {
                        String key = entry.getKey();
                        List<TimeRange> value = entry.getValue();
                        JSONArray array = new JSONArray();
                        for (TimeRange tr : value) {
                            JSONObject timeRangeObject = new JSONObject();
                            timeRangeObject.put(InteractiveConstants.FROM_TIME, tr.getFrom());
                            timeRangeObject.put(InteractiveConstants.TO_TIME, tr.getTo());
                            array.put(timeRangeObject);
                        }
                        availabilityJsonObject.put(key, array);
                    }
                } catch (Exception e) {
                    CometChatLogger.e(TAG, e.toString());
                }
                jsonObject.put(InteractiveConstants.AVAILABILITY, availabilityJsonObject);
            }
            if (getDateRangeStart() != null && !getDateRangeStart().isEmpty()) {
                jsonObject.put(InteractiveConstants.DATE_RANGE_START, getDateRangeStart());
            }
            if (getDateRangeEnd() != null && !getDateRangeEnd().isEmpty()) {
                jsonObject.put(InteractiveConstants.DATE_RANGE_END, getDateRangeEnd());
            }
            if (getIcsFileUrl() != null && !getIcsFileUrl().isEmpty()) {
                jsonObject.put(InteractiveConstants.ICS_FILE_URL, getIcsFileUrl());
            }
            if (getScheduleElement() != null) {
                jsonObject.put(InteractiveConstants.INTERACTIVE_MESSAGE_SCHEDULE_ELEMENT, getScheduleElement().toJson());
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        interactiveMessage.setInteractiveData(jsonObject);
        return interactiveMessage;
    }
}
