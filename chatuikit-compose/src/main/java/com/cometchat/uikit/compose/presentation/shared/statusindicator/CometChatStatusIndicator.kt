package com.cometchat.uikit.compose.presentation.shared.statusindicator

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cometchat.uikit.compose.R

/**
 * CometChatStatusIndicator displays the online/offline status for users
 * or group type icons (public, private, protected) for groups.
 *
 * This is a standalone reusable component that can be used across the UIKit.
 *
 * The indicator is visible for:
 * - ONLINE: Shows the online icon
 * - PRIVATE_GROUP: Shows the private group icon
 * - PROTECTED_GROUP: Shows the protected group icon
 *
 * The indicator is hidden (not rendered) for:
 * - OFFLINE: User is offline
 * - PUBLIC_GROUP: Public groups don't show an indicator
 *
 * @param status The StatusIndicator enum value representing the current status
 * @param modifier Modifier for the status indicator container
 * @param style Style configuration for the status indicator
 */
@Composable
public fun CometChatStatusIndicator(
    status: StatusIndicator,
    modifier: Modifier = Modifier,
    style: CometChatStatusIndicatorStyle = CometChatStatusIndicatorStyle.default()
) {
    // Don't render for OFFLINE or PUBLIC_GROUP
    if (status == StatusIndicator.OFFLINE || status == StatusIndicator.PUBLIC_GROUP) {
        return
    }

    // Get the appropriate icon based on status
    val icon = when (status) {
        StatusIndicator.ONLINE -> style.onlineIcon
        StatusIndicator.PRIVATE_GROUP -> style.privateGroupIcon
        StatusIndicator.PROTECTED_GROUP -> style.protectedGroupIcon
        else -> null
    }

    // Don't render if no icon is available
    if (icon == null) {
        return
    }

    val shape = RoundedCornerShape(style.cornerRadius)

    Card(
        modifier = modifier
            .size(style.size)
            .border(
                width = style.strokeWidth,
                color = style.strokeColor,
                shape = shape
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = style.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = icon,
                contentDescription = when (status) {
                    StatusIndicator.ONLINE -> stringResource(R.string.cometchat_online)
                    StatusIndicator.PRIVATE_GROUP -> stringResource(R.string.cometchat_a11y_private_group)
                    StatusIndicator.PROTECTED_GROUP -> stringResource(R.string.cometchat_a11y_protected_group)
                    else -> ""
                },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
