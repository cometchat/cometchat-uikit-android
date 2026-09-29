package com.cometchat.uikit.core.mentions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [MentionInserter], [SelectedMention] and [SelectedMentionsManager]
 * (ENG-38677 / L — the `mentions` package was at 0% coverage).
 *
 * Pure text/collection logic, no Android dependency. Covers insertion maths
 * (before/after slicing, auto-appended space, span bounds), prompt→underlying
 * replacement, and the mentions manager (add/dedupe, remove by id/position,
 * position shifting on edits, lookups).
 */
class MentionInserterTest : FunSpec({

    context("calculateInsertion") {
        test("inserts at end, appends a space, computes cursor and span") {
            val r = MentionInserter.calculateInsertion(
                currentText = "hi @jo",
                triggerIndex = 3,
                cursorPosition = 6,
                promptText = "@John Paul",
                underlyingText = "<@uid:1>"
            )
            r.newText shouldBe "hi @John Paul "
            r.newCursorPosition shouldBe 14
            r.promptText shouldBe "@John Paul"
            r.underlyingText shouldBe "<@uid:1>"
            r.spanStart shouldBe 3
            r.spanEnd shouldBe 13 // 3 + length("@John Paul")
        }

        test("preserves text after the cursor") {
            val r = MentionInserter.calculateInsertion(
                currentText = "@jo world",
                triggerIndex = 0,
                cursorPosition = 3,
                promptText = "@Joe",
                underlyingText = "<@2>"
            )
            r.newText shouldBe "@Joe  world" // "@Joe " + " world"
            r.newCursorPosition shouldBe 5
            r.spanStart shouldBe 0
            r.spanEnd shouldBe 4
        }

        test("does not double the trailing space when prompt already ends with one") {
            val r = MentionInserter.calculateInsertion(
                currentText = "@a",
                triggerIndex = 0,
                cursorPosition = 2,
                promptText = "@Ann ",
                underlyingText = "<@3>"
            )
            r.newText shouldBe "@Ann "
            r.newCursorPosition shouldBe 5
            r.spanEnd shouldBe 4 // trimEnd() drops the trailing space
        }
    }

    context("replacePromptsWithUnderlying") {
        test("replaces every prompt with its underlying form") {
            val out = MentionInserter.replacePromptsWithUnderlying(
                "hi @Joe and @Ann",
                mapOf("@Joe" to "<@1>", "@Ann" to "<@2>")
            )
            out shouldBe "hi <@1> and <@2>"
        }
        test("returns text unchanged when there are no mentions") {
            MentionInserter.replacePromptsWithUnderlying("plain", emptyMap()) shouldBe "plain"
        }
    }

    context("SelectedMentionsManager") {
        fun mention(id: String, start: Int, end: Int) = SelectedMention(
            id = id, name = id, promptText = "@$id", underlyingText = "<@$id>",
            spanStart = start, spanEnd = end
        )

        test("addMention appends and de-dupes by id") {
            val m = SelectedMentionsManager()
            m.addMention(mention("a", 0, 2))
            m.addMention(mention("b", 5, 7))
            m.addMention(mention("a", 10, 12)) // same id replaces

            val all = m.getMentions()
            all.size shouldBe 2
            all.first { it.id == "a" }.spanStart shouldBe 10
        }

        test("removeMention deletes by id; removeMentionAt deletes by span hit") {
            val m = SelectedMentionsManager()
            m.addMention(mention("a", 0, 4))
            m.addMention(mention("b", 10, 14))

            m.removeMention("a")
            m.getMentions().map { it.id } shouldBe listOf("b")

            m.removeMentionAt(12) // inside b's span
            m.getMentions().isEmpty() shouldBe true
        }

        test("getPromptToUnderlyingMap maps prompt to underlying") {
            val m = SelectedMentionsManager()
            m.addMention(mention("a", 0, 2))
            m.getPromptToUnderlyingMap() shouldBe mapOf("@a" to "<@a>")
        }

        test("clear removes everything") {
            val m = SelectedMentionsManager()
            m.addMention(mention("a", 0, 2))
            m.clear()
            m.getMentions().isEmpty() shouldBe true
        }

        test("updatePositions leaves earlier mentions, shifts later ones, drops edited ones") {
            val m = SelectedMentionsManager()
            m.addMention(mention("before", 0, 4))   // ends before change
            m.addMention(mention("after", 10, 14))  // starts at/after change
            m.addMention(mention("inside", 6, 12))  // change falls within it

            m.updatePositions(changeStart = 7, changeLength = 3)

            val byId = m.getMentions().associateBy { it.id }
            byId["before"]!!.let { it.spanStart shouldBe 0; it.spanEnd shouldBe 4 }
            byId["after"]!!.let { it.spanStart shouldBe 13; it.spanEnd shouldBe 17 }
            byId.containsKey("inside") shouldBe false
        }

        test("isPositionInMention and getMentionAt locate spans") {
            val m = SelectedMentionsManager()
            m.addMention(mention("a", 3, 8))

            m.isPositionInMention(5) shouldBe true
            m.isPositionInMention(20) shouldBe false
            m.getMentionAt(5)?.id shouldBe "a"
            m.getMentionAt(20) shouldBe null
        }
    }
})
