package com.cometchat.uikit.core.mentions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [TextFormatterProcessor] and [TextFormatterProcessingManager]
 * (ENG-38677 / L — the `mentions` package was at 0% coverage).
 *
 * The processing methods are pure String/IntRange logic (the Android imports are
 * unused by them), so no Robolectric is needed. Covers span replacement in
 * reverse order, out-of-bounds span guards, prompt replacement, the empty-map
 * short-circuits, and the manager's callback hooks.
 */
class TextFormatterProcessorTest : FunSpec({

    context("processSpans") {
        test("empty map returns the text unchanged") {
            val r = TextFormatterProcessor.processSpans("hello", emptyMap())
            r.processedText shouldBe "hello"
            r.originalText shouldBe "hello"
        }

        test("replaces spans with underlying text (reverse order keeps indices valid)") {
            // "hi @Joe and @Ann" — "@Joe" = 3..6, "@Ann" = 12..15
            val r = TextFormatterProcessor.processSpans(
                "hi @Joe and @Ann",
                mapOf(3..6 to "<@1>", 12..15 to "<@2>")
            )
            r.processedText shouldBe "hi <@1> and <@2>"
            r.originalText shouldBe "hi @Joe and @Ann"
        }

        test("skips spans that fall out of bounds") {
            val r = TextFormatterProcessor.processSpans("@Joe", mapOf(3..100 to "<@1>"))
            r.processedText shouldBe "@Joe" // range.last >= length -> skipped
        }
    }

    context("processPrompts") {
        test("empty map returns the text unchanged") {
            TextFormatterProcessor.processPrompts("hello", emptyMap()).processedText shouldBe "hello"
        }
        test("replaces each prompt with its underlying form") {
            val r = TextFormatterProcessor.processPrompts(
                "hi @Joe and @Ann",
                mapOf("@Joe" to "<@1>", "@Ann" to "<@2>")
            )
            r.processedText shouldBe "hi <@1> and <@2>"
            r.originalText shouldBe "hi @Joe and @Ann"
        }
    }

    context("TextFormatterProcessingManager") {
        // records callback invocations
        class RecordingCallback : TextFormatterProcessorCallback {
            var before: String? = null
            var after: TextFormatterProcessor.ProcessingResult? = null
            override fun onBeforeProcessing(text: String) { before = text }
            override fun onAfterProcessing(result: TextFormatterProcessor.ProcessingResult) { after = result }
        }

        test("process replaces prompts and fires both callbacks") {
            val cb = RecordingCallback()
            val mgr = TextFormatterProcessingManager().apply { setCallback(cb) }

            val out = mgr.process("hi @Joe", mapOf("@Joe" to "<@1>"))

            out shouldBe "hi <@1>"
            cb.before shouldBe "hi @Joe"
            cb.after?.processedText shouldBe "hi <@1>"
        }

        test("processWithSpans replaces spans and fires both callbacks") {
            val cb = RecordingCallback()
            val mgr = TextFormatterProcessingManager().apply { setCallback(cb) }

            val out = mgr.processWithSpans("hi @Joe", mapOf(3..6 to "<@1>"))

            out shouldBe "hi <@1>"
            cb.before shouldBe "hi @Joe"
            cb.after?.processedText shouldBe "hi <@1>"
        }

        test("works with no callback set") {
            val mgr = TextFormatterProcessingManager()
            mgr.process("hi @Joe", mapOf("@Joe" to "<@1>")) shouldBe "hi <@1>"
        }

        test("clearing the callback stops invocations") {
            val cb = RecordingCallback()
            val mgr = TextFormatterProcessingManager().apply { setCallback(cb) }
            mgr.setCallback(null)

            mgr.process("plain", emptyMap()) shouldBe "plain"
            cb.before shouldBe null
        }
    }
})
