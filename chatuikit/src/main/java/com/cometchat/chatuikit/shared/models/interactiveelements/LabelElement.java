package com.cometchat.chatuikit.shared.models.interactiveelements;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.constants.UIKitConstants;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONObject;

public class LabelElement extends ElementEntity {
    private static final String TAG = LabelElement.class.getSimpleName();
    private String text;

    private LabelElement() {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_LABEL, null);
    }

    public LabelElement(String elementId, String text) {
        super(UIKitConstants.UIElementsType.UI_ELEMENT_LABEL, elementId);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public static LabelElement fromJson(JSONObject json) {
        LabelElement labelElement = new LabelElement();
        if (json != null) {
            try {
                labelElement.setElementId(json.getString(InteractiveConstants.ELEMENT_ID));
                labelElement.setElementId(json.getString(InteractiveConstants.LabelUIConstants.TEXT));
            } catch (Exception e) {
                CometChatLogger.e(TAG, e.getMessage());
            }
        }
        return labelElement;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(InteractiveConstants.ELEMENT_TYPE, getElementType());
            if (getElementId() != null && !getElementId().isEmpty()) {
                jsonObject.put(InteractiveConstants.ELEMENT_ID, getElementId());
            }

            if (getText() != null && !getText().isEmpty()) {
                jsonObject.put(InteractiveConstants.LabelUIConstants.TEXT, getText());
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return jsonObject;
    }
}
