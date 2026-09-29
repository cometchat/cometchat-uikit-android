package com.cometchat.uikit.core.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.cometchat.uikit.core.constants.SearchFilter
import com.cometchat.uikit.core.constants.SearchMode
import com.cometchat.uikit.core.constants.SearchScope
import com.cometchat.uikit.core.domain.usecase.FetchConversationsUseCase
import com.cometchat.uikit.core.domain.usecase.FetchMessagesUseCase
import com.cometchat.uikit.core.viewmodel.CometChatSearchViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Which half of search actually runs: conversations, messages, both, or neither.
 *
 * `getSearchMode` is the router in front of every search. Before a single request goes out it
 * decides, from the query text, the chips the user has selected and the scopes the integrator
 * configured, whether to hit the conversations endpoint, the messages endpoint, both, or stop.
 * Getting it wrong is expensive in two directions: too narrow and the user's results silently go
 * missing, too broad and every keystroke fires a request nobody asked for.
 *
 * The rules it encodes, in the order it applies them:
 *
 * - **Contextual search wins outright.** Once a uid or guid is set the user is searching *inside*
 *   one conversation, so there is nothing to look up in the conversation list and the mode is
 *   always MESSAGES — whatever the text, chips or scopes say.
 * - **Chips override scopes.** A selected chip is an explicit instruction, so it decides the mode
 *   even when the configured scopes would have said otherwise.
 * - **Mixing the two chip families is a conflict, not a union.** Asking for photos *and* unread
 *   conversations describes no coherent result set, so the mode is NONE and nothing is fetched.
 * - **With no chips, the integrator's scopes decide** — including the degenerate case of an empty
 *   scope list, which searches nothing at all rather than quietly defaulting to everything.
 *
 * The filter families: PHOTOS, VIDEOS, DOCUMENTS, LINKS and AUDIO are message filters; GROUPS and
 * UNREAD are conversation filters.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*SearchModeSelectionTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchModeSelectionTest : FunSpec({

    // Every VM here is hosted and cleared. Most of these tests never start a search, but the one
    // that does launches a real fetch on viewModelScope -- left running past the spec, an
    // unstubbed use case returns null and the resulting NPE surfaces against whichever spec runs
    // next rather than this one.
    val stores = mutableListOf<ViewModelStore>()

    beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }

    afterTest {
        stores.forEach { it.clear() }
        stores.clear()
        Dispatchers.resetMain()
    }

    fun searcher(): CometChatSearchViewModel {
        val conversations = mock<FetchConversationsUseCase> {
            onBlocking { invoke(any()) } doReturn Result.success(emptyList())
            on { hasMore() } doReturn false
        }
        val messages = mock<FetchMessagesUseCase> {
            onBlocking { invoke(any()) } doReturn Result.success(emptyList())
            on { hasMore() } doReturn false
        }
        val store = ViewModelStore()
        stores += store
        return ViewModelProvider(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CometChatSearchViewModel(conversations, messages) as T
            }
        )[CometChatSearchViewModel::class.java]
    }

    val noFilters = emptySet<SearchFilter>()
    val messageChip = setOf(SearchFilter.PHOTOS)
    val conversationChip = setOf(SearchFilter.UNREAD)

    // ==================== text alone, decided by the configured scopes ====================

    test("text with both scopes configured searches both halves") {
        searcher().getSearchMode("hello", noFilters) shouldBe SearchMode.BOTH
    }

    test("text with only the conversations scope searches conversations") {
        val vm = searcher()
        vm.setSearchScopes(listOf(SearchScope.CONVERSATIONS))

        vm.getSearchMode("hello", noFilters) shouldBe SearchMode.CONVERSATIONS
    }

    test("text with only the messages scope searches messages") {
        val vm = searcher()
        vm.setSearchScopes(listOf(SearchScope.MESSAGES))

        vm.getSearchMode("hello", noFilters) shouldBe SearchMode.MESSAGES
    }

    test("text with no scopes at all searches nothing") {
        val vm = searcher()
        vm.setSearchScopes(emptyList())

        vm.getSearchMode("hello", noFilters) shouldBe SearchMode.NONE
    }

    test("empty text and no chips searches nothing") {
        searcher().getSearchMode("", noFilters) shouldBe SearchMode.NONE
    }

    // ==================== chips with no text ====================

    test("a conversation chip alone browses conversations") {
        searcher().getSearchMode("", conversationChip) shouldBe SearchMode.CONVERSATIONS
    }

    test("a message chip alone browses messages") {
        searcher().getSearchMode("", messageChip) shouldBe SearchMode.MESSAGES
    }

    test("every message chip routes to messages") {
        val vm = searcher()
        listOf(
            SearchFilter.PHOTOS, SearchFilter.VIDEOS, SearchFilter.DOCUMENTS,
            SearchFilter.LINKS, SearchFilter.AUDIO
        ).forEach { chip ->
            vm.getSearchMode("", setOf(chip)) shouldBe SearchMode.MESSAGES
        }
    }

    test("every conversation chip routes to conversations") {
        val vm = searcher()
        listOf(SearchFilter.GROUPS, SearchFilter.UNREAD).forEach { chip ->
            vm.getSearchMode("", setOf(chip)) shouldBe SearchMode.CONVERSATIONS
        }
    }

    test("several message chips together still route to messages") {
        searcher().getSearchMode("", setOf(SearchFilter.PHOTOS, SearchFilter.VIDEOS)) shouldBe
            SearchMode.MESSAGES
    }

    // ==================== chips with text ====================

    test("text plus a conversation chip searches conversations only") {
        searcher().getSearchMode("hello", conversationChip) shouldBe SearchMode.CONVERSATIONS
    }

    test("text plus a message chip searches messages only") {
        searcher().getSearchMode("hello", messageChip) shouldBe SearchMode.MESSAGES
    }

    test("a chip overrides the configured scopes rather than intersecting with them") {
        val vm = searcher()
        // Scopes say conversations only; the chip says photos, which live in messages.
        vm.setSearchScopes(listOf(SearchScope.CONVERSATIONS))

        vm.getSearchMode("hello", messageChip) shouldBe SearchMode.MESSAGES
    }

    // ==================== mixing the two chip families ====================

    test("chips from both families with text is a conflict and searches nothing") {
        searcher().getSearchMode("hello", messageChip + conversationChip) shouldBe SearchMode.NONE
    }

    test("chips from both families without text is also a conflict") {
        searcher().getSearchMode("", messageChip + conversationChip) shouldBe SearchMode.NONE
    }

    // ==================== contextual search ====================

    test("a uid forces messages whatever the text and chips say") {
        val vm = searcher()
        vm.setUid("partner")

        vm.getSearchMode("", noFilters) shouldBe SearchMode.MESSAGES
        vm.getSearchMode("hello", conversationChip) shouldBe SearchMode.MESSAGES
        vm.getSearchMode("hello", messageChip + conversationChip) shouldBe SearchMode.MESSAGES
    }

    test("a guid forces messages the same way") {
        val vm = searcher()
        vm.setGuid("group_alpha")

        vm.getSearchMode("", conversationChip) shouldBe SearchMode.MESSAGES
    }

    test("a uid beats even an empty scope list") {
        val vm = searcher()
        vm.setSearchScopes(emptyList())
        vm.setUid("partner")

        vm.getSearchMode("hello", noFilters) shouldBe SearchMode.MESSAGES
    }

    test("clearing the uid hands the decision back to the scopes") {
        val vm = searcher()
        vm.setUid("partner")
        vm.setUid(null)

        vm.getSearchMode("hello", noFilters) shouldBe SearchMode.BOTH
    }

    // ==================== which chips the user is offered ====================

    test("a global search offers every chip") {
        searcher().visibleFilters.value shouldBe SearchFilter.entries.toList()
    }

    test("entering a conversation hides the conversation chips") {
        val vm = searcher()
        vm.setUid("partner")

        val visible = vm.visibleFilters.value
        visible shouldContain SearchFilter.PHOTOS
        visible shouldNotContain SearchFilter.GROUPS
        visible shouldNotContain SearchFilter.UNREAD
    }

    test("a guid narrows the chips the same way a uid does") {
        val vm = searcher()
        vm.setGuid("group_alpha")

        vm.visibleFilters.value shouldNotContain SearchFilter.UNREAD
    }

    test("leaving a conversation offers every chip again") {
        val vm = searcher()
        vm.setUid("partner")
        vm.setUid(null)

        vm.visibleFilters.value shouldBe SearchFilter.entries.toList()
    }

    test("a message chip is untouched by entering a conversation") {
        val vm = searcher()
        vm.searchConversationsAndMessages("hello", messageChip)
        vm.setUid("partner")

        vm.selectedFilters.value shouldBe messageChip
    }
})
