package com.cometchat.uikit.compose.presentation.shared.mentions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import com.cometchat.uikit.core.mentions.MentionInserter
import com.cometchat.uikit.core.mentions.SelectedMention
import com.cometchat.uikit.core.mentions.SelectedMentionsManager
import com.cometchat.uikit.compose.presentation.shared.formatters.CometChatTextFormatter
import com.cometchat.uikit.compose.presentation.shared.formatters.SuggestionItem
import com.cometchat.uikit.core.utils.CometChatLogger

/**
 * State holder for mention insertion in Compose.
 * 
 * This class manages the state of inserted mentions and provides methods
 * for inserting, tracking, and processing mentions.
 */
public class ComposeMentionInsertionState {
    private val mentionsManager = SelectedMentionsManager()
    private val selectedSuggestionItems = mutableMapOf<String, SuggestionItem>()
    
    // Map of mention ID to its span style for rendering
    private val mentionStyles = mutableMapOf<String, SpanStyle>()
    
    public companion object {
        private const val TAG = "MentionInsertionState"
    }
    
    /**
     * Inserts a mention into the text field value.
     * 
     * @param currentValue The current text field value
     * @param mentionState The current mention detection state
     * @param suggestionItem The suggestion item to insert
     * @param formatter The text formatter
     * @param mentionStyle The style to apply to the mention
     * @return New TextFieldValue with the mention inserted
     */
    public fun insertMention(
        currentValue: TextFieldValue,
        mentionState: ComposeMentionState,
        suggestionItem: SuggestionItem,
        formatter: CometChatTextFormatter,
        mentionStyle: SpanStyle? = null
    ): TextFieldValue {
        if (!mentionState.isActive) return currentValue
        
        
        val result = MentionInserter.calculateInsertion(
            currentText = currentValue.text,
            triggerIndex = mentionState.triggerIndex,
            cursorPosition = mentionState.cursorPosition,
            promptText = suggestionItem.promptText,
            underlyingText = suggestionItem.underlyingText
        )
        
        
        // Track the mention
        val selectedMention = SelectedMention(
            id = suggestionItem.id,
            name = suggestionItem.name,
            promptText = suggestionItem.promptText,
            underlyingText = suggestionItem.underlyingText,
            spanStart = result.spanStart,
            spanEnd = result.spanEnd
        )
        mentionsManager.addMention(selectedMention)
        selectedSuggestionItems[suggestionItem.id] = suggestionItem
        
        CometChatLogger.d(TAG, "insertMention: total tracked mentions=${mentionsManager.getMentions().size}")
        
        // Store style for rendering
        mentionStyle?.let { mentionStyles[suggestionItem.id] = it }
        
        return TextFieldValue(
            text = result.newText,
            selection = TextRange(result.newCursorPosition)
        )
    }
    
    /**
     * Gets the processed text for sending, replacing mentions with underlying text.
     * Uses span positions for accurate replacement instead of string matching.
     */
    public fun getProcessedText(text: String): String {
        
        val mentions = mentionsManager.getMentions()
        CometChatLogger.d(TAG, "getProcessedText: tracked mentions count=${mentions.size}")
        
        if (mentions.isEmpty()) {
            CometChatLogger.d(TAG, "getProcessedText: no mentions tracked, returning original text")
            return text
        }
        
        // Log all tracked mentions
        mentions.forEachIndexed { index, mention ->
        }
        
        // Sort by position descending to replace from end to start (preserves indices)
        val sortedMentions = mentions.sortedByDescending { it.spanStart }
        
        val result = StringBuilder(text)
        for (mention in sortedMentions) {
            val start = mention.spanStart
            val end = mention.spanEnd
            
            
            // Validate bounds
            if (start >= 0 && end <= result.length && start < end) {
                // Verify the text at position matches the prompt text
                val textAtPosition = result.substring(start, end)
                val expectedText = mention.promptText.take(end - start)
                
                
                if (textAtPosition == expectedText) {
                    result.replace(start, end, mention.underlyingText)
                } else {
                }
            } else {
                CometChatLogger.w(TAG, "getProcessedText: invalid bounds! start=$start, end=$end, resultLength=${result.length}")
            }
        }
        
        CometChatLogger.d(TAG, "getProcessedText: final result='$result'")
        return result.toString()
    }
    
