package com.cometchat.uikit.core.offline

import androidx.annotation.RestrictTo

/**
 * A one-line, transient user notice raised by the offline path (e.g. "1 unsent message was
 * removed"). The list ViewModels expose these as `offlineNotices`; the UI renders each once.
 *
 * Public so the UI modules can render it, but [RestrictTo] `LIBRARY_GROUP]` — internal to the kit.
 * Nothing emits these in W1; W4/W5 raise them and add the public `hideOfflineNotices` setter.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class OfflineNotice {
    /** The offline send queue is full (BEH-SEND, `cometchat_queue_full`). */
    public object QueueFull : OfflineNotice()

    /** A file is too large to send offline (`cometchat_file_too_large_offline`). */
    public object FileTooLargeOffline : OfflineNotice()

    /** Mid-send Retry/Delete while the SDK reports the message in flight (D33b). */
    public object SendingTryAgain : OfflineNotice()

    /** An unsent row was removed; [text] is its text for Copy, when available (R14). */
    public data class UnsentRemoved(public val text: String?) : OfflineNotice()

    /** [count] queued messages did not send. */
    public data class MessagesNotSent(public val count: Int) : OfflineNotice()

    /** [count] unsent messages were lost on a cache reset. */
    public data class UnsentLost(public val count: Int) : OfflineNotice()

    /** Unsent messages may have been lost on a cache reset (count unknown). */
    public object UnsentMaybeLost : OfflineNotice()
}
