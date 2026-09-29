package com.cometchat.uikit.compose.presentation.messagecomposer.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.presentation.messagecomposer.style.CometChatMessageComposerStyle

/**
 * Row 2 button layout for multiline mode of the message composer.
 *
 * Layout (left to right):
 * - Attachment (⊕)
 * - Voice Recording (🎤)
 * - Sticker (😊)
 * - Aa formatting toggle
 * - [Spacer]
 * - Send button (➤) — right aligned
 *
 * @param modifier Modifier for the row
 * @param style Style configuration for button icons and tints
 * @param hideAttachmentButton Whether to hide the attachment button
 * @param hideVoiceRecordingButton Whether to hide the voice recording button
 * @param hideStickersButton Whether to hide the stickers button
 * @param hideSendButton Whether to hide the send button
 * @param showFormattingToggle Whether to show the Aa formatting toggle button
 * @param isStickerKeyboardOpen Whether the sticker keyboard is currently open
 * @param isSendButtonActive Whether the send button is in active state
 * @param isAIGenerating Whether AI is currently generating content
 * @param isAgentChat Whether the current chat is with an AI agent
 * @param isAttachmentPopupExpanded Whether the attachment popup is currently expanded
 * @param onAttachmentClick Callback when attachment button is clicked
 * @param onVoiceRecordClick Callback when voice recording button is clicked
 * @param onStickerClick Callback when sticker button is clicked
 * @param onFormattingToggleClick Callback when Aa formatting toggle is clicked
 * @param onSendClick Callback when send button is clicked
 * @param sendButtonView Optional custom send button composable
 * @param attachmentButtonContent Optional composable content wrapping the attachment button (for popup anchoring)
 */
@Composable
public fun MultilineButtonRow(
    modifier: Modifier = Modifier,
    style: CometChatMessageComposerStyle = CometChatMessageComposerStyle.default(),
    hideAttachmentButton: Boolean = false,
    hideVoiceRecordingButton: Boolean = false,
    hideStickersButton: Boolean = false,
    hideSendButton: Boolean = false,
    showFormattingToggle: Boolean = false,
    isStickerKeyboardOpen: Boolean = false,
    isSendButtonActive: Boolean = false,
    isAIGenerating: Boolean = false,
    isAgentChat: Boolean = false,
    isAttachmentPopupExpanded: Boolean = false,
    onAttachmentClick: () -> Unit = {},
    onVoiceRecordClick: () -> Unit = {},
    onStickerClick: () -> Unit = {},
    onFormattingToggleClick: () -> Unit = {},
    onSendClick: () -> Unit = {},
    sendButtonView: (@Composable (onClick: () -> Unit, isActive: Boolean, isAIGenerating: Boolean) -> Unit)? = null,
    attachmentButtonContent: (@Composable () -> Unit)? = null
) {
    // Match chatuikit-kotlin's Row 2: a 56dp row with 8dp side padding holding flush
    // 40dp buttons. Material3's IconButton would otherwise inflate each button to the
    // 48dp minimum touch target, spreading the icons apart and insetting the send button.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        val cdHoist4 = stringResource(R.string.cometchat_a11y_composer_action_buttons)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp)
                .semantics { contentDescription = cdHoist4 },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Attachment button (⊕)
            if (!hideAttachmentButton) {
                if (attachmentButtonContent != null) {
                    attachmentButtonContent()
                } else {
                    AnimatedAttachmentButton(
                        isExpanded = isAttachmentPopupExpanded,
                        style = style,
                        onClick = onAttachmentClick
                    )
                }
            }

            // 2. Voice Recording button (🎤)
            if (!hideVoiceRecordingButton) {
                val cdHoist3 = stringResource(R.string.cometchat_a11y_voice_recording)
                IconButton(
                    onClick = onVoiceRecordClick,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = cdHoist3 }
                ) {
                    style.voiceRecordingIcon?.let { icon ->
                        Icon(
                            painter = icon,
                            contentDescription = stringResource(R.string.cometchat_a11y_record_voice_message),
                            tint = style.voiceRecordingIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 3. Sticker button (😊)
            if (!hideStickersButton) {
                val cdHoist2 = stringResource(R.string.cometchat_a11y_stickers)
                val visualState = resolveStickerVisualState(isStickerKeyboardOpen)
                val stickerIcon = if (visualState == StickerButtonVisualState.ACTIVE) style.stickerActiveIcon else style.stickerIcon
                val stickerTint = if (visualState == StickerButtonVisualState.ACTIVE) style.stickerActiveIconTint else style.stickerIconTint
                IconButton(
                    onClick = onStickerClick,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = cdHoist2 }
                ) {
                    stickerIcon?.let { icon ->
                        Icon(
                            painter = icon,
                            contentDescription = if (isStickerKeyboardOpen) stringResource(R.string.cometchat_a11y_close_stickers) else stringResource(R.string.cometchat_a11y_open_stickers),
                            tint = stickerTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 4. Aa formatting toggle button
            if (showFormattingToggle) {
                val cdHoist1 = stringResource(R.string.cometchat_a11y_format_text)
                IconButton(
                    onClick = onFormattingToggleClick,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = cdHoist1 }
                ) {
                    style.richTextToggleIcon?.let { icon ->
                        Icon(
                            painter = icon,
                            contentDescription = stringResource(R.string.cometchat_a11y_show_formatting_options),
                            tint = style.richTextToggleIconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Spacer pushes send button to the right
            Spacer(modifier = Modifier.weight(1f))

            // 5. Send button (➤) — right aligned
            if (!hideSendButton) {
                if (sendButtonView != null) {
                    sendButtonView(onSendClick, isSendButtonActive, isAIGenerating)
                } else {
                    DefaultSendButton(
                        isActive = isSendButtonActive,
                        isAIGenerating = isAIGenerating,
                        isAgentChat = isAgentChat,
                        style = style,
                        onClick = onSendClick
                    )
                }
            }
        }
    }
}
