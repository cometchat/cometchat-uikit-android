package com.cometchat.uikit.core.mentions

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [MentionDetector] and [MentionDetectionState] (ENG-38677 / L —
 * the `mentions` package was at 0% coverage).
 *
 * Pure text/cursor logic, no Android dependency. Covers the detection branches:
 * guard conditions, trigger validation (start / preceded by space / preceded by
 * non-space), the "closed by space" rules, names containing spaces, newline
 * boundaries, custom tracking characters, replacement ranges, and the stateful
 * wrapper.
 */
class MentionDetectorTest : FunSpec({

    val detector = MentionDetector() // default '@'

    context("detectMention — guard conditions return INACTIVE") {
        test("empty text") {
            detector.detectMention("", 0) shouldBe MentionDetector.MentionDetectionResult.INACTIVE
        }
        test("cursor at or before 0") {
            detector.detectMention("@a", 0) shouldBe MentionDetector.MentionDetectionResult.INACTIVE
        }
        test("cursor beyond text length") {
            detector.detectMention("@a", 5) shouldBe MentionDetector.MentionDetectionResult.INACTIVE
        }
    }

    context("detectMention — active detection") {
        test("trigger at start of text extracts query") {
            val r = detector.detectMention("@j", 2)
            r.isActive shouldBe true
            r.query shouldBe "j"
            r.triggerIndex shouldBe 0
            r.cursorPosition shouldBe 2
        }

        test("trigger preceded by a space is valid") {
            val r = detector.detectMention("hi @bo", 6)
            r.isActive shouldBe true
            r.query shouldBe "bo"
            r.triggerIndex shouldBe 3
        }

        test("supports names containing a single space") {
            val r = detector.detectMention("hi @John P", 10)
            r.isActive shouldBe true
            r.query shouldBe "John P"
            r.triggerIndex shouldBe 3
        }

        test("empty query right after typing the trigger") {
            val r = detector.detectMention("@", 1)
            r.isActive shouldBe true
            r.query shouldBe ""
            r.triggerIndex shouldBe 0
        }
    }

    context("detectMention — INACTIVE cases") {
        test("trigger preceded by a non-space character") {
            detector.detectMention("a@b", 3).isActive shouldBe false
        }
        test("trigger immediately followed by a space closes the mention") {
            detector.detectMention("@ ", 2).isActive shouldBe false
        }
        test("query ending in two spaces closes the mention") {
            detector.detectMention("@John  ", 7).isActive shouldBe false
        }
        test("backward search stops at a newline") {
            detector.detectMention("@bob\nhi", 7).isActive shouldBe false
        }
        test("no trigger present") {
            detector.detectMention("plain text", 5).isActive shouldBe false
        }
    }

    context("tracking character") {
        test("isTrackingCharacter matches only the configured char") {
            detector.isTrackingCharacter('@') shouldBe true
            detector.isTrackingCharacter('#') shouldBe false
        }
        test("getTrackingCharacter returns the configured char") {
            detector.getTrackingCharacter() shouldBe '@'
        }
        test("custom tracking character is honoured") {
            val hash = MentionDetector('#')
            val r = hash.detectMention("#tag", 4)
            r.isActive shouldBe true
            r.query shouldBe "tag"
            hash.detectMention("@tag", 4).isActive shouldBe false
        }
    }

    context("getReplacementRange") {
        test("active result returns trigger..cursor range") {
            val r = detector.detectMention("@Jo", 3)
            detector.getReplacementRange("@Jo", r) shouldBe Pair(0, 3)
        }
        test("inactive result returns (-1, -1)") {
            detector.getReplacementRange("x", MentionDetector.MentionDetectionResult.INACTIVE) shouldBe Pair(-1, -1)
        }
    }

    context("MentionDetectionState") {
        test("update stores and exposes the latest result") {
            val state = MentionDetectionState()
            val r = state.update("hi @bo", 6)

            r.isActive shouldBe true
            state.getCurrentResult() shouldBe r
            state.isActive() shouldBe true
            state.getQuery() shouldBe "bo"
            state.getTriggerIndex() shouldBe 3
            state.getReplacementRange("hi @bo") shouldBe Pair(3, 6)
        }

        test("reset returns to the inactive result") {
            val state = MentionDetectionState()
            state.update("@bob", 4)
            state.isActive() shouldBe true

            state.reset()

            state.isActive() shouldBe false
            state.getCurrentResult() shouldBe MentionDetector.MentionDetectionResult.INACTIVE
        }

        test("exposes the detector's tracking character") {
            MentionDetectionState(MentionDetector('#')).getTrackingCharacter() shouldBe '#'
        }
    }
})
