package com.cometchat.uikit.core.state

import com.cometchat.chat.exceptions.CometChatException

/**
 * UI states shared by the Pinned Messages and Saved Messages list screens.
 */
public sealed class PinnedSavedListUIState {
    /** Displayed while the first page is loading. */
    public object Loading : PinnedSavedListUIState()

    /** Displayed when there is at least one message to show. */
    public object Content : PinnedSavedListUIState()

    /** Displayed when the list is empty. */
    public object Empty : PinnedSavedListUIState()

    /** Displayed when loading fails. */
    public data class Error(val exception: CometChatException) : PinnedSavedListUIState()
}
