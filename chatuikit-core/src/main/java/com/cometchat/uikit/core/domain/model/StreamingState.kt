package com.cometchat.uikit.core.domain.model

import com.cometchat.chat.exceptions.CometChatException

/**
 * Represents the lifecycle of a streaming session for a given Run ID
 * within the [CometChatAIStreamService].
 *
 * Each active Run ID transitions through these states as AI assistant
 * streaming events are received and processed.
 */
sealed public class StreamingState {

    /** No active streaming session for this Run ID. */
    public object Idle : StreamingState()

    /** Events are actively being received and dispatched for this Run ID. */
    public object Streaming : StreamingState()

    /** All events for this Run ID have been dispatched and the run finished successfully. */
    public object Completed : StreamingState()

    /** The streaming session was interrupted due to an error (e.g., disconnection). */
    public data class Interrupted(val error: CometChatException) : StreamingState()
}
