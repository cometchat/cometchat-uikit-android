package com.cometchat.uikit.core.offline

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.data.datasource.MessageListDataSourceImpl
import com.cometchat.uikit.core.testutils.MockFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

/**
 * ENG-41135: the act-once settlement of the two-phase single reads (BEH-READ-32 / BEH-READ-35),
 * and the core callers built on it. Each read settles exactly once: on cached-then-server, on
 * server-only, and offline with nothing cached (today's error). With a cached fallback, a cached
 * copy answers when the server phase fails; without one, the error stands (today's handling).
 */
class SingleReadOnceTest : FunSpec({

    val offline = CometChatException("ERROR_INTERNET_UNAVAILABLE", "offline")

    fun collect(fallback: Boolean): Pair<SingleReadOnce<String>, MutableList<Result<String>>> {
        val out = mutableListOf<Result<String>>()
        return SingleReadOnce<String>(fallback) { out += it } to out
    }

    // ---- SingleReadOnce: one settlement per read ----

    test("cached then server settles once, with the server value") {
        for (fallback in listOf(true, false)) {
            val (read, out) = collect(fallback)
            read.cached("cached")
            read.server("server")
            out shouldBe listOf(Result.success("server"))
        }
    }

    test("server only settles once, with the server value") {
        val (read, out) = collect(fallback = true)
        read.server("server")
        out shouldBe listOf(Result.success("server"))
    }

    test("offline with nothing cached settles once, with the error (today's handling)") {
        for (fallback in listOf(true, false)) {
            val (read, out) = collect(fallback)
            read.error(offline)
            out shouldBe listOf(Result.failure(offline))
        }
    }

    test("offline after a cached hit: the cached copy with a fallback, the error without") {
        val (withFallback, out1) = collect(fallback = true)
        withFallback.cached("cached")
        withFallback.error(offline)
        out1 shouldBe listOf(Result.success("cached"))

        val (noFallback, out2) = collect(fallback = false)
        noFallback.cached("cached")
        noFallback.error(offline)
        out2 shouldBe listOf(Result.failure(offline))
    }

    test("a server phase without a value settles as an error, so nothing waits forever") {
        val (read, out) = collect(fallback = false)
        read.server(null)
        out.size shouldBe 1
        (out[0].exceptionOrNull() as CometChatException).code shouldBe SingleReadOnce.NO_VALUE_CODE
    }

    test("later phases after settlement are ignored") {
        val (read, out) = collect(fallback = false)
        read.server("server")
        read.error(offline)
        read.server("again")
        out shouldBe listOf(Result.success("server"))
    }

    // ---- SDK listener adapters ----

    test("MessageDetailsOnce and ConversationDetailsOnce route the SDK phases") {
        val message = MockFactory.createTextMessage(id = 7L, text = "m")
        val messages = mutableListOf<Result<BaseMessage>>()
        MessageDetailsOnce(cachedFallback = true) { messages += it }.apply {
            onCachedMessage(message)
            onError(offline)
        }
        messages shouldBe listOf(Result.success<BaseMessage>(message))

        val conversation = MockFactory.createUserConversations(1).first()
        val conversations = mutableListOf<Result<Conversation>>()
        ConversationDetailsOnce(cachedFallback = false) { conversations += it }.apply {
            onCachedConversation(conversation)
            onServerConversation(conversation)
            onError(offline)
        }
        conversations shouldBe listOf(Result.success(conversation))
    }

    // ---- Core caller: MessageListDataSourceImpl single reads ----

    lateinit var sdk: MockedStatic<CometChat>
    beforeTest { sdk = Mockito.mockStatic(CometChat::class.java) }
    afterTest { sdk.close() }

    fun stubMessage(id: Long, phases: (CometChat.MessageDetailsListener) -> Unit) {
        sdk.`when`<Unit> { CometChat.getMessageDetails(eq(id), any<CometChat.MessageDetailsListener>()) }
            .thenAnswer { phases(it.getArgument(1)); null }
    }

    fun stubConversation(phases: (CometChat.ConversationDetailsListener) -> Unit) {
        sdk.`when`<Unit> {
            CometChat.getConversation(eq("u1"), eq("user"), any<CometChat.ConversationDetailsListener>())
        }.thenAnswer { phases(it.getArgument(2)); null }
    }

    test("getMessage: cached then server resumes once with the server message") {
        runTest {
            val cached = MockFactory.createTextMessage(id = 5L, text = "cached")
            val server = MockFactory.createTextMessage(id = 5L, text = "server")
            stubMessage(5L) { it.onCachedMessage(cached); it.onServerMessage(server) }
            MessageListDataSourceImpl().getMessage(5L) shouldBe server
        }
    }

    test("getMessage: server only resumes with the server message") {
        runTest {
            val server = MockFactory.createTextMessage(id = 5L, text = "server")
            stubMessage(5L) { it.onServerMessage(server) }
            MessageListDataSourceImpl().getMessage(5L) shouldBe server
        }
    }

    test("getMessage: offline with nothing cached throws today's error") {
        runTest {
            stubMessage(5L) { it.onError(offline) }
            shouldThrow<CometChatException> { MessageListDataSourceImpl().getMessage(5L) } shouldBe offline
        }
    }

    test("getConversation: cached then server resumes once with the server conversation") {
        runTest {
            val (cached, server) = MockFactory.createUserConversations(2)
            stubConversation { it.onCachedConversation(cached); it.onServerConversation(server) }
            MessageListDataSourceImpl().getConversation("u1", "user") shouldBe server
        }
    }

    test("getConversation: offline after a cached hit throws today's error (no fallback here)") {
        runTest {
            val cached = MockFactory.createUserConversations(1).first()
            stubConversation { it.onCachedConversation(cached); it.onError(offline) }
            shouldThrow<CometChatException> {
                MessageListDataSourceImpl().getConversation("u1", "user")
            } shouldBe offline
        }
    }
})
