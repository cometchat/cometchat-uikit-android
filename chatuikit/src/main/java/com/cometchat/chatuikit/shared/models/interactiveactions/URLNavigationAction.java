package com.cometchat.chatuikit.shared.models.interactiveactions;

import androidx.annotation.NonNull;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONObject;

public class URLNavigationAction extends ActionEntity {
    private static final String TAG = URLNavigationAction.class.getSimpleName();


    private String url;

    private URLNavigationAction() {
        super(InteractiveConstants.ACTION_TYPE_URL_NAVIGATION);
    }

    public URLNavigationAction(String url) {
        super(InteractiveConstants.ACTION_TYPE_URL_NAVIGATION);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    private void setUrl(String url) {
        this.url = url;
    }

    @NonNull
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

    public static URLNavigationAction fromJson(JSONObject jsonObject) {
        URLNavigationAction urlNavigationAction = new URLNavigationAction();
        try {
            urlNavigationAction.setUrl(jsonObject.getString(InteractiveConstants.ButtonUIConstants.ACTION_URL));
        } catch (Exception e) {
            CometChatLogger.e("URLNavigationAction", e.toString());
        }
        return urlNavigationAction;
    }
}
