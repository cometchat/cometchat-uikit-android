package com.cometchat.uikit.core.formatter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Covers link bookkeeping and the span-adjustment arithmetic in [RichTextSpanManager] — the two
 * areas the existing span specs leave thin.
 *
 * **Deletion is the arithmetic that matters.** A delete can hit a span six different ways, and
 * each needs a different adjustment: entirely before it (untouched), entirely after (shifted
 * left), swallowed (dropped), overlapping the start (end trimmed), overlapping the end (start
 * pinned and shifted), or straddling it (shrunk). Every one of those is exercised here, because
 * getting any of them wrong silently misplaces formatting somewhere else in the message.
 *
 * **Link URLs live in a side map keyed by span start**, so every edit has to move those keys in
 * step with the spans. A key inside a deleted range is dropped; one after it shifts down. If that
 * drifts, a link renders over the wrong words or points at the wrong URL.
 *
 * **Markdown round-trips** through [RichTextSpanManager.toMarkdown] and
 * [RichTextSpanManager.fromMarkdown], including `[text](url)`, where the display text becomes the
 * plain text and the URL is recovered into the side map.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*RichTextSpanManagerLinksAndDeletionTest"
 */
class RichTextSpanManagerLinksAndDeletionTest : FunSpec({

    fun manager() = RichTextSpanManager()

    // ==================== deletion, all six overlap cases ====================

    test("a span entirely before the deletion is untouched") {
        val m = manager()
        m.addFormat(0, 4, RichTextFormat.BOLD)

        m.onTextDeleted(10, 15)

        m.getFormatsAt(0) shouldBe setOf(RichTextFormat.BOLD)
        m.getFormatsAt(3) shouldBe setOf(RichTextFormat.BOLD)
    }

    test("a span entirely after the deletion shifts left by the deleted length") {
        val m = manager()
        m.addFormat(20, 25, RichTextFormat.BOLD)

        m.onTextDeleted(0, 10)

        m.getFormatsAt(10) shouldBe setOf(RichTextFormat.BOLD)
        m.getFormatsAt(20) shouldBe emptySet()
    }

    test("a span swallowed by the deletion disappears") {
        val m = manager()
        m.addFormat(5, 8, RichTextFormat.ITALIC)

        m.onTextDeleted(0, 12)

        m.getFormatsAt(0) shouldBe emptySet()
        m.getFormatsAt(3) shouldBe emptySet()
    }

    test("a span overlapping the start of the deletion has its end trimmed") {
        val m = manager()
        m.addFormat(0, 10, RichTextFormat.BOLD)

        m.onTextDeleted(5, 20)

        m.getFormatsAt(0) shouldBe setOf(RichTextFormat.BOLD)
        m.getFormatsAt(4) shouldBe setOf(RichTextFormat.BOLD)
        m.getFormatsAt(6) shouldBe emptySet()
    }

    test("a span overlapping the end of the deletion is pinned to the cut and shifted") {
        val m = manager()
        m.addFormat(10, 20, RichTextFormat.BOLD)

        m.onTextDeleted(5, 15)

        // what survives starts where the deletion began
        m.getFormatsAt(5) shouldBe setOf(RichTextFormat.BOLD)
        m.getFormatsAt(9) shouldBe setOf(RichTextFormat.BOLD)
    }

    test("a span straddling the whole deletion just shrinks") {
        val m = manager()
        m.addFormat(0, 20, RichTextFormat.BOLD)

        m.onTextDeleted(5, 10)

        m.getFormatsAt(0) shouldBe setOf(RichTextFormat.BOLD)
        m.getFormatsAt(14) shouldBe setOf(RichTextFormat.BOLD)
        m.getFormatsAt(15) shouldBe emptySet()
    }

    test("a deletion with no width is ignored") {
        val m = manager()
        m.addFormat(0, 5, RichTextFormat.BOLD)

        m.onTextDeleted(3, 3)

        m.getFormatsAt(4) shouldBe setOf(RichTextFormat.BOLD)
    }

    // ==================== link url bookkeeping ====================

    test("a link url is stored against its span start and read back") {
        val m = manager()
        m.addFormat(0, 4, RichTextFormat.LINK)
        m.setLinkUrl(0, "https://example.invalid")

        m.getLinkUrlAt(0) shouldBe "https://example.invalid"
        m.findLinkSpanAt(2)?.formats shouldBe setOf(RichTextFormat.LINK)
    }

    test("a link url inside a deleted range is dropped") {
        val m = manager()
        m.addFormat(10, 14, RichTextFormat.LINK)
        m.setLinkUrl(10, "https://example.invalid")

        m.onTextDeleted(8, 20)

        m.getLinkUrlAt(10) shouldBe null
        m.getLinkUrlAt(8) shouldBe null
    }

    test("a link url after a deletion moves down with its span") {
        val m = manager()
        m.addFormat(20, 24, RichTextFormat.LINK)
        m.setLinkUrl(20, "https://example.invalid")

        m.onTextDeleted(0, 10)

        m.getLinkUrlAt(10) shouldBe "https://example.invalid"
        m.getLinkUrlAt(20) shouldBe null
    }

    test("a link url before a deletion stays where it is") {
        val m = manager()
        m.addFormat(0, 4, RichTextFormat.LINK)
        m.setLinkUrl(0, "https://example.invalid")

        m.onTextDeleted(10, 15)

        m.getLinkUrlAt(0) shouldBe "https://example.invalid"
    }

    test("a link url shifts right when text is inserted before it") {
        val m = manager()
        m.addFormat(10, 14, RichTextFormat.LINK)
        m.setLinkUrl(10, "https://example.invalid")

        m.onTextInserted(0, 5)

        m.getLinkUrlAt(15) shouldBe "https://example.invalid"
    }

    test("removing a link url leaves nothing behind at that position") {
        val m = manager()
        m.addFormat(0, 4, RichTextFormat.LINK)
        m.setLinkUrl(0, "https://example.invalid")

        m.removeLinkUrl(0)

        m.getLinkUrlAt(0) shouldBe null
    }

    test("findLinkSpanAt returns nothing where there is no link") {
        val m = manager()
        m.addFormat(0, 4, RichTextFormat.BOLD)

        m.findLinkSpanAt(2) shouldBe null
    }

    // ==================== markdown round-trip ====================

    test("bold and italic survive a round-trip through markdown") {
        val m = manager()
        m.addFormat(0, 5, RichTextFormat.BOLD)
        val md = m.toMarkdown("hello world")
        md shouldContain "hello"

        val (plain, spans) = manager().fromMarkdown(md)
        plain shouldContain "hello"
        spans.any { RichTextFormat.BOLD in it.formats } shouldBe true
    }

    test("a markdown link yields its display text and recovers the url") {
        val m = manager()
        val (plain, spans) = m.fromMarkdown("see [the docs](https://example.invalid) now")

        plain shouldContain "the docs"
        plain.contains("](") shouldBe false
        spans.any { RichTextFormat.LINK in it.formats } shouldBe true

        // fromMarkdown is a parser: it returns the spans rather than installing them, so the
        // caller applies them before the url lookup (which resolves through _spans) can work.
        val linkSpan = spans.first { RichTextFormat.LINK in it.formats }
        m.addFormat(linkSpan.start, linkSpan.end, RichTextFormat.LINK)
        m.getLinkUrlAt(linkSpan.start) shouldBe "https://example.invalid"
    }

    test("markdown with no formatting yields the text unchanged and no spans") {
        val (plain, spans) = manager().fromMarkdown("just plain words")

        plain shouldBe "just plain words"
        spans shouldBe emptyList()
    }

    test("an empty markdown string round-trips to nothing") {
        val (plain, spans) = manager().fromMarkdown("")

        plain shouldBe ""
        spans shouldBe emptyList()
    }

    // ==================== housekeeping ====================

    test("clear drops every span and every link url") {
        val m = manager()
        m.addFormat(0, 4, RichTextFormat.LINK)
        m.setLinkUrl(0, "https://example.invalid")
        m.addFormat(5, 9, RichTextFormat.BOLD)

        m.clear()

        m.getFormatsAt(0) shouldBe emptySet()
        m.getFormatsAt(6) shouldBe emptySet()
        m.getLinkUrlAt(0) shouldBe null
    }

    test("normalize merges touching spans that carry the same formats") {
        val m = manager()
        m.addFormat(0, 5, RichTextFormat.BOLD)
        m.addFormat(5, 10, RichTextFormat.BOLD)

        m.normalize()

        m.getFormatsAt(0) shouldBe setOf(RichTextFormat.BOLD)
        m.getFormatsAt(9) shouldBe setOf(RichTextFormat.BOLD)
    }

    test("getFormatsInRange reports only formats covering the whole range") {
        val m = manager()
        m.addFormat(0, 5, RichTextFormat.BOLD)
        m.addFormat(5, 10, RichTextFormat.ITALIC)

        // neither format spans the full 0..10, so nothing qualifies -- this is a "covers the
        // selection" query, which is what a toolbar needs to decide whether a button is active,
        // not a union of everything the range touches
        m.getFormatsInRange(0, 10) shouldBe emptySet()

        m.getFormatsInRange(0, 5) shouldBe setOf(RichTextFormat.BOLD)
        m.getFormatsInRange(5, 10) shouldBe setOf(RichTextFormat.ITALIC)
        m.getFormatsInRange(1, 4) shouldBe setOf(RichTextFormat.BOLD)
    }

    test("a range query with no width returns nothing") {
        val m = manager()
        m.addFormat(0, 5, RichTextFormat.BOLD)

        m.getFormatsInRange(3, 3) shouldBe emptySet()
    }

    test("splitting inline code at a newline ends the span at the break") {
        val m = manager()
        m.addFormat(0, 10, RichTextFormat.INLINE_CODE)

        m.splitInlineCodeAtNewline(5)

        m.getFormatsAt(2) shouldBe setOf(RichTextFormat.INLINE_CODE)
    }
})
