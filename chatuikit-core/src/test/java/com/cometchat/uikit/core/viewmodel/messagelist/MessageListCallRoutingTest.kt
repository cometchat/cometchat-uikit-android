package com.cometchat.uikit.core.viewmodel.messagelist

import androidx.lifecycle.viewModelScope
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.Call
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.repository.MessageListRepository
import com.cometchat.uikit.core.events.CometChatCallEvent
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.testutils.MockFactory
import com.cometchat.uikit.core.viewmodel.CometChatMessageListViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

/**
 * Two things the message list decides for itself: whether a call belongs on screen, and whether a
 * message is worth asking the AI for reply suggestions.
 *
 * **Calls.** Every call event is broadcast in-process to every open message list, not addressed to
 * one. So each list has to decide for itself whether a given call belongs in it, and the check is
 * not symmetric: a call I placed names the other person as its receiver, while a call I answered
 * names me as receiver and the other person as initiator. Both are the same conversation and both
 * must appear. Anything else — a call with a third party, a call for another group — must not, or
 * a call bubble shows up in the wrong chat. Thread views never show calls at all: a call is not a
 * reply to anything.
 *
 * **Smart replies.** Suggestions cost an API call per message, so the gate in front of them is
 * deliberately narrow: off unless enabled, text messages only, incoming messages only (suggesting
 * replies to yourself is absurd), main conversation only, and — when the integrator configured
 * keywords — only when the text actually contains one. Every one of those is its own early return,
 * and a missed one means paying for a request on every message that arrives.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*MessageListCallRoutingTest"
 */
