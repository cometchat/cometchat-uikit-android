package com.cometchat.chatuikit.shared.spans;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.formatters.style.PromptTextStyle;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.cometchat.chatuikit.shared.views.suggestionlist.SuggestionItem;

import javax.annotation.Nonnull;

public class TagSpan extends ClickableSpan {
    private static final String TAG = TagSpan.class.getSimpleName();
    private String mText;
    private char id;
    private PromptTextStyle textAppearance;
    private SuggestionItem suggestionItem;
    private final OnTagClick<User> onTagCLick;

    public TagSpan(char id, @Nonnull String text, @NonNull SuggestionItem suggestionItem, OnTagClick<User> onTagCLick) {
        this.mText = text;
        this.id = id;
        this.suggestionItem = suggestionItem;
        this.textAppearance = suggestionItem.getPromptTextAppearance();
        this.onTagCLick = onTagCLick;
    }

    @Override
    public void onClick(@NonNull View widget) {
        // handle click event
        onTagCLick.onClick(widget.getContext(), User.fromJson(suggestionItem.getData().toString()));
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        super.updateDrawState(ds);
        if (textAppearance == null) return;
        if (textAppearance.getTextAppearance() != null)
            ds.setTypeface(textAppearance.getTextAppearance());

        if (textAppearance.getTextSize() > -1) {
            ds.setTextSize(textAppearance.getTextSize());
        }
        if (textAppearance.getColor() != 0) ds.setColor(textAppearance.getColor());
        if (textAppearance.getBackgroundColor() != 0) {
            ds.bgColor = Utils.applyColorWithAlphaValue(textAppearance.getBackgroundColor(), 51);
        }
        ds.setUnderlineText(false);
    }

    public char getId() {
        return id;
    }

    public SuggestionItem getSuggestionItem() {
        return suggestionItem;
    }

    public String getText() {
        return mText;
    }

    public PromptTextStyle getTextAppearance() {
        return textAppearance;
    }

    public void setTextAppearance(PromptTextStyle textAppearance) {
        this.textAppearance = textAppearance;
    }

    public void setSuggestionItem(SuggestionItem suggestionItem) {
        this.suggestionItem = suggestionItem;
    }

    public void setText(String mText) {
        this.mText = mText;
    }

    public void setId(char id) {
        this.id = id;
    }
}
