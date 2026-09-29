package com.cometchat.uikit.core.formatter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Covers the line-format and markdown-shortcut behaviour of [RichTextEditorController] — the paths
 * that fire while someone is typing, rather than the span bookkeeping the existing specs exercise.
 *
 * None of these are public: they hang off [RichTextEditorController.onTextChanged] and
 * [RichTextEditorController.toggleFormat], so everything here drives them the way the composer
 * does, by reporting new text and a cursor position.
 *
 * The behaviours pinned here:
 *
 * - **Ordered-list numbering continues across a block.** Applying the format to a run of lines
 *   numbers them from one past the last numbered line above, so a second list under an existing
 *   one does not restart at 1.
 * - **Toggling a line format off strips the prefix** rather than nesting another one, and swapping
 *   between formats replaces rather than stacks.
 * - **`[text](url)` collapses to its display text** when typed out, but only when the brackets are
 *   well formed — nested brackets or parentheses in the URL leave the text alone rather than
 *   producing a mangled link.
 *
 * Plain Kotlin throughout: this controller has no Android imports.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*RichTextEditorControllerLineFormatsTest"
 */
class RichTextEditorControllerLineFormatsTest : FunSpec({

    fun controller() = RichTextEditorController()

    /** Types [text] wholesale with the cursor at the end. */
    fun RichTextEditorController.type(text: String) = onTextChanged(text, text.length, text.length)

    fun RichTextEditorController.select(from: Int, to: Int) =
        onTextChanged(state.text, from, to)

    // The multi-line paste continuation ("paste three lines into a bullet, get three bullets")
    // is NOT tested here, and cannot be: RichTextEditorController.handleMultiLinePasteContinuation
    // is private and has no caller anywhere in chatuikit-core, -kotlin or -compose. It is dead
    // code, and its ~30 branches are unreachable from any public entry point. Tests asserting
    // that behaviour were written and then removed once that was established -- they were
    // asserting a feature the editor does not actually have.

    // ==================== line formats ====================

    test("applying a bullet format prefixes the current line") {
        val c = controller()
        c.type("shopping")
        c.toggleFormat(RichTextFormat.BULLET_LIST)

        c.state.text shouldBe "- shopping"
    }

    test("applying the same line format twice removes the prefix") {
        val c = controller()
        c.type("shopping")
        c.toggleFormat(RichTextFormat.BULLET_LIST)
        c.toggleFormat(RichTextFormat.BULLET_LIST)

        c.state.text shouldBe "shopping"
    }

    test("swapping between line formats replaces the prefix rather than stacking it") {
        val c = controller()
        c.type("note")
        c.toggleFormat(RichTextFormat.BULLET_LIST)
        c.toggleFormat(RichTextFormat.BLOCKQUOTE)

        c.state.text shouldBe "> note"
    }

    test("an ordered list starts at one when nothing is numbered above it") {
        val c = controller()
        c.type("first")
        c.toggleFormat(RichTextFormat.ORDERED_LIST)

        c.state.text shouldBe "1. first"
    }

    test("ordered numbering continues from the last numbered line above") {
        val c = controller()
        c.type("1. one\n2. two\nthree")
        // put the cursor on the last line and number it
        val cursor = c.state.text.length
        c.onTextChanged(c.state.text, cursor, cursor)
        c.toggleFormat(RichTextFormat.ORDERED_LIST)

        c.state.text shouldContain "3. three"
    }

    test("applying a list format across a selection numbers each line in turn") {
        val c = controller()
        c.type("alpha\nbeta\ngamma")
        c.select(0, c.state.text.length)
        c.toggleFormat(RichTextFormat.ORDERED_LIST)

        c.state.text shouldContain "1. alpha"
        c.state.text shouldContain "2. beta"
        c.state.text shouldContain "3. gamma"
    }

    test("removing a list format across a selection strips every prefix") {
        val c = controller()
        c.type("alpha\nbeta")
        c.select(0, c.state.text.length)
        c.toggleFormat(RichTextFormat.BULLET_LIST)
        c.select(0, c.state.text.length)
        c.toggleFormat(RichTextFormat.BULLET_LIST)

        c.state.text shouldBe "alpha\nbeta"
    }

    // ==================== markdown link shortcut ====================

    test("a well formed markdown link collapses to its display text") {
        val c = controller()
        c.type("see [the docs](https://example.invalid)")

        c.state.text shouldContain "the docs"
        c.state.text.contains("](") shouldBe false
    }

    test("a link with an empty url is left as typed") {
        val c = controller()
        val typed = "see [the docs]()"
        c.type(typed)

        c.state.text shouldBe typed
    }

    test("a link with empty display text is left as typed") {
        val c = controller()
        val typed = "see [](https://example.invalid)"
        c.type(typed)

        c.state.text shouldBe typed
    }

    test("nested brackets in the display text are left alone") {
        val c = controller()
        val typed = "see [[nested]](https://example.invalid)"
        c.type(typed)

        c.state.text shouldContain "["
    }

    test("parentheses inside the url leave the text untouched") {
        val c = controller()
        val typed = "see [docs](https://example.invalid/a(b))"
        c.type(typed)

        c.state.text shouldContain "]("
    }

    test("a closing paren with no opening bracket is left alone") {
        val c = controller()
        val typed = "just a paren) here"
        c.type(typed)

        c.state.text shouldBe typed
    }

    // ==================== triple-backtick shortcut ====================

    test("a line of three backticks is detected as a code-block shortcut and removed") {
        val c = controller()
        c.type("```")

        c.detectTripleBacktickShortcut() shouldBe true
        c.state.text shouldBe ""
    }

    test("fewer than three backticks is not a shortcut") {
        val c = controller()
        c.type("``")

        c.detectTripleBacktickShortcut() shouldBe false
        c.state.text shouldBe "``"
    }

    test("backticks with text after them are not a bare shortcut") {
        val c = controller()
        c.type("```kotlin")

        c.detectTripleBacktickShortcut() shouldBe false
    }

    // ==================== cursor guards ====================

    test("inserting at the cursor appends at the caret") {
        val c = controller()
        c.type("hello")
        c.insertAtCursor(" world")

        c.state.text shouldBe "hello world"
    }

    test("clear empties the text and its formatting state") {
        val c = controller()
        c.type("- something")
        c.clear()

        c.state.text shouldBe ""
        c.activeFormats().isEmpty() shouldBe true
    }
})

/** The controller exposes active formats through its state; this reads them for brevity. */
private fun RichTextEditorController.activeFormats(): Set<RichTextFormat> = state.activeFormats
