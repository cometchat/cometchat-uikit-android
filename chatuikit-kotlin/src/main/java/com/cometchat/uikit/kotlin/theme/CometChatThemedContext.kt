package com.cometchat.uikit.kotlin.theme

import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import com.cometchat.uikit.kotlin.R

/**
 * Returns a [Context] whose theme is guaranteed to resolve the kit's `?attr/cometchat*` attributes.
 *
 * ENG-39309: kit view layouts reference `?attr/cometchatPrimaryColor` (and friends) at inflate time.
 * In a host app whose theme does not derive from `CometChatTheme.DayNight`, those attrs are
 * unresolved and the first kit view crashes with an `InflateException`. Wrapping the context in the
 * kit's own theme makes the attrs available regardless of the host theme.
 *
 * If the host theme already provides the kit attrs (it derives from `CometChatTheme.DayNight`), the
 * original context is returned unchanged so the host's customization is fully preserved.
 */
internal fun cometchatThemedContext(context: Context): Context {
    val tv = TypedValue()
    val hostProvidesKitAttrs =
        context.theme.resolveAttribute(R.attr.cometchatPrimaryColor, tv, true) &&
            tv.type != TypedValue.TYPE_NULL
    return if (hostProvidesKitAttrs) context
    else ContextThemeWrapper(context, R.style.CometChatTheme_DayNight)
}
