package com.cometchat.uikit.core

import com.cometchat.uikit.core.events.CometChatEvents
import io.kotest.core.config.AbstractProjectConfig
import kotlinx.coroutines.Dispatchers

/**
 * Kotest project-level configuration.
 *
 * Sets concurrentSpecs = 1 to prevent multiple specs from running in parallel.
 * This avoids "Dispatchers.Main is used concurrently with setting it" errors
 * that occur when one spec's afterSpec (resetMain) races with another spec's
 * beforeSpec (setMain).
 *
 * Pins the [CometChatEvents] emission dispatcher to [Dispatchers.Unconfined] for the whole
 * test JVM. In production the bus launches every emit on `Dispatchers.Default`, so an event a
 * ViewModel emits at the end of a test (an outgoing call ending, say) is still being delivered
 * after that test has returned. If a collector left alive by an earlier spec is suspended on
 * `Dispatchers.Main`, resuming it dispatches onto whichever Main the *next* spec has installed;
 * an `UnconfinedTestDispatcher` refuses a dispatch from another thread, and the resulting
 * uncaught exception fails the next `runTest` as `UncaughtExceptionsBeforeTest`. Emitting on
 * the caller's thread makes every emission finish inside the test that caused it.
 */
class KotestProjectConfig : AbstractProjectConfig() {
    override val concurrentSpecs: Int = 1

    override suspend fun beforeProject() {
        CometChatEvents.setEmitDispatcherForTesting(Dispatchers.Unconfined)
    }
}
