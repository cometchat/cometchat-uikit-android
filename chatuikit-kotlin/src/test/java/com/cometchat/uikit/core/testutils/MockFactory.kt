package com.cometchat.uikit.core.testutils

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.CardMessage
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.Reaction
import com.cometchat.chat.models.User
import org.json.JSONArray
import org.json.JSONObject
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test-only MockFactory for chatuikit-kotlin unit tests.
 *
 * The canonical MockFactory lives in chatuikit-core's test sources, which are not
 * available on the chatuikit-kotlin unit test classpath (only main sources are
 * shared via testImplementation(project(":chatuikit-core"))).
 *
 * This provides the same mock-creation utilities needed by card bubble tests.
 */
object MockFactory {

    /**
     * Default developer-card payload used by [createCardMessage] — a realistic
     * "order shipped" card (heading + Track Order button). This is the value of
     * the message's `card` field.
     */
    const val DEFAULT_CARD_JSON: String =
        """{"version":"1.0","body":[{"id":"txt_1","type":"text","content":"Your order has shipped!","variant":"heading3"},{"id":"btn_1","type":"button","label":"Track Order","action":{"type":"openUrl","url":"https://example.com/track"}}],"fallbackText":"Your order has shipped! Track at https://example.com/track","style":{"borderRadius":12,"padding":12}}"""

    // ==================== User ====================

    fun createUser(
        uid: String = "user-1",
        name: String = "Test User",
        status: String = CometChatConstants.USER_STATUS_ONLINE,
        isBlockedByMe: Boolean = false,
        hasBlockedMe: Boolean = false
    ): User {
        val user = mock<User>()
        whenever(user.uid).thenReturn(uid)
        whenever(user.name).thenReturn(name)
        whenever(user.status).thenReturn(status)
        whenever(user.avatar).thenReturn(null)
        whenever(user.isBlockedByMe).thenReturn(isBlockedByMe)
        whenever(user.isHasBlockedMe).thenReturn(hasBlockedMe)
        return user
    }

    fun createGroup(
        guid: String = "group-1",
        name: String = "Test Group",
        membersCount: Int = 3,
        type: String = CometChatConstants.GROUP_TYPE_PUBLIC
    ): Group {
        val group = mock<Group>()
        whenever(group.guid).thenReturn(guid)
        whenever(group.name).thenReturn(name)
        whenever(group.membersCount).thenReturn(membersCount)
        whenever(group.groupType).thenReturn(type)
        whenever(group.icon).thenReturn(null)
        return group
    }

    // ==================== Card Message ====================

    /**
     * Creates a mock CardMessage for developer card (category "card") tests.
     */
    fun createCardMessage(
        id: Long = 1L,
        type: String = "",
        cardJson: Any? = DEFAULT_CARD_JSON,
        text: String? = "Check out this card",
        fallbackText: String? = "Card Message",
        senderUid: String = "user-1",
        receiverId: String = "user-2",
        receiverType: String = CometChatConstants.RECEIVER_TYPE_USER,
        sentAt: Long = System.currentTimeMillis() / 1000,
        deletedAt: Long = 0L
    ): CardMessage {
        val sender = createUser(uid = senderUid, name = "Sender")
        val message = mock<CardMessage>()
        whenever(message.id).thenReturn(id)
        whenever(message.type).thenReturn(type)
        whenever(message.category).thenReturn("card")
        // Convert cardJson to JSONObject if it's a Map, pass null/JSONObject through directly
        val jsonObj: org.json.JSONObject? = when (cardJson) {
            is org.json.JSONObject -> cardJson
            is Map<*, *> -> org.json.JSONObject(cardJson)
            is String -> if (cardJson.isNotEmpty()) {
                try { org.json.JSONObject(cardJson) } catch (_: Exception) { null }
            } else null
            else -> null
        }
        whenever(message.card).thenReturn(jsonObj)
        whenever(message.text).thenReturn(text)
        whenever(message.fallbackText).thenReturn(fallbackText)
        whenever(message.sender).thenReturn(sender)
        whenever(message.receiverUid).thenReturn(receiverId)
        whenever(message.receiverType).thenReturn(receiverType)
        whenever(message.sentAt).thenReturn(sentAt)
        whenever(message.readAt).thenReturn(0L)
        whenever(message.deliveredAt).thenReturn(0L)
        whenever(message.deletedAt).thenReturn(deletedAt)
        whenever(message.editedAt).thenReturn(0L)
        whenever(message.parentMessageId).thenReturn(0L)
        return message
    }

    /**
     * Creates a CardMessage with an empty/null card payload (triggers fallback rendering).
     */
    fun createEmptyCardMessage(
        id: Long = 1L,
        text: String? = null,
        fallbackText: String? = null
    ): CardMessage {
        return createCardMessage(
            id = id,
            cardJson = null,
            text = text,
            fallbackText = fallbackText
        )
    }

    // ==================== Media messages ====================

