package com.cometchat.uikit.core.formatter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContain

/**
 * Second pass over [SegmentComposerController], covering the paths the first suite left alone:
 * pasted fenced code, paragraph-level conversion in both directions, backspace navigation, and
 * the adjacent-Normal merge that several of those rebuilds depend on.
 *
 * The behaviours worth stating, because they are not obvious from the method names:
 *
 * - **A paste is only a code block when it has two fences.** A single stray ``` is left as typed,
 *   so someone mid-sentence does not lose their text to a half-typed fence. A language tag after
 *   the opening fence is skipped rather than becoming the first line of the code.
 * - **Converting the cursor paragraph only isolates one paragraph when the text actually has
 *   line formats and more than one line.** Otherwise it degrades to converting everything, which
 *   is why a plain two-line note becomes a single code block rather than being split.
 * - **Extraction is the inverse.** Pulling a paragraph out of a code block leaves code blocks on
 *   either side only where text remains, and can re-apply a blockquote or list prefix on the way
 *   out.
 * - **Backspace on an empty Normal is navigation, not deletion of content** — it moves focus to a
 *   preceding code block, and refuses when there is no code behind it or when it is the only
 *   Normal left.
 * - **Two trailing newlines exit a code block**, which is how the composer lets someone type their
 *   way out without reaching for the toolbar.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*SegmentComposerControllerPasteAndExtractTest"
 */
