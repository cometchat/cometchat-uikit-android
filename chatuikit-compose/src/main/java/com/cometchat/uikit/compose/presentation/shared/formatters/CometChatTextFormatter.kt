package com.cometchat.uikit.compose.presentation.shared.formatters

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.compose.presentation.shared.formatters.style.PromptTextStyle
import org.json.JSONObject

abstract public class CometChatTextFormatter(private val trackingCharacter: Char) : Formatter {
    private val _suggestionItemList = mutableStateOf<List<SuggestionItem>>(emptyList())
    private val _tagInfoMessage = mutableStateOf<String>("")
    private val _tagInfoVisible = mutableStateOf(false)
    private val _selectedSuggestionItemList = mutableStateOf<List<SuggestionItem>>(emptyList())
    private val _showLoadingIndicator = mutableStateOf(false)
    private var disableSuggestions = false
    private var user: User? = null
    private var group: Group? = null
    
    /**
     * Compose State for suggestion item list. Read this in composables to observe changes.
     */
    public val suggestionItemListState: androidx.compose.runtime.MutableState<List<SuggestionItem>> get() = _suggestionItemList
    
    /**
     * Compose State for loading indicator. Read this in composables to observe changes.
     */
    public val showLoadingIndicatorState: androidx.compose.runtime.MutableState<Boolean> get() = _showLoadingIndicator
    
    /**
     * Compose State for tag info message. Read this in composables to observe changes.
     */
    public val tagInfoMessageState: androidx.compose.runtime.MutableState<String> get() = _tagInfoMessage
    
    /**
     * Compose State for tag info visibility. Read this in composables to observe changes.
     */
    public val tagInfoVisibleState: androidx.compose.runtime.MutableState<Boolean> get() = _tagInfoVisible

    protected fun setDisableSuggestions(disableSuggestions: Boolean) {
        this.disableSuggestions = disableSuggestions
    }

    public fun setSuggestionItemList(suggestionItemList: List<SuggestionItem>) {
        this._suggestionItemList.value = suggestionItemList
    }

    public fun setShowLoadingIndicator(show: Boolean) {
        this._showLoadingIndicator.value = show
    }

    abstract public fun search(context: Context, queryString: String?)

    open public fun onItemClick(context: Context, suggestionItem: SuggestionItem, user: User?, group: Group?) { }

    open public fun handlePreMessageSend(context: Context, baseMessage: BaseMessage) { }

    abstract public fun onScrollToBottom()

    public fun prepareMessageString(
        context: Context,
        baseMessage: BaseMessage,
        text: AnnotatedString,
        messageBubbleAlignment: UIKitConstants.MessageBubbleAlignment,
        formattingType: UIKitConstants.FormattingType
    ): AnnotatedString {
        return when (formattingType) {
            UIKitConstants.FormattingType.MESSAGE_BUBBLE -> {
                when (messageBubbleAlignment) {
                    UIKitConstants.MessageBubbleAlignment.RIGHT ->
                        prepareRightMessageBubbleSpan(context, baseMessage, text)
                    else -> prepareLeftMessageBubbleSpan(context, baseMessage, text)
                }
            }
            UIKitConstants.FormattingType.CONVERSATIONS ->
                prepareConversationSpan(context, baseMessage, text)
            else -> prepareComposerSpan(context, baseMessage, text)
        }
    }

    open public fun prepareLeftMessageBubbleSpan(context: Context, baseMessage: BaseMessage, text: AnnotatedString): AnnotatedString = text
    open public fun prepareRightMessageBubbleSpan(context: Context, baseMessage: BaseMessage, text: AnnotatedString): AnnotatedString = text
    open public fun prepareComposerSpan(context: Context, baseMessage: BaseMessage, text: AnnotatedString): AnnotatedString = text
    open public fun prepareConversationSpan(context: Context, baseMessage: BaseMessage, text: AnnotatedString): AnnotatedString = text

    /**
     * Display span for the reply/edit PREVIEW panels — a read-only surface, distinct from the live
     * composer input. Defaults to [prepareComposerSpan] (so existing formatters are unaffected), but
     * a formatter whose composer span keeps a token intact for the input (e.g. a colour token) should
     * override this to strip the token and apply its style, so the preview shows styled text rather
     * than the raw token.
     */
    open public fun preparePreviewSpan(context: Context, baseMessage: BaseMessage, text: AnnotatedString): AnnotatedString =
        prepareComposerSpan(context, baseMessage, text)

    /**
     * Reverse of the `prepare*Span` rendering: strip THIS formatter's display markup out of [text],
     * returning the storable token form that goes on the wire (e.g. `"{color:#f00}hi{/color}"` from a
     * coloured span). Default is identity — mentions/URLs already keep their token in the text, so they
     * need no reverse step.
     *
     * A custom (Approach-2) formatter that renders its own inline style overrides this so its style
     * survives send: the token stays in the message text and re-renders via `prepare*Span` on every
     * surface. Call it when reading composer text whose display form differs from the stored token
     * (e.g. before send, or when populating the composer to edit an existing message).
     */
    open public fun getOriginalText(text: String): String = text

    /**
     * Optional live-composer rendering: return a [VisualTransformation] that styles THIS formatter's
     * tokens directly in the editable field (e.g. render `{color:#…}…{/color}` as coloured text while
     * hiding the markers). The composer folds every formatter's transformation into its own pipeline,
     * alongside the built-in rich-text and mention transformations.
     *
     * Default is null — the token then simply shows as plain source in the composer and only renders
     * via `prepare*Span` once sent. Override this to make a custom format WYSIWYG while typing. The
     * transformation is display-only; the underlying text (with tokens) is unchanged, so the token
     * still goes on the wire.
     */
    open public fun composerVisualTransformation(): VisualTransformation? = null

    open public fun observeSelectionList(context: Context, selectedSuggestionItemList: List<SuggestionItem>) { }

    public fun setSelectedList(context: Context, selectedSuggestionItemList: List<SuggestionItem>) {
        this._selectedSuggestionItemList.value = selectedSuggestionItemList
        observeSelectionList(context, selectedSuggestionItemList)
    }

    public fun setInfoText(tagInfoMessage: String) { this._tagInfoMessage.value = tagInfoMessage }
    public fun setInfoVisibility(tagInfoVisible: Boolean) { this._tagInfoVisible.value = tagInfoVisible }
    open public fun setGroup(group: Group?) { this.group = group }
    open public fun setUser(user: User?) { this.user = user }

    public fun getSelectedList(): List<SuggestionItem> = _selectedSuggestionItemList.value
    public fun getSuggestionItemList(): List<SuggestionItem> = _suggestionItemList.value
    public fun getTagInfoMessage(): String = _tagInfoMessage.value
    public fun getTagInfoVisibility(): Boolean = _tagInfoVisible.value
    public fun getShowLoadingIndicator(): Boolean = _showLoadingIndicator.value
    open public fun getDisableSuggestions(): Boolean = disableSuggestions
    public fun getTrackingCharacter(): Char = trackingCharacter
    public fun getId(): Char = trackingCharacter
    public fun getUser(): User? = user
    public fun getGroup(): Group? = group
}

public data class SuggestionItem(
    val id: String,
    val name: String,
    val leadingIconUrl: String? = null,
    val status: String? = null,
    val promptText: String,
    val underlyingText: String,
    val data: JSONObject? = null,
    val promptTextStyle: PromptTextStyle? = null,
    val leadingIcon: Int = 0,
    val hideLeadingIcon: Boolean = false,
    val leadingIconStyle: Int = 0
) {
    public fun getPromptTextAppearance(): PromptTextStyle? = promptTextStyle
}
