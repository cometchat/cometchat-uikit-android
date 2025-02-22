package com.cometchat.chatuikit.shared.views.reaction.emojikeyboard.model;

import androidx.annotation.DrawableRes;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class EmojiCategory {
    private static final String TAG = EmojiCategory.class.getSimpleName();


    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("name")
    @Expose
    private String name;

    @SerializedName("symbol")
    @Expose
    private @DrawableRes int symbol;

    @SerializedName("emojis")
    @Expose
    private List<Emoji> emojis = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSymbol() {
        return symbol;
    }

    public void setSymbol(int symbol) {
        this.symbol = symbol;
    }

    public List<Emoji> getEmojis() {
        return emojis;
    }

    public void setEmojis(List<Emoji> emojis) {
        this.emojis = emojis;
    }
}
