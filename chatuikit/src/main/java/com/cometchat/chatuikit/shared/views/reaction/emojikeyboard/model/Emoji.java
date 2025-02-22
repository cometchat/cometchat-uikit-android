package com.cometchat.chatuikit.shared.views.reaction.emojikeyboard.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Emoji {
    private static final String TAG = Emoji.class.getSimpleName();
    @SerializedName("keywords")
    @Expose
    private List<String> keywords = null;

    @SerializedName("emoji")
    @Expose
    private String emoji;

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}
