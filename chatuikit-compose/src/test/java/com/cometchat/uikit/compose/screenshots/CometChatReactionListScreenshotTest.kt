package com.cometchat.uikit.compose.screenshots

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.ReactionsRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.reactionlist.ui.CometChatReactionList
import com.cometchat.uikit.compose.presentation.utils.RoborazziConfig
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme
import com.cometchat.uikit.core.domain.usecase.FetchReactionsUseCase
import com.cometchat.uikit.core.domain.usecase.RemoveReactionUseCase
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatReactionListViewModel
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot layer for the compose reaction list — it had no baselines of any kind.
 *
 * The ViewModel is given a fetch that returns rows directly, so these capture the
 * loaded list rather than a shimmer. Without it the SDK call throws offline and the
 * list never leaves its loading state.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatReactionListScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "../screenshot-gallery/compose/reactionlist"
        )
    )

    private companion object {
        val canvasLight = Color(0xFFF4F4F4)
        val canvasDark = Color(0xFF121212)
        const val THUMBS_UP = "👍"
        const val HEART = "❤️"
    }

    private lateinit var cometChat: MockedStatic<CometChat>

    @Before
    fun stubLoggedInUser() {
        val me = MockFactory.createUser(uid = "me", name = "Me")
        cometChat = Mockito.mockStatic(CometChat::class.java)
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)
    }

    @After fun closeStatic() = cometChat.close()

    private class OnePageFetch(private val rows: List<Reaction>) : FetchReactionsUseCase(mock()) {
        private var served = false
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

    private fun capture(
        dark: Boolean = false,
        rtl: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                CometChatTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (dark) canvasDark else canvasLight)
                    ) { content() }
                }
                }
            }
        }
        scenario.onActivity { activity ->
            activity.window.decorView
                .findViewById<ViewGroup>(android.R.id.content)
                .getChildAt(0)
                .captureRoboImage(roborazziOptions = RoborazziConfig.options())
        }
        scenario.close()
    }

    private fun list(rows: List<Reaction>): @Composable () -> Unit = {
        CometChatReactionList(
            baseMessage = MockFactory.createTextMessage(sentAt = MockFactory.FIXED_SENT_AT, text = "hello"),
            viewModel = CometChatReactionListViewModel(
                OnePageFetch(rows), InertRemove(), enableListeners = false,
            ),
        )
    }

    private fun reactors(vararg pairs: Pair<String, String>): List<Reaction> =
        pairs.mapIndexed { i, (name, emoji) ->
            MockFactory.createReaction(uid = "u$i", name = name, emoji = emoji, reactedAt = i.toLong())
        }

    @Test fun singleReactor() = capture(content = list(reactors("Alice" to THUMBS_UP)))

    @Test fun severalReactors() = capture(
        content = list(reactors("Alice" to THUMBS_UP, "Bob" to THUMBS_UP, "Carol" to HEART)),
    )

    @Test fun severalReactors_dark() = capture(
        dark = true,
        content = list(reactors("Alice" to THUMBS_UP, "Bob" to THUMBS_UP, "Carol" to HEART)),
    )

    @Test fun mixedEmoji() = capture(
        content = list(reactors("Alice" to THUMBS_UP, "Bob" to HEART)),
    )

    // ── right-to-left ───────────────────────────────────────────────────────

    /** Each row is an avatar, a name and an emoji, and the row mirrors as a whole. */
    @Test fun severalReactors_rtl() = capture(
        rtl = true,
        content = list(reactors("Alice" to THUMBS_UP, "Bob" to THUMBS_UP, "Carol" to HEART)),
    )
}
