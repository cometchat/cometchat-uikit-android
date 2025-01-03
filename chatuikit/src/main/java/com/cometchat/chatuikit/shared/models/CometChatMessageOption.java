package com.cometchat.chatuikit.shared.models;

import androidx.annotation.ColorInt;

import com.cometchat.chatuikit.shared.interfaces.OnClick;

import org.jetbrains.annotations.NotNull;

/**
 * The CometChatMessageOption class represents an option associated with a chat
 * message. It extends the CometChatOption class to inherit common option
 * properties and behaviors. This class provides constructors to create a
 * message option with specified properties.
 *
 * <p>
 * Note: This class is typically used in conjunction with the
 * {@code CometChatMessageTemplate} class to define and retrieve options
 * associated with a chat message. <br>
 * Example :
 *
 * <pre>
 * {
 * 	&#64;code
 * 	CometChatMessageOption option = new CometChatMessageOption(UIKitConstants.MessageOption.COPY,
 * 			context.getString(R.string.copy_message), _getTheme(context).getPalette().getAccent(),
 * 			R.drawable.ic_copy_paste, _getTheme(context).getPalette().getAccent700(),
 * 			_getTheme(context).getTypography().getSubtitle1(), android.R.color.transparent, null);
 * }
 * </pre>
 */
public class CometChatMessageOption extends CometChatOption {
    private static final String TAG = CometChatMessageOption.class.getSimpleName();

    /**
     * Constructs a CometChatMessageOption with the specified properties.
     *
     * @param id      the unique identifier of the option
     * @param title   the title of the option
     * @param icon    the icon resource of the option
     * @param onClick the click listener for the option
     */
    public CometChatMessageOption(@NotNull String id, @NotNull String title, @ColorInt int icon, OnClick onClick) {
        super(id, title, icon, onClick);
    }
}
