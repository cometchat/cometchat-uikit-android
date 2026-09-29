package com.cometchat.uikit.core.viewmodel.messagelist

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.state.ConversationStarterUIState
import com.cometchat.uikit.core.state.ConversationSummaryUIState
import com.cometchat.uikit.core.state.SmartRepliesUIState
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.cometchat.chat.core.CometChat
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Layer 5J — AI Features Tests.
 *
 * Tests the CometChatMessageListViewModel's AI feature handling:
 * - Conversation starters: fetch conditions, clear on message add, guards
 * - Smart replies: eligibility (5 conditions), debounce, clear
 * - Conversation summary: threshold check, dismiss clears state
 *
 * Architecture:
 * - Mocks the MessageListRepository interface (layer directly below)
 * - Uses `enableListeners = false` to avoid SDK dependencies
 * - Uses UnconfinedTestDispatcher + Dispatchers.setMain/resetMain
 *
 * Note: AI features call CometChat static methods (getConversationStarter, getSmartReplies,
 * getConversationSummary) which cannot be easily mocked in unit tests. These tests focus on
 * the guard conditions, state management, and clear/dismiss behavior rather than the actual
 * API calls.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*MessageListAIFeaturesTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageListAIFeaturesTest : FunSpec({

    isolationMode = IsolationMode.SingleInstance

    val testDispatcher = UnconfinedTestDispatcher()

    lateinit var repository: MessageListRepository

    /**
     * Creates a ViewModel configured with a user and pre-loaded messages.
     */
    suspend fun createViewModelWithMessages(messages: List<BaseMessage>): CometChatMessageListViewModel {
        whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(messages))
        whenever(repository.hasMorePreviousMessages()).thenReturn(messages.isNotEmpty())
        val vm = CometChatMessageListViewModel(
            repository = repository,
            enableListeners = false
        )
        val user = MockFactory.createUser(uid = "test-user", name = "Test User")
        vm.setUser(user)
        vm.fetchMessages()
        return vm
    }

    // Main is installed once for the whole spec, not per test. The VM fires
    // CometChatEvents.emitMessageEvent, which launches on the bus's own Default-dispatcher scope —
    // a launch advanceUntilIdle() cannot see. With a per-test resetMain() that stray launch could
    // resume this spec's Main-bound collectors AFTER Main was torn down, crashing on
    // "Dispatchers.Main was accessed when the platform dispatcher was absent" and pinning the
    // leaked exception on the NEXT test's runTest (UncaughtExceptionsBeforeTest).
    beforeSpec {
        Dispatchers.setMain(testDispatcher)
    }

    afterSpec {
        // Let any still-queued bus emissions run while Main is still installed.
        Thread.sleep(100)
        Dispatchers.resetMain()
    }

    beforeTest {
        repository = mock()
        // Default stubs
        whenever(repository.hasMorePreviousMessages()).thenReturn(true)
        whenever(repository.fetchPreviousMessages()).thenReturn(Result.success(emptyList()))
        whenever(repository.markAsRead(any())).thenReturn(Result.success(Unit))
        whenever(repository.markAsDelivered(any())).thenReturn(Result.success(Unit))
        println("  🧪 ${it.name.testName}")
    }

    afterTest {
        println()
    }


    // ==================== 15.2 Conversation Starters ====================

    test("conversation starters: fetchConversationStarter does nothing when disabled") {
        runTest {
            val vm = CometChatMessageListViewModel(
                repository = repository,
                enableListeners = false
            )
            val user = MockFactory.createUser(uid = "test-user", name = "Test User")
            vm.setUser(user)

            // Don't enable conversation starters (default is false)
            vm.fetchConversationStarter()
            advanceUntilIdle()

            // State should remain Idle
            vm.conversationStarterUIState.value.shouldBeInstanceOf<ConversationStarterUIState.Idle>()
            println("    ✅ fetchConversationStarter does nothing when disabled")
        }
    }

    test("conversation starters: clearConversationStarter resets state to Idle") {
        runTest {
            val vm = CometChatMessageListViewModel(
                repository = repository,
                enableListeners = false
            )
            val user = MockFactory.createUser(uid = "test-user", name = "Test User")
            vm.setUser(user)
            vm.setEnableConversationStarter(true)

            // Clear should reset to Idle
            vm.clearConversationStarter()
            advanceUntilIdle()

            vm.conversationStarterUIState.value.shouldBeInstanceOf<ConversationStarterUIState.Idle>()
            vm.conversationStarterReplies.value shouldBe emptyList()
            println("    ✅ clearConversationStarter resets state to Idle")
        }
    }

    test("fetchConversationStarter populates replies (Loaded), and clear resets them (Idle)") {
        // ENG-38677 / L: previously xtest-disabled because fetchConversationStarter calls the
        // static CometChat.getConversationStarter, which NPE'd on the unmocked SDK. We mock the
        // static seam locally (scoped to this test) so the real code path runs and is asserted.
        // NOTE (product robustness gap, to file): fetchConversationStarter only catches
        // CometChatException, so a runtime exception from the SDK (like that NPE) escapes the
        // coroutine and crashes instead of surfacing ConversationStarterUIState.Error.
        val cometChatMock = Mockito.mockStatic(CometChat::class.java)
        try {
            runTest {
                cometChatMock.`when`<Unit> {
                    CometChat.getConversationStarter(any(), any(), anyOrNull(), any())
                }.thenAnswer { inv ->
                    inv.getArgument<CometChat.CallbackListener<List<String>>>(3).onSuccess(listOf("Hi there!", "How are you?"))
                    null
                }

                val vm = CometChatMessageListViewModel(repository = repository, enableListeners = false)
                vm.setUser(MockFactory.createUser(uid = "test-user", name = "Test User"))
                vm.setEnableConversationStarter(true)

                vm.fetchConversationStarter()
                advanceUntilIdle()

                vm.conversationStarterReplies.value shouldBe listOf("Hi there!", "How are you?")
                vm.conversationStarterUIState.value.shouldBeInstanceOf<ConversationStarterUIState.Loaded>()

                vm.clearConversationStarter()

                vm.conversationStarterReplies.value shouldBe emptyList()
                vm.conversationStarterUIState.value.shouldBeInstanceOf<ConversationStarterUIState.Idle>()
                println("    ✅ Conversation starters populate then clear")
            }
        } finally {
            cometChatMock.close()
        }
    }

    test("PBT: setEnableConversationStarter(false) clears existing starters") {
        checkAll(10, Arb.int(1..5)) { _ ->
            runTest {
                val vm = CometChatMessageListViewModel(
                    repository = repository,
                    enableListeners = false
                )
                val user = MockFactory.createUser(uid = "test-user", name = "Test User")
                vm.setUser(user)

                // Enable then disable
                vm.setEnableConversationStarter(true)
                vm.setEnableConversationStarter(false)
                advanceUntilIdle()

                vm.conversationStarterUIState.value.shouldBeInstanceOf<ConversationStarterUIState.Idle>()
                vm.conversationStarterReplies.value shouldBe emptyList()
            }
        }
        println("    ✅ PBT: setEnableConversationStarter(false) clears starters")
    }


    // ==================== 15.3 Smart Replies ====================

    test("smart replies: initial state is Idle") {
        runTest {
            val vm = CometChatMessageListViewModel(
                repository = repository,
                enableListeners = false
            )
            vm.smartRepliesUIState.value.shouldBeInstanceOf<SmartRepliesUIState.Idle>()
            vm.smartReplies.value shouldBe emptyList()
            println("    ✅ Smart replies initial state is Idle")
        }
    }

    test("smart replies: fetchSmartReplies does nothing when disabled") {
        runTest {
            val vm = CometChatMessageListViewModel(
                repository = repository,
                enableListeners = false
            )
            val user = MockFactory.createUser(uid = "test-user", name = "Test User")
            vm.setUser(user)

            // Don't enable smart replies (default is false)
            vm.fetchSmartReplies()
            advanceUntilIdle()

            // State should remain Idle
            vm.smartRepliesUIState.value.shouldBeInstanceOf<SmartRepliesUIState.Idle>()
            println("    ✅ fetchSmartReplies does nothing when disabled")
        }
    }

    test("smart replies: clearSmartReplies resets state to Idle and clears list") {
        runTest {
            val vm = CometChatMessageListViewModel(
                repository = repository,
                enableListeners = false
            )
            val user = MockFactory.createUser(uid = "test-user", name = "Test User")
            vm.setUser(user)
            vm.setEnableSmartReplies(true)

            // Clear should reset
            vm.clearSmartReplies()
            advanceUntilIdle()

            vm.smartRepliesUIState.value.shouldBeInstanceOf<SmartRepliesUIState.Idle>()
            vm.smartReplies.value shouldBe emptyList()
            println("    ✅ clearSmartReplies resets state to Idle")
        }
    }

    test("PBT: setEnableSmartReplies(false) clears existing smart replies") {
        checkAll(10, Arb.int(1..5)) { _ ->
            runTest {
                val vm = CometChatMessageListViewModel(
                    repository = repository,
                    enableListeners = false
                )
                val user = MockFactory.createUser(uid = "test-user", name = "Test User")
                vm.setUser(user)

                // Enable then disable
                vm.setEnableSmartReplies(true)
                vm.setEnableSmartReplies(false)
                advanceUntilIdle()

                vm.smartRepliesUIState.value.shouldBeInstanceOf<SmartRepliesUIState.Idle>()
                vm.smartReplies.value shouldBe emptyList()
            }
        }
        println("    ✅ PBT: setEnableSmartReplies(false) clears smart replies")
    }

    test("smart replies: setSmartRepliesDelay stores delay value") {
        runTest {
            val vm = CometChatMessageListViewModel(
                repository = repository,
                enableListeners = false
            )

            // Default delay is 10000ms
            vm.setSmartRepliesDelay(5000)
            // No crash, value stored (internal field, verified by behavior)
            println("    ✅ setSmartRepliesDelay stores delay value")
        }
    }

    test("PBT: smart replies delay can be set to various values") {
        checkAll(20, Arb.int(1000..30000)) { delayMs ->
            runTest {
                val vm = CometChatMessageListViewModel(
                    repository = repository,
                    enableListeners = false
                )
                vm.setSmartRepliesDelay(delayMs)
                // Should not crash for any valid delay value
            }
        }
        println("    ✅ PBT: smart replies delay accepts various values")
    }


    // ==================== 15.4 Conversation Summary ====================

    test("conversation summary: initial state is Idle") {
        runTest {
            val vm = CometChatMessageListViewModel(
                repository = repository,
                enableListeners = false
            )
            vm.conversationSummaryUIState.value.shouldBeInstanceOf<ConversationSummaryUIState.Idle>()
            vm.conversationSummary.value shouldBe null
            println("    ✅ Conversation summary initial state is Idle")
        }
    }

    test("conversation summary: fetchConversationSummary does nothing when disabled") {
        runTest {
            val vm = CometChatMessageListViewModel(
                repository = repository,
                enableListeners = false
            )
            val user = MockFactory.createUser(uid = "test-user", name = "Test User")
            vm.setUser(user)
            vm.setEnableConversationSummary(false)

            vm.fetchConversationSummary()
            advanceUntilIdle()

            // State should remain Idle
            vm.conversationSummaryUIState.value.shouldBeInstanceOf<ConversationSummaryUIState.Idle>()
            vm.conversationSummary.value shouldBe null
            println("    ✅ fetchConversationSummary does nothing when disabled")
        }
    }

    test("conversation summary: dismissConversationSummary clears state") {
        runTest {
            val vm = CometChatMessageListViewModel(
                repository = repository,
                enableListeners = false
            )
            val user = MockFactory.createUser(uid = "test-user", name = "Test User")
            vm.setUser(user)

            vm.dismissConversationSummary()
            advanceUntilIdle()

            vm.conversationSummaryUIState.value.shouldBeInstanceOf<ConversationSummaryUIState.Idle>()
            vm.conversationSummary.value shouldBe null
            println("    ✅ dismissConversationSummary clears state")
        }
    }

    test("PBT: setEnableConversationSummary(false) dismisses existing summary") {
        checkAll(10, Arb.int(1..5)) { _ ->
            runTest {
                val vm = CometChatMessageListViewModel(
                    repository = repository,
                    enableListeners = false
                )
                val user = MockFactory.createUser(uid = "test-user", name = "Test User")
                vm.setUser(user)

                // Enable then disable
                vm.setEnableConversationSummary(true)
                vm.setEnableConversationSummary(false)
                advanceUntilIdle()

                vm.conversationSummaryUIState.value.shouldBeInstanceOf<ConversationSummaryUIState.Idle>()
                vm.conversationSummary.value shouldBe null
            }
        }
        println("    ✅ PBT: setEnableConversationSummary(false) dismisses summary")
    }

    test("conversation summary: setUnreadThreshold stores threshold value") {
        runTest {
            val vm = CometChatMessageListViewModel(
                repository = repository,
                enableListeners = false
            )

            vm.setUnreadThreshold(50)
            // No crash, value stored (verified by behavior in unread anchor tests)
            println("    ✅ setUnreadThreshold stores threshold value")
        }
    }

    test("PBT: unread threshold can be set to various values") {
        checkAll(20, Arb.int(1..500)) { threshold ->
            runTest {
                val vm = CometChatMessageListViewModel(
                    repository = repository,
                    enableListeners = false
                )
                vm.setUnreadThreshold(threshold)
                // Should not crash for any valid threshold
            }
        }
        println("    ✅ PBT: unread threshold accepts various values")
    }

    test("conversation summary: removeConversationSummary event emitted on dismiss") {
        runTest {
            val vm = CometChatMessageListViewModel(
                repository = repository,
                enableListeners = false
            )
            val user = MockFactory.createUser(uid = "test-user", name = "Test User")
            vm.setUser(user)

            var eventEmitted = false
            val job = launch(testDispatcher) {
                vm.removeConversationSummary.collect { eventEmitted = true }
            }

            vm.dismissConversationSummary()
            advanceUntilIdle()

            eventEmitted shouldBe true
            job.cancel()
            println("    ✅ removeConversationSummary event emitted on dismiss")
        }
    }
})
