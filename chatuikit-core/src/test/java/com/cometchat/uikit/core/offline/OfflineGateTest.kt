package com.cometchat.uikit.core.offline

import com.cometchat.chat.core.CometChat
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.mockito.Mockito

/**
 * [OfflineGate] is the single gate (G1). It must be defensive (throw/false → off), honour the test
 * override, and remain the ONLY call site of `CometChat.isOfflineEnabled` (invariant 2).
 */
class OfflineGateTest : FunSpec({

    afterTest { OfflineGate.testOverride = null }

    test("test override forces the result") {
        OfflineGate.testOverride = true
        OfflineGate.isOn() shouldBe true
        OfflineGate.testOverride = false
        OfflineGate.isOn() shouldBe false
    }

    test("SDK reporting true → on") {
        OfflineGate.testOverride = null
        Mockito.mockStatic(CometChat::class.java).use { m ->
            m.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(true)
            OfflineGate.isOn() shouldBe true
        }
    }

    test("SDK reporting false → off") {
        OfflineGate.testOverride = null
        Mockito.mockStatic(CometChat::class.java).use { m ->
            m.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenReturn(false)
            OfflineGate.isOn() shouldBe false
        }
    }

    test("SDK throwing → off (defensive)") {
        OfflineGate.testOverride = null
        Mockito.mockStatic(CometChat::class.java).use { m ->
            m.`when`<Boolean> { CometChat.isOfflineEnabled() }.thenThrow(RuntimeException("no offline symbol"))
            OfflineGate.isOn() shouldBe false
        }
    }

    test("isOfflineEnabled has exactly one call site in every UI Kit module's main source — OfflineGate") {
        // Test working dir is chatuikit-core; its parent is the repo root holding every module.
        // The rule covers the kit modules (chatuikit-*) only: an app may ask the SDK directly.
        val repoRoot = java.io.File("..").canonicalFile
        val skipDirs = setOf("build", ".gradle", ".git", ".idea", ".claude", "node_modules")
        // Any call named isOfflineEnabled, qualified or not: the gate must be the only one.
        val sdkCall = Regex("""\bisOfflineEnabled\s*\(""")
        val hits = repoRoot.walkTopDown()
            .onEnter { it.name !in skipDirs }
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .filter { it.relativeTo(repoRoot).invariantSeparatorsPath.startsWith("chatuikit-") }
            .filter { it.invariantSeparatorsPath.contains("/src/main/") }
            .filter { sdkCall.containsMatchIn(it.readText()) }
            .map { it.relativeTo(repoRoot).invariantSeparatorsPath }
            .toList()
        hits shouldBe listOf("chatuikit-core/src/main/java/com/cometchat/uikit/core/offline/OfflineGate.kt")
    }
})
