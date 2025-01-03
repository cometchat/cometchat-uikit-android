package com.cometchat.chatuikit.ai.aiconversationstarter;

import android.content.Context;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.StyleRes;

import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.interfaces.Function2;
import com.cometchat.chatuikit.shared.interfaces.Function3;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class AIConversationStarterConfiguration {
    private static final String TAG = AIConversationStarterConfiguration.class.getSimpleName();
    private Function3<HashMap<String, String>, User, Group, JSONObject> apiConfiguration;
    private Function2<Context, List<String>, View> customView;
    private @LayoutRes int ErrorStateView;
    private @LayoutRes int loadingStateView;
    private String errorText;
    private @StyleRes int aiConversationStarterStyle;

    public AIConversationStarterConfiguration setStyle(@StyleRes int aiConversationStarterStyle) {
        this.aiConversationStarterStyle = aiConversationStarterStyle;
        return this;
    }

    public AIConversationStarterConfiguration setLoadingStateView(int loadingStateView) {
        this.loadingStateView = loadingStateView;
        return this;
    }

    public AIConversationStarterConfiguration setErrorStateView(int errorStateView) {
        ErrorStateView = errorStateView;
        return this;
    }

    public AIConversationStarterConfiguration setErrorText(String errorText) {
        this.errorText = errorText;
        return this;
    }

    public int getErrorStateView() {
        return ErrorStateView;
    }

    public int getLoadingStateView() {
        return loadingStateView;
    }

    public String getErrorText() {
        return errorText;
    }

    public @StyleRes int getStyle() {
        return aiConversationStarterStyle;
    }

    public AIConversationStarterConfiguration setAPIConfiguration(Function3<HashMap<String, String>, User, Group, JSONObject> apiConfiguration) {
        this.apiConfiguration = apiConfiguration;
        return this;
    }

    public AIConversationStarterConfiguration setCustomView(Function2<Context, List<String>, View> customView) {
        this.customView = customView;
        return this;
    }

    public Function2<Context, List<String>, View> getCustomView() {
        return customView;
    }

    public Function3<HashMap<String, String>, User, Group, JSONObject> getAPIConfiguration() {
        return apiConfiguration;
    }
}
