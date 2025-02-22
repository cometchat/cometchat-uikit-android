package com.cometchat.chatuikit.shared.views.reaction.emojikeyboard;

import androidx.annotation.StyleRes;

public class EmojiKeyboardConfiguration {
    private static final String TAG = EmojiKeyboardConfiguration.class.getSimpleName();
    private EmojiKeyBoardView.OnClick onClick;

    private @StyleRes int emojiKeyboardStyle;

    public EmojiKeyBoardView.OnClick getOnClick() {
        return onClick;
    }

    public EmojiKeyboardConfiguration setOnClick(EmojiKeyBoardView.OnClick onClick) {
        this.onClick = onClick;
        return this;
    }

    public @StyleRes int getEmojiKeyboardStyle() {
        return emojiKeyboardStyle;
    }

    public EmojiKeyboardConfiguration setEmojiKeyboardStyle(@StyleRes int emojiKeyboardStyle) {
        this.emojiKeyboardStyle = emojiKeyboardStyle;
        return this;
    }
}
