package com.cometchat.chatuikit.shared.models.interactiveactions;

import com.cometchat.chatuikit.logger.CometChatLogger;
import com.cometchat.chatuikit.shared.models.interactivemessage.InteractiveConstants;

import org.json.JSONObject;

public class APIAction extends ActionEntity {
    private static final String TAG = APIAction.class.getSimpleName();

    private String url;
    private String method;
    private JSONObject payload;
    private JSONObject headers;

    private APIAction() {
        super(InteractiveConstants.ACTION_TYPE_API_ACTION);
    }

    public APIAction(String url, String method, String dataKey) {
        super(InteractiveConstants.ACTION_TYPE_API_ACTION);
        setUrl(url);
        setMethod(method);
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }

    public void setHeaders(JSONObject headers) {
        this.headers = headers;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public JSONObject getHeaders() {
        return headers;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public static APIAction fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        APIAction apiAction = new APIAction();
        try {
            if (jsonObject.has(InteractiveConstants.ButtonUIConstants.ACTION_URL)) {
                apiAction.setUrl(jsonObject.getString(InteractiveConstants.ButtonUIConstants.ACTION_URL));
            }
            if (jsonObject.has(InteractiveConstants.ButtonUIConstants.METHOD)) {
                apiAction.setMethod(jsonObject.getString(InteractiveConstants.ButtonUIConstants.METHOD));
            }
            if (jsonObject.has(InteractiveConstants.ButtonUIConstants.ACTION_PAYLOAD)) {
                apiAction.setPayload(jsonObject.getJSONObject(InteractiveConstants.ButtonUIConstants.ACTION_PAYLOAD));
            }
            if (jsonObject.has(InteractiveConstants.ButtonUIConstants.ACTION_HEADERS)) {
                apiAction.setHeaders(jsonObject.getJSONObject(InteractiveConstants.ButtonUIConstants.ACTION_HEADERS));
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.getMessage());
        }
        return apiAction;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(InteractiveConstants.ButtonUIConstants.ACTION_TYPE, getActionType());
            if (getUrl() != null) {
                jsonObject.put(InteractiveConstants.ButtonUIConstants.ACTION_URL, getUrl());
            }
            if (getMethod() != null) {
                jsonObject.put(InteractiveConstants.ButtonUIConstants.METHOD, getMethod());
            }
            if (getPayload() != null) {
                jsonObject.put(InteractiveConstants.ButtonUIConstants.ACTION_PAYLOAD, getPayload());
            }
            if (getHeaders() != null) {
                jsonObject.put(InteractiveConstants.ButtonUIConstants.ACTION_HEADERS, getHeaders());
            }
        } catch (Exception e) {
            CometChatLogger.e(TAG, e.getMessage());
        }
        return jsonObject;
    }
}
