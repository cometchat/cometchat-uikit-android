package com.cometchat.chatuikit.shared.models.interactiveelements;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONObject;

public class OptionElement {
    private static final String TAG = OptionElement.class.getSimpleName();
    private String id;
    private String value;

    private OptionElement() {
    }

    public OptionElement(String id, String value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static OptionElement fromJson(JSONObject json) {
        OptionElement optionElement = new OptionElement();
        if (json != null) {
            try {
                optionElement.setId(json.getString(InteractiveConstants.OptionElementConstants.VALUE));
                optionElement.setValue(json.getString(InteractiveConstants.OptionElementConstants.LABEL));
            } catch (Exception e) {
                CometChatLogger.e(TAG, e.getMessage());
            }
        }
        return optionElement;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (getId() != null && !getId().isEmpty()) {
                jsonObject.put(InteractiveConstants.OptionElementConstants.VALUE, getId());
            }

            if (getValue() != null && !getValue().isEmpty()) {
                jsonObject.put(InteractiveConstants.OptionElementConstants.LABEL, getValue());
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return jsonObject;
    }
}
