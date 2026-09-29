package com.cometchat.uikit.core.viewmodel.messagecomposer

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.domain.usecase.EditMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendCustomMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendMediaMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendTextMessageUseCase
import com.cometchat.uikit.core.viewmodel.CometChatMessageComposerViewModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

/**
 * Editing a message, and quoting one in a reply.
 *
 * Two composer paths that both rewrite a message before it leaves the device, and both have a
 * quiet failure mode.
 *
 * **Editing** builds a brand-new message rather than mutating the one on screen — if the edit is
 * rejected by the server, the original bubble must still read the way it always did. That makes
 * the construction the interesting part: a text edit replaces the text, a media edit replaces the
 * *caption* and has to re-attach the media (an edit that dropped the attachment would turn a photo
 * into an empty bubble), and anything else — an action message, a call — is not editable at all
 * and must be refused rather than half-converted.
 *
 * **Quoting** turns the reply target into an id the server will accept, and the check is not a
 * formality: the composer can still be holding a reply target from a conversation the user has
 * since navigated away from. `getQuotedMessageId` returns -1 for anything that does not belong to
 * the conversation currently open, and -1 means the quote is dropped rather than attached to the
 * wrong thread. Conversation ids are `uid_uid` pairs, so membership is a split-and-match; groups
 * compare guids directly.
 *
 * Extension-backed creates (polls, whiteboards, documents) go out through `CometChat.callExtension`
 * and carry the quoted id in their payload, so they are driven here through the captured callback.
 *
 * Run with:
 *   ./gradlew :chatuikit-core:testDebugUnitTest --tests "*MessageComposerEditAndReplyTest"
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageComposerEditAndReplyTest : FunSpec({

    val me = "meUid"
    val partner = "partnerUid"

    lateinit var editMessageUseCase: EditMessageUseCase
    lateinit var cometChatStatic: MockedStatic<CometChat>

    beforeTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        editMessageUseCase = mock()
        wheneverBlocking { editMessageUseCase.invoke(any()) }.thenReturn(
            Result.success(TextMessage(partner, "edited", CometChatConstants.RECEIVER_TYPE_USER))
        )
        cometChatStatic = Mockito.mockStatic(CometChat::class.java)
        cometChatStatic.`when`<User?> { CometChat.getLoggedInUser() }
            .thenReturn(User().apply { uid = me })
    }

    afterTest {
        cometChatStatic.close()
        Dispatchers.resetMain()
    }

    fun composer(): CometChatMessageComposerViewModel = CometChatMessageComposerViewModel(
        sendTextMessageUseCase = mock<SendTextMessageUseCase>(),
        sendMediaMessageUseCase = mock<SendMediaMessageUseCase>(),
        sendCustomMessageUseCase = mock<SendCustomMessageUseCase>(),
        editMessageUseCase = editMessageUseCase,
        enableListeners = false
    )

    fun text(id: Long, to: String = partner, body: String = "original"): TextMessage =
        TextMessage(to, body, CometChatConstants.RECEIVER_TYPE_USER).apply { this.id = id }

    /**
     * The message handed to the edit use case, or null if it was never called.
     *
     * atLeast(0) rather than atLeastOnce: half these tests expect no call at all, and a
     * verification that *fails* leaves Mockito mid-verification, which then blows up the next
     * test's first interaction instead of failing this one.
     */
    fun editedPayload(): BaseMessage? {
        val captor = argumentCaptor<BaseMessage>()
        verifyBlocking(editMessageUseCase, Mockito.atLeast(0)) { invoke(captor.capture()) }
        return captor.allValues.lastOrNull()
    }

    // ==================== editing: what gets built ====================

    test("editing a text message sends the new text under the original id") {
        val vm = composer()
        vm.setUser(User().apply { uid = partner })
        vm.setEditMessage(text(7, body = "before"))

        vm.editMessage("after")

        val sent = editedPayload()
        sent.shouldBeInstanceOf<TextMessage>()
        sent.text shouldBe "after"
        sent.id shouldBe 7L
    }

    test("the new text is trimmed before it is sent") {
        val vm = composer()
        vm.setEditMessage(text(7))

        vm.editMessage("   spaced out   ")

        (editedPayload() as TextMessage).text shouldBe "spaced out"
    }

    test("editing a media message replaces the caption and keeps the attachments") {
        val attachment = mock<Attachment>()
        val original = mock<MediaMessage>()
        whenever(original.id).thenReturn(8L)
        whenever(original.receiverUid).thenReturn(partner)
        whenever(original.receiverType).thenReturn(CometChatConstants.RECEIVER_TYPE_USER)
        whenever(original.type).thenReturn(CometChatConstants.MESSAGE_TYPE_IMAGE)
        whenever(original.attachments).thenReturn(listOf(attachment))

        val vm = composer()
        vm.setEditMessage(original)
        vm.editMessage("new caption")

        val sent = editedPayload()
        sent.shouldBeInstanceOf<MediaMessage>()
        sent.caption shouldBe "new caption"
        sent.attachments shouldBe listOf(attachment)
        sent.id shouldBe 8L
    }

    test("a media message carrying a single attachment rather than a list keeps that one") {
        val attachment = mock<Attachment>()
        val original = mock<MediaMessage>()
        whenever(original.id).thenReturn(9L)
        whenever(original.receiverUid).thenReturn(partner)
        whenever(original.receiverType).thenReturn(CometChatConstants.RECEIVER_TYPE_USER)
        whenever(original.type).thenReturn(CometChatConstants.MESSAGE_TYPE_IMAGE)
        whenever(original.attachments).thenReturn(emptyList())
        whenever(original.attachment).thenReturn(attachment)

        val vm = composer()
        vm.setEditMessage(original)
        vm.editMessage("new caption")

        (editedPayload() as MediaMessage).attachments shouldBe listOf(attachment)
    }

    test("a message kind that cannot be edited is refused rather than converted") {
        val vm = composer()
        vm.setEditMessage(mock<com.cometchat.chat.models.Action>())

        vm.editMessage("anything")

        editedPayload() shouldBe null
    }

    // ==================== editing: when nothing should happen ====================

    test("editing with nothing staged does nothing") {
        composer().editMessage("new text")

        editedPayload() shouldBe null
    }

    test("an empty edit is refused so a message cannot be blanked by accident") {
        val vm = composer()
        vm.setEditMessage(text(7))

        vm.editMessage("")

        editedPayload() shouldBe null
    }

    test("a whitespace-only edit is refused too") {
        val vm = composer()
        vm.setEditMessage(text(7))

        vm.editMessage("    ")

        editedPayload() shouldBe null
    }

    // ==================== editing: the two outcomes ====================

    test("a successful edit clears the staged message") {
        val vm = composer()
        vm.setEditMessage(text(7))

        vm.editMessage("after")

        vm.editMessage.value shouldBe null
    }

    test("a failed edit keeps the staged message so the user can try again") {
        wheneverBlocking { editMessageUseCase.invoke(any()) }
            .thenReturn(Result.failure(CometChatException("ERR", "server said no")))
        val vm = composer()
        vm.setEditMessage(text(7))

        vm.editMessage("after")

        vm.editMessage.value shouldNotBe null
    }

    test("a plain exception from the edit is wrapped rather than escaping") {
        wheneverBlocking { editMessageUseCase.invoke(any()) }
            .thenReturn(Result.failure(IllegalStateException("network down")))
        val vm = composer()
        vm.setEditMessage(text(7))

        // Reaching the assertion at all is the point: an unwrapped throw would fail the test.
        vm.editMessage("after")

        vm.editMessage.value shouldNotBe null
    }

    // ==================== quoting: which replies survive ====================

    /** Runs createPoll and returns the payload handed to callExtension, or null if none was. */
    fun payloadOf(block: () -> Unit): org.json.JSONObject? {
        block()
        val captor = argumentCaptor<org.json.JSONObject>()
        // atLeast(0): several of these expect the extension never to be called, and a failed
        // verification would poison the next test rather than fail this one.
        cometChatStatic.verify(
            { CometChat.callExtension(any(), any(), any(), captor.capture(), any()) },
            Mockito.atLeast(0)
        )
        return captor.allValues.lastOrNull()
    }

    fun quotedIn(payload: org.json.JSONObject?): Long? =
        payload?.takeIf { it.has("quotedMessageId") }?.getLong("quotedMessageId")

    test("a reply from the open one-to-one conversation is quoted") {
        val vm = composer()
        vm.setUser(User().apply { uid = partner })
        vm.setReplyMessage(text(11).apply {
            receiver = User().apply { uid = partner }
            conversationId = "${me}_$partner"
        })

        quotedIn(payloadOf { vm.createPoll("Lunch?", org.json.JSONArray(listOf("yes", "no"))) }) shouldBe 11L
    }

    test("a reply from a different one-to-one conversation is dropped") {
        val vm = composer()
        vm.setUser(User().apply { uid = partner })
        vm.setReplyMessage(text(11).apply {
            receiver = User().apply { uid = "someoneElse" }
            conversationId = "${me}_someoneElse"
        })

        quotedIn(payloadOf { vm.createPoll("Lunch?", org.json.JSONArray(listOf("yes", "no"))) }) shouldBe null
    }

    test("a reply with no conversation id is dropped") {
        val vm = composer()
        vm.setUser(User().apply { uid = partner })
        vm.setReplyMessage(text(11).apply { receiver = User().apply { uid = partner } })

        quotedIn(payloadOf { vm.createPoll("Lunch?", org.json.JSONArray(listOf("yes", "no"))) }) shouldBe null
    }

    test("a reply with no receiver at all is dropped") {
        val vm = composer()
        vm.setUser(User().apply { uid = partner })
        vm.setReplyMessage(text(11))

        quotedIn(payloadOf { vm.createPoll("Lunch?", org.json.JSONArray(listOf("yes", "no"))) }) shouldBe null
    }

    test("a reply from the open group is quoted") {
        val vm = composer()
        vm.setGroup(Group().apply { guid = "group_alpha" })
        vm.setReplyMessage(
            TextMessage("group_alpha", "hi", CometChatConstants.RECEIVER_TYPE_GROUP).apply {
                id = 12L
                receiver = Group().apply { guid = "group_alpha" }
            }
        )

        quotedIn(payloadOf { vm.createPoll("Lunch?", org.json.JSONArray(listOf("yes", "no"))) }) shouldBe 12L
    }

    test("a reply from a different group is dropped") {
        val vm = composer()
        vm.setGroup(Group().apply { guid = "group_alpha" })
        vm.setReplyMessage(
            TextMessage("group_beta", "hi", CometChatConstants.RECEIVER_TYPE_GROUP).apply {
                id = 12L
                receiver = Group().apply { guid = "group_beta" }
            }
        )

        quotedIn(payloadOf { vm.createPoll("Lunch?", org.json.JSONArray(listOf("yes", "no"))) }) shouldBe null
    }

    test("a group reply staged while a one-to-one chat is open is dropped") {
        val vm = composer()
        vm.setUser(User().apply { uid = partner })
        vm.setReplyMessage(
            TextMessage("group_alpha", "hi", CometChatConstants.RECEIVER_TYPE_GROUP).apply {
                id = 12L
                receiver = Group().apply { guid = "group_alpha" }
            }
        )

        quotedIn(payloadOf { vm.createPoll("Lunch?", org.json.JSONArray(listOf("yes", "no"))) }) shouldBe null
    }

    test("with no reply staged the payload carries no quote") {
        val vm = composer()
        vm.setUser(User().apply { uid = partner })

        quotedIn(payloadOf { vm.createPoll("Lunch?", org.json.JSONArray(listOf("yes", "no"))) }) shouldBe null
    }

    // ==================== extension creates ====================

    test("creating a poll with no conversation open reports an error instead of calling out") {
        val vm = composer()
        var reported: CometChatException? = null

        vm.createPoll("Lunch?", org.json.JSONArray(listOf("yes", "no")), onError = { reported = it })

        reported?.code shouldBe "ERR_NO_RECEIVER"
        cometChatStatic.verify({ CometChat.callExtension(any(), any(), any(), any(), any()) }, never())
    }

    test("creating a whiteboard with no conversation open reports an error") {
        val vm = composer()
        var reported: CometChatException? = null

        vm.createCollaborativeWhiteboard(onError = { reported = it })

        reported?.code shouldBe "ERR_NO_RECEIVER"
    }

    test("creating a document with no conversation open reports an error") {
        val vm = composer()
        var reported: CometChatException? = null

        vm.createCollaborativeDocument(onError = { reported = it })

        reported?.code shouldBe "ERR_NO_RECEIVER"
    }

    test("a poll the server accepts clears the staged reply") {
        val vm = composer()
        vm.setUser(User().apply { uid = partner })
        vm.setReplyMessage(text(11).apply {
            receiver = User().apply { uid = partner }
            conversationId = "${me}_$partner"
        })

        var succeeded = false
        vm.createPoll("Lunch?", org.json.JSONArray(listOf("yes", "no")), onSuccess = { succeeded = true })

        val captor = argumentCaptor<CometChat.CallbackListener<org.json.JSONObject>>()
        cometChatStatic.verify({ CometChat.callExtension(any(), any(), any(), any(), captor.capture()) })
        captor.lastValue.onSuccess(org.json.JSONObject())

        succeeded shouldBe true
        vm.replyMessage.value shouldBe null
    }

    test("a poll the server rejects reports the error and keeps the reply staged") {
        val vm = composer()
        vm.setUser(User().apply { uid = partner })
        vm.setReplyMessage(text(11).apply {
            receiver = User().apply { uid = partner }
            conversationId = "${me}_$partner"
        })

        var reported: CometChatException? = null
        vm.createPoll("Lunch?", org.json.JSONArray(listOf("yes", "no")), onError = { reported = it })

        val captor = argumentCaptor<CometChat.CallbackListener<org.json.JSONObject>>()
        cometChatStatic.verify({ CometChat.callExtension(any(), any(), any(), any(), captor.capture()) })
        captor.lastValue.onError(CometChatException("ERR_POLL", "rejected"))

        reported?.code shouldBe "ERR_POLL"
        vm.replyMessage.value shouldNotBe null
    }

    test("a whiteboard create goes to the whiteboard extension") {
        val vm = composer()
        vm.setUser(User().apply { uid = partner })

        vm.createCollaborativeWhiteboard()

        val slug = argumentCaptor<String>()
        cometChatStatic.verify({ CometChat.callExtension(slug.capture(), any(), any(), any(), any()) })
        slug.firstValue shouldBe "whiteboard"
    }

    test("a document create goes to the document extension") {
        val vm = composer()
        vm.setUser(User().apply { uid = partner })

        vm.createCollaborativeDocument()

        val slug = argumentCaptor<String>()
        cometChatStatic.verify({ CometChat.callExtension(slug.capture(), any(), any(), any(), any()) })
        slug.firstValue shouldBe "document"
    }
})
