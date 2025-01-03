package com.cometchat.chatuikit.shared.views.optionsheet;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.StyleRes;

@SuppressWarnings("unused")
public class OptionSheetMenuItem implements Parcelable {
    private static final String TAG = OptionSheetMenuItem.class.getSimpleName();

    private String id;
    private String text;
    private String textFont = null;
    private int cornerRadius = -1;

    private @DrawableRes int startIcon;
    private @DrawableRes int endIcon;
    private @ColorInt int startIconTint;
    private @ColorInt int iconBackgroundColor;
    private @ColorInt int endIconTint;
    private @StyleRes int appearance;
    private @ColorInt int textColor;
    private @ColorInt int background;

    /**
     * Creates an action item with text and a start icon.
     *
     * @param text      The text to display for the item.
     * @param startIcon The resource ID of the icon to display at the start of the item.
     */
    public OptionSheetMenuItem(String text, int startIcon) {
        this.text = text;
        this.startIcon = startIcon;
    }

    /**
     * Creates an action item with an ID, text, and a start icon.
     *
     * @param id        The unique ID of the item.
     * @param text      The text to display for the item.
     * @param startIcon The resource ID of the icon to display at the start of the item.
     */
    public OptionSheetMenuItem(String id, String text, int startIcon) {
        this.id = id;
        this.text = text;
        this.startIcon = startIcon;
    }

    protected OptionSheetMenuItem(Parcel in) {
        text = in.readString();
        startIcon = in.readInt();
    }

    /**
     * Creates an action item with advanced options, including start and end icons,
     * icon colors, appearance, and text color.
     *
     * @param id            The unique ID of the item.
     * @param text          The text to display for the item.
     * @param startIcon     The resource ID of the icon to display at the start of the item.
     * @param endIcon       The resource ID of the icon to display at the end of the item.
     * @param startIconTint The color to apply to the start icon.
     * @param endIconTint   The color to apply to the end icon.
     * @param appearance    The resource ID of the appearance to apply to the item.
     * @param textColor     The color to apply to the item text.
     */
    public OptionSheetMenuItem(String id, String text, @DrawableRes int startIcon, @DrawableRes int endIcon, @ColorInt int startIconTint, @ColorInt int endIconTint, @StyleRes int appearance, @ColorInt int textColor) {
        this.id = id;
        this.text = text;
        this.startIcon = startIcon;
        this.endIcon = endIcon;
        this.startIconTint = startIconTint;
        this.endIconTint = endIconTint;
        this.appearance = appearance;
        this.textColor = textColor;
    }

    /**
     * Creates an action item with advanced options, including start and end icons,
     * icon colors, appearance, text color, and background color.
     *
     * @param id                  The unique ID of the item.
     * @param startIcon           The resource ID of the icon to display at the start of the item.
     * @param startIconTint       The color to apply to the start icon.
     * @param iconBackgroundColor The color to apply to the icon background.
     * @param text                The text to display for the item.
     * @param textFont            The font to apply to the item text.
     * @param appearance          The resource ID of the appearance to apply to the item.
     * @param textColor           The color to apply to the item text.
     * @param background          The color to apply to the item background.
     * @param cornerRadius        The corner radius to apply to the item background.
     */
    public OptionSheetMenuItem(String id, @DrawableRes int startIcon, @ColorInt int startIconTint, @ColorInt int iconBackgroundColor, String text, String textFont, @StyleRes int appearance, @ColorInt int textColor, @ColorInt int background, int cornerRadius) {
        this.id = id;
        this.text = text;
        this.textFont = textFont;
        this.startIcon = startIcon;
        this.startIconTint = startIconTint;
        this.iconBackgroundColor = iconBackgroundColor;
        this.appearance = appearance;
        this.textColor = textColor;
        this.background = background;
        this.cornerRadius = cornerRadius;
    }

