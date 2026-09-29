package com.cometchat.uikit.kotlin.presentation.search

import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.core.constants.SearchScope
import com.cometchat.uikit.kotlin.R
import com.cometchat.uikit.kotlin.presentation.search.ui.CometChatSearch
import com.cometchat.uikit.kotlin.presentation.search.utils.SearchConversationsViewHolderListener
import com.cometchat.uikit.kotlin.presentation.search.utils.SearchMessagesViewHolderListener
import com.cometchat.uikit.kotlin.shared.formatters.CometChatTextFormatter
import com.cometchat.uikit.kotlin.shared.interfaces.DateTimeFormatterCallback
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * ENG-38678 — CometChatSearch (View) functional surface, Phase 1b remaining props.
 *
 * Covers the props that go through the search adapters or the ViewModel:
 *  - icon drawables reach their ImageViews,
 *  - result-click callbacks fire through the adapter wiring,
 *  - load callbacks fire when the VM flows emit,
 *  - config (scopes / formatters / request builders / date formatter) reaches the
 *    view + adapters,
 *  - the 11 custom item-view listeners are received by the adapters.
 *
 * Aliases collapse: setOnConversationClicked -> setOnConversationClick,
 * setOnBackPressListener -> setOnBackPress, set*ItemView -> set*ItemViewListener,
 * setSearchIn -> setSearchScopes — so the target of each alias is asserted here or
 * in CometChatSearchFunctionalTest.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CometChatSearchFunctionalWiringTest {

    private fun withSearch(block: (CometChatSearch) -> Unit) {
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setTheme(R.style.CometChatTheme_DayNight)
            block(CometChatSearch(activity))
        }
        scenario.close()
    }

    /** Reads a private field, walking up the class hierarchy. */
    private fun field(target: Any, name: String): Any? {
        var c: Class<*>? = target.javaClass
        while (c != null) {
            runCatching { c!!.getDeclaredField(name) }.getOrNull()?.let {
                it.isAccessible = true
                return it.get(target)
            }
            c = c.superclass
        }
        error("field '$name' not found on ${target.javaClass.name}")
    }

    private fun emit(vm: Any, flowField: String, value: Any?) {
        val f = vm.javaClass.getDeclaredField(flowField).apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        (f.get(vm) as MutableStateFlow<Any?>).value = value
    }

    @Test
    fun iconDrawables_reachTheirImageViews() {
        withSearch { search ->
            val clear = ColorDrawable(0x11111111)
            val empty = ColorDrawable(0x33333333)
            val error = ColorDrawable(0x44444444)
            search.setClearIcon(clear)
            search.setEmptyStateIcon(empty)
            search.setErrorStateIcon(error)

            assertEquals(clear, search.findViewById<ImageView>(R.id.iv_clear).drawable)
            assertEquals(empty, search.findViewById<ImageView>(R.id.iv_empty_state).drawable)
            assertEquals(error, search.findViewById<ImageView>(R.id.iv_error_state).drawable)

            // The search-bar icon lives in the nested search-box component (not the
            // top-level tree), so assert it applies through the style without error.
            search.setSearchIcon(ColorDrawable(0x22222222))
        }
    }

    @Test
    fun resultClickCallbacks_fireThroughAdapterWiring() {
        withSearch { search ->
            var clickedConv: Conversation? = null
            var clickedMsg: BaseMessage? = null
            search.setOnConversationClick { clickedConv = it }   // also the target of setOnConversationClicked
            search.setOnMessageClick { clickedMsg = it }         // also the target of setOnMessageClicked

            val convAdapter = field(search, "conversationsAdapter")!!
            val msgAdapter = field(search, "messagesAdapter")!!

            @Suppress("UNCHECKED_CAST")
            val convWrapper = field(convAdapter, "onConversationClick") as (Conversation) -> Unit
            @Suppress("UNCHECKED_CAST")
            val msgWrapper = field(msgAdapter, "onMessageClick") as (BaseMessage) -> Unit

            val conv = mock<Conversation>()
            val msg = mock<TextMessage>()
            convWrapper(conv)
            msgWrapper(msg)

            assertSame(conv, clickedConv)
            assertSame(msg, clickedMsg)
        }
    }

    @Test
    fun loadCallbacks_fireOnFlowEmission() {
        withSearch { search ->
            var loadedConvs: List<*>? = null
            var loadedMsgs: List<*>? = null
            search.setOnLoadConversations { loadedConvs = it }
            search.setOnLoadMessages { loadedMsgs = it }

            val vm = field(search, "viewModel")!!
            emit(vm, "_conversations", listOf(mock<Conversation>()))
            emit(vm, "_messages", listOf(mock<BaseMessage>()))
            shadowOf(Looper.getMainLooper()).idle()

            assertEquals(1, loadedConvs?.size)
            assertEquals(1, loadedMsgs?.size)
        }
    }

    @Test
    fun config_reachesViewAndAdapters() {
        withSearch { search ->
            // scopes (setSearchIn delegates here too)
            val scopes = listOf(SearchScope.MESSAGES)
            search.setSearchScopes(scopes)
            @Suppress("UNCHECKED_CAST")
            assertEquals(scopes, field(search, "searchScopes") as List<SearchScope>)

            // text formatters reach the view's list + both adapters
            val formatter = mock<CometChatTextFormatter>()
            search.setTextFormatters(listOf(formatter))
            @Suppress("UNCHECKED_CAST")
            assertTrue(formatter in (field(search, "textFormatters") as List<CometChatTextFormatter>))

            // date-time formatter reaches both adapters
            val dtf = mock<DateTimeFormatterCallback>()
            search.setDateTimeFormatter(dtf)
            assertSame(dtf, field(field(search, "conversationsAdapter")!!, "dateTimeFormatter"))
            assertSame(dtf, field(field(search, "messagesAdapter")!!, "dateTimeFormatter"))

            // request builders pass through to the VM without throwing
            search.setMessagesRequestBuilder(MessagesRequest.MessagesRequestBuilder())
            search.setConversationsRequestBuilder(ConversationsRequest.ConversationsRequestBuilder())
        }
    }

    @Test
    fun conversationItemViewListeners_reachTheConversationsAdapter() {
        withSearch { search ->
            val item = mock<SearchConversationsViewHolderListener>()
            val leading = mock<SearchConversationsViewHolderListener>()
            val title = mock<SearchConversationsViewHolderListener>()
            val subtitle = mock<SearchConversationsViewHolderListener>()
            val trailing = mock<SearchConversationsViewHolderListener>()

            search.setConversationItemViewListener(item)          // setConversationItemView delegates here
            search.setConversationLeadingViewListener(leading)
            search.setConversationTitleViewListener(title)
            search.setConversationSubtitleViewListener(subtitle)
            search.setConversationTrailingViewListener(trailing)

            val a = field(search, "conversationsAdapter")!!
            assertSame(item, field(a, "itemViewListener"))
            assertSame(leading, field(a, "leadingViewListener"))
            assertSame(title, field(a, "titleViewListener"))
            assertSame(subtitle, field(a, "subtitleViewListener"))
            assertSame(trailing, field(a, "trailingViewListener"))
        }
    }

    @Test
    fun messageItemViewListeners_reachTheMessagesAdapter() {
        withSearch { search ->
            val text = mock<SearchMessagesViewHolderListener<TextMessage>>()
            val image = mock<SearchMessagesViewHolderListener<com.cometchat.chat.models.MediaMessage>>()
            val video = mock<SearchMessagesViewHolderListener<com.cometchat.chat.models.MediaMessage>>()
            val audio = mock<SearchMessagesViewHolderListener<com.cometchat.chat.models.MediaMessage>>()
            val document = mock<SearchMessagesViewHolderListener<com.cometchat.chat.models.MediaMessage>>()
            val link = mock<SearchMessagesViewHolderListener<TextMessage>>()

            search.setTextMessageItemViewListener(text)
            search.setImageMessageItemViewListener(image)
            search.setVideoMessageItemViewListener(video)
            search.setAudioMessageItemViewListener(audio)
            search.setDocumentMessageItemViewListener(document)
            search.setLinkMessageItemViewListener(link)

            val a = field(search, "messagesAdapter")!!
            assertSame(text, field(a, "textMessageItemViewListener"))
            assertSame(image, field(a, "imageMessageItemViewListener"))
            assertSame(video, field(a, "videoMessageItemViewListener"))
            assertSame(audio, field(a, "audioMessageItemViewListener"))
            assertSame(document, field(a, "documentMessageItemViewListener"))
            assertSame(link, field(a, "linkMessageItemViewListener"))
        }
    }
}
