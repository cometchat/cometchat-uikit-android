package com.cometchat.uikit.core.testutils

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.internal.MainDispatcherFactory
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * Gives the unit-test JVM a Main dispatcher to fall back to after `Dispatchers.resetMain()`.
 *
 * There is no main looper here, so the Android Main dispatcher fails to load and, between
 * tests, `Dispatchers.Main` is absent. A ViewModel an earlier test left alive still collects
 * the shared [com.cometchat.uikit.core.events.CometChatEvents] flows on Main; when a later test
 * emits an event (the emit runs on `Dispatchers.Default`, after the test has returned), resuming
 * that collector threw, and the exception failed whichever `runTest` came next.
 *
 * `kotlinx-coroutines-test` uses the highest-priority factory after its own as the dispatcher
 * `resetMain()` restores, so this one sits just above Android's. Registered in
 * `META-INF/services`; the test task turns off the fast service loader so the file is read.
 *
 * This covers the gap *between* tests. An emit that lands while the next spec has already set
 * its own test Main is handled by [com.cometchat.uikit.core.KotestProjectConfig], which makes
 * the event bus emit synchronously in unit tests.
 */
@OptIn(InternalCoroutinesApi::class)
class UnitTestMainDispatcherFactory : MainDispatcherFactory {

    override val loadPriority: Int = Int.MAX_VALUE / 2 + 1

    override fun createDispatcher(allFactories: List<MainDispatcherFactory>): MainCoroutineDispatcher =
        UnitTestMainDispatcher

    private object UnitTestMainDispatcher : MainCoroutineDispatcher() {
        private val thread = Executors.newSingleThreadExecutor { r ->
            Thread(r, "unit-test-main").apply { isDaemon = true }
        }.asCoroutineDispatcher()

        override val immediate: MainCoroutineDispatcher get() = this

        override fun dispatch(context: CoroutineContext, block: Runnable) = thread.dispatch(context, block)

        override fun toString(): String = "UnitTestMain"
    }
}
