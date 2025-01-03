package com.cometchat.chatuikit.shared.models.interactiveactions;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONObject;

public class DeepLinkingAction extends ActionEntity {
    private static final String TAG = DeepLinkingAction.class.getSimpleName();


    private String url;

    private DeepLinkingAction() {
        super(InteractiveConstants.ACTION_TYPE_DEEP_LINKING);
    }

    public DeepLinkingAction(String url) {
        super(InteractiveConstants.ACTION_TYPE_DEEP_LINKING);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static DeepLinkingAction fromJson(JSONObject jsonObject) {
        DeepLinkingAction deepLinkingAction = new DeepLinkingAction();
        try {
            deepLinkingAction.setUrl(jsonObject.getString(InteractiveConstants.ButtonUIConstants.ACTION_URL));
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return deepLinkingAction;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(InteractiveConstants.ButtonUIConstants.ACTION_TYPE, getActionType());
            jsonObject.put(InteractiveConstants.ButtonUIConstants.ACTION_URL, getUrl());
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.toString());
        }
        return jsonObject;
    }
}
