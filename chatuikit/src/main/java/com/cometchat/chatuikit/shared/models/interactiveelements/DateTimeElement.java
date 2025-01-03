package com.cometchat.chatuikit.shared.models.interactiveelements;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimeElement extends BaseInputElement<String> {
    private static final String TAG = DateTimeElement.class.getSimpleName();


    private String label;
    private UIKitConstants.DateTimeMode mode;
    private SimpleDateFormat simpleDateFormat;
    private String from;
    private String to;
    private String timeZoneCode;
    private PlaceHolder placeHolder;

    public DateTimeElement(String elementId, String defaultValue) {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_DATE_TIME, elementId, defaultValue);
    }

    private DateTimeElement() {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_DATE_TIME, null, null);
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setPlaceHolder(PlaceHolder placeHolder) {
        this.placeHolder = placeHolder;
    }

    public void setMode(UIKitConstants.DateTimeMode mode) {
        this.mode = mode;
    }

    public void setSimpleDateFormat(SimpleDateFormat simpleDateFormat) {
        this.simpleDateFormat = simpleDateFormat;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setTimeZoneCode(String timeZoneCode) {
        this.timeZoneCode = timeZoneCode;
    }

    public String getLabel() {
        return label;
    }

    public UIKitConstants.DateTimeMode getMode() {
        return mode;
    }

    public SimpleDateFormat getSimpleDateFormat() {
        return simpleDateFormat;
    }

    public PlaceHolder getPlaceHolder() {
        return placeHolder;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getTimeZoneCode() {
        return timeZoneCode;
    }

    public static DateTimeElement fromJson(JSONObject json) {
        DateTimeElement dateTimeElement = new DateTimeElement();
        if (json != null) {
            try {
                if (json.has(InteractiveConstants.ELEMENT_ID))
                    dateTimeElement.setElementId(json.getString(InteractiveConstants.ELEMENT_ID));
                if (json.has(InteractiveConstants.DateTimeUIConstants.LABEL))
                    dateTimeElement.setLabel(json.getString(InteractiveConstants.DropDownUIConstants.LABEL));
                if (json.has(InteractiveConstants.DropDownUIConstants.OPTIONAL))
                    dateTimeElement.setOptional(json.getBoolean(InteractiveConstants.DropDownUIConstants.OPTIONAL));
                if (json.has(InteractiveConstants.DEFAULT_VALUE))
                    dateTimeElement.setDefaultValue(json.getString(InteractiveConstants.DEFAULT_VALUE));

                if (json.has(InteractiveConstants.DateTimeUIConstants.MODE)) {
                    if (json.getString(InteractiveConstants.DateTimeUIConstants.MODE).equalsIgnoreCase(InteractiveConstants.DateTimeMode.DATE))
                        dateTimeElement.setMode(UIKitConstants.DateTimeMode.DATE);
                    else if (json.getString(InteractiveConstants.DateTimeUIConstants.MODE).equalsIgnoreCase(InteractiveConstants.DateTimeMode.TIME))
                        dateTimeElement.setMode(UIKitConstants.DateTimeMode.TIME);
                    else if (json.getString(InteractiveConstants.DateTimeUIConstants.MODE).equalsIgnoreCase(InteractiveConstants.DateTimeMode.DATE_TIME))
                        dateTimeElement.setMode(UIKitConstants.DateTimeMode.DATE_TIME);
                }
                if (json.has(InteractiveConstants.DateTimeUIConstants.FROM)) {
                    dateTimeElement.setFrom(json.getString(InteractiveConstants.DateTimeUIConstants.FROM));
                }
                if (json.has(InteractiveConstants.DateTimeUIConstants.PLACEHOLDER)) {
                    JSONObject placeHolderObject = json.optJSONObject(InteractiveConstants.TextInputUIConstants.PLACEHOLDER);
                    if (placeHolderObject != null) {
                        dateTimeElement.setPlaceHolder(PlaceHolder.fromJson(placeHolderObject));
                    }
                }
                if (json.has(InteractiveConstants.DateTimeUIConstants.TO))
                    dateTimeElement.setTo(json.getString(InteractiveConstants.DateTimeUIConstants.TO));
                if (json.has(InteractiveConstants.DateTimeUIConstants.TIME_ZONE_CODE)) {
                    json.getString(InteractiveConstants.DateTimeUIConstants.TIME_ZONE_CODE);
                    dateTimeElement.setTimeZoneCode(json.getString(InteractiveConstants.DateTimeUIConstants.TIME_ZONE_CODE));
                }
                if (json.has(InteractiveConstants.DateTimeUIConstants.DATE_TIME_FORMAT))
                    dateTimeElement.setSimpleDateFormat(new SimpleDateFormat(json.getString(InteractiveConstants.DateTimeUIConstants.DATE_TIME_FORMAT), Locale.US));
            } catch (Exception e) {
                CometChatLogger.e(TAG, e.getMessage());
            }
        }
        return dateTimeElement;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(InteractiveConstants.ELEMENT_TYPE, getElementType());
            if (getElementId() != null && !getElementId().isEmpty()) {
                jsonObject.put(InteractiveConstants.ELEMENT_ID, getElementId());
            }
            if (getLabel() != null && !getLabel().isEmpty()) {
                jsonObject.put(InteractiveConstants.DropDownUIConstants.LABEL, getLabel());
            }

            jsonObject.put(InteractiveConstants.DropDownUIConstants.OPTIONAL, isOptional());

            if (getDefaultValue() != null && !getDefaultValue().isEmpty()) {
                jsonObject.put(InteractiveConstants.DEFAULT_VALUE, getDefaultValue());
            }
            if (getMode() != null) {
                if (getMode().equals(UIKitConstants.DateTimeMode.DATE_TIME))
                    jsonObject.put(InteractiveConstants.DateTimeUIConstants.MODE, InteractiveConstants.DateTimeMode.DATE_TIME);
                else if (getMode().equals(UIKitConstants.DateTimeMode.DATE))
                    jsonObject.put(InteractiveConstants.DateTimeUIConstants.MODE, InteractiveConstants.DateTimeMode.DATE);
                else if (getMode().equals(UIKitConstants.DateTimeMode.TIME))
                    jsonObject.put(InteractiveConstants.DateTimeUIConstants.MODE, InteractiveConstants.DateTimeMode.TIME);
            }
            if (getFrom() != null && !getFrom().isEmpty())
                jsonObject.put(InteractiveConstants.DateTimeUIConstants.FROM, getFrom());
            if (getTo() != null && !getTo().isEmpty())
                jsonObject.put(InteractiveConstants.DateTimeUIConstants.TO, getTo());
            if (getTimeZoneCode() != null && !getTimeZoneCode().isEmpty())
                jsonObject.put(InteractiveConstants.DateTimeUIConstants.TIME_ZONE_CODE, getTimeZoneCode());
            if (getSimpleDateFormat() != null)
                jsonObject.put(InteractiveConstants.DateTimeUIConstants.DATE_TIME_FORMAT, getSimpleDateFormat().toPattern());
            PlaceHolder placeholder = getPlaceHolder();
            if (placeholder != null) {
                jsonObject.put(InteractiveConstants.DateTimeUIConstants.PLACEHOLDER, placeholder.toJson());
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return jsonObject;
    }
}
