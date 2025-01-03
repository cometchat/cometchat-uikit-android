package com.cometchat.chatuikit.extensions.collaborative;

import androidx.annotation.StyleRes;

public class CollaborativeBoardBubbleConfiguration {
    private static final String TAG = CollaborativeBoardBubbleConfiguration.class.getSimpleName();
    private String title;
    private String subtitle;
    private String buttonText;
    private @StyleRes int style;

    public CollaborativeBoardBubbleConfiguration setTitle(String title) {
        this.title = title;
        return this;
    }

    public CollaborativeBoardBubbleConfiguration setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public CollaborativeBoardBubbleConfiguration setButtonText(String buttonText) {
        this.buttonText = buttonText;
        return this;
    }

    public CollaborativeBoardBubbleConfiguration setStyle(@StyleRes int style) {
        this.style = style;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getButtonText() {
        return buttonText;
    }

    public @StyleRes int getStyle() {
        return style;
    }
}
