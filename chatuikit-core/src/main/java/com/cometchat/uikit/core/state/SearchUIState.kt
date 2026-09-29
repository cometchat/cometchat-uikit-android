package com.cometchat.uikit.core.state

import com.cometchat.chat.exceptions.CometChatException

/**
 * Sealed interface representing UI states for the search screen.
 * Used by the ViewModel to communicate current state to the UI.
 */
sealed public interface SearchUIState {
    /**
     * Initial state - displayed before any search is performed.
     */
    public data object Initial : SearchUIState

    /**
     * Loading state - displayed while search is in progress.
     */
    public data object Loading : SearchUIState

    /**
     * Content state - displayed when search results are available.
     */
    public data object Content : SearchUIState

    /**
     * Empty state - displayed when no search results are found.
     */
    public data object Empty : SearchUIState

    /**
     * Error state - displayed when search fails.
     * @param exception The exception that caused the error
     */
    public data class Error(val exception: CometChatException) : SearchUIState
}
