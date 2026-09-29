package com.cometchat.uikit.core.state

import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group

/**
 * Sealed class representing UI states for the groups screen.
 * Used by the ViewModel to communicate current state to the UI.
 */
sealed public class GroupsUIState {
    /**
     * Loading state - displayed while fetching groups.
     */
    public object Loading : GroupsUIState()
    
    /**
     * Empty state - displayed when no groups exist.
     */
    public object Empty : GroupsUIState()
    
    /**
     * Error state - displayed when fetching fails.
     * @param exception The exception that caused the error
     */
    public data class Error(val exception: CometChatException) : GroupsUIState()
    
    /**
     * Content state - displayed when groups are available.
     * @param groups The list of groups to display
     */
    public data class Content(val groups: List<Group>) : GroupsUIState()
}
