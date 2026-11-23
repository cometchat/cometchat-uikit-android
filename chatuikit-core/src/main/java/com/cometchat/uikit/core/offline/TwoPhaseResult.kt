package com.cometchat.uikit.core.offline

import androidx.annotation.RestrictTo
import com.cometchat.chat.exceptions.CometChatException

/**
 * One emission of a two-phase read (G2, BEH-READ-01..09b). A two-phase fetch emits the cached
 * phase first (fast first paint), then the server phase (authoritative), or an [Error].
 *
 * Public so the data-source and repository layers (which already expose custom-implementable
 * interfaces) can carry it across modules, but [RestrictTo] `LIBRARY_GROUP]` — it is internal to
 * the kit, not part of the customer API (which gains only the G14 additions).
 *
 * @param T the row type ([com.cometchat.chat.models.BaseMessage] or
 * [com.cometchat.chat.models.Conversation]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class TwoPhaseResult<out T> {
    /** The locally cached page — the first paint. Never ends pagination, even when empty. */
    public data class Cached<out T>(public val rows: List<T>) : TwoPhaseResult<T>()

    /** The authoritative server page. An empty server page ends history (BEH-READ-09b). */
    public data class Server<out T>(public val rows: List<T>) : TwoPhaseResult<T>()

    /** The read failed. Cached rows already shown are kept; the caller decides the footer. */
    public data class Error(public val exception: CometChatException) : TwoPhaseResult<Nothing>()
}