    /**
     * A fixed instant for every fixture: 15 Oct 2024, 4:56 PM GMT.
     *
     * Screenshot baselines that let a component format this will encode the
     * recorder's time zone unless the test pins one — see the GMT pin in the bubble
     * screenshot suites.
     */
    const val FIXED_SENT_AT: Long = 1_729_011_360L

    /** One attachment, numbered so several are distinguishable in assertions. */
    fun createAttachment(
        index: Int = 1,
        mimeType: String = "image/jpeg",
        extension: String = "jpg",
        sizeBytes: Int = 3_200_000
    ): Attachment = Attachment().apply {
        fileUrl = "https://cdn.example.com/media_$index.$extension"
        fileName = "media_$index.$extension"
        fileExtension = extension
        fileMimeType = mimeType
        fileSize = sizeBytes
    }

    /** The metadata shape the bubbles parse: `{url, fileName, extension, mimeType, size}`. */
    fun attachmentJson(
        index: Int = 1,
        mimeType: String = "image/jpeg",
        extension: String = "jpg",
        sizeBytes: Int = 3_200_000
    ): JSONObject = JSONObject().apply {
        put("url", "https://cdn.example.com/media_$index.$extension")
        put("fileName", "media_$index.$extension")
        put("extension", extension)
        put("mimeType", mimeType)
        put("size", sizeBytes)
    }

    /**
     * A media message carrying [count] attachments.
     *
     * The delivery path is not cosmetic — it decides what renders:
     *
     * * `count == 1` sets `message.attachment`, the single-attachment path.
     * * `count > 1` fills `metadata.attachments`, which is the **only** multi-attachment
     *   source the image bubble reads. It never consults `message.attachments`, unlike
     *   the shared `resolveAttachments`, so a fixture built on that property renders
     *   nothing. Pinned by `sdkAttachmentsList_isIgnoredByThisBubble_unlikeTheSharedResolver`.
     */
    fun createMediaMessage(
        count: Int = 1,
        type: String = CometChatConstants.MESSAGE_TYPE_IMAGE,
        mimeType: String = "image/jpeg",
        extension: String = "jpg",
        caption: String? = null,
        sentAt: Long = FIXED_SENT_AT,
        senderUid: String = "sender-1",
        receiverId: String = "receiver-1"
    ): MediaMessage = MediaMessage(
        receiverId,
        type,
        CometChatConstants.RECEIVER_TYPE_USER
    ).apply {
        this.id = 1L
        this.sender = createUser(uid = senderUid, name = "Sender")
        this.sentAt = sentAt
        this.category = CometChatConstants.CATEGORY_MESSAGE
        if (count == 1) {
            this.attachment = createAttachment(1, mimeType, extension)
        } else if (count > 1) {
            this.metadata = JSONObject().put(
                "attachments",
                JSONArray().apply {
                    (1..count).forEach { put(attachmentJson(it, mimeType, extension)) }
                }
            )
        }
        caption?.let { this.caption = it }
    }

    // ==================== Poll messages ====================

