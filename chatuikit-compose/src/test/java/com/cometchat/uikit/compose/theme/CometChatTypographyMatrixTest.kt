package com.cometchat.uikit.compose.theme

import androidx.compose.ui.text.TextStyle
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * ENG-38678 — CometChatTheme typography-token matrix, Phase 3 Slice 3a-typography.
 *
 * LAYER: unit (pure JVM/Kotest). CometChatTypography() is all-defaulted, so this
 * catalogues the TextStyle token surface by reflection and proves the tokens are
 * readable and differentiated. Typography is theme-independent (no light/dark
 * flip), so the deliberate-break guard is role differentiation (title vs body,
 * bold vs regular). The per-token VISUAL proof (does the style paint?) is the
 * snapshot layer (Slice 3b).
 */
class CometChatTypographyMatrixTest : FunSpec({

    val tokens: List<KProperty1<CometChatTypography, *>> =
        CometChatTypography::class.memberProperties
            .filter { it.returnType.classifier == TextStyle::class }

    fun read(t: CometChatTypography, p: KProperty1<CometChatTypography, *>): TextStyle =
        p.get(t) as TextStyle

    test("catalogues the typography token surface (the matrix denominator)") {
        tokens.size shouldBeGreaterThanOrEqual 20
    }

    test("every typography token is readable from the default typography") {
        val t = CometChatTypography()
        tokens.forEach { read(t, it) } // no token throws / is missing a getter
        tokens.size shouldBeGreaterThanOrEqual 20
    }

    test("tokens are differentiated, not one shared style") {
        val t = CometChatTypography()
        tokens.map { read(t, it) }.distinct().size shouldBeGreaterThanOrEqual 8
    }

    test("role tokens differ (deliberate-break guard): title vs body, bold vs regular") {
        val t = CometChatTypography()
        val byName = tokens.associateBy { it.name }
        (read(t, byName.getValue("titleBold")) != read(t, byName.getValue("bodyRegular"))) shouldBe true
        (read(t, byName.getValue("bodyBold")) != read(t, byName.getValue("bodyRegular"))) shouldBe true
    }
})
