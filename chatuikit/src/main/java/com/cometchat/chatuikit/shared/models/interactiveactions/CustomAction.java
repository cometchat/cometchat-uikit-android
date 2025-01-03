package com.cometchat.chatuikit.shared.models.interactiveactions;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONObject;

public class CustomAction extends ActionEntity {
    private static final String TAG = CustomAction.class.getSimpleName();


    public CustomAction() {
        super(InteractiveConstants.ACTION_TYPE_CUSTOM);
    }

    public static CustomAction fromJson(JSONObject jsonObject) {
        return new CustomAction();
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(InteractiveConstants.ButtonUIConstants.ACTION_TYPE, getActionType());
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return jsonObject;
    }
}
