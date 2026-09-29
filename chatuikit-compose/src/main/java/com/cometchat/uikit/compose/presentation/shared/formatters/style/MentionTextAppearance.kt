package com.cometchat.uikit.compose.presentation.shared.formatters.style

import android.graphics.Typeface
import androidx.annotation.ColorInt

/**
 * A style class for customizing the appearance of mention text in various contexts.
 * Provides separate styling options for regular mentions and mentions of the logged-in user.
 */
public class MentionTextAppearance {
    private var textColor: Int = 0
    private var textAppearance: Typeface? = null
    private var textBackgroundColor: Int = 0
    private var loggedInUserTextColor: Int = 0
    private var loggedInUserTextAppearance: Typeface? = null
    private var loggedInUserTextBackgroundColor: Int = 0

    /**
     * Sets the text color for regular mentions.
     *
     * @param textColor The text color to set
     * @return This MentionTextAppearance instance for method chaining
     */
    public fun setTextColor(@ColorInt textColor: Int): MentionTextAppearance {
        this.textColor = textColor
        return this
    }

    /**
     * Sets the text appearance (typeface) for regular mentions.
     *
     * @param textAppearance The typeface to set
     * @return This MentionTextAppearance instance for method chaining
     */
    public fun setTextAppearance(textAppearance: Typeface?): MentionTextAppearance {
        this.textAppearance = textAppearance
        return this
    }

    /**
     * Sets the background color for regular mentions.
     *
     * @param textBackgroundColor The background color to set
     * @return This MentionTextAppearance instance for method chaining
     */
    public fun setTextBackgroundColor(@ColorInt textBackgroundColor: Int): MentionTextAppearance {
        this.textBackgroundColor = textBackgroundColor
        return this
    }

    /**
     * Sets the text color for mentions of the logged-in user.
     *
     * @param loggedInUserTextColor The text color to set
     * @return This MentionTextAppearance instance for method chaining
     */
    public fun setLoggedInUserTextColor(@ColorInt loggedInUserTextColor: Int): MentionTextAppearance {
        this.loggedInUserTextColor = loggedInUserTextColor
        return this
    }

    /**
     * Sets the text appearance (typeface) for mentions of the logged-in user.
     *
     * @param loggedInUserTextAppearance The typeface to set
     * @return This MentionTextAppearance instance for method chaining
     */
    public fun setLoggedInUserTextAppearance(loggedInUserTextAppearance: Typeface?): MentionTextAppearance {
        this.loggedInUserTextAppearance = loggedInUserTextAppearance
        return this
    }

    /**
     * Sets the background color for mentions of the logged-in user.
     *
     * @param loggedInUserTextBackgroundColor The background color to set
     * @return This MentionTextAppearance instance for method chaining
     */
    public fun setLoggedInUserTextBackgroundColor(@ColorInt loggedInUserTextBackgroundColor: Int): MentionTextAppearance {
        this.loggedInUserTextBackgroundColor = loggedInUserTextBackgroundColor
        return this
    }

    /**
     * Gets the text color for regular mentions.
     *
     * @return The text color
     */
    public fun getTextColor(): Int = textColor

    /**
     * Gets the text appearance (typeface) for regular mentions.
     *
     * @return The typeface
     */
    public fun getTextAppearance(): Typeface? = textAppearance

    /**
     * Gets the background color for regular mentions.
     *
     * @return The background color
     */
    public fun getTextBackgroundColor(): Int = textBackgroundColor

    /**
     * Gets the text color for mentions of the logged-in user.
     *
     * @return The text color
     */
    public fun getLoggedInUserTextColor(): Int = loggedInUserTextColor

    /**
     * Gets the text appearance (typeface) for mentions of the logged-in user.
     *
     * @return The typeface
     */
    public fun getLoggedInUserTextAppearance(): Typeface? = loggedInUserTextAppearance

    /**
     * Gets the background color for mentions of the logged-in user.
     *
     * @return The background color
     */
    public fun getLoggedInUserTextBackgroundColor(): Int = loggedInUserTextBackgroundColor
}
