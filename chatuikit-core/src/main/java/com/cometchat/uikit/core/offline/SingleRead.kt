package com.cometchat.uikit.core.offline

import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation

/**
 * Act-once settlement for the SDK's two-phase single reads: `getMessageDetails` (BEH-READ-35) and
 * `getConversation` (BEH-READ-32). The SDK fires up to two phases (cached on a hit, then server)
 * and offline ends a cached hit with `onError`. Every caller here needs exactly one answer, so
 * [onDone] runs once:
 *
 * - the server value, when it arrives (online: today's result, unchanged);
 * - otherwise, when [cachedFallback] is set and a cached value was delivered, that cached value
 *   (offline with a cached copy: the caller proceeds instead of failing);
 * - otherwise the error (offline with nothing cached, or offline off: today's error handling).
 *
 * A server phase without a value counts as an error, so the caller never waits forever. Offline
 * off, the SDK fires the server phase only, so the result equals the legacy overload's.
 */
internal class SingleReadOnce<T : Any>(
    private val cachedFallback: Boolean,
    /** Which errors the cached fallback answers; every error by default. */
    private val fallbackOn: (CometChatException?) -> Boolean = { true },
    private val onDone: (Result<T>) -> Unit
) {
    private var cached: T? = null
    private var settled = false

    fun cached(value: T?) {
        if (cachedFallback && value != null) cached = value
    }

    fun server(value: T?) {
        if (value != null) settle(Result.success(value)) else error(null)
    }

    fun error(exception: CometChatException?) {
        val fallback = cached?.takeIf { fallbackOn(exception) }
        settle(
            if (fallback != null) Result.success(fallback)
            else Result.failure(exception ?: noValue())
        )
    }

    private fun settle(result: Result<T>) {
        if (settled) return
        settled = true
        onDone(result)
    }

    private fun noValue() = CometChatException(NO_VALUE_CODE, "The server returned no value")

    internal companion object {
        const val NO_VALUE_CODE: String = "ERR_NO_VALUE"
    }
}

/** [CometChat.MessageDetailsListener] that settles a [SingleReadOnce]. */
internal class MessageDetailsOnce(
    cachedFallback: Boolean,
    fallbackOn: (CometChatException?) -> Boolean = { true },
    onDone: (Result<BaseMessage>) -> Unit
) : CometChat.MessageDetailsListener() {
    private val read = SingleReadOnce(cachedFallback, fallbackOn, onDone)
    override fun onCachedMessage(message: BaseMessage?) = read.cached(message)
    override fun onServerMessage(message: BaseMessage?) = read.server(message)
    override fun onError(exception: CometChatException?) = read.error(exception)
}

/** [CometChat.ConversationDetailsListener] that settles a [SingleReadOnce]. */
internal class ConversationDetailsOnce(
    cachedFallback: Boolean,
    onDone: (Result<Conversation>) -> Unit
) : CometChat.ConversationDetailsListener() {
    private val read = SingleReadOnce(cachedFallback, onDone = onDone)
    override fun onCachedConversation(conversation: Conversation?) = read.cached(conversation)
    override fun onServerConversation(conversation: Conversation?) = read.server(conversation)
    override fun onError(exception: CometChatException?) = read.error(exception)
}

/** A settled single read, and whether the server answered it (false: the cached copy, offline). */
internal data class SettledRead<T : Any>(val value: T, val fromServer: Boolean)

/**
 * [CometChat.ConversationDetailsListener] for the chat-open path only (D60): the cached copy
 * answers only when the server phase fails with the not-connected error (offline), and the settled
 * value says which phase answered. Online every error stands, as today. Every other conversation
 * read keeps [ConversationDetailsOnce] without a fallback (ENG-41135).
 */
internal class ConversationDetailsSourceOnce(
    onDone: (Result<SettledRead<Conversation>>) -> Unit
) : CometChat.ConversationDetailsListener() {
    private val read = SingleReadOnce(cachedFallback = true, SearchOffline::isNotConnected, onDone)
    override fun onCachedConversation(conversation: Conversation?) =
        read.cached(conversation?.let { SettledRead(it, fromServer = false) })
    override fun onServerConversation(conversation: Conversation?) =
        read.server(conversation?.let { SettledRead(it, fromServer = true) })
    override fun onError(exception: CometChatException?) = read.error(exception)
}
