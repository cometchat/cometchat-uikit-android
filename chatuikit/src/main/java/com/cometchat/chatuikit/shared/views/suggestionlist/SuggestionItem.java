package com.cometchat.chatuikit.shared.views.suggestionlist;

import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.formatters.style.PromptTextStyle;

import org.json.JSONObject;

public class SuggestionItem {
    private static final String TAG = SuggestionItem.class.getSimpleName();


    private String id;
    private String name;
    private String leadingIconUrl;
    private @DrawableRes int leadingIcon;
    private String status;
    private String promptText;
    private String underlyingText;
    private JSONObject data;
    private PromptTextStyle promptTextStyle;
    private boolean hideLeadingIcon;
    private @StyleRes int leadingIconStyle;

    /**
     * Constructor for creating a SuggestionItem instance with a leading icon URL.
     *
     * @param id             The unique identifier for the suggestion item.
     * @param name           The name of the suggestion item.
     * @param leadingIconUrl The URL of the leading icon for the suggestion item.
     * @param status         The status of the suggestion item.
     * @param promptText     The prompt text to be shown for the suggestion item.
     * @param underlyingText The underlying text of the suggestion item.
     * @param metaData       Additional data associated with the suggestion item.
     */
    public SuggestionItem(String id, String name, String leadingIconUrl, String status, String promptText, String underlyingText, JSONObject metaData) {
        this.id = id;
        this.name = name;
        this.leadingIconUrl = leadingIconUrl;
        this.promptText = promptText;
        this.underlyingText = underlyingText;
        this.status = status;
        this.data = metaData;
    }

    /**
     * Constructor for creating a SuggestionItem instance with a leading icon URL
     * and custom style.
     *
     * @param id              The unique identifier for the suggestion item.
     * @param name            The name of the suggestion item.
     * @param leadingIconUrl  The URL of the leading icon for the suggestion item.
     * @param status          The status of the suggestion item.
     * @param promptText      The prompt text to be shown for the suggestion item.
     * @param underlyingText  The underlying text of the suggestion item.
     * @param metaData        Additional data associated with the suggestion item.
     * @param promptTextStyle The custom style for the prompt text.
     */
    public SuggestionItem(String id, String name, String leadingIconUrl, String status, String promptText, String underlyingText, JSONObject metaData, PromptTextStyle promptTextStyle) {
        this.id = id;
        this.name = name;
        this.leadingIconUrl = leadingIconUrl;
        this.promptText = promptText;
        this.underlyingText = underlyingText;
        this.data = metaData;
        this.status = status;
        this.promptTextStyle = promptTextStyle;
    }

    /**
     * Constructor for creating a SuggestionItem instance with a leading icon
     * drawable.
     *
     * @param id              The unique identifier for the suggestion item.
     * @param name            The name of the suggestion item.
     * @param leadingIcon     The drawable resource identifier of the leading icon for the
     *                        suggestion item.
     * @param status          The status of the suggestion item.
     * @param promptText      The prompt text to be shown for the suggestion item.
     * @param underlyingText  The underlying text of the suggestion item.
     * @param metaData        Additional data associated with the suggestion item.
     * @param promptTextStyle The custom style for the prompt text.
     */
    public SuggestionItem(String id, String name, @DrawableRes int leadingIcon, String status, String promptText, String underlyingText, JSONObject metaData, PromptTextStyle promptTextStyle) {
        this.id = id;
        this.name = name;
        this.leadingIcon = leadingIcon;
        this.promptText = promptText;
        this.underlyingText = underlyingText;
        this.data = metaData;
        this.status = status;
        this.promptTextStyle = promptTextStyle;
    }

    public void setHideLeadingIcon(boolean hideLeadingIcon) {
        this.hideLeadingIcon = hideLeadingIcon;
    }

    public void setLeadingIconStyle(@StyleRes int leadingIconStyle) {
        this.leadingIconStyle = leadingIconStyle;
    }

    public void setLeadingIcon(@DrawableRes int leadingIcon) {
        this.leadingIcon = leadingIcon;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLeadingIconUrl(String leadingIconUrl) {
        this.leadingIconUrl = leadingIconUrl;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public void setUnderlyingText(String underlyingText) {
        this.underlyingText = underlyingText;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public void setPromptTextAppearance(PromptTextStyle promptTextStyle) {
        this.promptTextStyle = promptTextStyle;
    }

    public String getStatus() {
        return status;
    }

    public PromptTextStyle getPromptTextAppearance() {
        return promptTextStyle;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLeadingIconUrl() {
        return leadingIconUrl;
    }

    public String getPromptText() {
        return promptText;
    }

    public String getUnderlyingText() {
        return underlyingText;
    }

    public JSONObject getData() {
        return data;
    }

    public int getLeadingIcon() {
        return leadingIcon;
    }

    public boolean isHideLeadingIcon() {
        return hideLeadingIcon;
    }

    public @StyleRes int getLeadingIconStyle() {
        return leadingIconStyle;
    }
}
