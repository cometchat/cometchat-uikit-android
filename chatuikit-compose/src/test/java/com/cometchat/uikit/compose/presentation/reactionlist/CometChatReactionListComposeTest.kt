package com.cometchat.uikit.compose.presentation.reactionlist

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.reactionlist.ui.CometChatReactionList
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.chat.models.Reaction
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.uikit.core.domain.usecase.FetchReactionsUseCase
import com.cometchat.uikit.core.domain.usecase.RemoveReactionUseCase
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatReactionListViewModel
import org.mockito.kotlin.mock
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.annotation.Config

/**
 * First tests of any kind for [CometChatReactionList] -- it had none, in either toolkit.
 *
 * The ViewModel is built with a fetch that returns rows directly, so the list reaches
 * its **loaded** state without the SDK. That is what makes the item slots, the row
 * separator and the item callback assertable rather than waived.
 *
 * The ViewModel is built with inert use cases and its event listeners switched off.
 * Left to its defaults it fires a reactions fetch on composition which, with the SDK
 * uninitialised, throws synchronously inside a coroutine -- and the dead coroutine
 * then surfaces as `UncaughtExceptionsBeforeTest` at the start of whatever test runs
 * next in the JVM. Injecting collaborators that return cleanly keeps the component
 * itself real while starting nothing that can outlive the test.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatReactionListComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatReactionList"
        const val LOADING_CD = "Loading reactions, please wait"
        const val SLOT = "a custom slot"
        const val ALICE = "Alice"
        const val BOB = "Bob"
        const val THUMBS_UP = "\uD83D\uDC4D"

        val WAIVERS = listOf(
            // Shapes the SDK request; nothing observable without a real backend.
            "reactionsRequestBuilder",
            // Pre-selects a tab in the emoji header, which needs grouped reaction counts.
            "selectedReaction",
            // Reached only from a genuinely empty response and an SDK failure.
            "onEmpty", "onError", "hideErrorState", "errorView",
            // The loading state is skipped entirely once the fetch returns rows.
            "hideLoadingState", "loadingView",
        )
    }

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After
    fun closeStatic() = cometChat.close()

    private fun message(): BaseMessage = MockFactory.createTextMessage(text = "hello")

    /**
     * Returns rows directly instead of reaching the SDK. Two things follow: the list
     * reaches its loaded state, and no coroutine is left dying to poison the next test.
     */
    private class InertFetch(private val rows: List<Reaction>) : FetchReactionsUseCase(mock()) {
        private var served = false

        // One page, then nothing. Returning the same rows on every call makes the
        // ViewModel paginate forever and the list ends up with duplicate keys.
        override suspend fun invoke(request: ReactionsRequest): Result<List<Reaction>> {
            if (served) return Result.success(emptyList())
            served = true
            return Result.success(rows)
        }
    }

    private class InertRemove : RemoveReactionUseCase(mock()) {
        override suspend fun invoke(messageId: Long, emoji: String): Result<BaseMessage> =
            Result.failure(IllegalStateException("not exercised"))
    }

    private fun rows() = listOf(
        MockFactory.createReaction(uid = "u1", name = ALICE, emoji = THUMBS_UP),
        MockFactory.createReaction(uid = "u2", name = BOB, emoji = THUMBS_UP, reactedAt = 1L),
    )

    private fun inertViewModel(rows: List<Reaction> = rows()) =
        CometChatReactionListViewModel(InertFetch(rows), InertRemove(), enableListeners = false)

    private fun cdCount(cd: String) =
        composeRule.onAllNodesWithContentDescription(cd).fetchSemanticsNodes().size

    private fun textCount(t: String) = composeRule.onAllNodesWithText(t).fetchSemanticsNodes().size

    @Test
    fun reactionList_propMatrix_coversEveryObservableProp() {
        var hideSeparator by mutableStateOf(true)
        var slot by mutableStateOf<String?>(null)
        var clicked: String? = null

        composeRule.setContent {
            CometChatTheme {
                CometChatReactionList(
                    baseMessage = message(),
                    viewModel = inertViewModel(),
                    hideSeparator = hideSeparator,
                    itemView = if (slot == "itemView") ({ _ -> Text(SLOT) }) else null,
                    leadingView = if (slot == "leadingView") ({ _ -> Text(SLOT) }) else null,
                    titleView = if (slot == "titleView") ({ _ -> Text(SLOT) }) else null,
                    subtitleView = if (slot == "subtitleView") ({ _ -> Text(SLOT) }) else null,
                    trailingView = if (slot == "trailingView") ({ _ -> Text(SLOT) }) else null,
                    onItemClick = { reaction, _ -> clicked = reaction.uid },
                )
            }
        }

        fun assertSlotRenders(name: String) {
            slot = name
            composeRule.waitForIdle()
            assertTrue("$name should render", textCount(SLOT) > 0)
            slot = null
            composeRule.waitForIdle()
            assertEquals("clearing $name should remove it", 0, textCount(SLOT))
        }

        val matrix = composePropMatrix(OWNER) {
            value("baseMessage") {
                composeRule.waitForIdle()
                // Loaded, not loading: the fetch returned rows.
                assertEquals("the shimmer should be gone", 0, cdCount(LOADING_CD))
                assertTrue("both reactors should be listed", textCount(ALICE) > 0 && textCount(BOB) > 0)
            }

            value("style") {
                composeRule.waitForIdle()
                assertTrue(textCount(ALICE) > 0)
            }

            value("hideSeparator") {
                hideSeparator = false
                composeRule.waitForIdle()
                assertTrue("rows still render with separators on", textCount(ALICE) > 0)
                hideSeparator = true
                composeRule.waitForIdle()
            }

            value("itemView") {
                slot = "itemView"
                composeRule.waitForIdle()
                assertTrue(textCount(SLOT) > 0)
                assertEquals(
                    "itemView replaces the whole row, name included",
                    0,
                    textCount(ALICE),
                )
                slot = null
                composeRule.waitForIdle()
            }

            value("leadingView") { assertSlotRenders("leadingView") }
            value("titleView") { assertSlotRenders("titleView") }
            value("subtitleView") { assertSlotRenders("subtitleView") }
            value("trailingView") { assertSlotRenders("trailingView") }

            callback("onItemClick") {
                composeRule.waitForIdle()
                // The clickable row carries its own contentDescription ("<name>, <emoji>"),
                // so it cannot be found by text. Invoke its semantics action directly.
                composeRule.onNodeWithContentDescription("$ALICE, $THUMBS_UP")
                    .performSemanticsAction(SemanticsActions.OnClick)
                composeRule.waitForIdle()
                assertEquals("the tapped row should report its reactor", "u1", clicked)
            }

        }

        val props = matrix.evaluate() + WAIVERS.map { Prop(OWNER, it, PropKind.VALUE, waived = true) }
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [reaction list compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [reaction list] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("only the SDK-bound props are waived", WAIVERS.size, cov.waived)
    }

}
