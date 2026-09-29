package com.cometchat.uikit.core.formatter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Tests for [SegmentComposerController], which holds the composer's ordered list of segments —
 * rich-text ones and code blocks — and every transition between them.
 *
 * The model is small but the invariants are easy to break:
 *
 * - **A code block is never an edge.** Inserting one always leaves a Normal segment before and
 *   after it, so there is somewhere to type on either side. Several methods rebuild the whole
 *   list, and each has to preserve that.
 * - **Removing a code block merges three segments into one**, joining previous text, code text and
 *   following text with newlines only where both sides are non-empty — so an empty part never
 *   leaves a stray blank line — and parks the cursor at the end of the re-absorbed code.
 * - **Segment ids are never reused.** Rebuilds mint fresh ids deliberately, because the UI keys
 *   its editors off them and a recycled id shows stale text. `clear()` does not reset the counter.
 * - **Serialisation skips empties.** `toMarkdown` fences code with its language and drops blank
 *   segments; `toPlainText` joins the non-empty ones.
 *
 * Everything here is plain Kotlin — the controller and [RichTextEditorController] have no Android
 * imports — so no Robolectric host is needed.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*SegmentComposerControllerTest"
 */
class SegmentComposerControllerTest : FunSpec({

    /** Types [text] into the focused Normal segment, leaving the cursor at [cursor] (default end). */
    fun SegmentComposerController.type(text: String, cursor: Int = text.length) {
        val normal = focusedSegment as? ComposerSegment.Normal
            ?: segments.filterIsInstance<ComposerSegment.Normal>().first()
        setFocusedSegment(normal.id)
        normal.controller.onTextChanged(text, cursor, cursor)
    }

    fun SegmentComposerController.select(text: String, start: Int, end: Int) {
        val normal = focusedSegment as? ComposerSegment.Normal
            ?: segments.filterIsInstance<ComposerSegment.Normal>().first()
        setFocusedSegment(normal.id)
        normal.controller.onTextChanged(text, start, end)
    }

    fun SegmentComposerController.codeSegments() = segments.filterIsInstance<ComposerSegment.Code>()

    // ==================== initial state ====================

    test("a new controller starts with a single empty normal segment") {
        val c = SegmentComposerController()
        c.segments.size shouldBe 1
        (c.segments.single() is ComposerSegment.Normal) shouldBe true
        c.hasContent shouldBe false
        c.hasCodeBlocks shouldBe false
        c.toMarkdown() shouldBe ""
        c.toPlainText() shouldBe ""
    }

    // ==================== focus ====================

    test("focusSegment sets both the focused and the pending id, and pending is consumed once") {
        val c = SegmentComposerController()
        val id = c.segments.single().id

        c.focusSegment(id)
        c.focusedSegmentId shouldBe id
        c.pendingFocusSegmentId shouldBe id

        c.consumePendingFocus() shouldBe id
        c.pendingFocusSegmentId shouldBe null
        c.consumePendingFocus() shouldBe null   // nothing left to consume
        c.focusedSegmentId shouldBe id          // focus itself is unaffected
    }

    test("setFocusedSegment moves focus without requesting it from the UI") {
        val c = SegmentComposerController()
        c.setFocusedSegment(c.segments.single().id)
        c.pendingFocusSegmentId shouldBe null
    }

    test("isTypingInCode reflects whether the focused segment is a code block") {
        val c = SegmentComposerController()
        c.isTypingInCode shouldBe false

        c.insertCodeBlock()
        c.isTypingInCode shouldBe true

        val firstNormal = c.segments.filterIsInstance<ComposerSegment.Normal>().first()
        c.setFocusedSegment(firstNormal.id)
        c.isTypingInCode shouldBe false
    }

    test("focusedSegment is null when the focused id matches nothing") {
        val c = SegmentComposerController()
        c.setFocusedSegment("no-such-segment")
        c.focusedSegment shouldBe null
        c.isTypingInCode shouldBe false
    }

    // ==================== inserting a code block ====================

    test("inserting into an empty composer leaves a normal segment on each side") {
        val c = SegmentComposerController()
        c.insertCodeBlock()

        c.codeSegments().size shouldBe 1
        c.hasCodeBlocks shouldBe true

        val idx = c.segments.indexOfFirst { it is ComposerSegment.Code }
        (c.segments[idx - 1] is ComposerSegment.Normal) shouldBe true
        (c.segments[idx + 1] is ComposerSegment.Normal) shouldBe true
        c.focusedSegmentId shouldBe c.codeSegments().single().id
    }

    test("a selection becomes the code block, and the text around it is preserved") {
        val c = SegmentComposerController()
        c.select("before SELECTED after", start = 7, end = 15)

        c.insertCodeBlock()

        c.codeSegments().single().text shouldBe "SELECTED"
        c.toPlainText() shouldContain "before "
        c.toPlainText() shouldContain " after"
    }

    test("a cursor at the end of the text keeps the text and adds an empty code block below") {
        val c = SegmentComposerController()
        c.type("all of my text")

        c.insertCodeBlock()

        c.codeSegments().single().text shouldBe ""
        c.toPlainText() shouldContain "all of my text"
    }

    test("toggleCodeBlock inserts from a normal segment and removes from a code one") {
        val c = SegmentComposerController()

        c.toggleCodeBlock()
        c.hasCodeBlocks shouldBe true

        c.toggleCodeBlock()   // focus is on the code block, so this removes it
        c.hasCodeBlocks shouldBe false
    }

    test("toggleCodeBlock with nothing focused still inserts") {
        val c = SegmentComposerController()
        c.setFocusedSegment("no-such-segment")
        c.toggleCodeBlock()
        c.hasCodeBlocks shouldBe true
    }

    // ==================== convertAllToCodeBlock ====================

    test("converting all text moves every character into the code block") {
        val c = SegmentComposerController()
        c.type("line one\nline two")

        c.convertAllToCodeBlock()

        c.codeSegments().single().text shouldBe "line one\nline two"
        c.focusedSegmentId shouldBe c.codeSegments().single().id
    }

    test("converting all strips blockquote and list prefixes") {
        val c = SegmentComposerController()
        c.type("> quoted\n- bullet\n1. numbered")

        c.convertAllToCodeBlock()

        val code = c.codeSegments().single().text
        code shouldBe "quoted\nbullet\nnumbered"
    }

    test("converting all on empty text falls back to inserting an empty code block") {
        val c = SegmentComposerController()
        c.convertAllToCodeBlock()

        c.codeSegments().size shouldBe 1
        c.codeSegments().single().text shouldBe ""
    }

    test("converting all is a no-op when a code block is focused") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val before = c.segments.map { it.id }

        c.convertAllToCodeBlock()

        c.segments.map { it.id } shouldBe before
    }

    // ==================== removing a code block ====================

    test("removing a code block merges the text around it and drops the trailing segment") {
        val c = SegmentComposerController()
        c.type("before")
        c.insertCodeBlock()

        val code = c.codeSegments().single()
        code.text = "CODE"
        c.removeCodeSegment(code)

        c.hasCodeBlocks shouldBe false
        c.toPlainText() shouldContain "before"
        c.toPlainText() shouldContain "CODE"
    }

    test("merging inserts a newline only where both sides have text") {
        val c = SegmentComposerController()
        c.insertCodeBlock()               // nothing before it
        val code = c.codeSegments().single()
        code.text = "only code"

        c.removeCodeSegment(code)

        // no leading blank line from an empty "before" part
        c.toPlainText() shouldBe "only code"
    }

    test("removing a segment that is not in the list is ignored") {
        val c = SegmentComposerController()
        val orphan = ComposerSegment.Code(id = "not-mine", text = "x")
        val before = c.segments.map { it.id }

        c.removeCodeSegment(orphan)

        c.segments.map { it.id } shouldBe before
    }

    // ==================== backspace handling ====================

    test("backspace on an empty code block removes it") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val code = c.codeSegments().single()
        code.text = ""

        c.handleBackspaceOnEmptyCodeBlock(code) shouldBe true
        c.hasCodeBlocks shouldBe false
    }

    test("backspace on a code block that still has text does nothing") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val code = c.codeSegments().single()
        code.text = "still here"

        c.handleBackspaceOnEmptyCodeBlock(code) shouldBe false
        c.hasCodeBlocks shouldBe true
    }

    test("code text changes are recorded on the segment") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val code = c.codeSegments().single()

        c.handleCodeTextChanged(code, "fun main() {}")

        c.codeSegments().single().text shouldBe "fun main() {}"
    }

    // ==================== serialisation ====================

    test("markdown fences code blocks and skips empty segments") {
        val c = SegmentComposerController()
        c.type("intro text")
        c.insertCodeBlock()
        val code = c.codeSegments().single()
        code.text = "val x = 1"

        val md = c.toMarkdown()
        md shouldContain "intro text"
        md shouldContain "```"
        md shouldContain "val x = 1"
        // the trailing empty normal segment contributes nothing
        md.endsWith("```") shouldBe true
    }

    test("markdown carries the language hint when one is set") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        val code = c.codeSegments().single()
        code.text = "print(1)"
        code.language = "python"

        c.toMarkdown() shouldContain "```python"
    }

    test("plain text joins the non-empty segments and ignores the empty ones") {
        val c = SegmentComposerController()
        c.type("first")
        c.insertCodeBlock()
        c.codeSegments().single().text = "second"

        c.toPlainText() shouldBe "first\nsecond"
    }

    // ==================== content flags ====================

    test("hasContent is true once any segment holds text") {
        val c = SegmentComposerController()
        c.hasContent shouldBe false

        c.type("something")
        c.hasContent shouldBe true
    }

    test("hasContent is true for a code block with text even when the normal segments are empty") {
        val c = SegmentComposerController()
        c.insertCodeBlock()
        c.codeSegments().single().text = "code only"
        c.hasContent shouldBe true
    }

    // ==================== lifecycle ====================

    test("clear returns to a single empty normal segment with a fresh id") {
        val c = SegmentComposerController()
        c.type("some text")
        c.insertCodeBlock()
        val oldIds = c.segments.map { it.id }.toSet()

        c.clear()

        c.segments.size shouldBe 1
        (c.segments.single() is ComposerSegment.Normal) shouldBe true
        c.hasContent shouldBe false
        // ids are never recycled -- the UI keys its editors off them
        oldIds.contains(c.segments.single().id) shouldBe false
        c.focusedSegmentId shouldBe c.segments.single().id
    }

    test("the listener is notified when the segment list changes, and not after it is detached") {
        val c = SegmentComposerController()
        var notifications = 0
        val listener = object : SegmentComposerController.Listener {
            override fun onSegmentsChanged() { notifications++ }
        }

        c.setListener(listener)
        c.insertCodeBlock()
        notifications shouldNotBe 0

        val afterInsert = notifications
        c.setListener(null)
        c.clear()
        notifications shouldBe afterInsert
    }
})