    public static final Creator<OptionSheetMenuItem> CREATOR = new Creator<OptionSheetMenuItem>() {
        /**
         * Creates a new instance of ActionItem from a Parcel.
         *
         * @param in
         *            The Parcel containing the data to construct the ActionItem.
         * @return A new instance of ActionItem.
         */
        @Override
        public OptionSheetMenuItem createFromParcel(Parcel in) {
            return new OptionSheetMenuItem(in);
        }

        /**
         * Creates a new array of ActionItem with the specified size.
         *
         * @param size
         *            The size of the array to create.
         * @return An array of ActionItem with the specified size.
         */
        @Override
        public OptionSheetMenuItem[] newArray(int size) {
            return new OptionSheetMenuItem[size];
        }
    };

    /**
     * Describes the contents of this parcelable instance.
     *
     * @return A bitmask indicating the set of special object types marshaled by the
     * parcel.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes the ActionItem instance's data to the specified Parcel.
     *
     * @param dest  The Parcel to which the data should be written.
     * @param flags Flags to modify the behavior of the write operation.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeInt(startIcon);
    }

    /**
     * Gets the unique identifier for this ActionItem.
     *
     * @return The unique identifier for this ActionItem.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this ActionItem.
     *
     * @param id The unique identifier to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the text associated with this ActionItem.
     *
     * @return The text associated with this ActionItem.
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text associated with this ActionItem.
     *
     * @param text The text to set.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Gets the start icon resource ID for this ActionItem.
     *
     * @return The start icon resource ID.
     */
    public int getStartIcon() {
        return startIcon;
    }

    /**
     * Sets the start icon resource ID for this ActionItem.
     *
     * @param startIcon The start icon resource ID to set.
     */
    public void setStartIcon(int startIcon) {
        this.startIcon = startIcon;
    }

    /**
     * Gets the appearance style of this ActionItem.
     *
     * @return The appearance style of this ActionItem.
     */
    public int getAppearance() {
        return appearance;
    }

    /**
     * Gets the text color for this ActionItem.
     *
     * @return The text color.
     */
    public int getTextColor() {
        return textColor;
    }

    /**
     * Sets the appearance style of this ActionItem.
     *
     * @param appearance The appearance style to set.
     */
    public void setTextAppearance(int appearance) {
        this.appearance = appearance;
    }

    /**
     * Gets the end icon resource ID for this ActionItem.
     *
     * @return The end icon resource ID.
     */
    public int getEndIcon() {
        return endIcon;
    }

    /**
     * Gets the tint color for the start icon of this ActionItem.
     *
     * @return The tint color for the start icon.
     */
    public int getStartIconTint() {
        return startIconTint;
    }

    /**
     * Gets the tint color for the end icon of this ActionItem.
     *
     * @return The tint color for the end icon.
     */
    public int getEndIconTint() {
        return endIconTint;
    }

    /**
     * Gets the background color for the icon of this ActionItem.
     *
     * @return The icon background color.
     */
    public int getIconBackgroundColor() {
        return iconBackgroundColor;
    }

    /**
     * Sets the background color for the icon of this ActionItem.
     *
     * @param iconBackgroundColor The background color to set.
     */
    public void setIconBackgroundColor(int iconBackgroundColor) {
        this.iconBackgroundColor = iconBackgroundColor;
    }

    /**
     * Gets the background color of this ActionItem.
     *
     * @return The background color.
     */
    public int getBackground() {
        return background;
    }

    /**
     * Sets the background color of this ActionItem.
     *
     * @param background The background color to set.
     */
    public void setBackground(int background) {
        this.background = background;
    }

    /**
     * Gets the corner radius of this ActionItem.
     *
     * @return The corner radius.
     */
    public int getCornerRadius() {
        return cornerRadius;
    }

    /**
     * Sets the corner radius of this ActionItem.
     *
     * @param cornerRadius The corner radius to set.
     */
    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    /**
     * Gets the text font for this ActionItem.
     *
     * @return The text font.
     */
    public String getTextFont() {
        return textFont;
    }

    /**
     * Sets the text font for this ActionItem.
     *
     * @param textFont The text font to set.
     */
    public void setTextFont(String textFont) {
        this.textFont = textFont;
    }

    /**
     * Sets the text color for this ActionItem.
     *
     * @param textColor The text color to set.
     */
    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }
}
