package com.cometchat.uikit.core.utils

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes

/**
 * The UIKit's colours for the Push SDK's incoming-call screen, in light and dark.
 *
 * In ringing v2 the Push SDK draws the incoming-call screen itself, on a process a push may have
 * started with none of the app's UI running, so it cannot read the UIKit's theme there. The app
 * hands it a `CometChatPNIncomingCallTheme` instead, at `init` and whenever its theme changes. This
 * class is the UIKit's half of that: it turns the UIKit theme into the screen's eleven colours,
 * with no dependency on the Push SDK. [Colors] uses the same field names as
 * `CometChatPNIncomingCallTheme.Colors`, so the app copies them across one to one:
 *
 * ```kotlin
 * fun CometChatIncomingCallTheme.Colors.toPNColors() = CometChatPNIncomingCallTheme.Colors.Builder()
 *     .setBackground(background).setCallerName(callerName).setSubtitle(subtitle)
 *     .setAvatarBackground(avatarBackground).setAvatarText(avatarText)
 *     .setAcceptButton(acceptButton).setAcceptIcon(acceptIcon).setAcceptLabel(acceptLabel)
 *     .setDeclineButton(declineButton).setDeclineIcon(declineIcon).setDeclineLabel(declineLabel)
 *     .build()
 *
 * val uikitTheme = CometChatIncomingCallTheme.fromTheme(context) { ctx -> /* read the kit's theme in ctx */ }
 * val pnTheme = CometChatPNIncomingCallTheme.Builder()
 *     .setLightColors(uikitTheme.light.toPNColors())
 *     .setDarkColors(uikitTheme.dark.toPNColors())
 *     .build()
 * ```
 *
 * Both modes are worked out up front, so the screen is right the first time it rings in either
 * mode, a killed app at night included.
 *
 * @property light used when the device is in light mode
 * @property dark used when the device is in dark mode
 */
public data class CometChatIncomingCallTheme(
    public val light: Colors,
    public val dark: Colors
) {

    /**
     * One mode's incoming-call screen colours. Each maps to the `CometChatPNIncomingCallTheme.Colors`
     * field of the same name.
     */
    public data class Colors(
        /** The screen, status bar and navigation bar. */
        @ColorInt public val background: Int,
        /** The caller's name. */
        @ColorInt public val callerName: Int,
        /** The "Incoming audio call" line. */
        @ColorInt public val subtitle: Int,
        /** The circle behind the caller's initials. */
        @ColorInt public val avatarBackground: Int,
        /** The caller's initials. */
        @ColorInt public val avatarText: Int,
        /** The Answer button. */
        @ColorInt public val acceptButton: Int,
        /** The icon on the Answer button. */
        @ColorInt public val acceptIcon: Int,
        /** The "Answer" label under the button. */
        @ColorInt public val acceptLabel: Int,
        /** The Decline button. */
        @ColorInt public val declineButton: Int,
        /** The icon on the Decline button. */
        @ColorInt public val declineIcon: Int,
        /** The "Decline" label under the button. */
        @ColorInt public val declineLabel: Int
    ) {
        public companion object {

            /**
             * The UIKit's mapping from its theme colours to the incoming-call screen, shared by both
             * kits so the View and Compose UIKits draw the same screen from the same theme.
             *
             * The page is full screen, so it takes `backgroundColor1` rather than the old
             * incoming-call card's `backgroundColor3`. The labels sit under the buttons, not on
             * them, so they take the secondary text colour rather than white.
             */
            @JvmStatic
            public fun fromThemeColors(
                @ColorInt backgroundColor1: Int,
                @ColorInt textColorPrimary: Int,
                @ColorInt textColorSecondary: Int,
                @ColorInt primaryColor: Int,
                @ColorInt successColor: Int,
                @ColorInt errorColor: Int,
                @ColorInt colorWhite: Int
            ): Colors = Colors(
                background = backgroundColor1,
                callerName = textColorPrimary,
                subtitle = textColorSecondary,
                avatarBackground = primaryColor,
                avatarText = colorWhite,
                acceptButton = successColor,
                acceptIcon = colorWhite,
                acceptLabel = textColorSecondary,
                declineButton = errorColor,
                declineIcon = colorWhite,
                declineLabel = textColorSecondary
            )
        }
    }

    /** Reads one mode's [Colors] from a context already set to that mode. */
    public fun interface ThemeReader {
        public fun read(context: Context): Colors
    }

    public companion object {

        /**
         * Reads the UIKit theme twice, once in light mode and once in dark, whatever mode the
         * device is in now. [reader] is given a context with [themeRes] applied and the night
         * qualifier set, so a DayNight theme resolves to that mode's colours.
         *
         * @param themeRes the theme that carries the UIKit's colours; the app's manifest theme
         *   unless the app applies its UIKit theme per activity
         */
        @JvmStatic
        @JvmOverloads
        public fun fromTheme(
            context: Context,
            @StyleRes themeRes: Int = context.applicationInfo.theme,
            reader: ThemeReader
        ): CometChatIncomingCallTheme = CometChatIncomingCallTheme(
            light = reader.read(inMode(context, themeRes, Configuration.UI_MODE_NIGHT_NO)),
            dark = reader.read(inMode(context, themeRes, Configuration.UI_MODE_NIGHT_YES))
        )

        // A configuration context carries no theme of its own, so the theme is applied on top of
        // it; its resources then resolve the theme's attributes in the forced mode.
        private fun inMode(context: Context, @StyleRes themeRes: Int, night: Int): Context {
            val config = Configuration(context.resources.configuration)
            config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
            val forced = context.createConfigurationContext(config)
            return if (themeRes != 0) ContextThemeWrapper(forced, themeRes) else forced
        }
    }
}