class MessageListCallRoutingTest : FunSpec({

    val me = "test_logged_in_user"
    val partner = "chat_partner"
    val thisGroup = "group_alpha"

    lateinit var cometChatStatic: MockedStatic<CometChat>
    lateinit var repository: MessageListRepository

    class ListeningViewModel(
        repository: MessageListRepository
    ) : CometChatMessageListViewModel(repository, enableListeners = true) {
        override fun getLoggedInUserUid(): String? = "test_logged_in_user"
        init { setEnableConversationSummary(false) }

        fun teardown() {
            onCleared()
            viewModelScope.cancel()
            runBlocking { viewModelScope.coroutineContext.job.join() }
        }
    }

    val liveViewModels = mutableListOf<ListeningViewModel>()

    fun user(uid: String) = User().apply { this.uid = uid; name = uid }

    /** A call placed BY [from] TO [to]. */
    fun call(id: Long, from: String, to: String, type: String = CometChatConstants.RECEIVER_TYPE_USER): Call {
        val c = MockFactory.createCall(
            sessionId = "session-$id",
            receiverUid = to,
            receiverType = type,
            callerUid = from
        )
        whenever(c.id).thenReturn(id)
        return c
    }

    fun userChat(): ListeningViewModel {
        val vm = ListeningViewModel(repository)
        liveViewModels += vm
        vm.setUser(user(partner), gotoMessageId = 0)
        return vm
    }

    fun groupChat(): ListeningViewModel {
        val vm = ListeningViewModel(repository)
        liveViewModels += vm
        vm.setGroup(Group().apply { guid = thisGroup; name = "Alpha" }, gotoMessageId = 0)
        return vm
    }

    fun textFrom(senderUid: String, id: Long, body: String = "message $id") =
        TextMessage(me, body, CometChatConstants.RECEIVER_TYPE_USER).apply {
            this.id = id
            sentAt = System.currentTimeMillis()
            sender = user(senderUid)
        }

    beforeTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        cometChatStatic = Mockito.mockStatic(CometChat::class.java)
        val loggedIn = mock<User>()
        whenever(loggedIn.uid).thenReturn(me)
        cometChatStatic.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(loggedIn)

        repository = mock()
        wheneverBlocking { repository.fetchPreviousMessages() }.thenReturn(Result.success(emptyList()))
        wheneverBlocking { repository.fetchNextMessages(any()) }.thenReturn(Result.success(emptyList()))
        wheneverBlocking { repository.fetchActionMessages(any()) }.thenReturn(Result.success(emptyList()))
        wheneverBlocking { repository.markAsRead(any()) }.thenReturn(Result.success(Unit))
        wheneverBlocking { repository.markAsDelivered(any()) }.thenReturn(Result.success(Unit))
        whenever(repository.hasMorePreviousMessages()).thenReturn(false)
        whenever(repository.getLatestMessageId()).thenReturn(0L)
        whenever(repository.getEffectiveMessagesTypes()).thenReturn(emptyList())
        whenever(repository.getEffectiveMessagesCategories()).thenReturn(emptyList())
    }

    afterTest {
        liveViewModels.forEach { it.teardown() }
        liveViewModels.clear()
        cometChatStatic.close()
        Dispatchers.resetMain()
    }

    fun ids(vm: ListeningViewModel) = vm.messages.value.map { it.id }

    // ==================== calls in a one-to-one chat ====================

    test("a call I placed to the person I am chatting with appears") {
        val vm = userChat()

        CometChatEvents.emitCallEventSync(CometChatCallEvent.OutgoingCall(call(1, from = me, to = partner)))

        ids(vm) shouldBe listOf(1L)
    }

    test("a call that person placed to me appears in the same chat") {
        val vm = userChat()

        CometChatEvents.emitCallEventSync(CometChatCallEvent.OutgoingCall(call(2, from = partner, to = me)))

        ids(vm) shouldBe listOf(2L)
    }

    test("a call with somebody else entirely does not appear") {
        val vm = userChat()

        CometChatEvents.emitCallEventSync(CometChatCallEvent.OutgoingCall(call(3, from = me, to = "stranger")))

        ids(vm) shouldBe emptyList()
    }

    test("a call from a third party to me does not appear in this chat") {
        val vm = userChat()

        CometChatEvents.emitCallEventSync(CometChatCallEvent.OutgoingCall(call(4, from = "stranger", to = me)))

        ids(vm) shouldBe emptyList()
    }

    // ==================== calls in a group chat ====================

    test("a call for this group appears") {
        val vm = groupChat()

        CometChatEvents.emitCallEventSync(
            CometChatCallEvent.OutgoingCall(
                call(5, from = "anyone", to = thisGroup, type = CometChatConstants.RECEIVER_TYPE_GROUP)
            )
        )

        ids(vm) shouldBe listOf(5L)
    }

    test("a call for a different group does not appear") {
        val vm = groupChat()

        CometChatEvents.emitCallEventSync(
            CometChatCallEvent.OutgoingCall(
                call(6, from = "anyone", to = "group_beta", type = CometChatConstants.RECEIVER_TYPE_GROUP)
            )
        )

        ids(vm) shouldBe emptyList()
    }

    // ==================== calls and threads ====================

    test("a thread view shows no calls at all") {
        val vm = userChat()
        vm.setParentMessage(textFrom(partner, 99))

        CometChatEvents.emitCallEventSync(CometChatCallEvent.OutgoingCall(call(7, from = me, to = partner)))

        ids(vm) shouldBe emptyList()
    }

    // ==================== the other three call events ====================

    test("an accepted call is added when there is nothing to update") {
        val vm = userChat()

        CometChatEvents.emitCallEventSync(CometChatCallEvent.CallAccepted(call(8, from = me, to = partner)))

        ids(vm) shouldBe listOf(8L)
    }

    test("a rejected call is added when there is nothing to update") {
        val vm = userChat()

        CometChatEvents.emitCallEventSync(CometChatCallEvent.CallRejected(call(9, from = me, to = partner)))

        ids(vm) shouldBe listOf(9L)
    }

    test("an ended call updates the bubble already there rather than adding a second one") {
        val vm = userChat()
        CometChatEvents.emitCallEventSync(CometChatCallEvent.OutgoingCall(call(10, from = me, to = partner)))

        CometChatEvents.emitCallEventSync(CometChatCallEvent.CallEnded(call(10, from = me, to = partner)))

        ids(vm) shouldBe listOf(10L)
    }

    test("the three update-or-add events all drop a call for another conversation") {
        val vm = userChat()
        val elsewhere = { id: Long -> call(id, from = me, to = "stranger") }

        CometChatEvents.emitCallEventSync(CometChatCallEvent.CallAccepted(elsewhere(11)))
        CometChatEvents.emitCallEventSync(CometChatCallEvent.CallRejected(elsewhere(12)))
        CometChatEvents.emitCallEventSync(CometChatCallEvent.CallEnded(elsewhere(13)))

        ids(vm) shouldBe emptyList()
    }

    // ==================== smart replies ====================

    /**
     * Answers the SDK's pending smart-reply request with [replies], reporting whether one was
     * actually made.
     *
     * atLeast(0) rather than a plain verify: half these tests expect no request at all, and a
     * failed verification leaves Mockito mid-verification, which then breaks the NEXT test's first
     * interaction instead of failing this one.
     */
    fun answerSmartReplies(vararg replies: String): Boolean {
        val captor = argumentCaptor<CometChat.CallbackListener<HashMap<String, String>>>()
        cometChatStatic.verify(
            { CometChat.getSmartReplies(any(), any(), anyOrNull(), captor.capture()) },
            Mockito.atLeast(0)
        )
        val pending = captor.allValues.lastOrNull() ?: return false
        pending.onSuccess(HashMap(replies.withIndex().associate { (i, r) -> "reply$i" to r }))
        return true
    }

    /**
     * The smart-reply gate lives in the private incoming-message path, which only the SDK's
     * MessageListener reaches. The static mock swallows the registration but records it, so the
     * captured listener is the real object and calling it reproduces a socket delivery.
     */
    fun listenerOf(): CometChat.MessageListener {
        val captor = argumentCaptor<CometChat.MessageListener>()
        cometChatStatic.verify(
            { CometChat.addMessageListener(any(), captor.capture()) },
            Mockito.atLeastOnce()
        )
        return captor.lastValue
    }

    fun listeningWithSmartReplies(
        keywords: List<String> = emptyList()
    ): Pair<ListeningViewModel, CometChat.MessageListener> {
        val vm = userChat().apply {
            setEnableSmartReplies(true)
            setSmartRepliesDelay(0)
            if (keywords.isNotEmpty()) setSmartReplyKeywords(keywords)
            fetchMessages()
        }
        return vm to listenerOf()
    }

    test("an incoming text message asks for suggestions when smart replies are on") {
        val (vm, listener) = listeningWithSmartReplies()

        listener.onTextMessageReceived(textFrom(partner, 20))

        answerSmartReplies("Sure", "On my way") shouldBe true
        vm.smartReplies.value.toSet() shouldBe setOf("Sure", "On my way")
    }

    test("nothing is asked for when smart replies are off") {
        val vm = userChat().apply { fetchMessages() }
        liveViewModels.contains(vm) shouldBe true

        listenerOf().onTextMessageReceived(textFrom(partner, 21))

        answerSmartReplies("Sure") shouldBe false
    }

    test("your own message does not get suggestions") {
        val (_, listener) = listeningWithSmartReplies()

        listener.onTextMessageReceived(textFrom(me, 22))

        answerSmartReplies("Sure") shouldBe false
    }

    test("a thread reply does not get suggestions") {
        val (vm, listener) = listeningWithSmartReplies()
        vm.setParentMessage(textFrom(partner, 98))

        listener.onTextMessageReceived(textFrom(partner, 23).apply { parentMessageId = 98L })

        answerSmartReplies("Sure") shouldBe false
    }

    test("a media message does not get suggestions") {
        val (_, listener) = listeningWithSmartReplies()

        listener.onMediaMessageReceived(
            MockFactory.createMediaMessage(
                id = 24L,
                senderUid = partner,
                receiverId = me,
                receiverType = CometChatConstants.RECEIVER_TYPE_USER
            )
        )

        answerSmartReplies("Sure") shouldBe false
    }

    // ==================== smart reply keywords ====================

    test("with keywords configured only a matching message asks for suggestions") {
        val (_, listener) = listeningWithSmartReplies(keywords = listOf("lunch"))

        listener.onTextMessageReceived(textFrom(partner, 25, body = "shall we get lunch"))

        answerSmartReplies("Sure") shouldBe true
    }

    test("a message missing every keyword is skipped") {
        val (_, listener) = listeningWithSmartReplies(keywords = listOf("lunch"))

        listener.onTextMessageReceived(textFrom(partner, 26, body = "see you tomorrow"))

        answerSmartReplies("Sure") shouldBe false
    }

    test("keyword matching ignores case in both directions") {
        val (_, listener) = listeningWithSmartReplies(keywords = listOf("LUNCH"))

        listener.onTextMessageReceived(textFrom(partner, 27, body = "Shall we get Lunch?"))

        answerSmartReplies("Sure") shouldBe true
    }

    test("any one of several keywords is enough") {
        val (_, listener) = listeningWithSmartReplies(keywords = listOf("lunch", "dinner"))

        listener.onTextMessageReceived(textFrom(partner, 28, body = "dinner tonight?"))

        answerSmartReplies("Sure") shouldBe true
    }
})
