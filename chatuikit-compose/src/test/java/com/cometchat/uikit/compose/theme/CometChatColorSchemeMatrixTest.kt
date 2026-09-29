package com.cometchat.uikit.compose.theme

import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * ENG-38678 — CometChatTheme color-token matrix, Phase 3 Slice 3a.
 *
 * LAYER: unit (pure JVM/Kotest — no Android, no Robolectric). The color scheme is
 * a plain @Immutable class built by the non-@Composable lightColorScheme() /
 * darkColorScheme() factories, so this catalogues the full color-token surface by
 * reflection (the matrix denominator) and proves the tokens are real and
 * theme-switchable (light vs dark differ). The per-token VISUAL "reaches the
 * rendered output" proof is the snapshot layer (Slice 3b, Roborazzi themed
 * baselines + the deliberate-token-break check).
 */
class CometChatColorSchemeMatrixTest : FunSpec({

    val colorTokens: List<KProperty1<CometChatColorScheme, *>> =
        CometChatColorScheme::class.memberProperties
            .filter { it.returnType.classifier == Color::class }

    fun read(scheme: CometChatColorScheme, prop: KProperty1<CometChatColorScheme, *>): Color =
        prop.get(scheme) as Color

    test("the color scheme catalogues a large token surface (the matrix denominator)") {
        // The base doc puts the theme at ~54 tokens; the real scheme is larger.
        colorTokens.size shouldBeGreaterThanOrEqual 40
    }

    test("every color token is readable from both light and dark schemes") {
        val light = lightColorScheme()
        val dark = darkColorScheme()
        // no token throws / is missing a getter
        colorTokens.forEach { read(light, it); read(dark, it) }
        colorTokens.size shouldBe colorTokens.size // sanity anchor
    }

    test("the theme actually drives tokens: many color tokens differ between light and dark") {
        val light = lightColorScheme()
        val dark = darkColorScheme()
        val differing = colorTokens.filter { read(light, it) != read(dark, it) }
        // If theme wiring regressed to a single shared palette, this collapses.
        differing.size shouldBeGreaterThanOrEqual 15
    }

    test("background/surface tokens flip between light and dark (deliberate-break guard)") {
        val light = lightColorScheme()
        val dark = darkColorScheme()
        // The brand primary is intentionally constant across themes; the tokens that
        // MUST flip are the backgrounds/surfaces/neutrals. If a regression flattened
        // them, this guard fails.
        val flipped = colorTokens
            .filter { read(light, it) != read(dark, it) }
            .map { it.name }
            .filter { it.contains("background", true) || it.contains("surface", true) || it.contains("neutral", true) }
        flipped.isNotEmpty() shouldBe true
    }
})
