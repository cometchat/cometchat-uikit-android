package com.cometchat.uikit.compose.preview.presentation.ui.shared

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.uikit.compose.presentation.shared.aiconversationstarter.CometChatAIConversationStarterView
import com.cometchat.uikit.compose.presentation.shared.aiconversationsummary.CometChatAIConversationSummaryView
import com.cometchat.uikit.compose.presentation.shared.aismartreplies.CometChatAISmartRepliesView
import com.cometchat.uikit.core.state.ConversationStarterUIState
import com.cometchat.uikit.core.state.ConversationSummaryUIState
import com.cometchat.uikit.core.state.SmartRepliesUIState
import com.cometchat.uikit.compose.theme.CometChatTheme

// ============================================================================
// AI SMART REPLIES
// ============================================================================

@Preview(showBackground = true, name = "AISmartReplies - Idle")
@Composable
public fun PreviewAISmartRepliesIdle() {
    CometChatTheme {
        CometChatAISmartRepliesView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = SmartRepliesUIState.Idle
        )
    }
}

@Preview(showBackground = true, name = "AISmartReplies - Loading")
@Composable
public fun PreviewAISmartRepliesLoading() {
    CometChatTheme {
        CometChatAISmartRepliesView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = SmartRepliesUIState.Loading
        )
    }
}

@Preview(showBackground = true, name = "AISmartReplies - Loaded")
@Composable
public fun PreviewAISmartRepliesLoaded() {
    CometChatTheme {
        CometChatAISmartRepliesView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = SmartRepliesUIState.Loaded(
                listOf("Sounds good!", "I'll be there", "Let me check my schedule")
            ),
            onClick = { _, _ -> },
            onCloseClick = { }
        )
    }
}

@Preview(showBackground = true, name = "AISmartReplies - Error")
@Composable
public fun PreviewAISmartRepliesError() {
    CometChatTheme {
        CometChatAISmartRepliesView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = SmartRepliesUIState.Error(
                CometChatException("AI_ERROR", "Failed to generate replies")
            )
        )
    }
}

// ============================================================================
// AI CONVERSATION STARTER
// ============================================================================

@Preview(showBackground = true, name = "AIConversationStarter - Idle")
@Composable
public fun PreviewAIConversationStarterIdle() {
    CometChatTheme {
        CometChatAIConversationStarterView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = ConversationStarterUIState.Idle
        )
    }
}

@Preview(showBackground = true, name = "AIConversationStarter - Loading")
@Composable
public fun PreviewAIConversationStarterLoading() {
    CometChatTheme {
        CometChatAIConversationStarterView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = ConversationStarterUIState.Loading
        )
    }
}

@Preview(showBackground = true, name = "AIConversationStarter - Loaded")
@Composable
public fun PreviewAIConversationStarterLoaded() {
    CometChatTheme {
        CometChatAIConversationStarterView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = ConversationStarterUIState.Loaded(
                listOf("How's your day going?", "What are you working on?", "Any plans for the weekend?")
            ),
            onClick = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "AIConversationStarter - Error")
@Composable
public fun PreviewAIConversationStarterError() {
    CometChatTheme {
        CometChatAIConversationStarterView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = ConversationStarterUIState.Error(
                CometChatException("AI_ERROR", "Failed to generate starters")
            )
        )
    }
}

// ============================================================================
// AI CONVERSATION SUMMARY
// ============================================================================

@Preview(showBackground = true, name = "AIConversationSummary - Idle")
@Composable
public fun PreviewAIConversationSummaryIdle() {
    CometChatTheme {
        CometChatAIConversationSummaryView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = ConversationSummaryUIState.Idle
        )
    }
}

@Preview(showBackground = true, name = "AIConversationSummary - Loading")
@Composable
public fun PreviewAIConversationSummaryLoading() {
    CometChatTheme {
        CometChatAIConversationSummaryView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = ConversationSummaryUIState.Loading
        )
    }
}

@Preview(showBackground = true, name = "AIConversationSummary - Loaded")
@Composable
public fun PreviewAIConversationSummaryLoaded() {
    CometChatTheme {
        CometChatAIConversationSummaryView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = ConversationSummaryUIState.Loaded(
                "Alice and Bob discussed the upcoming product launch. Key decisions: 1) Launch date set for next Friday, 2) Marketing team to prepare press release, 3) Engineering to complete final QA by Wednesday."
            ),
            onCloseClick = { }
        )
    }
}

@Preview(showBackground = true, name = "AIConversationSummary - Error")
@Composable
public fun PreviewAIConversationSummaryError() {
    CometChatTheme {
        CometChatAIConversationSummaryView(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            uiState = ConversationSummaryUIState.Error(
                CometChatException("AI_ERROR", "Failed to generate summary")
            )
        )
    }
}
