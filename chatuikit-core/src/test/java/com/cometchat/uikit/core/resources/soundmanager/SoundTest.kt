package com.cometchat.uikit.core.resources.soundmanager

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for the [Sound] enum (ENG-38677 / L — the `soundmanager` package was at
 * 0% coverage). Pure JVM: the enum maps each sound to a generated `R.raw`
 * resource id and exposes get/set.
 *
 * Note: [Sound] holds a mutable `rawFile` on a singleton enum constant, so the
 * set test restores the original value in a `finally` to avoid leaking state
 * into other tests.
 */
class SoundTest : FunSpec({

    test("defines exactly the five expected sounds") {
        Sound.values().map { it.name } shouldContainExactly listOf(
            "INCOMING_CALL",
            "OUTGOING_CALL",
            "INCOMING_MESSAGE",
            "OUTGOING_MESSAGE",
            "INCOMING_MESSAGE_FROM_OTHER"
        )
    }

    test("every sound maps to a non-zero raw resource id") {
        Sound.values().forEach { it.getRawFile() shouldNotBe 0 }
    }

    test("setRawFile updates the id, returns it, and is restorable") {
        val sound = Sound.OUTGOING_MESSAGE
        val original = sound.getRawFile()
        try {
            sound.setRawFile(987654) shouldBe 987654
            sound.getRawFile() shouldBe 987654
        } finally {
            sound.setRawFile(original)
        }
        sound.getRawFile() shouldBe original
    }
})