class SegmentComposerControllerPasteAndExtractTest : FunSpec({

    fun SegmentComposerController.type(text: String, cursor: Int = text.length) {
        val normal = focusedSegment as? ComposerSegment.Normal
            ?: segments.filterIsInstance<ComposerSegment.Normal>().first()
        setFocusedSegment(normal.id)
        normal.controller.onTextChanged(text, cursor, cursor)
    }

    fun SegmentComposerController.codes() = segments.filterIsInstance<ComposerSegment.Code>()
    fun SegmentComposerController.normals() = segments.filterIsInstance<ComposerSegment.Normal>()
    fun SegmentComposerController.normalTexts() = normals().map { it.controller.state.text }

    /** Focuses a code segment so the code-side operations apply to it. */
    fun SegmentComposerController.focusCode(): ComposerSegment.Code {
        val code = codes().first()
        setFocusedSegment(code.id)
        return code
    }

    // ==================== pasted fenced code ====================

    test("pasting a complete fence splits into before, code and after") {
        val c = SegmentComposerController()
        c.type("intro\n```\nval x = 1\n```\noutro")

        c.detectAndConvertPastedCodeBlocks() shouldBe true

        c.codes().single().text shouldBe "val x = 1"
        c.normalTexts() shouldContain "intro"
        c.normalTexts() shouldContain "outro"
        c.focusedSegmentId shouldBe c.codes().single().id
    }

    test("a language tag after the opening fence is skipped, not treated as code") {
        val c = SegmentComposerController()
        c.type("```kotlin\nfun main() {}\n```")

        c.detectAndConvertPastedCodeBlocks() shouldBe true

        c.codes().single().text shouldBe "fun main() {}"
    }

    test("text with no fence at all is left alone") {
        val c = SegmentComposerController()
        c.type("just ordinary text")

        c.detectAndConvertPastedCodeBlocks() shouldBe false
        c.hasCodeBlocks shouldBe false
    }

    test("a single unmatched fence is left as typed") {
        val c = SegmentComposerController()
        c.type("half a fence ``` and nothing else")

        c.detectAndConvertPastedCodeBlocks() shouldBe false
        c.hasCodeBlocks shouldBe false
    }

    test("overlapping backticks are not a fence") {
        val c = SegmentComposerController()
        // four backticks: the first ``` starts at 0 and the last at 1, so the two matches
        // overlap and the guard rejects them
        c.type("````")

        c.detectAndConvertPastedCodeBlocks() shouldBe false
        c.hasCodeBlocks shouldBe false
    }

    test("six backticks are a complete, empty fence") {
        val c = SegmentComposerController()
        // first ``` at 0, last at 3 -- non-overlapping, so this is an empty code block rather
        // than a rejection
        c.type("``````")

        c.detectAndConvertPastedCodeBlocks() shouldBe true
        c.codes().single().text shouldBe ""
    }

    test("a fence with nothing around it yields empty normals on both sides") {
        val c = SegmentComposerController()
        c.type("```\nbody\n```")

        c.detectAndConvertPastedCodeBlocks() shouldBe true

        c.codes().single().text shouldBe "body"
        val idx = c.segments.indexOfFirst { it is ComposerSegment.Code }
        (c.segments[idx - 1] as ComposerSegment.Normal).controller.state.text shouldBe ""
        (c.segments[idx + 1] as ComposerSegment.Normal).controller.state.text shouldBe ""
    }

    test("pasting while a code block is focused does nothing") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        c.focusCode()

        c.detectAndConvertPastedCodeBlocks() shouldBe false
    }

    // ==================== cursor-paragraph conversion ====================

    test("plain multi-line text converts wholesale rather than by paragraph") {
        val c = SegmentComposerController()
        c.type("first line\nsecond line", cursor = 3)

        c.convertCursorParagraphToCodeBlock()

        // no line formats present, so the whole thing becomes one code block
        c.codes().single().text shouldBe "first line\nsecond line"
    }

    test("a single line converts wholesale even when it carries a list prefix") {
        val c = SegmentComposerController()
        c.type("- only one bullet", cursor = 5)

        c.convertCursorParagraphToCodeBlock()

        c.codes().single().text shouldBe "only one bullet"
    }

    test("with line formats and several lines, only the cursor paragraph is converted") {
        val c = SegmentComposerController()
        val text = "- first\n- second\n- third"
        c.type(text, cursor = text.indexOf("second") + 2)

        c.convertCursorParagraphToCodeBlock()

        c.codes().single().text shouldBe "second"          // prefix stripped
        c.normalTexts() shouldContain "- first"
        c.normalTexts() shouldContain "- third"
    }

    test("converting the cursor paragraph is a no-op from inside a code block") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        c.focusCode()
        val before = c.segments.map { it.id }

        c.convertCursorParagraphToCodeBlock()

        c.segments.map { it.id } shouldBe before
    }

    // ==================== extracting back out of a code block ====================

    test("extracting the only paragraph leaves a normal segment and no code") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val code = c.focusCode()
        code.text = "single line"

        c.extractParagraphFromCodeBlock(cursorPosition = 3)

        c.hasCodeBlocks shouldBe false
        c.normalTexts() shouldContain "single line"
    }

    test("extracting a middle paragraph leaves code blocks on both sides") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val code = c.focusCode()
        code.text = "one\ntwo\nthree"

        c.extractParagraphFromCodeBlock(cursorPosition = code.text.indexOf("two") + 1)

        c.codes().map { it.text } shouldBe listOf("one", "three")
        c.normalTexts() shouldContain "two"
    }

    test("extraction can re-apply a blockquote prefix on the way out") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val code = c.focusCode()
        code.text = "quote me"

        c.extractParagraphFromCodeBlock(0, RichTextFormat.BLOCKQUOTE)

        c.normalTexts() shouldContain "> quote me"
    }

    test("extraction can re-apply bullet and ordered list prefixes") {
        val bullet = SegmentComposerController()
        bullet.insertCodeBlock()
        bullet.focusCode().text = "item"
        bullet.extractParagraphFromCodeBlock(0, RichTextFormat.BULLET_LIST)
        bullet.normalTexts() shouldContain "- item"

        val ordered = SegmentComposerController()
        ordered.insertCodeBlock()
        ordered.focusCode().text = "item"
        ordered.extractParagraphFromCodeBlock(0, RichTextFormat.ORDERED_LIST)
        ordered.normalTexts() shouldContain "1. item"
    }

    test("a cursor past the end of the code text is clamped rather than throwing") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val code = c.focusCode()
        code.text = "short"

        c.extractParagraphFromCodeBlock(cursorPosition = 9_999)

        c.normalTexts() shouldContain "short"
    }

    test("extracting is a no-op when a normal segment is focused") {
        val c = SegmentComposerController()
        c.type("not a code block")
        val before = c.segments.map { it.id }

        c.extractParagraphFromCodeBlock(0)

        c.segments.map { it.id } shouldBe before
    }

    // ==================== backspace navigation ====================

    test("backspace on an empty normal after a code block moves focus to that code block") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        c.codes().single().text = "some code"

        val trailing = c.normals().last()
        c.handleBackspaceOnEmptyNormalSegment(trailing) shouldBe true

        c.focusedSegmentId shouldBe c.codes().single().id
        c.normals().contains(trailing) shouldBe false
    }

    test("backspace on a normal that still has text does nothing") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val trailing = c.normals().last()
        trailing.controller.onTextChanged("typed something", 15, 15)

        c.handleBackspaceOnEmptyNormalSegment(trailing) shouldBe false
    }

    test("backspace does nothing when there is only one normal segment") {
        val c = SegmentComposerController()
        val only = c.normals().single()

        c.handleBackspaceOnEmptyNormalSegment(only) shouldBe false
    }

    test("backspace does nothing when no code block precedes the empty normal") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val leading = c.normals().first()   // sits before the code block

        c.handleBackspaceOnEmptyNormalSegment(leading) shouldBe false
    }

    test("backspace on a segment that is not in the list is refused") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val orphan = ComposerSegment.Normal(id = "not-mine")

        c.handleBackspaceOnEmptyNormalSegment(orphan) shouldBe false
    }

    // ==================== typing out of a code block ====================

    test("two trailing newlines exit the code block and trim them") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val code = c.focusCode()

        c.handleCodeTextChanged(code, "done\n\n") shouldBe true

        code.text shouldBe "done"
        c.focusedSegmentId shouldNotBeCodeId c.codes().single().id
    }

    test("a single trailing newline stays inside the code block") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val code = c.focusCode()

        c.handleCodeTextChanged(code, "still typing\n") shouldBe false

        code.text shouldBe "still typing\n"
        c.focusedSegmentId shouldBe code.id
    }

    test("exiting from a code block with nothing after it creates a normal to land in") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val code = c.codes().single()
        // drop the trailing normal so the exit has nowhere to go
        val trailing = c.normals().last()
        c.handleBackspaceOnEmptyNormalSegment(trailing)

        c.handleCodeTextChanged(code, "text\n\n") shouldBe true

        val idx = c.segments.indexOf(code)
        (c.segments.getOrNull(idx + 1) is ComposerSegment.Normal) shouldBe true
    }
})

/** Asserts the focused id moved off the code block, without caring which normal took it. */
private infix fun String?.shouldNotBeCodeId(codeId: String) {
    if (this == codeId) throw AssertionError("expected focus to leave the code block $codeId")
}
