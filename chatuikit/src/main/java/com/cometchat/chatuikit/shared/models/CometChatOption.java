package com.cometchat.chatuikit.shared.models;

import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;

import com.cometchat.chatuikit.shared.interfaces.OnClick;

/**
 * The CometChatOption class represents an option that can be displayed in a
 * Uikit.
 *
 * <p>
 * It encapsulates properties such as the option's ID, title, icon, colors,
 * appearance, and click listener.
 */
public class CometChatOption {
    private static final String TAG = CometChatOption.class.getSimpleName();
    private String id;
    private String title;
    private @ColorInt int titleColor;
    private @StyleRes int titleAppearance;
    private @DrawableRes int icon;
    private @ColorInt int iconTintColor;
    private Drawable drawableIcon;
    private @ColorInt int backgroundColor;
    private OnClick onClick;

    /**
     * Constructs a new instance of CometChatOption.
     */
    public CometChatOption() {
    }

    /**
     * Constructs a new instance of CometChatOption with the specified ID, title,
     * and icon.
     *
     * @param id    the unique identifier of the option
     * @param title the title of the option
     * @param icon  the icon resource of the option
     */
    public CometChatOption(String id, String title, @DrawableRes int icon) {
        this.id = id;
        this.title = title;
        this.icon = icon;
    }

    /**
     * Constructs a new instance of CometChatOption with the specified ID, title,
     * icon, and click listener.
     *
     * @param id      the unique identifier of the option
     * @param title   the title of the option
     * @param icon    the icon resource of the option
     * @param onClick the click listener for the option
     */
    public CometChatOption(String id, String title, int icon, OnClick onClick) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.onClick = onClick;
    }

    /**
     * Constructs a new instance of CometChatOption with the specified properties.
     *
     * @param id              the unique identifier of the option
     * @param title           the title of the option
     * @param icon            the icon resource of the option
     * @param backgroundColor the background color of the option
     * @param onClick         the click listener for the option
     */
    public CometChatOption(String id, String title, int icon, int backgroundColor, OnClick onClick) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.backgroundColor = backgroundColor;
        this.onClick = onClick;
    }

    /**
     * Constructs a new instance of CometChatOption with the specified properties.
     *
     * @param id              the unique identifier of the option
     * @param title           the title of the option
     * @param titleColor      the color of the option title
     * @param icon            the icon resource of the option
     * @param iconTintColor   the tint color of the option icon
     * @param titleAppearance the appearance style resource of the option title
     * @param onClick         the click listener for the option
     */
    public CometChatOption(String id, String title, int titleColor, int icon, int iconTintColor, @StyleRes int titleAppearance, OnClick onClick) {
        this.id = id;
        this.title = title;
        this.titleColor = titleColor;
        this.icon = icon;
        this.iconTintColor = iconTintColor;
        this.titleAppearance = titleAppearance;
        this.onClick = onClick;
    }

    /**
     * Retrieves the color of the option title.
     *
     * @return the color of the option title
     */
    public int getTitleColor() {
        return titleColor;
    }

    /**
     * Retrieves the tint color of the option icon.
     *
     * @return the tint color of the option icon
     */
    public int getIconTintColor() {
        return iconTintColor;
    }

    /**
     * Retrieves the background color of the option.
     *
     * @return the background color of the option
     */
    public int getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Retrieves the ID of the option.
     *
     * @return the ID of the option
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the title of the option.
     *
     * @return the title of the option
     */
    public String getTitle() {
        return title;
    }

    /**
     * Retrieves the icon resource of the option.
     *
     * @return the icon resource of the option
     */
    public int getIcon() {
        return icon;
    }

    /**
     * Retrieves the drawable icon of the option.
     *
     * @return the drawable icon of the option
     */
    public Drawable getDrawableIcon() {
        return drawableIcon;
    }

    /**
     * Retrieves the click listener for the option.
     *
     * @return the click listener for the option
     */
    public OnClick getClick() {
        return onClick;
    }

    /**
     * Sets the ID of the option.
     *
     * @param id the ID of the option
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the title of the option.
     *
     * @param title the title of the option
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets the icon resource of the option.
     *
     * @param icon the icon resource of the option
     */
    public void setIcon(int icon) {
        this.icon = icon;
    }

    /**
     * Sets the click listener for the option.
     *
     * @param onClick the click listener for the option
     */
    public void setOnClick(OnClick onClick) {
        this.onClick = onClick;
    }

    /**
     * Sets the color of the option title.
     *
     * @param titleColor the color of the option title
     */
    public void setTitleColor(int titleColor) {
        this.titleColor = titleColor;
    }

    /**
     * Sets the tint color of the option icon.
     *
     * @param iconTintColor the tint color of the option icon
     */
    public void setIconTintColor(int iconTintColor) {
        this.iconTintColor = iconTintColor;
    }

    /**
     * Sets the drawable icon of the option.
     *
     * @param drawableIcon the drawable icon of the option
     */
    public void setDrawableIcon(Drawable drawableIcon) {
        this.drawableIcon = drawableIcon;
    }

    /**
     * Retrieves the appearance style resource of the option title.
     *
     * @return the appearance style resource of the option title
     */
    public int getTitleAppearance() {
        return titleAppearance;
    }

    /**
     * Sets the background color of the option.
     *
     * @param backgroundColor the background color of the option
     */
    public void setBackgroundColor(@ColorInt int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