    /**
     * A poll message in the shape `extractPollData` parses.
     *
     * `customData` carries the question and a 1-indexed options map; the results
     * (per-option counts and voters, plus the total) live in
     * `metadata.@injected.extensions.polls.results`. Both halves are needed — with
     * customData alone the poll renders with every count at zero.
     */
    fun createPollMessage(
        question: String = "What is your favourite colour?",
        options: List<String> = listOf("Red", "Blue", "Green"),
        counts: List<Int> = listOf(2, 3, 0),
        pollId: String = "poll-1",
        senderUid: String = "sender-1",
        receiverId: String = "receiver-1",
        sentAt: Long = FIXED_SENT_AT
    ): CustomMessage {
        val optionsJson = JSONObject().apply {
            options.forEachIndexed { i, text -> put("${i + 1}", text) }
        }
        val resultOptions = JSONObject().apply {
            options.forEachIndexed { i, _ ->
                val count = counts.getOrElse(i) { 0 }
                put(
                    "${i + 1}",
                    JSONObject().apply {
                        put("count", count)
                        put(
                            "voters",
                            JSONObject().apply {
                                repeat(count) { v ->
                                    put(
                                        "voter-$i-$v",
                                        JSONObject().apply {
                                            put("name", "Voter $v")
                                            put("avatar", "")
                                        }
                                    )
                                }
                            }
                        )
                    }
                )
            }
        }
        val message = CustomMessage(
            receiverId,
            CometChatConstants.RECEIVER_TYPE_USER,
            "extension_poll",
            JSONObject()
        )
        // Set explicitly rather than through the constructor: the SDK's constructor
        // does not surface the payload on `customData`.
        message.customData = JSONObject().apply {
            put("id", pollId)
            put("question", question)
            put("options", optionsJson)
        }
        message.id = 1L
        message.sender = createUser(uid = senderUid, name = "Sender")
        message.sentAt = sentAt
        message.category = CometChatConstants.CATEGORY_CUSTOM
        message.metadata = JSONObject().apply {
            put(
                "@injected",
                JSONObject().apply {
                    put(
                        "extensions",
                        JSONObject().apply {
                            put(
                                "polls",
                                JSONObject().apply {
                                    put(
                                        "results",
                                        JSONObject().apply {
                                            put("total", counts.sum())
                                            put("options", resultOptions)
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
        return message
    }

    // ==================== Collaborative / meet-call messages ====================

    /**
     * A collaborative document or whiteboard message.
     *
     * `InternalContentRenderer` routes on the custom type — `extension_document` or
     * `extension_whiteboard` — and the bubble reads its title, subtitle and button
     * label from `customData`.
     */
    fun createCollaborativeMessage(
        whiteboard: Boolean = false,
        title: String = "Collaborative Document",
        subtitle: String = "Open to edit together",
        buttonText: String = "Join",
        receiverId: String = "receiver-1",
        sentAt: Long = FIXED_SENT_AT
    ): CustomMessage {
        val message = CustomMessage(
            receiverId,
            CometChatConstants.RECEIVER_TYPE_USER,
            if (whiteboard) "extension_whiteboard" else "extension_document",
            JSONObject()
        )
        message.customData = JSONObject().apply {
            put("title", title)
            put("subtitle", subtitle)
            put("button_text", buttonText)
            put("url", "https://cometchat.com/collab/1")
        }
        message.id = 1L
        message.sender = createUser(uid = "sender-1", name = "Sender")
        message.sentAt = sentAt
        message.category = CometChatConstants.CATEGORY_CUSTOM
        return message
    }

    /** A meeting message; the renderer routes on the `meeting` custom type. */
    fun createMeetCallMessage(
        title: String = "Video call",
        subtitle: String = "Tap to join",
        sessionId: String = "session-1",
        receiverId: String = "receiver-1",
        sentAt: Long = FIXED_SENT_AT
    ): CustomMessage {
        val message = CustomMessage(
            receiverId,
            CometChatConstants.RECEIVER_TYPE_USER,
            "meeting",
            JSONObject()
        )
        message.customData = JSONObject().apply {
            put("title", title)
            put("subtitle", subtitle)
            put("sessionID", sessionId)
            put("callType", "video")
        }
        message.id = 1L
        message.sender = createUser(uid = "sender-1", name = "Sender")
        message.sentAt = sentAt
        message.category = CometChatConstants.CATEGORY_CUSTOM
        return message
    }

    /** A sticker message; the renderer routes on the `extension_sticker` custom type. */
    fun createStickerMessage(
        name: String? = "Party Popper",
        url: String = "https://cdn.example.com/stickers/party.png",
        useLegacyUrlKey: Boolean = false,
        receiverId: String = "receiver-1",
        sentAt: Long = FIXED_SENT_AT
    ): CustomMessage {
        val message = CustomMessage(
            receiverId,
            CometChatConstants.RECEIVER_TYPE_USER,
            "extension_sticker",
            JSONObject()
        )
        message.customData = JSONObject().apply {
            put(if (useLegacyUrlKey) "url" else "sticker_url", url)
            name?.let { put("sticker_name", it) }
        }
        message.id = 1L
        message.sender = createUser(uid = "sender-1", name = "Sender")
        message.sentAt = sentAt
        message.category = CometChatConstants.CATEGORY_CUSTOM
        return message
    }

    /** A message the sender deleted; `deletedAt` is what routes it to the delete bubble. */
    fun createDeletedMessage(
        senderUid: String = "sender-1",
        receiverId: String = "receiver-1",
        sentAt: Long = FIXED_SENT_AT
    ): TextMessage = TextMessage(
        receiverId,
        "the original text",
        CometChatConstants.RECEIVER_TYPE_USER
    ).apply {
        id = 1L
        sender = createUser(uid = senderUid, name = "Sender")
        this.sentAt = sentAt
        category = CometChatConstants.CATEGORY_MESSAGE
        deletedAt = sentAt + 60
    }

    /** A plain text message. Real object rather than a mock, so later mutation sticks. */
    fun createTextMessage(
        id: Long = 1L,
        text: String = "Hello",
        senderUid: String = "sender-1",
        receiverId: String = "receiver-1",
        sentAt: Long = FIXED_SENT_AT
    ): TextMessage = TextMessage(
        receiverId,
        text,
        CometChatConstants.RECEIVER_TYPE_USER
    ).apply {
        this.id = id
        sender = createUser(uid = senderUid, name = "Sender")
        this.sentAt = sentAt
        category = CometChatConstants.CATEGORY_MESSAGE
    }

    /** A reaction row as the reactions API returns it. */
    fun createReaction(
        uid: String = "user-1",
        name: String = "Alice",
        emoji: String = "\uD83D\uDC4D",
        reactedAt: Long = FIXED_SENT_AT
    ): Reaction {
        // Build the user first: creating a mock inside a thenReturn leaves Mockito
        // with unfinished stubbing.
        val reactedByUser = createUser(uid = uid, name = name)
        val reaction = mock<Reaction>()
        whenever(reaction.uid).thenReturn(uid)
        whenever(reaction.reaction).thenReturn(emoji)
        whenever(reaction.reactedAt).thenReturn(reactedAt)
        whenever(reaction.reactedBy).thenReturn(reactedByUser)
        return reaction
    }
}
