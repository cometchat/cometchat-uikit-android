package com.cometchat.uikit.compose.theme

import androidx.compose.ui.graphics.toArgb
import com.cometchat.uikit.core.utils.CometChatIncomingCallTheme

/**
 * This scheme as colours for the Push SDK's incoming-call screen, by the mapping the View UIKit
 * uses too ([CometChatIncomingCallTheme.Colors.fromThemeColors]).
 */
public fun CometChatColorScheme.toIncomingCallColors(): CometChatIncomingCallTheme.Colors =
    CometChatIncomingCallTheme.Colors.fromThemeColors(
        backgroundColor1 = backgroundColor1.toArgb(),
        textColorPrimary = textColorPrimary.toArgb(),
        textColorSecondary = textColorSecondary.toArgb(),
        primaryColor = primary.toArgb(),
        successColor = successColor.toArgb(),
        errorColor = errorColor.toArgb(),
        colorWhite = colorWhite.toArgb()
    )

/**
 * The colour schemes the app gives [CometChatTheme], as colours for the Push SDK's incoming-call
 * screen, in light and dark.
 *
 * The Push SDK draws that screen itself, often on a process with no UI running, so it cannot read
 * the Compose theme. The app copies these into a `CometChatPNIncomingCallTheme` (the field names match) and
 * passes it to the Push SDK at `init`, and again whenever its schemes change. Not composable, so
 * it can run in `Application.onCreate`.
 *
 * @param light the scheme used in light mode; the UIKit's default unless the app customises it
 * @param dark the scheme used in dark mode; the UIKit's default unless the app customises it
 */
public fun incomingCallTheme(
    light: CometChatColorScheme = lightColorScheme(),
    dark: CometChatColorScheme = darkColorScheme()
): CometChatIncomingCallTheme = CometChatIncomingCallTheme(light.toIncomingCallColors(), dark.toIncomingCallColors())
