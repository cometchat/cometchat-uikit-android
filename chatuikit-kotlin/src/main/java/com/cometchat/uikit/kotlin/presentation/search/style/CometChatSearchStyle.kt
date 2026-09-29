package com.cometchat.uikit.kotlin.presentation.search.style

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import com.cometchat.uikit.kotlin.theme.CometChatTheme
import com.cometchat.uikit.kotlin.presentation.shared.baseelements.avatar.CometChatAvatarStyle
import com.cometchat.uikit.kotlin.presentation.shared.baseelements.badgecount.CometChatBadgeCountStyle
import com.cometchat.uikit.kotlin.presentation.shared.baseelements.date.CometChatDateStyle
import com.cometchat.uikit.kotlin.presentation.shared.statusindicator.CometChatStatusIndicatorStyle

/**
 * Style configuration for CometChatSearch component.
 *
 * This class encapsulates all visual styling properties for the search component,
 * including container, search bar, filter chips, section headers, date separators,
 * conversation items, message items, and state views.
 */
public class CometChatSearchStyle private constructor(
    // Container styling
    @ColorInt public val backgroundColor: Int?,

    // Search bar styling
    @ColorInt public val searchBarBackgroundColor: Int?,
    @ColorInt public val searchBarStrokeColor: Int?,
    public val searchBarStrokeWidth: Float?,
    public val searchBarCornerRadius: Float?,
    @ColorInt public val searchBarTextColor: Int?,
    @StyleRes public val searchBarTextAppearance: Int?,
    @ColorInt public val searchBarHintTextColor: Int?,
    @StyleRes public val searchBarHintTextAppearance: Int?,

    // Search bar icons
    public val backIcon: Drawable?,
    @ColorInt public val backIconTint: Int?,
    public val clearIcon: Drawable?,
    @ColorInt public val clearIconTint: Int?,
    public val searchIcon: Drawable?,
    @ColorInt public val searchIconTint: Int?,

    // Filter chip styling
    @ColorInt public val filterChipBackgroundColor: Int?,
    @ColorInt public val filterChipSelectedBackgroundColor: Int?,
    @ColorInt public val filterChipTextColor: Int?,
    @ColorInt public val filterChipSelectedTextColor: Int?,
    @StyleRes public val filterChipTextAppearance: Int?,
    @ColorInt public val filterChipStrokeColor: Int?,
    @ColorInt public val filterChipSelectedStrokeColor: Int?,
    public val filterChipStrokeWidth: Float?,
    public val filterChipCornerRadius: Float?,

    // Section header styling
    @ColorInt public val sectionHeaderTextColor: Int?,
    @StyleRes public val sectionHeaderTextAppearance: Int?,
    @ColorInt public val sectionHeaderBackgroundColor: Int?,

    // Conversation item styling
    @ColorInt public val conversationItemBackgroundColor: Int?,
    @ColorInt public val conversationTitleTextColor: Int?,
    @StyleRes public val conversationTitleTextAppearance: Int?,
    @ColorInt public val conversationSubtitleTextColor: Int?,
    @StyleRes public val conversationSubtitleTextAppearance: Int?,
    @ColorInt public val conversationTimestampTextColor: Int?,
    @StyleRes public val conversationTimestampTextAppearance: Int?,
    @ColorInt public val conversationSeparatorColor: Int?,

    // Message item styling
    @ColorInt public val messageItemBackgroundColor: Int?,
    @ColorInt public val messageTitleTextColor: Int?,
    @StyleRes public val messageTitleTextAppearance: Int?,
    @ColorInt public val messageSubtitleTextColor: Int?,
    @StyleRes public val messageSubtitleTextAppearance: Int?,
    @ColorInt public val messageTimestampTextColor: Int?,
    @StyleRes public val messageTimestampTextAppearance: Int?,
    @ColorInt public val messageSeparatorColor: Int?,
    @ColorInt public val messageLinkTextColor: Int?,
    @StyleRes public val messageLinkTextAppearance: Int?,
    public val messageThreadIcon: Drawable?,
    @ColorInt public val messageThreadIconTint: Int?,

    // Date separator styling
    @ColorInt public val dateSeparatorBackgroundColor: Int?,
    @ColorInt public val dateSeparatorTextColor: Int?,
    @StyleRes public val dateSeparatorTextAppearance: Int?,
    public val dateSeparatorStyle: CometChatDateStyle?,

    // Avatar styling
    public val avatarStyle: CometChatAvatarStyle?,

    // Badge styling
    public val badgeStyle: CometChatBadgeCountStyle?,

    // Status indicator styling
    public val statusIndicatorStyle: CometChatStatusIndicatorStyle?,

    // Loading state styling
    @ColorInt public val loadingStateBackgroundColor: Int?,

    // Empty state styling
    @ColorInt public val emptyStateTextColor: Int?,
    @StyleRes public val emptyStateTextAppearance: Int?,
    @ColorInt public val emptyStateSubtitleTextColor: Int?,
    @StyleRes public val emptyStateSubtitleTextAppearance: Int?,
    public val emptyStateIcon: Drawable?,
    @ColorInt public val emptyStateIconTint: Int?,

    // Initial state styling
    @ColorInt public val initialStateTextColor: Int?,
    @StyleRes public val initialStateTextAppearance: Int?,
    @ColorInt public val initialStateSubtitleTextColor: Int?,
    @StyleRes public val initialStateSubtitleTextAppearance: Int?,
    public val initialStateIcon: Drawable?,
    @ColorInt public val initialStateIconTint: Int?,

    // Error state styling
    @ColorInt public val errorStateTextColor: Int?,
    @StyleRes public val errorStateTextAppearance: Int?,
    @ColorInt public val errorStateSubtitleTextColor: Int?,
    @StyleRes public val errorStateSubtitleTextAppearance: Int?,
    public val errorStateIcon: Drawable?,
    @ColorInt public val errorStateIconTint: Int?,

    // See more button styling
    @ColorInt public val seeMoreTextColor: Int?,
    @StyleRes public val seeMoreTextAppearance: Int?
) {

    /**
     * Builder for creating CometChatSearchStyle instances.
     */
    public class Builder(private val context: Context) {
        // Container styling
        @ColorInt private var backgroundColor: Int? = null

        // Search bar styling
        @ColorInt private var searchBarBackgroundColor: Int? = null
        @ColorInt private var searchBarStrokeColor: Int? = null
        private var searchBarStrokeWidth: Float? = null
        private var searchBarCornerRadius: Float? = null
        @ColorInt private var searchBarTextColor: Int? = null
        @StyleRes private var searchBarTextAppearance: Int? = null
        @ColorInt private var searchBarHintTextColor: Int? = null
        @StyleRes private var searchBarHintTextAppearance: Int? = null

        // Search bar icons
        private var backIcon: Drawable? = null
        @ColorInt private var backIconTint: Int? = null
        private var clearIcon: Drawable? = null
        @ColorInt private var clearIconTint: Int? = null
        private var searchIcon: Drawable? = null
        @ColorInt private var searchIconTint: Int? = null

        // Filter chip styling
        @ColorInt private var filterChipBackgroundColor: Int? = null
        @ColorInt private var filterChipSelectedBackgroundColor: Int? = null
        @ColorInt private var filterChipTextColor: Int? = null
        @ColorInt private var filterChipSelectedTextColor: Int? = null
        @StyleRes private var filterChipTextAppearance: Int? = null
        @ColorInt private var filterChipStrokeColor: Int? = null
        @ColorInt private var filterChipSelectedStrokeColor: Int? = null
        private var filterChipStrokeWidth: Float? = null
        private var filterChipCornerRadius: Float? = null

        // Section header styling
        @ColorInt private var sectionHeaderTextColor: Int? = null
        @StyleRes private var sectionHeaderTextAppearance: Int? = null
        @ColorInt private var sectionHeaderBackgroundColor: Int? = null

        // Conversation item styling
        @ColorInt private var conversationItemBackgroundColor: Int? = null
        @ColorInt private var conversationTitleTextColor: Int? = null
        @StyleRes private var conversationTitleTextAppearance: Int? = null
        @ColorInt private var conversationSubtitleTextColor: Int? = null
        @StyleRes private var conversationSubtitleTextAppearance: Int? = null
        @ColorInt private var conversationTimestampTextColor: Int? = null
        @StyleRes private var conversationTimestampTextAppearance: Int? = null
        @ColorInt private var conversationSeparatorColor: Int? = null

        // Message item styling
        @ColorInt private var messageItemBackgroundColor: Int? = null
        @ColorInt private var messageTitleTextColor: Int? = null
        @StyleRes private var messageTitleTextAppearance: Int? = null
        @ColorInt private var messageSubtitleTextColor: Int? = null
        @StyleRes private var messageSubtitleTextAppearance: Int? = null
        @ColorInt private var messageTimestampTextColor: Int? = null
        @StyleRes private var messageTimestampTextAppearance: Int? = null
        @ColorInt private var messageSeparatorColor: Int? = null
        @ColorInt private var messageLinkTextColor: Int? = null
        @StyleRes private var messageLinkTextAppearance: Int? = null
        private var messageThreadIcon: Drawable? = null
        @ColorInt private var messageThreadIconTint: Int? = null

        // Date separator styling
        @ColorInt private var dateSeparatorBackgroundColor: Int? = null
        @ColorInt private var dateSeparatorTextColor: Int? = null
        @StyleRes private var dateSeparatorTextAppearance: Int? = null
        private var dateSeparatorStyle: CometChatDateStyle? = null

        // Avatar styling
        private var avatarStyle: CometChatAvatarStyle? = null

        // Badge styling
        private var badgeStyle: CometChatBadgeCountStyle? = null

        // Status indicator styling
        private var statusIndicatorStyle: CometChatStatusIndicatorStyle? = null

        // Loading state styling
        @ColorInt private var loadingStateBackgroundColor: Int? = null

        // Empty state styling
        @ColorInt private var emptyStateTextColor: Int? = null
        @StyleRes private var emptyStateTextAppearance: Int? = null
        @ColorInt private var emptyStateSubtitleTextColor: Int? = null
        @StyleRes private var emptyStateSubtitleTextAppearance: Int? = null
        private var emptyStateIcon: Drawable? = null
        @ColorInt private var emptyStateIconTint: Int? = null

        // Initial state styling
        @ColorInt private var initialStateTextColor: Int? = null
        @StyleRes private var initialStateTextAppearance: Int? = null
        @ColorInt private var initialStateSubtitleTextColor: Int? = null
        @StyleRes private var initialStateSubtitleTextAppearance: Int? = null
        private var initialStateIcon: Drawable? = null
        @ColorInt private var initialStateIconTint: Int? = null

        // Error state styling
        @ColorInt private var errorStateTextColor: Int? = null
        @StyleRes private var errorStateTextAppearance: Int? = null
        @ColorInt private var errorStateSubtitleTextColor: Int? = null
        @StyleRes private var errorStateSubtitleTextAppearance: Int? = null
        private var errorStateIcon: Drawable? = null
        @ColorInt private var errorStateIconTint: Int? = null

        // See more button styling
        @ColorInt private var seeMoreTextColor: Int? = null
        @StyleRes private var seeMoreTextAppearance: Int? = null

        // Builder methods
        public fun setBackgroundColor(@ColorInt color: Int): Builder = apply { backgroundColor = color }
        public fun setSearchBarBackgroundColor(@ColorInt color: Int): Builder = apply { searchBarBackgroundColor = color }
        public fun setSearchBarStrokeColor(@ColorInt color: Int): Builder = apply { searchBarStrokeColor = color }
        public fun setSearchBarStrokeWidth(width: Float): Builder = apply { searchBarStrokeWidth = width }
        public fun setSearchBarCornerRadius(radius: Float): Builder = apply { searchBarCornerRadius = radius }
        public fun setSearchBarTextColor(@ColorInt color: Int): Builder = apply { searchBarTextColor = color }
        public fun setSearchBarTextAppearance(@StyleRes appearance: Int): Builder = apply { searchBarTextAppearance = appearance }
        public fun setSearchBarHintTextColor(@ColorInt color: Int): Builder = apply { searchBarHintTextColor = color }
        public fun setSearchBarHintTextAppearance(@StyleRes appearance: Int): Builder = apply { searchBarHintTextAppearance = appearance }
        public fun setBackIcon(icon: Drawable?): Builder = apply { backIcon = icon }
        public fun setBackIconTint(@ColorInt color: Int): Builder = apply { backIconTint = color }
        public fun setClearIcon(icon: Drawable?): Builder = apply { clearIcon = icon }
        public fun setClearIconTint(@ColorInt color: Int): Builder = apply { clearIconTint = color }
        public fun setSearchIcon(icon: Drawable?): Builder = apply { searchIcon = icon }
        public fun setSearchIconTint(@ColorInt color: Int): Builder = apply { searchIconTint = color }
        public fun setFilterChipBackgroundColor(@ColorInt color: Int): Builder = apply { filterChipBackgroundColor = color }
        public fun setFilterChipSelectedBackgroundColor(@ColorInt color: Int): Builder = apply { filterChipSelectedBackgroundColor = color }
        public fun setFilterChipTextColor(@ColorInt color: Int): Builder = apply { filterChipTextColor = color }
        public fun setFilterChipSelectedTextColor(@ColorInt color: Int): Builder = apply { filterChipSelectedTextColor = color }
        public fun setFilterChipTextAppearance(@StyleRes appearance: Int): Builder = apply { filterChipTextAppearance = appearance }
        public fun setFilterChipStrokeColor(@ColorInt color: Int): Builder = apply { filterChipStrokeColor = color }
        public fun setFilterChipSelectedStrokeColor(@ColorInt color: Int): Builder = apply { filterChipSelectedStrokeColor = color }
        public fun setFilterChipStrokeWidth(width: Float): Builder = apply { filterChipStrokeWidth = width }
        public fun setFilterChipCornerRadius(radius: Float): Builder = apply { filterChipCornerRadius = radius }
        public fun setSectionHeaderTextColor(@ColorInt color: Int): Builder = apply { sectionHeaderTextColor = color }
        public fun setSectionHeaderTextAppearance(@StyleRes appearance: Int): Builder = apply { sectionHeaderTextAppearance = appearance }
        public fun setSectionHeaderBackgroundColor(@ColorInt color: Int): Builder = apply { sectionHeaderBackgroundColor = color }
        public fun setConversationItemBackgroundColor(@ColorInt color: Int): Builder = apply { conversationItemBackgroundColor = color }
        public fun setConversationTitleTextColor(@ColorInt color: Int): Builder = apply { conversationTitleTextColor = color }
        public fun setConversationTitleTextAppearance(@StyleRes appearance: Int): Builder = apply { conversationTitleTextAppearance = appearance }
        public fun setConversationSubtitleTextColor(@ColorInt color: Int): Builder = apply { conversationSubtitleTextColor = color }
        public fun setConversationSubtitleTextAppearance(@StyleRes appearance: Int): Builder = apply { conversationSubtitleTextAppearance = appearance }
        public fun setConversationTimestampTextColor(@ColorInt color: Int): Builder = apply { conversationTimestampTextColor = color }
        public fun setConversationTimestampTextAppearance(@StyleRes appearance: Int): Builder = apply { conversationTimestampTextAppearance = appearance }
        public fun setConversationSeparatorColor(@ColorInt color: Int): Builder = apply { conversationSeparatorColor = color }
        public fun setMessageItemBackgroundColor(@ColorInt color: Int): Builder = apply { messageItemBackgroundColor = color }
        public fun setMessageTitleTextColor(@ColorInt color: Int): Builder = apply { messageTitleTextColor = color }
        public fun setMessageTitleTextAppearance(@StyleRes appearance: Int): Builder = apply { messageTitleTextAppearance = appearance }
        public fun setMessageSubtitleTextColor(@ColorInt color: Int): Builder = apply { messageSubtitleTextColor = color }
        public fun setMessageSubtitleTextAppearance(@StyleRes appearance: Int): Builder = apply { messageSubtitleTextAppearance = appearance }
        public fun setMessageTimestampTextColor(@ColorInt color: Int): Builder = apply { messageTimestampTextColor = color }
        public fun setMessageTimestampTextAppearance(@StyleRes appearance: Int): Builder = apply { messageTimestampTextAppearance = appearance }
        public fun setMessageSeparatorColor(@ColorInt color: Int): Builder = apply { messageSeparatorColor = color }
        public fun setMessageLinkTextColor(@ColorInt color: Int): Builder = apply { messageLinkTextColor = color }
        public fun setMessageLinkTextAppearance(@StyleRes appearance: Int): Builder = apply { messageLinkTextAppearance = appearance }
        public fun setMessageThreadIcon(icon: Drawable?): Builder = apply { messageThreadIcon = icon }
        public fun setMessageThreadIconTint(@ColorInt color: Int): Builder = apply { messageThreadIconTint = color }
        public fun setDateSeparatorBackgroundColor(@ColorInt color: Int): Builder = apply { dateSeparatorBackgroundColor = color }
        public fun setDateSeparatorTextColor(@ColorInt color: Int): Builder = apply { dateSeparatorTextColor = color }
        public fun setDateSeparatorTextAppearance(@StyleRes appearance: Int): Builder = apply { dateSeparatorTextAppearance = appearance }
        public fun setDateSeparatorStyle(style: CometChatDateStyle?): Builder = apply { dateSeparatorStyle = style }
        public fun setAvatarStyle(style: CometChatAvatarStyle?): Builder = apply { avatarStyle = style }
        public fun setBadgeStyle(style: CometChatBadgeCountStyle?): Builder = apply { badgeStyle = style }
        public fun setStatusIndicatorStyle(style: CometChatStatusIndicatorStyle?): Builder = apply { statusIndicatorStyle = style }
        public fun setLoadingStateBackgroundColor(@ColorInt color: Int): Builder = apply { loadingStateBackgroundColor = color }
        public fun setEmptyStateTextColor(@ColorInt color: Int): Builder = apply { emptyStateTextColor = color }
        public fun setEmptyStateTextAppearance(@StyleRes appearance: Int): Builder = apply { emptyStateTextAppearance = appearance }
        public fun setEmptyStateSubtitleTextColor(@ColorInt color: Int): Builder = apply { emptyStateSubtitleTextColor = color }
        public fun setEmptyStateSubtitleTextAppearance(@StyleRes appearance: Int): Builder = apply { emptyStateSubtitleTextAppearance = appearance }
        public fun setEmptyStateIcon(icon: Drawable?): Builder = apply { emptyStateIcon = icon }
        public fun setEmptyStateIconTint(@ColorInt color: Int): Builder = apply { emptyStateIconTint = color }
        public fun setInitialStateTextColor(@ColorInt color: Int): Builder = apply { initialStateTextColor = color }
        public fun setInitialStateTextAppearance(@StyleRes appearance: Int): Builder = apply { initialStateTextAppearance = appearance }
        public fun setInitialStateSubtitleTextColor(@ColorInt color: Int): Builder = apply { initialStateSubtitleTextColor = color }
        public fun setInitialStateSubtitleTextAppearance(@StyleRes appearance: Int): Builder = apply { initialStateSubtitleTextAppearance = appearance }
        public fun setInitialStateIcon(icon: Drawable?): Builder = apply { initialStateIcon = icon }
        public fun setInitialStateIconTint(@ColorInt color: Int): Builder = apply { initialStateIconTint = color }
        public fun setErrorStateTextColor(@ColorInt color: Int): Builder = apply { errorStateTextColor = color }
        public fun setErrorStateTextAppearance(@StyleRes appearance: Int): Builder = apply { errorStateTextAppearance = appearance }
        public fun setErrorStateSubtitleTextColor(@ColorInt color: Int): Builder = apply { errorStateSubtitleTextColor = color }
        public fun setErrorStateSubtitleTextAppearance(@StyleRes appearance: Int): Builder = apply { errorStateSubtitleTextAppearance = appearance }
        public fun setErrorStateIcon(icon: Drawable?): Builder = apply { errorStateIcon = icon }
        public fun setErrorStateIconTint(@ColorInt color: Int): Builder = apply { errorStateIconTint = color }
        public fun setSeeMoreTextColor(@ColorInt color: Int): Builder = apply { seeMoreTextColor = color }
        public fun setSeeMoreTextAppearance(@StyleRes appearance: Int): Builder = apply { seeMoreTextAppearance = appearance }

        /**
         * Builds the CometChatSearchStyle instance.
         * Sets default text colors from CometChatTheme when not explicitly provided,
         * matching the Java reference implementation behavior.
         */
        public fun build(): CometChatSearchStyle {
            // Set default text colors from CometChatTheme if not explicitly set
            // This matches the Java reference implementation behavior
            val defaultConversationTitleTextColor = conversationTitleTextColor 
                ?: CometChatTheme.getTextColorPrimary(context)
            val defaultConversationSubtitleTextColor = conversationSubtitleTextColor 
                ?: CometChatTheme.getTextColorSecondary(context)
            val defaultConversationTimestampTextColor = conversationTimestampTextColor 
                ?: CometChatTheme.getTextColorSecondary(context)
            val defaultMessageTitleTextColor = messageTitleTextColor 
                ?: CometChatTheme.getTextColorPrimary(context)
            val defaultMessageSubtitleTextColor = messageSubtitleTextColor 
                ?: CometChatTheme.getTextColorSecondary(context)
            val defaultMessageTimestampTextColor = messageTimestampTextColor 
                ?: CometChatTheme.getTextColorSecondary(context)
            val defaultMessageLinkTextColor = messageLinkTextColor 
                ?: CometChatTheme.getInfoColor(context)
            val defaultSectionHeaderTextColor = sectionHeaderTextColor 
                ?: CometChatTheme.getTextColorSecondary(context)

            return CometChatSearchStyle(
                backgroundColor = backgroundColor,
                searchBarBackgroundColor = searchBarBackgroundColor,
                searchBarStrokeColor = searchBarStrokeColor,
                searchBarStrokeWidth = searchBarStrokeWidth,
                searchBarCornerRadius = searchBarCornerRadius,
                searchBarTextColor = searchBarTextColor,
                searchBarTextAppearance = searchBarTextAppearance,
                searchBarHintTextColor = searchBarHintTextColor,
                searchBarHintTextAppearance = searchBarHintTextAppearance,
                backIcon = backIcon,
                backIconTint = backIconTint,
                clearIcon = clearIcon,
                clearIconTint = clearIconTint,
                searchIcon = searchIcon,
                searchIconTint = searchIconTint,
                filterChipBackgroundColor = filterChipBackgroundColor,
                filterChipSelectedBackgroundColor = filterChipSelectedBackgroundColor,
                filterChipTextColor = filterChipTextColor,
                filterChipSelectedTextColor = filterChipSelectedTextColor,
                filterChipTextAppearance = filterChipTextAppearance,
                filterChipStrokeColor = filterChipStrokeColor,
                filterChipSelectedStrokeColor = filterChipSelectedStrokeColor,
                filterChipStrokeWidth = filterChipStrokeWidth,
                filterChipCornerRadius = filterChipCornerRadius,
                sectionHeaderTextColor = defaultSectionHeaderTextColor,
                sectionHeaderTextAppearance = sectionHeaderTextAppearance,
                sectionHeaderBackgroundColor = sectionHeaderBackgroundColor,
                conversationItemBackgroundColor = conversationItemBackgroundColor,
                conversationTitleTextColor = defaultConversationTitleTextColor,
                conversationTitleTextAppearance = conversationTitleTextAppearance,
                conversationSubtitleTextColor = defaultConversationSubtitleTextColor,
                conversationSubtitleTextAppearance = conversationSubtitleTextAppearance,
                conversationTimestampTextColor = defaultConversationTimestampTextColor,
                conversationTimestampTextAppearance = conversationTimestampTextAppearance,
                conversationSeparatorColor = conversationSeparatorColor,
                messageItemBackgroundColor = messageItemBackgroundColor,
                messageTitleTextColor = defaultMessageTitleTextColor,
                messageTitleTextAppearance = messageTitleTextAppearance,
                messageSubtitleTextColor = defaultMessageSubtitleTextColor,
                messageSubtitleTextAppearance = messageSubtitleTextAppearance,
                messageTimestampTextColor = defaultMessageTimestampTextColor,
                messageTimestampTextAppearance = messageTimestampTextAppearance,
                messageSeparatorColor = messageSeparatorColor,
                messageLinkTextColor = defaultMessageLinkTextColor,
                messageLinkTextAppearance = messageLinkTextAppearance,
                messageThreadIcon = messageThreadIcon,
                messageThreadIconTint = messageThreadIconTint,
                dateSeparatorBackgroundColor = dateSeparatorBackgroundColor,
                dateSeparatorTextColor = dateSeparatorTextColor,
                dateSeparatorTextAppearance = dateSeparatorTextAppearance,
                dateSeparatorStyle = dateSeparatorStyle,
                avatarStyle = avatarStyle,
                badgeStyle = badgeStyle,
                statusIndicatorStyle = statusIndicatorStyle,
                loadingStateBackgroundColor = loadingStateBackgroundColor,
                emptyStateTextColor = emptyStateTextColor,
                emptyStateTextAppearance = emptyStateTextAppearance,
                emptyStateSubtitleTextColor = emptyStateSubtitleTextColor,
                emptyStateSubtitleTextAppearance = emptyStateSubtitleTextAppearance,
                emptyStateIcon = emptyStateIcon,
                emptyStateIconTint = emptyStateIconTint,
                initialStateTextColor = initialStateTextColor,
                initialStateTextAppearance = initialStateTextAppearance,
                initialStateSubtitleTextColor = initialStateSubtitleTextColor,
                initialStateSubtitleTextAppearance = initialStateSubtitleTextAppearance,
                initialStateIcon = initialStateIcon,
                initialStateIconTint = initialStateIconTint,
                errorStateTextColor = errorStateTextColor,
                errorStateTextAppearance = errorStateTextAppearance,
                errorStateSubtitleTextColor = errorStateSubtitleTextColor,
                errorStateSubtitleTextAppearance = errorStateSubtitleTextAppearance,
                errorStateIcon = errorStateIcon,
                errorStateIconTint = errorStateIconTint,
                seeMoreTextColor = seeMoreTextColor,
                seeMoreTextAppearance = seeMoreTextAppearance
            )
        }
    }

    public companion object {
        /**
         * Creates a default CometChatSearchStyle with theme-based values.
         *
         * @param context The Android context
         * @return A new CometChatSearchStyle instance with default values
         */
        public fun default(context: Context): CometChatSearchStyle {
            return Builder(context).build()
        }
    }
}
