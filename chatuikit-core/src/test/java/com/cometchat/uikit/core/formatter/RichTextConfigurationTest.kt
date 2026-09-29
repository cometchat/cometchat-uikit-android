package com.cometchat.uikit.core.formatter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Tests for [RichTextConfiguration], the on/off switchboard for composer formatting.
 *
 * Two behaviours carry real weight and are pinned here:
 * - Formatting is **off by default**. A host that constructs the configuration without arguments
 *   gets no formatting at all, so nothing appears in the composer that the integrator did not
 *   ask for.
 * - [RichTextConfiguration.getEnabledFormats] returns the enabled formats in a fixed declaration
 *   order, not the order the flags were set. Toolbars render straight from this list, so the
 *   order is part of the contract rather than an accident.
 *
 * Each flag is exercised on its own as well as in the presets, because `hasAnyEnabled` and
 * `getEnabledFormats` branch once per flag — ten independent conditions that a preset-only test
 * would leave half-covered.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*RichTextConfigurationTest"
 */
class RichTextConfigurationTest : FunSpec({

    /** Every flag paired with the format it should produce, in declaration order. */
    val flags: List<Pair<RichTextFormat, (Boolean) -> RichTextConfiguration>> = listOf(
        RichTextFormat.BOLD to { on -> RichTextConfiguration(enableBold = on) },
        RichTextFormat.ITALIC to { on -> RichTextConfiguration(enableItalic = on) },
        RichTextFormat.UNDERLINE to { on -> RichTextConfiguration(enableUnderline = on) },
        RichTextFormat.STRIKETHROUGH to { on -> RichTextConfiguration(enableStrikethrough = on) },
        RichTextFormat.INLINE_CODE to { on -> RichTextConfiguration(enableInlineCode = on) },
        RichTextFormat.CODE_BLOCK to { on -> RichTextConfiguration(enableCodeBlock = on) },
        RichTextFormat.LINK to { on -> RichTextConfiguration(enableLink = on) },
        RichTextFormat.BULLET_LIST to { on -> RichTextConfiguration(enableBulletList = on) },
        RichTextFormat.ORDERED_LIST to { on -> RichTextConfiguration(enableOrderedList = on) },
        RichTextFormat.BLOCKQUOTE to { on -> RichTextConfiguration(enableBlockquote = on) }
    )

    // ==================== defaults ====================

    test("formatting is entirely off by default") {
        val config = RichTextConfiguration()
        config.hasAnyEnabled() shouldBe false
        config.getEnabledFormats() shouldBe emptyList()
    }

    // ==================== one flag at a time ====================

    test("each flag on its own reports enabled and yields exactly its own format") {
        flags.forEach { (format, build) ->
            val config = build(true)
            withClue(format) {
                config.hasAnyEnabled() shouldBe true
                config.getEnabledFormats() shouldContainExactly listOf(format)
            }
        }
    }

    test("each flag explicitly off leaves the configuration empty") {
        flags.forEach { (format, build) ->
            val config = build(false)
            withClue(format) {
                config.hasAnyEnabled() shouldBe false
                config.getEnabledFormats() shouldBe emptyList()
            }
        }
    }

    // ==================== ordering ====================

    test("enabled formats come back in declaration order, not the order they were switched on") {
        // blockquote is declared last but set first here
        val config = RichTextConfiguration(enableBlockquote = true, enableBold = true)
        config.getEnabledFormats() shouldContainExactly listOf(
            RichTextFormat.BOLD, RichTextFormat.BLOCKQUOTE
        )
    }

    // ==================== presets ====================

    test("allEnabled turns on every format, in declaration order") {
        val config = RichTextConfiguration.allEnabled()
        config.hasAnyEnabled() shouldBe true
        config.getEnabledFormats() shouldContainExactly flags.map { it.first }
    }

    test("basicFormatting is bold, italic, underline and strikethrough only") {
        val config = RichTextConfiguration.basicFormatting()
        config.hasAnyEnabled() shouldBe true
        config.getEnabledFormats() shouldContainExactly listOf(
            RichTextFormat.BOLD,
            RichTextFormat.ITALIC,
            RichTextFormat.UNDERLINE,
            RichTextFormat.STRIKETHROUGH
        )
    }

    test("codeFormatting is inline code and code block only") {
        RichTextConfiguration.codeFormatting().getEnabledFormats() shouldContainExactly listOf(
            RichTextFormat.INLINE_CODE, RichTextFormat.CODE_BLOCK
        )
    }

    test("listFormatting is bullet and ordered lists only") {
        RichTextConfiguration.listFormatting().getEnabledFormats() shouldContainExactly listOf(
            RichTextFormat.BULLET_LIST, RichTextFormat.ORDERED_LIST
        )
    }

    // ==================== data-class semantics ====================

    test("two configurations with the same flags are equal and copy preserves the rest") {
        RichTextConfiguration.basicFormatting() shouldBe RichTextConfiguration(
            enableBold = true, enableItalic = true,
            enableUnderline = true, enableStrikethrough = true
        )

        val withLink = RichTextConfiguration.basicFormatting().copy(enableLink = true)
        withLink.getEnabledFormats() shouldContainExactly listOf(
            RichTextFormat.BOLD,
            RichTextFormat.ITALIC,
            RichTextFormat.UNDERLINE,
            RichTextFormat.STRIKETHROUGH,
            RichTextFormat.LINK
        )
    }
})

/** Names the format under test so a failing flag is identifiable from the report alone. */
private inline fun withClue(format: RichTextFormat, block: () -> Unit) {
    try {
        block()
    } catch (e: AssertionError) {
        throw AssertionError("format $format: ${e.message}", e)
    }
}
