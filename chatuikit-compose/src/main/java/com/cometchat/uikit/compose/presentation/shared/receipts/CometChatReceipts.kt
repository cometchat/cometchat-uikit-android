package com.cometchat.uikit.compose.presentation.shared.receipts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.compose.R

/**
 * CometChatReceipts displays the message receipt status with appropriate icons and colors.
 *
 * This is a standalone reusable component that can be used across the UIKit.
 *
 * The component displays different icons and tints based on the receipt status:
 * - IN_PROGRESS: Shows wait icon with waitIconTint
 * - SENT: Shows sent icon with sentIconTint
 * - DELIVERED: Shows delivered icon with deliveredIconTint
 * - READ: Shows read icon with readIconTint
 * - ERROR: Shows error icon with errorIconTint
 *
 * @param receipt The Receipt enum value representing the current receipt status
 * @param modifier Modifier for the receipt indicator
 * @param style Style configuration for the receipt indicator
 */
@Composable
public fun CometChatReceipts(
    receipt: Receipt,
    modifier: Modifier = Modifier,
    style: CometChatReceiptsStyle = CometChatReceiptsStyle.default()
) {
    // Get the appropriate icon and tint based on receipt status
    val (icon, tint) = when (receipt) {
        Receipt.IN_PROGRESS -> style.waitIcon to style.waitIconTint
        Receipt.SENT -> style.sentIcon to style.sentIconTint
        Receipt.DELIVERED -> style.deliveredIcon to style.deliveredIconTint
        Receipt.READ -> style.readIcon to style.readIconTint
        Receipt.ERROR -> style.errorIcon to style.errorIconTint
    }

    // Don't render if no icon is available
    if (icon == null) {
        return
    }

    Image(
        painter = icon,
        contentDescription = when (receipt) {
            Receipt.IN_PROGRESS -> stringResource(R.string.cometchat_a11y_message_in_progress)
            Receipt.SENT -> stringResource(R.string.cometchat_a11y_message_sent)
            Receipt.DELIVERED -> stringResource(R.string.cometchat_a11y_message_delivered)
            Receipt.READ -> stringResource(R.string.cometchat_a11y_message_read)
            Receipt.ERROR -> stringResource(R.string.cometchat_a11y_message_error)
        },
        modifier = modifier.size(style.size),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint)
    )
}

/**
 * CometChatReceipts overload that accepts a BaseMessage and extracts the receipt status.
 * This is a convenience function that determines the receipt status based on message properties.
 *
 * Receipt status is determined using [MessageReceiptUtils.getMessageReceipt] which handles:
 * - Metadata error checks
 * - Moderation status (DISAPPROVED → ERROR, PENDING → SENT)
 * - Standard receipt status (READ, DELIVERED, SENT, IN_PROGRESS)
 *
 * @param message The BaseMessage to extract receipt status from
 * @param modifier Modifier for the receipt indicator
 * @param style Style configuration for the receipt indicator
 *
 * @sample
 * ```
 * CometChatReceipts(
 *     message = baseMessage,
 *     style = CometChatReceiptsStyle.default()
 * )
 * ```
 */
@Composable
public fun CometChatReceipts(
    message: BaseMessage,
    modifier: Modifier = Modifier,
    style: CometChatReceiptsStyle = CometChatReceiptsStyle.default()
) {
    val receipt = MessageReceiptUtils.getMessageReceipt(message)
    CometChatReceipts(
        receipt = receipt,
        modifier = modifier,
        style = style
    )
}
