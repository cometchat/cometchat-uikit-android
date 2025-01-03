package com.cometchat.chatuikit.shared.models;

import com.cometchat.chat.models.Group;
import com.cometchat.chat.models.User;
import com.cometchat.chatuikit.shared.interfaces.Function2;

import java.util.ArrayList;
import java.util.List;

/**
 * CometChatDetailsTemplate is a class representing a template for displaying
 * details in CometChat. It provides customizable options for configuring the
 * appearance and behavior of the details template. The template includes a
 * unique identifier, a title, font settings, colors, and separator options. It
 * also allows defining a function to generate a list of CometChatDetailsOption
 * objects based on user and group data. Use the various setter methods to
 * configure the template and retrieve the configured values using the getter
 * methods. The template can be used to create consistent and visually appealing
 * details views. <br>
 * example :
 *
 * <pre>
 * {
 * 	&#64;code
 * 	CometChatDetailsTemplate template = new CometChatDetailsTemplate().setId("id").setOptions((user, group) -> {
 * 		List<CometChatDetailsOption> detailsOptions = new ArrayList<>();
 * 		return detailsOptions;
 *    }).setTitle("title").setTitleColor(getResources().getColor(R.color.red)).setTitleAppearance(R.style.Name)
 * 			.setSectionSeparatorColor(getResources().getColor(R.color.black)).hideSectionSeparator(false)
 * 			.setItemSeparatorColor(getResources().getColor(R.color.blue)).hideItemSeparator(false);
 * }
 * </pre>
 */
public class CometChatDetailsTemplate {
    private static final String TAG = CometChatDetailsTemplate.class.getSimpleName();


    private String id;
    private Function2<User, Group, List<CometChatDetailsOption>> options;
    private String title;
    private String titleFont;
    private int titleColor;
    private int titleAppearance;
    private int sectionSeparatorColor;
    private boolean hideSectionSeparator;
    private int itemSeparatorColor;
    private boolean hideItemSeparator;

    /**
     * Sets the unique identifier for the template.
     *
     * @param id The unique identifier for the template.
     * @return The updated CometChatDetailsTemplate object.
     */
    public CometChatDetailsTemplate setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the options function for generating a list of CometChatDetailsOption
     * objects.
     *
     * @param options The function that generates the options list.
     * @return The updated CometChatDetailsTemplate object.
     */
    public CometChatDetailsTemplate setOptions(Function2<User, Group, List<CometChatDetailsOption>> options) {
        this.options = options;
        return this;
    }

    /**
     * Sets the title of the template.
     *
     * @param title The title of the template.
     * @return The updated CometChatDetailsTemplate object.
     */
    public CometChatDetailsTemplate setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the font of the template title.
     *
     * @param titleFont The font of the template title.
     * @return The updated CometChatDetailsTemplate object.
     */
    public CometChatDetailsTemplate setTitleFont(String titleFont) {
        this.titleFont = titleFont;
        return this;
    }

    /**
     * Sets the color of the template title.
     *
     * @param titleColor The color of the template title.
     * @return The updated CometChatDetailsTemplate object.
     */
    public CometChatDetailsTemplate setTitleColor(int titleColor) {
        this.titleColor = titleColor;
        return this;
    }

    /**
     * Sets the appearance style of the template title.
     *
     * @param titleAppearance The appearance style of the template title.
     * @return The updated CometChatDetailsTemplate object.
     */
    public CometChatDetailsTemplate setTitleAppearance(int titleAppearance) {
        this.titleAppearance = titleAppearance;
        return this;
    }

    /**
     * Sets the color of the section separator in the template.
     *
     * @param sectionSeparatorColor The color of the section separator.
     * @return The updated CometChatDetailsTemplate object.
     */
    public CometChatDetailsTemplate setSectionSeparatorColor(int sectionSeparatorColor) {
        this.sectionSeparatorColor = sectionSeparatorColor;
        return this;
    }

    /**
     * Sets whether the section separator should be hidden in the template.
     *
     * @param hideSectionSeparator True if the section separator should be hidden, false otherwise.
     * @return The updated CometChatDetailsTemplate object.
     */
    public CometChatDetailsTemplate hideSectionSeparator(boolean hideSectionSeparator) {
        this.hideSectionSeparator = hideSectionSeparator;
        return this;
    }

    /**
     * Sets the color of the item separator in the template.
     *
     * @param itemSeparatorColor The color of the item separator.
     * @return The updated CometChatDetailsTemplate object.
     */
    public CometChatDetailsTemplate setItemSeparatorColor(int itemSeparatorColor) {
        this.itemSeparatorColor = itemSeparatorColor;
        return this;
    }

    /**
     * Sets whether the item separator should be hidden in the template.
     *
     * @param hideItemSeparator True if the item separator should be hidden, false otherwise.
     * @return The updated CometChatDetailsTemplate object.
     */
    public CometChatDetailsTemplate hideItemSeparator(boolean hideItemSeparator) {
        this.hideItemSeparator = hideItemSeparator;
        return this;
    }

    /**
     * Returns the unique identifier of the template.
     *
     * @return The unique identifier of the template.
     */
    public String getId() {
        return id;
    }

    /**
     * Generates a list of CometChatDetailsOption objects based on the provided user
     * and group.
     *
     * @param user  The user object.
     * @param group The group object.
     * @return The list of CometChatDetailsOption objects.
     */
    public List<CometChatDetailsOption> getOptions(User user, Group group) {
        List<CometChatDetailsOption> optionArrayList = new ArrayList<>();
        if (options != null) optionArrayList = options.apply(user, group);
        return optionArrayList;
    }

    /**
     * Returns the title of the template.
     *
     * @return The title of the template.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the font of the template title.
     *
     * @return The font of the template title.
     */
    public String getTitleFont() {
        return titleFont;
    }

    /**
     * Returns the color of the template title.
     *
     * @return The color of the template title.
     */
    public int getTitleColor() {
        return titleColor;
    }

    /**
     * Returns the appearance style of the template title.
     *
     * @return The appearance style of the template title.
     */
    public int getTitleAppearance() {
        return titleAppearance;
    }

    /**
     * Returns the color of the section separator in the template.
     *
     * @return The color of the section separator.
     */
    public int getSectionSeparatorColor() {
        return sectionSeparatorColor;
    }

    /**
     * Returns whether the section separator should be hidden in the template.
     *
     * @return True if the section separator should be hidden, false otherwise.
     */
    public boolean isHideSectionSeparator() {
        return hideSectionSeparator;
    }

    /**
     * Returns the color of the item separator in the template.
     *
     * @return The color of the item separator.
     */
    public int getItemSeparatorColor() {
        return itemSeparatorColor;
    }

    /**
     * Returns whether the item separator should be hidden in the template.
     *
     * @return True if the item separator should be hidden, false otherwise.
     */
    public boolean isHideItemSeparator() {
        return hideItemSeparator;
    }
}
