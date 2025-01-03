package com.cometchat.chatuikit.shared.models;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.cometchat.chatuikit.shared.interfaces.OnClick;

/**
 * CometChatMessageComposerAction is a class representing an action item in the
 * message composer of CometChat. It encapsulates the properties and behavior of
 * a message composer action, such as an icon, title, colors, and click
 * behavior. The action can be configured with various properties using the
 * setter methods provided. Use the getter methods to retrieve the configured
 * properties of the action. This class is typically used in conjunction with
 * the CometChatMessageComposer to define custom actions that can be displayed
 * in the message composer UI. <br>
 * example:
 *
 * <pre>
 * {@code
 * CometChatMessageComposerAction action = new CometChatMessageComposerAction().setId(UIKitConstants.ComposerAction.CAMERA).setTitle(context.getString(R.string.take_a_photo)).setIcon(R.drawable.ic_camera).setTitleColor(_getTheme(context).getPalette().getAccent()).setTitleAppearance(_getTheme(context).getTypography().getSubtitle1()).setIconTintColor(_getTheme(context).getPalette().getAccent700()).setBackground(_getTheme(context).getPalette().getAccent100())
 * }
 * </pre>
 */
public class CometChatMessageComposerAction {
    private static final String TAG = CometChatMessageComposerAction.class.getSimpleName();
    private String id;
    private String title;
    private String titleFont;
    private @ColorInt int titleColor;
    private @ColorInt int titleAppearance;
    private @DrawableRes int icon;
    private @ColorInt int iconTintColor;
    private @ColorInt int iconBackground;
    private @ColorInt int background;
    private int cornerRadius = -1;
    private OnClick onClick;

    /**
     * Sets the unique identifier for the action.
     *
     * @param id the identifier for the action
     * @return the {@code CometChatMessageComposerAction} instance
     */
    public CometChatMessageComposerAction setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the title of the action.
     *
     * @param title the title of the action
     * @return the {@code CometChatMessageComposerAction} instance
     */
    public CometChatMessageComposerAction setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the color of the action title.
     *
     * @param titleColor the color of the action title
     * @return the {@code CometChatMessageComposerAction} instance
     */
    public CometChatMessageComposerAction setTitleColor(int titleColor) {
        this.titleColor = titleColor;
        return this;
    }

    /**
     * Sets the background color of the action icon.
     *
     * @param iconBackground the background color of the action icon
     * @return the {@code CometChatMessageComposerAction} instance
     */
    public CometChatMessageComposerAction setIconBackground(int iconBackground) {
        this.iconBackground = iconBackground;
        return this;
    }

    /**
     * Sets the appearance style of the action title.
     *
     * @param titleAppearance the appearance style of the action title
     * @return the {@code CometChatMessageComposerAction} instance
     */
    public CometChatMessageComposerAction setTitleAppearance(int titleAppearance) {
        this.titleAppearance = titleAppearance;
        return this;
    }

    /**
     * Sets the icon resource for the action.
     *
     * @param icon the icon resource for the action
     * @return the {@code CometChatMessageComposerAction} instance
     */
    public CometChatMessageComposerAction setIcon(int icon) {
        this.icon = icon;
        return this;
    }

    /**
     * Sets the tint color of the action icon.
     *
     * @param iconTintColor the tint color of the action icon
     * @return the {@code CometChatMessageComposerAction} instance
     */
    public CometChatMessageComposerAction setIconTintColor(int iconTintColor) {
        this.iconTintColor = iconTintColor;
        return this;
    }

    /**
     * Sets the click behavior of the action.
     *
     * @param onClick the click behavior of the action
     * @return the {@code CometChatMessageComposerAction} instance
     */
    @NonNull
    public CometChatMessageComposerAction setOnClick(OnClick onClick) {
        this.onClick = onClick;
        return this;
    }

    /**
     * Sets the font style of the action title.
     *
     * @param titleFont the font style of the action title
     * @return the {@code CometChatMessageComposerAction} instance
     */
    public CometChatMessageComposerAction setTitleFont(String titleFont) {
        this.titleFont = titleFont;
        return this;
    }

    /**
     * Sets the background color of the action.
     *
     * @param background the background color of the action
     * @return the {@code CometChatMessageComposerAction} instance
     */
    public CometChatMessageComposerAction setBackground(int background) {
        this.background = background;
        return this;
    }

    /**
     * Sets the corner radius of the action background.
     *
     * @param cornerRadius the corner radius of the action background
     * @return the {@code CometChatMessageComposerAction} instance
     */
    public CometChatMessageComposerAction setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    /**
     * Returns the unique identifier of the action.
     *
     * @return the identifier of the action
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the title of the action.
     *
     * @return the title of the action
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the color of the action title.
     *
     * @return the color of the action title
     */
    public int getTitleColor() {
        return titleColor;
    }

    /**
     * Returns the appearance style of the action title.
     *
     * @return the appearance style of the action title
     */
    public int getTitleAppearance() {
        return titleAppearance;
    }

    /**
     * Returns the icon resource of the action.
     *
     * @return the icon resource of the action
     */
    public int getIcon() {
        return icon;
    }

    /**
     * Returns the tint color of the action icon.
     *
     * @return the tint color of the action icon
     */
    public int getIconTintColor() {
        return iconTintColor;
    }

    /**
     * Returns the font style of the action title.
     *
     * @return the font style of the action title
     */
    public String getTitleFont() {
        return titleFont;
    }

    /**
     * Returns the background color of the action icon.
     *
     * @return the background color of the action icon
     */
    public int getIconBackground() {
        return iconBackground;
    }

    /**
     * Returns the background color of the action.
     *
     * @return the background color of the action
     */
    public int getBackground() {
        return background;
    }

    /**
     * Returns the corner radius of the action background.
     *
     * @return the corner radius of the action background
     */
    public int getCornerRadius() {
        return cornerRadius;
    }

    /**
     * Returns the click behavior of the action.
     *
     * @return the click behavior of the action
     */
    public OnClick getOnClick() {
        return onClick;
    }
}
