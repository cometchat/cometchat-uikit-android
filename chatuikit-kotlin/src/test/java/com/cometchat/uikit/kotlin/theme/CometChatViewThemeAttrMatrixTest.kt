package com.cometchat.uikit.kotlin.theme

import android.content.res.Configuration
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.uikit.kotlin.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ENG-38678 — View-toolkit theme-attribute matrix (XML side of CometChatTheme).
 *
 * LAYER: unit / Robolectric. The View toolkit's styling is backed by theme
 * attributes on CometChatTheme.DayNight (the XML equivalent of the compose
 * ColorScheme). This resolves each palette attr on a themed Context and proves:
 *  - the palette surface is catalogued (denominator),
 *  - every palette attr resolves to a color under the theme,
 *  - day vs night actually differ (values-night overrides) — the theme drives the
 *    attrs, and a flattening regression fails the day/night guard.
 * The visual/pixel proof is the compose golden gallery (Slice 3b); the View
 * component goldens live under screenshot-gallery/kotlin/.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatViewThemeAttrMatrixTest {

    /** The CometChatTheme.DayNight colour/tint palette — the theme-token surface. */
    private val paletteAttrs = listOf(
        "cometchatPrimaryColor",
        "cometchatNeutralColor50", "cometchatNeutralColor100", "cometchatNeutralColor200",
        "cometchatNeutralColor300", "cometchatNeutralColor400", "cometchatNeutralColor500",
        "cometchatNeutralColor600", "cometchatNeutralColor700", "cometchatNeutralColor800",
        "cometchatNeutralColor900",
        "cometchatInfoColor", "cometchatWarningColor", "cometchatSuccessColor", "cometchatErrorColor",
        "cometchatBackgroundColor1", "cometchatBackgroundColor2", "cometchatBackgroundColor3", "cometchatBackgroundColor4",
        "cometchatStrokeColorDefault", "cometchatStrokeColorLight", "cometchatStrokeColorDark", "cometchatStrokeColorHighlight",
        "cometchatTextColorPrimary", "cometchatTextColorSecondary", "cometchatTextColorTertiary",
        "cometchatTextColorDisabled", "cometchatTextColorWhite", "cometchatTextColorHighlight",
        "cometchatIconTintPrimary", "cometchatIconTintSecondary", "cometchatIconTintTertiary",
        "cometchatIconTintHighlight", "cometchatIconTintWhite",
        "cometchatColorBlack", "cometchatColorWhite",
        "cometchatPrimaryButtonBackgroundColor", "cometchatPrimaryButtonTextColor", "cometchatPrimaryButtonIconTint",
        "cometchatSecondaryButtonBackgroundColor", "cometchatSecondaryButtonTextColor", "cometchatSecondaryButtonIconTint",
    )

    private fun attrId(name: String): Int =
        R.attr::class.java.getField(name).getInt(null)

    private fun isColor(tv: TypedValue): Boolean =
        tv.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT

    @Test
    fun palette_isCatalogued() {
        assertTrue("expected a substantial theme palette, was ${paletteAttrs.size}", paletteAttrs.size >= 40)
    }

    @Test
    fun everyPaletteAttr_resolvesToAColor_underTheTheme() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            val unresolved = paletteAttrs.filter { name ->
                val tv = TypedValue()
                val ok = activity.theme.resolveAttribute(attrId(name), tv, true)
                !(ok && isColor(tv))
            }
            assertTrue("these palette attrs did not resolve to a color: $unresolved", unresolved.isEmpty())
        }
        scenario.close()
    }

    @Test
    fun dayVsNight_manyPaletteAttrsDiffer_themeDrivesTheAttrs() {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)

            val nightConfig = Configuration(activity.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
            val nightContext = activity.createConfigurationContext(nightConfig)
            nightContext.setTheme(R.style.CometChatTheme_DayNight)

            fun resolve(theme: android.content.res.Resources.Theme, name: String): Int {
                val tv = TypedValue(); theme.resolveAttribute(attrId(name), tv, true); return tv.data
            }

            val differing = paletteAttrs.filter { resolve(activity.theme, it) != resolve(nightContext.theme, it) }
            assertTrue("theme should drive many attrs day->night; differing=${differing.size}", differing.size >= 10)

            // deliberate-break guard: a background token must flip between day and night.
            assertTrue(
                "cometchatBackgroundColor1 must differ day vs night",
                resolve(activity.theme, "cometchatBackgroundColor1") != resolve(nightContext.theme, "cometchatBackgroundColor1"),
            )
        }
        scenario.close()
    }
}
