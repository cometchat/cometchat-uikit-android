package com.cometchat.chatuikit.shared.models.interactiveelements;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONObject;

public class PlaceHolder {
    private static final String TAG = PlaceHolder.class.getSimpleName();


    private String hint;

    private PlaceHolder() {
    }

    public PlaceHolder(String hint) {
        this.hint = hint;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public static PlaceHolder fromJson(JSONObject json) {
        PlaceHolder placeHolder = new PlaceHolder();
        if (json != null) {
            try {
                placeHolder.setHint(json.getString(InteractiveConstants.TextInputUIConstants.PLACEHOLDER_TEXT));
            } catch (Exception e) {
                CometChatLogger.e(TAG, e.getMessage());
            }
        }
        return placeHolder;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (getHint() != null && !getHint().isEmpty()) {
                jsonObject.put(InteractiveConstants.TextInputUIConstants.PLACEHOLDER_TEXT, getHint());
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return jsonObject;
    }
}
