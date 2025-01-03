package com.cometchat.chatuikit.extensions.reaction.emojikeyboard;

import androidx.annotation.StyleRes;

public class EmojiKeyboardConfiguration {
    private static final String TAG = EmojiKeyboardConfiguration.class.getSimpleName();
    private EmojiKeyBoardView.OnClick onClick;

    private @StyleRes int emojiKeyboardStyle;

    public EmojiKeyboardConfiguration setOnClick(EmojiKeyBoardView.OnClick onClick) {
        this.onClick = onClick;
        return this;
    }

    public EmojiKeyboardConfiguration setEmojiKeyboardStyle(@StyleRes int emojiKeyboardStyle) {
        this.emojiKeyboardStyle = emojiKeyboardStyle;
        return this;
    }

    public EmojiKeyBoardView.OnClick getOnClick() {
        return onClick;
    }

    public @StyleRes int getEmojiKeyboardStyle() {
        return emojiKeyboardStyle;
    }
}
