package com.cometchat.uikit.compose.presentation.stickerkeyboard

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.compose.presentation.stickerkeyboard.style.CometChatStickerKeyboardStyle
import com.cometchat.uikit.compose.presentation.stickerkeyboard.ui.CometChatStickerKeyboard
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.domain.model.Sticker
import com.cometchat.uikit.core.domain.model.StickerSet
import com.cometchat.uikit.core.domain.repository.StickerRepository
import com.cometchat.uikit.core.domain.usecase.GetStickersUseCase
import com.cometchat.uikit.core.viewmodel.CometChatStickerKeyboardViewModel
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Property (prop-matrix) layer for [CometChatStickerKeyboard] — the largest of the
 * satellite surfaces and the Compose mirror of the View
 * `CometChatStickerKeyboardPropMatrixTest`.
 *
 * Eleven integrator params, `modifier` excluded by [Denominator], leaving ten, and
 * nothing waived.
 *
 * Every prop but `style` is gated on which of the keyboard's four states is showing, and
 * the keyboard does not take a state — it takes a ViewModel and reads one. So the fixture
 * is four ViewModels rather than four state values, each built on its own fake
 * [GetStickersUseCase]: one that succeeds with two sets, one that succeeds with none, one
 * that fails, and one that never returns. Swapping the `viewModel` param between them is
 * what drives the state machine, which is also why `viewModel` needs no separate
 * assertion of its own — every other entry depends on it having been honoured.
 *
 * The three `hide*` flags and the three custom-view slots are pinned as pairs on purpose:
 * hiding a state and replacing it both end in "the default state view is gone", and only
 * asserting that the replacement arrived tells them apart.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatStickerKeyboardComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatStickerKeyboard"

        const val LOADING = "Loading stickers, please wait"
        const val EMPTY = "No Stickers Available"
        const val ERROR = "Looks like something went wrong.\n Please try again."
        const val RETRY = "Retry"

        const val FIRST_STICKER = "wave"
        const val SECOND_SET = "Cats"

        const val CUSTOM_LOADING = "still fetching"
        const val CUSTOM_EMPTY = "no stickers here"
        const val CUSTOM_ERROR = "that did not work"

        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
        val MAGENTA = Color(0xFFFF00FF)
    }

    /** A repository the fakes never reach — the use case overrides `invoke` outright. */
    private object UnusedRepository : StickerRepository {
        override suspend fun getStickers(): Result<List<StickerSet>> =
            Result.success(emptyList())
    }

    private class FixedStickers(
        private val result: Result<List<StickerSet>>,
    ) : GetStickersUseCase(UnusedRepository) {
        override suspend fun invoke(): Result<List<StickerSet>> = result
    }

    /** Never completes, so the keyboard stays in its Loading state for that entry. */
    private class NeverReturns : GetStickersUseCase(UnusedRepository) {
        private val pending = CompletableDeferred<Result<List<StickerSet>>>()
        override suspend fun invoke(): Result<List<StickerSet>> = pending.await()
    }

    private fun sticker(name: String, set: String) =
        Sticker(name = name, url = "https://example.invalid/$name.png", setName = set)

    private fun stickerSets(): List<StickerSet> = listOf(
        StickerSet(
            name = "Dogs",
            stickers = listOf(sticker(FIRST_STICKER, "Dogs"), sticker("sit", "Dogs")),
            iconUrl = "https://example.invalid/dogs.png",
        ),
        StickerSet(
            name = SECOND_SET,
            stickers = listOf(sticker("purr", SECOND_SET)),
            iconUrl = "https://example.invalid/cats.png",
        ),
    )

    private fun viewModelWith(useCase: GetStickersUseCase) =
        CometChatStickerKeyboardViewModel(useCase)

    /** See the bubble matrices: `captureToImage()` has no window here. */
    private fun paintedColours(): Set<Int> {
        val view = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        view.draw(Canvas(bitmap))
        val seen = HashSet<Int>()
        for (y in 0 until bitmap.height step 2) {
            for (x in 0 until bitmap.width step 2) seen += bitmap.getPixel(x, y)
        }
        return seen
    }

    private fun textCount(text: String) =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size

    private fun descriptionCount(description: String) =
        composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().size

    @Test
    fun stickerKeyboard_propMatrix_coversEveryObservableProp() {
        val contentVm = viewModelWith(FixedStickers(Result.success(stickerSets())))
        val emptyVm = viewModelWith(FixedStickers(Result.success(emptyList())))
        val errorVm = viewModelWith(
            FixedStickers(Result.failure(CometChatException("ERR", "stickers unavailable")))
        )
        val loadingVm = viewModelWith(NeverReturns())

        var vm by mutableStateOf(contentVm)
        var styleOverride by mutableStateOf<CometChatStickerKeyboardStyle?>(null)
        var hideLoadingState by mutableStateOf(false)
        var hideEmptyState by mutableStateOf(false)
        var hideErrorState by mutableStateOf(false)
        var useCustomLoading by mutableStateOf(false)
        var useCustomEmpty by mutableStateOf(false)
        var useCustomError by mutableStateOf(false)

        var clickedSticker: Sticker? = null
        var reportedError: CometChatException? = null

        // `default()` is @Composable — it reads CometChatTheme — so it can only be
        // evaluated inside the composition. Captured for the style entry to copy() from.
        var defaultStyle: CometChatStickerKeyboardStyle? = null

        composeRule.setContent {
            CometChatTheme {
                defaultStyle = CometChatStickerKeyboardStyle.default()
                CometChatStickerKeyboard(
                    viewModel = vm,
                    style = styleOverride ?: defaultStyle!!,
                    hideLoadingState = hideLoadingState,
                    hideEmptyState = hideEmptyState,
                    hideErrorState = hideErrorState,
                    loadingView = if (useCustomLoading) ({ Text(CUSTOM_LOADING) }) else null,
                    emptyView = if (useCustomEmpty) ({ Text(CUSTOM_EMPTY) }) else null,
                    errorView = if (useCustomError) ({ _ -> Text(CUSTOM_ERROR) }) else null,
                    onStickerClick = { clickedSticker = it },
                    onError = { reportedError = it },
                )
            }
        }
        composeRule.waitForIdle()

        /** Points the keyboard at [target] and settles the recomposition. */
        fun show(target: CometChatStickerKeyboardViewModel) {
            vm = target
            composeRule.waitForIdle()
        }

        val matrix = composePropMatrix(OWNER) {
            value("viewModel") {
                // The content ViewModel's sets are what reaches the screen — which is the
                // only way to tell "the keyboard used the one it was given" from "it built
                // its own through the factory".
                show(contentVm)
                composeRule.onNodeWithContentDescription(FIRST_STICKER).assertIsDisplayed()
                show(emptyVm)
                assertEquals(
                    "swapping the ViewModel should swap the state with it",
                    1,
                    textCount(EMPTY),
                )
                show(contentVm)
            }

            value("style") {
                styleOverride = defaultStyle!!.copy(backgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the supplied backgroundColor should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                styleOverride = null
                composeRule.waitForIdle()
            }

            value("hideLoadingState") {
                show(loadingVm)
                assertEquals("the spinner shows by default", 1, descriptionCount(LOADING))
                hideLoadingState = true
                composeRule.waitForIdle()
                assertEquals(
                    "hiding the loading state should leave nothing in its place",
                    0,
                    descriptionCount(LOADING),
                )
                hideLoadingState = false
                show(contentVm)
            }

            value("hideEmptyState") {
                show(emptyVm)
                assertEquals(1, textCount(EMPTY))
                hideEmptyState = true
                composeRule.waitForIdle()
                assertEquals(0, textCount(EMPTY))
                hideEmptyState = false
                show(contentVm)
            }

            value("hideErrorState") {
                show(errorVm)
                assertEquals(1, textCount(ERROR))
                hideErrorState = true
                composeRule.waitForIdle()
                assertEquals(0, textCount(ERROR))
                hideErrorState = false
                show(contentVm)
            }

            slot("loadingView") {
                show(loadingVm)
                useCustomLoading = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(CUSTOM_LOADING).assertIsDisplayed()
                assertEquals(
                    "a supplied loading view replaces the default rather than joining it",
                    0,
                    descriptionCount(LOADING),
                )
                useCustomLoading = false
                show(contentVm)
            }

            slot("emptyView") {
                show(emptyVm)
                useCustomEmpty = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(CUSTOM_EMPTY).assertIsDisplayed()
                assertEquals(0, textCount(EMPTY))
                useCustomEmpty = false
                show(contentVm)
            }

            slot("errorView") {
                show(errorVm)
                useCustomError = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(CUSTOM_ERROR).assertIsDisplayed()
                assertEquals(0, textCount(ERROR))
                assertEquals(
                    "the default retry affordance goes with the default error view",
                    0,
                    textCount(RETRY),
                )
                useCustomError = false
                show(contentVm)
            }

            callback("onStickerClick") {
                show(contentVm)
                composeRule.onNodeWithContentDescription(FIRST_STICKER).performClick()
                composeRule.waitForIdle()
                assertEquals(
                    "the tapped sticker is the one handed back",
                    FIRST_STICKER,
                    clickedSticker?.name,
                )
            }

            callback("onError") {
                // Fired from the state itself rather than from a tap — reaching the Error
                // state is the whole trigger.
                show(errorVm)
                assertNotNull("an errored fetch should reach the integrator", reportedError)
                show(contentVm)
            }
        }

        val props = matrix.evaluate()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [sticker keyboard prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [sticker keyboard] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("the sticker keyboard has nothing worth waiving", 0, cov.waived)
    }
}
