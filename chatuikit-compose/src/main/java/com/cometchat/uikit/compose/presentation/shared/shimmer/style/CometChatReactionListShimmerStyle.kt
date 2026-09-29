package com.cometchat.uikit.compose.presentation.shared.shimmer.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cometchat.uikit.compose.presentation.shared.shimmer.config.CometChatShimmerConfig

/**
 * Style configuration for reaction list shimmer placeholder.
 *
 * This style class defines the appearance of shimmer placeholders used in reaction lists.
 * It matches the layout structure from chatuikit-kotlin's shimmer_cometchat_reaction_list_items.xml.
 *
 * Reference dimensions from shimmer_cometchat_reaction_list_items.xml:
 * - Avatar: 40dp circle (weight 1)
 * - Name: 20dp height (weight 4, flexible width)
 * - Action icon: 24dp circle (weight 0.8)
 * - Padding: 4dp horizontal (cometchat_padding_4), 2dp vertical (cometchat_padding_2)
 *
 * @property avatarSize Size of the avatar placeholder. Defaults to 40.dp
 * @property nameHeight Height of the name placeholder. Defaults to 20.dp
 * @property actionIconSize Size of the action icon placeholder. Defaults to 24.dp
 * @property placeholderCornerRadius Corner radius for rectangular placeholders. Defaults to 4.dp
 * @property horizontalPadding Horizontal padding for the list item. Defaults to 16.dp
 * @property verticalPadding Vertical padding for the list item. Defaults to 8.dp
 * @property contentSpacing Spacing between elements. Defaults to 12.dp
 * @property shimmerConfig Shimmer animation configuration
 */
@Immutable
public data class CometChatReactionListShimmerStyle(
    val avatarSize: Dp,
    val nameHeight: Dp,
    val actionIconSize: Dp,
    val placeholderCornerRadius: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val contentSpacing: Dp,
    val shimmerConfig: CometChatShimmerConfig
) {
    public companion object {
        /**
         * Default avatar size matching chatuikit-kotlin shimmer_cometchat_reaction_list_items.xml.
         */
        public val DEFAULT_AVATAR_SIZE: androidx.compose.ui.unit.Dp = 40.dp

        /**
         * Default name height matching chatuikit-kotlin (20dp).
         */
        public val DEFAULT_NAME_HEIGHT: androidx.compose.ui.unit.Dp = 20.dp

        /**
         * Default action icon size matching chatuikit-kotlin (24dp).
         */
        public val DEFAULT_ACTION_ICON_SIZE: androidx.compose.ui.unit.Dp = 24.dp

        /**
         * Default corner radius for rectangular placeholders.
         */
        public val DEFAULT_PLACEHOLDER_CORNER_RADIUS: androidx.compose.ui.unit.Dp = 4.dp

        /**
         * Default horizontal padding.
         */
        public val DEFAULT_HORIZONTAL_PADDING: androidx.compose.ui.unit.Dp = 16.dp

        /**
         * Default vertical padding.
         */
        public val DEFAULT_VERTICAL_PADDING: androidx.compose.ui.unit.Dp = 8.dp

        /**
         * Default spacing between elements.
         */
        public val DEFAULT_CONTENT_SPACING: androidx.compose.ui.unit.Dp = 12.dp

        /**
         * Creates a default CometChatReactionListShimmerStyle with values matching
         * chatuikit-kotlin shimmer_cometchat_reaction_list_items.xml layout.
         *
         * @param avatarSize Size of the avatar placeholder. Defaults to 40.dp
         * @param nameHeight Height of the name placeholder. Defaults to 20.dp
         * @param actionIconSize Size of the action icon placeholder. Defaults to 24.dp
         * @param placeholderCornerRadius Corner radius for rectangular placeholders. Defaults to 4.dp
         * @param horizontalPadding Horizontal padding. Defaults to 16.dp
         * @param verticalPadding Vertical padding. Defaults to 8.dp
         * @param contentSpacing Spacing between elements. Defaults to 12.dp
         * @param shimmerConfig Shimmer animation configuration
         * @return A configured CometChatReactionListShimmerStyle instance
         */
        @Composable
        public fun default(
            avatarSize: Dp = DEFAULT_AVATAR_SIZE,
            nameHeight: Dp = DEFAULT_NAME_HEIGHT,
            actionIconSize: Dp = DEFAULT_ACTION_ICON_SIZE,
            placeholderCornerRadius: Dp = DEFAULT_PLACEHOLDER_CORNER_RADIUS,
            horizontalPadding: Dp = DEFAULT_HORIZONTAL_PADDING,
            verticalPadding: Dp = DEFAULT_VERTICAL_PADDING,
            contentSpacing: Dp = DEFAULT_CONTENT_SPACING,
            shimmerConfig: CometChatShimmerConfig = CometChatShimmerConfig.default()
        ): CometChatReactionListShimmerStyle = CometChatReactionListShimmerStyle(
            avatarSize = avatarSize,
            nameHeight = nameHeight,
            actionIconSize = actionIconSize,
            placeholderCornerRadius = placeholderCornerRadius,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            contentSpacing = contentSpacing,
            shimmerConfig = shimmerConfig
        )
    }
}