    /**
     * Gets all selected suggestion items.
     */
    public fun getSelectedSuggestionItems(): List<SuggestionItem> {
        return selectedSuggestionItems.values.toList()
    }
    
    /**
     * Gets the mentions manager.
     */
    public fun getMentionsManager(): SelectedMentionsManager = mentionsManager
    
    /**
     * Clears all tracked mentions.
     */
    public fun clear() {
        CometChatLogger.d(TAG, "clear: clearing all mentions (was ${mentionsManager.getMentions().size})")
        mentionsManager.clear()
        selectedSuggestionItems.clear()
        mentionStyles.clear()
    }
    
    /**
     * Builds an AnnotatedString with styled mentions.
     * 
     * @param text The text to annotate
     * @param defaultMentionStyle The default style for mentions
     * @return AnnotatedString with styled mentions
     */
    public fun buildAnnotatedText(
        text: String,
        defaultMentionStyle: SpanStyle
    ): AnnotatedString {
        val mentions = mentionsManager.getMentions()
        if (mentions.isEmpty()) {
            return AnnotatedString(text)
        }
        
        return buildAnnotatedString {
            var lastEnd = 0
            val sortedMentions = mentions.sortedBy { it.spanStart }
            
            for (mention in sortedMentions) {
                // Validate span positions
                if (mention.spanStart < lastEnd || mention.spanEnd > text.length) {
                    continue
                }
                
                // Append text before mention
                if (mention.spanStart > lastEnd) {
                    append(text.substring(lastEnd, mention.spanStart))
                }
                
                // Append styled mention
                val style = mentionStyles[mention.id] ?: defaultMentionStyle
                withStyle(style) {
                    val mentionText = if (mention.spanEnd <= text.length) {
                        text.substring(mention.spanStart, mention.spanEnd)
                    } else {
                        mention.promptText
                    }
                    append(mentionText)
                }
                
                // Add annotation for click handling
                addStringAnnotation(
                    tag = "mention",
                    annotation = mention.id,
                    start = mention.spanStart,
                    end = mention.spanEnd
                )
                
                lastEnd = mention.spanEnd
            }
            
            // Append remaining text
            if (lastEnd < text.length) {
                append(text.substring(lastEnd))
            }
        }
    }
    
    /**
     * Updates mention positions after text changes.
     */
    public fun updatePositions(changeStart: Int, changeLength: Int) {
        CometChatLogger.d(TAG, "updatePositions: changeStart=$changeStart, changeLength=$changeLength")
        mentionsManager.updatePositions(changeStart, changeLength)
    }
    
    /**
     * Syncs the state with the current text.
     * Removes mentions that are no longer in the text.
     */
    public fun syncWithText(text: String) {
        val mentions = mentionsManager.getMentions().toList()
        CometChatLogger.d(TAG, "syncWithText: checking ${mentions.size} mentions")
        
        for (mention in mentions) {
            
            // Check if the mention text is still present at the expected position
            if (mention.spanEnd > text.length) {
                // Span extends beyond text - mention was partially deleted
                mentionsManager.removeMention(mention.id)
                selectedSuggestionItems.remove(mention.id)
                mentionStyles.remove(mention.id)
            } else if (mention.spanStart >= 0 && mention.spanEnd <= text.length && mention.spanStart < mention.spanEnd) {
                val textAtPosition = text.substring(mention.spanStart, mention.spanEnd)
                // Check if the text at the span position matches the prompt text
                // Use exact match for the span length
                val expectedText = mention.promptText.take(mention.spanEnd - mention.spanStart)
                
                if (textAtPosition != expectedText) {
                    // Also check if the full promptText exists anywhere in the text
                    // This handles cases where the mention was moved but still exists
                    val containsPrompt = text.contains(mention.promptText)
                    CometChatLogger.d(TAG, "syncWithText: text mismatch, containsPrompt=$containsPrompt")
                    
                    if (!containsPrompt) {
                        mentionsManager.removeMention(mention.id)
                        selectedSuggestionItems.remove(mention.id)
                        mentionStyles.remove(mention.id)
                    }
                } else {
                }
            }
        }
        
        CometChatLogger.d(TAG, "syncWithText: after sync, ${mentionsManager.getMentions().size} mentions remain")
    }
}

/**
 * Remembers a ComposeMentionInsertionState.
 */
@Composable
public fun rememberMentionInsertionState(): ComposeMentionInsertionState {
    return remember { ComposeMentionInsertionState() }
}
