package com.cometchat.uikit.core.offline

import androidx.annotation.RestrictTo
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MessageStatus

/**
 * The sending/failed mark of a conversation row's own unsent preview (W3, BEH-CONV-01, BEH-SEND-30).
 *
 * SDK 5.1.0's conversation preview carries no send status (D19), so the conversation list stamps
 * its own copy of the preview with the hub's state; this reads it back for the row's receipt icon in
 * both kits. Only a message without a server id can carry a mark, and with offline off such a
 * message never reaches the conversation list, so the OFF path renders as today.
 *
 * Public so both UI modules can call it; [RestrictTo] `LIBRARY_GROUP` — not customer API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object ConversationPreviewMark {

    /** [MessageSendState.SENDING] or [MessageSendState.FAILED] for an unsent preview; null otherwise. */
    @JvmStatic
    public fun of(message: BaseMessage?): MessageSendState? {
        if (message == null || message.id != 0L) return null
        return when (message.status) {
            MessageStatus.PENDING, MessageStatus.SENDING -> MessageSendState.SENDING
            MessageStatus.FAILED -> MessageSendState.FAILED
            else -> null
        }
    }
}
