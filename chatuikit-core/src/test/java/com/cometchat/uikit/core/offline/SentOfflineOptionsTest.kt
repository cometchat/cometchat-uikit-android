package com.cometchat.uikit.core.offline

import com.cometchat.chat.exceptions.CometChatException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

/**
 * Sent messages while offline (G10, BEH-MUT-01): they keep today's options, and a network-bound
 * action ends in today's error with no optimistic change left behind.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SentOfflineOptionsTest : FunSpec({

    val testDispatcher = UnconfinedTestDispatcher()
    val h = SendRowsHarness()
    val notConnected = CometChatException("ERROR_INTERNET_UNAVAILABLE", "offline")

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Thread.sleep(100); Dispatchers.resetMain() }
    afterTest { h.close() }

    test("warmup - absorb leaked exceptions") {
        try { runTest { } } catch (_: Exception) { }
    }

    test("a reaction that fails not-connected leaves the row exactly as it was") {
        runTest {
            val sent = h.served(5L)
            val vm = h.open(seeded = listOf(sent)) { repo ->
                runBlocking {
                    whenever(repo.addReaction(any(), any())).thenReturn(Result.failure(notConnected))
                    whenever(repo.removeReaction(any(), any())).thenReturn(Result.failure(notConnected))
                }
            }
            advanceUntilIdle()
            val before = vm.getItems().single()
            vm.addReaction(before, "👍")
            vm.removeReaction(before, "👍")
            advanceUntilIdle()
            vm.getItems().single() shouldBeSameInstanceAs before
            (before.reactions?.size ?: 0) shouldBe 0
        }
    }

    test("Delete on a sent message that fails not-connected keeps the row") {
        runTest {
            val sent = h.served(6L)
            val vm = h.open(seeded = listOf(sent)) { repo ->
                runBlocking { whenever(repo.deleteMessage(anyOrNull())).thenReturn(Result.failure(notConnected)) }
            }
            advanceUntilIdle()
            vm.deleteMessage(sent)
            advanceUntilIdle()
            vm.getItems().single() shouldBeSameInstanceAs sent
            h.sdk.discards.size shouldBe 0
        }
    }

    test("a sent row is never treated as unsent, so it keeps today's options") {
        h.open()
        OfflineRowState.isUnsent(h.served(7L), offlineOn = true) shouldBe false
    }
})
