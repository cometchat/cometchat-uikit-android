package com.cometchat.sampleapp.kotlin.push.calls

import com.cometchat.pushnotification.CometChatPNIncomingCallTheme
import com.cometchat.uikit.core.utils.CometChatIncomingCallTheme

/**
 * The UIKit's incoming call theme as the Push SDK's incoming call theme.
 *
 * The Push SDK draws the incoming-call screen itself, often on a process a push has just started,
 * so it cannot read the UIKit's theme there. The UIKit has no Push SDK dependency, so the app,
 * which has both, copies the colours across; the field names match one to one.
 */
fun CometChatIncomingCallTheme.toCometChatPNIncomingCallTheme(): CometChatPNIncomingCallTheme = CometChatPNIncomingCallTheme.Builder()
    .setLightColors(light.toPNColors())
    .setDarkColors(dark.toPNColors())
    .build()

private fun CometChatIncomingCallTheme.Colors.toPNColors(): CometChatPNIncomingCallTheme.Colors =
    CometChatPNIncomingCallTheme.Colors.Builder()
        .setBackground(background)
        .setCallerName(callerName)
        .setSubtitle(subtitle)
        .setAvatarBackground(avatarBackground)
        .setAvatarText(avatarText)
        .setAcceptButton(acceptButton)
        .setAcceptIcon(acceptIcon)
        .setAcceptLabel(acceptLabel)
        .setDeclineButton(declineButton)
        .setDeclineIcon(declineIcon)
        .setDeclineLabel(declineLabel)
        .build()
