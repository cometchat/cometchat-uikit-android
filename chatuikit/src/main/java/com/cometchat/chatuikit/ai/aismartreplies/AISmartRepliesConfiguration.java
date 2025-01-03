package com.cometchat.chatuikit.ai.aismartreplies;

import android.content.Context;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.interfaces.Function2;
import com.cometchat.chatuikit.shared.interfaces.Function3;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class AISmartRepliesConfiguration {
    private static final String TAG = AISmartRepliesConfiguration.class.getSimpleName();
    private Function3<HashMap<String, String>, User, Group, JSONObject> apiConfiguration;
    private Function2<Context, List<String>, View> customView;
    private @LayoutRes int ErrorStateView;
    private @LayoutRes int loadingStateView;
    private String errorText;
    private @StyleRes int style;
    private @DrawableRes int buttonIcon;

    public AISmartRepliesConfiguration setStyle(@StyleRes int aiSmartRepliesStyle) {
        this.style = aiSmartRepliesStyle;
        return this;
    }

    public AISmartRepliesConfiguration setButtonIcon(@DrawableRes int buttonIcon) {
        this.buttonIcon = buttonIcon;
        return this;
    }

    public AISmartRepliesConfiguration setCustomView(Function2<Context, List<String>, View> customView) {
        this.customView = customView;
        return this;
    }

    public AISmartRepliesConfiguration setAPIConfiguration(Function3<HashMap<String, String>, User, Group, JSONObject> apiConfiguration) {
        this.apiConfiguration = apiConfiguration;
        return this;
    }

    @NonNull
    public AISmartRepliesConfiguration setErrorText(String errorText) {
        this.errorText = errorText;
        return this;
    }

    public AISmartRepliesConfiguration setErrorStateView(@LayoutRes int errorStateView) {
        ErrorStateView = errorStateView;
        return this;
    }

    public AISmartRepliesConfiguration setLoadingStateView(@LayoutRes int loadingStateView) {
        this.loadingStateView = loadingStateView;
        return this;
    }

    public String getErrorText() {
        return errorText;
    }

    public @StyleRes int getStyle() {
        return style;
    }

    public @DrawableRes int getButtonIcon() {
        return buttonIcon;
    }

    public int getErrorStateView() {
        return ErrorStateView;
    }

    public int getLoadingStateView() {
        return loadingStateView;
    }

    public Function3<HashMap<String, String>, User, Group, JSONObject> getApiConfiguration() {
        return apiConfiguration;
    }

    public Function2<Context, List<String>, View> getCustomView() {
        return customView;
    }
}
