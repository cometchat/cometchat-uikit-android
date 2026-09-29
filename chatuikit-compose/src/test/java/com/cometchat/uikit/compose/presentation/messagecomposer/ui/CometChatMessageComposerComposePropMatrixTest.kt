package com.cometchat.uikit.compose.presentation.messagecomposer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.core.UploadFileRequest
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.cometchat.chat.upload.UploadFileListener
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.presentation.messagecomposer.style.CometChatAttachmentTileStyle
import com.cometchat.uikit.compose.presentation.messagecomposer.style.CometChatAttachmentTrayStyle
import com.cometchat.uikit.compose.presentation.messagecomposer.style.CometChatMessageComposerStyle
import com.cometchat.uikit.compose.presentation.shared.erroralert.style.CometChatErrorAlertStyle
import com.cometchat.uikit.compose.presentation.shared.formatters.CometChatTextFormatter
import com.cometchat.uikit.compose.presentation.shared.formatters.SuggestionItem
import com.cometchat.uikit.compose.presentation.shared.suggestionlist.CometChatSuggestionListStyle
import com.cometchat.uikit.compose.presentation.stickerkeyboard.style.CometChatStickerKeyboardStyle
import com.cometchat.uikit.compose.shared.views.popupmenu.CometChatPopupMenuStyle
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.data.datasource.MessageComposerDataSource
import com.cometchat.uikit.core.data.repository.MessageComposerRepositoryImpl
import com.cometchat.uikit.core.domain.model.CometChatMessageComposerAction
import com.cometchat.uikit.core.domain.model.ComposerLayoutMode
import com.cometchat.uikit.core.domain.usecase.EditMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendCustomMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendMediaMessageUseCase
import com.cometchat.uikit.core.domain.usecase.SendTextMessageUseCase
import com.cometchat.uikit.core.formatter.RichTextFormat
import com.cometchat.uikit.core.models.AttachmentSource
import com.cometchat.uikit.core.models.StagedAttachmentInput
import com.cometchat.uikit.core.viewmodel.CometChatMessageComposerViewModel
import com.cometchat.uikit.propmatrix.ComposePropMatrixBuilder
import com.cometchat.uikit.propmatrix.Denominator
import com.cometchat.uikit.propmatrix.Prop
import com.cometchat.uikit.propmatrix.PropKind
import com.cometchat.uikit.propmatrix.composePropMatrix
import com.cometchat.uikit.propmatrix.evaluate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Property (prop-matrix) layer for the Compose [CometChatMessageComposer] — the widest
 * parameter list in the toolkit at 57, `modifier` excluded by [Denominator], leaving 56.
 *
 * The View twin is a reflective sweep over 110 setters
 * ([com.cometchat.uikit.kotlin.presentation.messagecomposer.CometChatMessageComposerPropMatrixTest]);
 * this side is hand-declared, because `@Composable` parameters are invisible to runtime
 * reflection.
 *
 * The fixture is one composition whose every param is backed by a `mutableState`, so an
 * entry flips its own prop, asserts, and puts it back. That matters more here than on the
 * satellites: the composer's props interact (the voice button hides once there is text,
 * the extension options vanish inside a thread, staged attachments gate the send button),
 * so entries are ordered — thread id before staging, staging after the button-visibility
 * entries — and each one restores what it changed.
 *
 * Where a prop is observable in state, it is asserted in state rather than in pixels: the
 * eight `hide*Option` flags reach the ViewModel, the popup rebuilds from it, and the entry
 * watches the menu item appear and disappear. Colours are read from painted pixels only
 * where nothing else can tell "the composer applied it" from "a child did".
 *
 * Two of the composer's surfaces compose into their own windows (the attachment popup and
 * the transient error alert), which the decor-view bitmap does not include. Their style
 * entries assert a *measurable* consequence instead — a taller menu item, a taller alert
 * line — so neither passes on a style that was ignored.
 *
 * Six props are waived; see [waivedProps]. Every one is a defect — three dead parameters,
 * a pair that a stale `remember` makes unreachable, and a callback behind a panel the
 * composer populates from a ViewModel it does not expose — which is the finding this
 * matrix exists to surface, not a gap in the layer.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xxhdpi")
class CometChatMessageComposerComposePropMatrixTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private companion object {
        const val OWNER = "CometChatMessageComposer"

        const val USER_UID = "user_target"
        const val GROUP_GUID = "group_target"
        const val THREAD_PARENT = 42L

        // DefaultSendButton builds these in code rather than from a string resource.
        const val SEND_ACTIVE = "Send message"
        const val SEND_INACTIVE = "Send button disabled"

        const val PLACEHOLDER = "say something"
        const val HEADER_SLOT = "host header"
        const val FOOTER_SLOT = "host footer"
        const val SEND_SLOT = "host send"
        const val SECONDARY_SLOT = "host secondary"
        const val AUX_SLOT = "host auxiliary"
        const val EDIT_SLOT = "host edit preview"
        const val REPLY_SLOT = "host reply preview"

        const val CUSTOM_OPTION_ID = "LOCATION"
        const val CUSTOM_OPTION_TITLE = "Share Location"

        const val ATTACHMENT_NAME = "report.pdf"
        const val UPLOADING_LABEL = "Uploading…"
        const val REJECTED_LABEL = "Upload failed"
        const val REJECTION_REASON = "that file is too large"

        const val SUGGESTION_NAME = "Iron Man"

        const val MAGENTA_ARGB: Int = 0xFFFF00FF.toInt()
        const val CYAN_ARGB: Int = 0xFF00FFFF.toInt()
        val MAGENTA = Color(0xFFFF00FF)
        val CYAN = Color(0xFF00FFFF)
    }

    /**
     * A formatter that records what the composer does to it. The composer's only
     * contract with a host-supplied formatter is "sync the chat target onto it, then ask
     * it to search when a mention is typed", and both are observable here without
     * touching the SDK.
     */
    private class RecordingFormatter : CometChatTextFormatter('@') {
        var lastUser: User? = null
        var lastGroup: Group? = null
        var searchedFor: String? = null

        fun forget() {
            lastUser = null
            lastGroup = null
            searchedFor = null
        }

        override fun setUser(user: User?) {
            super.setUser(user)
            lastUser = user
        }

        override fun setGroup(group: Group?) {
            super.setGroup(group)
            lastGroup = group
        }

        override fun search(context: Context, queryString: String?) {
            searchedFor = queryString
            setSuggestionItemList(
                listOf(
                    SuggestionItem(
                        id = USER_UID,
                        name = SUGGESTION_NAME,
                        promptText = SUGGESTION_NAME,
                        underlyingText = "<@uid:$USER_UID>",
                        hideLeadingIcon = true,
                    ),
                ),
            )
        }

        override fun onScrollToBottom() = Unit
    }

    private lateinit var cometChat: MockedStatic<CometChat>
    private lateinit var uploadRequest: UploadFileRequest

    /** Captured from the mocked SDK upload request, to drive tile lifecycle events. */
    private var uploadListener: UploadFileListener? = null

    private val viewModel: CometChatMessageComposerViewModel by lazy {
        val repository = MessageComposerRepositoryImpl(mock<MessageComposerDataSource>())
        CometChatMessageComposerViewModel(
            sendTextMessageUseCase = SendTextMessageUseCase(repository),
            sendMediaMessageUseCase = SendMediaMessageUseCase(repository),
            sendCustomMessageUseCase = SendCustomMessageUseCase(repository),
            editMessageUseCase = EditMessageUseCase(repository),
            enableListeners = false,
        )
    }

    @Before
    fun stubSdkAndImageLoader() {
        cometChat = Mockito.mockStatic(CometChat::class.java)
        val me = mock<User>()
        whenever(me.uid).thenReturn("logged-in-user")
        whenever(me.name).thenReturn("Logged In User")
        cometChat.`when`<User?> { CometChat.getLoggedInUser() }.thenReturn(me)

        // The upload pipeline is intercepted at the same static seam the ViewModel's own
        // staging tests use, so a staged tile exists without a byte moving.
        uploadRequest = mock()
        whenever(uploadRequest.batchId).thenReturn("batch-1")
        whenever(uploadRequest.setBatchId(any<String>())).thenReturn(uploadRequest)
        whenever(uploadRequest.setParentMessageId(any<Long>())).thenReturn(uploadRequest)
        whenever(uploadRequest.uploadAttachments(any(), any())).thenAnswer { invocation ->
            uploadListener = invocation.getArgument(1)
            null
        }
        cometChat.`when`<UploadFileRequest> {
            CometChat.createUploadFileRequest(any<String>(), any<String>())
        }.thenReturn(uploadRequest)
    }

    @After
    fun closeStatic() = cometChat.close()

    // ==================== helpers ====================

    private fun string(id: Int): String = composeRule.activity.getString(id)

    private fun nodesWithCd(description: String): Int =
        composeRule.onAllNodesWithContentDescription(description).fetchSemanticsNodes().size

    private fun nodesWithText(text: String): Int =
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size

    /** Height of a node's box, for the two surfaces whose pixels live in another window. */
    private fun heightOf(text: String): Dp = composeRule.onNodeWithText(text)
        .getUnclippedBoundsInRoot()
        .let { it.bottom - it.top }

    /** See the bubble matrices: `captureToImage()` has no window here. */
    private fun paintedColours(): Set<Int> {
        val view = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(
            view.width.coerceAtLeast(1),
            view.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        view.draw(Canvas(bitmap))
        val seen = HashSet<Int>()
        for (y in 0 until bitmap.height step 2) {
            for (x in 0 until bitmap.width step 2) seen += bitmap.getPixel(x, y)
        }
        return seen
    }

    private fun type(text: String) {
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput(text)
        composeRule.waitForIdle()
    }

    private fun clearInput() {
        composeRule.onAllNodes(hasSetTextAction())[0].performTextClearance()
        composeRule.waitForIdle()
    }

    private fun openAttachmentPopup() {
        if (nodesWithText(string(R.string.cometchat_camera)) == 0) {
            composeRule.onNodeWithContentDescription(string(R.string.cometchat_a11y_open_attachments))
                .performClick()
            composeRule.waitForIdle()
        }
    }

    /** Clicks a popup option, which also dismisses the menu. */
    private fun clickOption(title: String) {
        openAttachmentPopup()
        composeRule.onNodeWithText(title).performClick()
        composeRule.waitForIdle()
    }

    private fun tileDescription(statusLabel: String) = "$ATTACHMENT_NAME, $statusLabel"

    private fun stagedInput() = StagedAttachmentInput(
        file = File(composeRule.activity.cacheDir, ATTACHMENT_NAME).apply { writeText("x") },
        name = ATTACHMENT_NAME,
        size = 1_000L,
        mimeType = "application/pdf",
        source = AttachmentSource.PICKER,
    )

    @Test
    fun messageComposer_propMatrix_coversEveryObservableProp() {
        val user = User().apply { uid = USER_UID; name = "Iron Man" }
        val formatter = RecordingFormatter()

        val editTarget = mock<TextMessage>()
        whenever(editTarget.id).thenReturn(101)
        whenever(editTarget.text).thenReturn("the message being edited")
        whenever(editTarget.sender).thenReturn(user)

        val replyTarget = mock<TextMessage>()
        whenever(replyTarget.id).thenReturn(202)
        whenever(replyTarget.text).thenReturn("the message being replied to")
        whenever(replyTarget.sender).thenReturn(user)

        // ---- every param, in a state the entries can move ----
        var group by mutableStateOf<Group?>(null)
        var parentMessageId by mutableStateOf(0L)
        var composerStyle by mutableStateOf<CometChatMessageComposerStyle?>(null)
        var popupStyle by mutableStateOf<CometChatPopupMenuStyle?>(null)
        var trayStyle by mutableStateOf<CometChatAttachmentTrayStyle?>(null)
        var tileStyle by mutableStateOf<CometChatAttachmentTileStyle?>(null)
        var errorAlertStyle by mutableStateOf<CometChatErrorAlertStyle?>(null)
        var stickerStyle by mutableStateOf<CometChatStickerKeyboardStyle?>(null)
        var suggestionStyle by mutableStateOf<CometChatSuggestionListStyle?>(null)
        var multipleAttachments by mutableStateOf(true)
        var hideAttachmentButton by mutableStateOf(false)
        var hideVoiceRecordingButton by mutableStateOf(false)
        var hideSendButton by mutableStateOf(false)
        var hideAuxiliaryButton by mutableStateOf(false)
        var hideStickersButton by mutableStateOf(false)
        var richTextFormatting by mutableStateOf(true)
        var enabledFormats by mutableStateOf(setOf(RichTextFormat.BOLD, RichTextFormat.ITALIC))
        var disableTypingEvents by mutableStateOf(false)
        var disableMentions by mutableStateOf(false)
        var headerSlot by mutableStateOf(false)
        var footerSlot by mutableStateOf(false)
        var sendSlot by mutableStateOf(false)
        var secondarySlot by mutableStateOf(false)
        var auxiliarySlot by mutableStateOf(false)
        var editSlot by mutableStateOf(false)
        var replySlot by mutableStateOf(false)
        var hideCameraOption by mutableStateOf(false)
        var hideImageOption by mutableStateOf(false)
        var hideVideoOption by mutableStateOf(false)
        var hideAudioOption by mutableStateOf(false)
        var hideFileOption by mutableStateOf(false)
        var hidePollOption by mutableStateOf(false)
        var hideCollaborativeDocumentOption by mutableStateOf(false)
        var hideCollaborativeWhiteboardOption by mutableStateOf(false)
        // Passed from the first composition, the way a host would, which is exactly the
        // configuration the waiver below documents as producing nothing.
        val attachmentOptions = listOf(
            CometChatMessageComposerAction(
                id = CUSTOM_OPTION_ID,
                title = CUSTOM_OPTION_TITLE,
                icon = R.drawable.cometchat_ic_file_upload,
            ),
        )
        var formatters by mutableStateOf<List<CometChatTextFormatter>?>(listOf(formatter))

        // ---- what the callbacks recorded ----
        var sentMessage: com.cometchat.chat.models.BaseMessage? = null
        var lastError: CometChatException? = null
        var lastText: String? = null
        var cameraClicked = false
        var imageClicked = false
        var videoClicked = false
        var audioClicked = false
        var documentClicked = false
        var pollClicked = false
        var documentBoardClicked = false
        var whiteboardClicked = false
        var clickedCustomOption: CometChatMessageComposerAction? = null
        var mentionClicked: SuggestionItem? = null
        var auxSlotUser: User? = null

        // The default() factories are @Composable — they read CometChatTheme — so they can
        // only be evaluated inside the composition. Captured for the entries to copy() from.
        var defaultComposerStyle: CometChatMessageComposerStyle? = null
        var defaultPopupStyle: CometChatPopupMenuStyle? = null
        var defaultTrayStyle: CometChatAttachmentTrayStyle? = null
        var defaultTileStyle: CometChatAttachmentTileStyle? = null
        var defaultErrorAlertStyle: CometChatErrorAlertStyle? = null
        var defaultStickerStyle: CometChatStickerKeyboardStyle? = null
        var defaultSuggestionStyle: CometChatSuggestionListStyle? = null

        composeRule.setContent {
            CometChatTheme {
                defaultComposerStyle = CometChatMessageComposerStyle.default()
                defaultPopupStyle = CometChatPopupMenuStyle.default()
                defaultTrayStyle = CometChatAttachmentTrayStyle.default()
                defaultTileStyle = CometChatAttachmentTileStyle.default()
                defaultErrorAlertStyle = CometChatErrorAlertStyle.default()
                defaultStickerStyle = CometChatStickerKeyboardStyle.default()
                defaultSuggestionStyle = CometChatSuggestionListStyle.default()

                CometChatMessageComposer(
                    modifier = Modifier.fillMaxWidth(),
                    user = user,
                    group = group,
                    parentMessageId = parentMessageId,
                    viewModel = viewModel,
                    style = composerStyle ?: defaultComposerStyle!!,
                    attachmentPopupStyle = popupStyle ?: defaultPopupStyle!!,
                    attachmentTrayStyle = trayStyle ?: defaultTrayStyle!!,
                    attachmentTileStyle = tileStyle ?: defaultTileStyle!!,
                    attachmentErrorAlertStyle = errorAlertStyle ?: defaultErrorAlertStyle!!,
                    enableMultipleAttachments = multipleAttachments,
                    hideAttachmentButton = hideAttachmentButton,
                    hideVoiceRecordingButton = hideVoiceRecordingButton,
                    hideSendButton = hideSendButton,
                    hideAuxiliaryButton = hideAuxiliaryButton,
                    hideStickersButton = hideStickersButton,
                    enableRichTextFormatting = richTextFormatting,
                    layoutMode = ComposerLayoutMode.SINGLE_LINE,
                    disableTypingEvents = disableTypingEvents,
                    disableSoundForMessages = false,
                    disableMentions = disableMentions,
                    maxLines = 5,
                    placeholderText = PLACEHOLDER,
                    enabledFormats = enabledFormats,
                    headerView = if (headerSlot) ({ Text(HEADER_SLOT) }) else null,
                    footerView = if (footerSlot) ({ Text(FOOTER_SLOT) }) else null,
                    sendButtonView = if (sendSlot) ({ _, _, _ -> Text(SEND_SLOT) }) else null,
                    secondaryButtonView = if (secondarySlot) ({ _, _ -> Text(SECONDARY_SLOT) }) else null,
                    auxiliaryButtonView = if (auxiliarySlot) ({ slotUser, _, _ ->
                        auxSlotUser = slotUser
                        Text(AUX_SLOT)
                    }) else null,
                    editPreviewView = if (editSlot) ({ _, _ -> Text(EDIT_SLOT) }) else null,
                    replyPreviewView = if (replySlot) ({ _, _ -> Text(REPLY_SLOT) }) else null,
                    onSendButtonClick = { _, message -> sentMessage = message },
                    onError = { lastError = it },
                    onTextChanged = { lastText = it },
                    onCameraClick = { cameraClicked = true; true },
                    onImageClick = { imageClicked = true; true },
                    onVideoClick = { videoClicked = true; true },
                    onAudioClick = { audioClicked = true; true },
                    onDocumentClick = { documentClicked = true; true },
                    stickerKeyboardStyle = stickerStyle ?: defaultStickerStyle!!,
                    onStickerSelected = { /* unreachable here — see waivedProps() */ },
                    hideCameraOption = hideCameraOption,
                    hideImageOption = hideImageOption,
                    hideVideoOption = hideVideoOption,
                    hideAudioOption = hideAudioOption,
                    hideFileOption = hideFileOption,
                    hidePollOption = hidePollOption,
                    hideCollaborativeDocumentOption = hideCollaborativeDocumentOption,
                    hideCollaborativeWhiteboardOption = hideCollaborativeWhiteboardOption,
                    attachmentOptions = attachmentOptions,
                    onAttachmentOptionClick = { clickedCustomOption = it },
                    onPollClick = { pollClicked = true; true },
                    onCollaborativeDocumentClick = { documentBoardClicked = true; true },
                    onCollaborativeWhiteboardClick = { whiteboardClicked = true; true },
                    textFormatters = formatters,
                    suggestionListStyle = suggestionStyle ?: defaultSuggestionStyle!!,
                    onMentionClick = { mentionClicked = it },
                )
            }
        }
        composeRule.waitForIdle()

        /**
         * The eight attachment-option flags are the same shape: the param reaches the
         * view model, the popup rebuilds from it, and the option leaves the menu. Written
         * once so the eight entries read as the eight props they are, not as eight copies.
         */
        val optionEntry: ComposePropMatrixBuilder.(String, String, (Boolean) -> Unit, () -> Boolean) -> Unit =
            { name, title, hide, flag ->
                value(name) {
                    openAttachmentPopup()
                    assertEquals("$title should start out in the menu", 1, nodesWithText(title))
                    assertTrue("and the view model should agree", flag())
                    hide(true)
                    composeRule.waitForIdle()
                    assertTrue("hiding it should reach the view model", !flag())
                    assertEquals("and take the option out of the menu", 0, nodesWithText(title))
                    hide(false)
                    composeRule.waitForIdle()
                    assertEquals(1, nodesWithText(title))
                }
            }

        val matrix = composePropMatrix(OWNER) {

            // ==================== chat target ====================

            value("user") {
                assertEquals(
                    "the composer should hand its user to the view model",
                    USER_UID,
                    viewModel.user.value?.uid,
                )
            }

            value("group") {
                group = Group().apply { guid = GROUP_GUID; name = "Avengers" }
                composeRule.waitForIdle()
                assertEquals(GROUP_GUID, viewModel.group.value?.guid)
                group = null
                composeRule.waitForIdle()
            }

            value("parentMessageId") {
                // Also the reason this entry runs before anything stages an attachment:
                // changing the thread target clears the staged batch.
                parentMessageId = THREAD_PARENT
                composeRule.waitForIdle()
                assertEquals(
                    "a thread id should reach the view model's id map",
                    THREAD_PARENT.toString(),
                    viewModel.idMap.value[UIKitConstants.MapId.PARENT_MESSAGE_ID],
                )
                parentMessageId = 0L
                composeRule.waitForIdle()
                assertNull(
                    "leaving the thread should drop the parent id again",
                    viewModel.idMap.value[UIKitConstants.MapId.PARENT_MESSAGE_ID],
                )
            }

            value("viewModel") {
                // The composer renders from the view model the host handed it, not one of
                // its own: state pushed in from outside has to reach the screen.
                viewModel.setEditMessage(editTarget)
                composeRule.waitForIdle()
                assertEquals(
                    "an edit set on the host's view model should open the edit preview",
                    1,
                    nodesWithCd(string(R.string.cometchat_a11y_close_edit_preview)),
                )
                // Clearing the edit leaves the text it pushed into the input behind, so the
                // box is emptied here rather than in the entries below, which need it empty.
                viewModel.clearEditMessage()
                composeRule.waitForIdle()
                clearInput()
            }

            // ==================== text input ====================

            value("placeholderText") {
                composeRule.onNodeWithText(PLACEHOLDER).assertIsDisplayed()
                assertEquals(
                    "the supplied placeholder should replace the built-in one",
                    0,
                    nodesWithText(string(R.string.cometchat_composer_place_holder_text)),
                )
            }

            callback("onTextChanged") {
                type("hello")
                assertEquals("hello", lastText)
                clearInput()
            }

            callback("onSendButtonClick") {
                type("ship it")
                composeRule.onNodeWithContentDescription(SEND_ACTIVE).performClick()
                composeRule.waitForIdle()
                assertEquals(
                    "the host's handler should receive the composed message",
                    "ship it",
                    (sentMessage as? TextMessage)?.text,
                )
                clearInput()
            }

            value("disableTypingEvents") {
                disableTypingEvents = true
                composeRule.waitForIdle()
                assertTrue(
                    "the flag should reach the view model that emits the events",
                    viewModel.disableTypingEvents,
                )
                disableTypingEvents = false
                composeRule.waitForIdle()
                assertTrue(!viewModel.disableTypingEvents)
            }

            // ==================== rich text ====================

            value("enableRichTextFormatting") {
                assertEquals("the toolbar should be up", 1, nodesWithCd(string(R.string.cometchat_a11y_bold)))
                richTextFormatting = false
                composeRule.waitForIdle()
                assertEquals(
                    "disabling formatting should take the toolbar away, not merely grey it",
                    0,
                    nodesWithCd(string(R.string.cometchat_a11y_bold)),
                )
                richTextFormatting = true
                composeRule.waitForIdle()
            }

            value("enabledFormats") {
                assertEquals(1, nodesWithCd(string(R.string.cometchat_a11y_italic)))
                enabledFormats = setOf(RichTextFormat.BOLD)
                composeRule.waitForIdle()
                assertEquals(
                    "a format left out of the set should not get a button",
                    0,
                    nodesWithCd(string(R.string.cometchat_a11y_italic)),
                )
                assertEquals(1, nodesWithCd(string(R.string.cometchat_a11y_bold)))
                enabledFormats = setOf(RichTextFormat.BOLD, RichTextFormat.ITALIC)
                composeRule.waitForIdle()
            }

            // ==================== mentions ====================

            value("textFormatters") {
                assertEquals(
                    "the composer should sync the chat target onto the host's formatter",
                    USER_UID,
                    formatter.lastUser?.uid,
                )
            }

            value("disableMentions") {
                formatter.forget()
                disableMentions = true
                composeRule.waitForIdle()
                assertNull(
                    "with mentions off the host's formatter should not be used at all",
                    formatter.lastUser,
                )
                disableMentions = false
                composeRule.waitForIdle()
                assertEquals(
                    "turning mentions back on should re-sync the formatter",
                    USER_UID,
                    formatter.lastUser?.uid,
                )
            }

            value("suggestionListStyle") {
                suggestionStyle = defaultSuggestionStyle!!.copy(backgroundColor = MAGENTA)
                type("@ir")
                composeRule.onNodeWithText(SUGGESTION_NAME).assertIsDisplayed()
                assertTrue(
                    "the suggestion list's own background should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                suggestionStyle = null
                clearInput()
            }

            callback("onMentionClick") {
                type("@ir")
                composeRule.onNodeWithText(SUGGESTION_NAME).performClick()
                composeRule.waitForIdle()
                assertEquals(
                    "the picked suggestion should reach the host",
                    USER_UID,
                    mentionClicked?.id,
                )
                clearInput()
            }

            // ==================== slots ====================

            slot("headerView") {
                headerSlot = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(HEADER_SLOT).assertIsDisplayed()
                headerSlot = false
                composeRule.waitForIdle()
            }

            slot("footerView") {
                footerSlot = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(FOOTER_SLOT).assertIsDisplayed()
                footerSlot = false
                composeRule.waitForIdle()
            }

            slot("sendButtonView") {
                sendSlot = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(SEND_SLOT).assertIsDisplayed()
                assertEquals(
                    "the slot should replace the default send button, not sit beside it",
                    0,
                    nodesWithCd(SEND_INACTIVE),
                )
                sendSlot = false
                composeRule.waitForIdle()
            }

            slot("secondaryButtonView") {
                secondarySlot = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(SECONDARY_SLOT).assertIsDisplayed()
                assertEquals(
                    "the slot should replace the default attachment button",
                    0,
                    nodesWithCd(string(R.string.cometchat_a11y_open_attachments)),
                )
                secondarySlot = false
                composeRule.waitForIdle()
            }

            slot("auxiliaryButtonView") {
                auxiliarySlot = true
                composeRule.waitForIdle()
                composeRule.onNodeWithText(AUX_SLOT).assertIsDisplayed()
                assertEquals(
                    "the slot is handed the chat target, which is what makes it useful",
                    USER_UID,
                    auxSlotUser?.uid,
                )
                auxiliarySlot = false
                composeRule.waitForIdle()
            }

            slot("editPreviewView") {
                editSlot = true
                viewModel.setEditMessage(editTarget)
                composeRule.waitForIdle()
                composeRule.onNodeWithText(EDIT_SLOT).assertIsDisplayed()
                assertEquals(
                    "the slot should replace the built-in edit preview",
                    0,
                    nodesWithCd(string(R.string.cometchat_a11y_close_edit_preview)),
                )
                viewModel.clearEditMessage()
                editSlot = false
                composeRule.waitForIdle()
                // Same as the viewModel entry: the edit's text outlives the edit, and the
                // button-visibility entries below read an empty composer.
                clearInput()
            }

            slot("replyPreviewView") {
                replySlot = true
                viewModel.setReplyMessage(replyTarget)
                composeRule.waitForIdle()
                composeRule.onNodeWithText(REPLY_SLOT).assertIsDisplayed()
                assertEquals(
                    "the slot should replace the built-in reply preview",
                    0,
                    nodesWithCd(string(R.string.cometchat_a11y_close_reply_preview)),
                )
                viewModel.clearReplyMessage()
                replySlot = false
                composeRule.waitForIdle()
            }

            // ==================== button visibility ====================

            value("hideAttachmentButton") {
                val cd = string(R.string.cometchat_a11y_open_attachments)
                assertEquals(1, nodesWithCd(cd))
                hideAttachmentButton = true
                composeRule.waitForIdle()
                assertEquals(0, nodesWithCd(cd))
                hideAttachmentButton = false
                composeRule.waitForIdle()
            }

            value("hideVoiceRecordingButton") {
                val cd = string(R.string.cometchat_a11y_record_voice_message)
                assertEquals(1, nodesWithCd(cd))
                hideVoiceRecordingButton = true
                composeRule.waitForIdle()
                assertEquals(0, nodesWithCd(cd))
                hideVoiceRecordingButton = false
                composeRule.waitForIdle()
            }

            value("hideSendButton") {
                assertEquals(1, nodesWithCd(SEND_INACTIVE))
                hideSendButton = true
                composeRule.waitForIdle()
                assertEquals(0, nodesWithCd(SEND_INACTIVE))
                hideSendButton = false
                composeRule.waitForIdle()
            }

            value("hideAuxiliaryButton") {
                // The auxiliary block carries the sticker button and the voice button, so
                // hiding the block takes both away — which is what tells this prop apart
                // from hideStickersButton below.
                val stickers = string(R.string.cometchat_a11y_open_stickers)
                val voice = string(R.string.cometchat_a11y_record_voice_message)
                hideAuxiliaryButton = true
                composeRule.waitForIdle()
                assertEquals(0, nodesWithCd(stickers))
                assertEquals(0, nodesWithCd(voice))
                hideAuxiliaryButton = false
                composeRule.waitForIdle()
                assertEquals(1, nodesWithCd(stickers))
            }

            value("hideStickersButton") {
                val cd = string(R.string.cometchat_a11y_open_stickers)
                assertEquals(1, nodesWithCd(cd))
                hideStickersButton = true
                composeRule.waitForIdle()
                assertEquals(0, nodesWithCd(cd))
                assertEquals(
                    "only the sticker button goes; the voice button stays",
                    1,
                    nodesWithCd(string(R.string.cometchat_a11y_record_voice_message)),
                )
                hideStickersButton = false
                composeRule.waitForIdle()
            }

            // ==================== inline styles ====================

            value("style") {
                composerStyle = defaultComposerStyle!!.copy(backgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the composer's own backgroundColor should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                composerStyle = null
                composeRule.waitForIdle()
            }

            value("stickerKeyboardStyle") {
                stickerStyle = defaultStickerStyle!!.copy(backgroundColor = CYAN)
                composeRule.onNodeWithContentDescription(string(R.string.cometchat_a11y_open_stickers))
                    .performClick()
                composeRule.waitForIdle()
                assertTrue(
                    "the sticker panel's background should reach the screen",
                    CYAN_ARGB in paintedColours(),
                )
                composeRule.onNodeWithContentDescription(string(R.string.cometchat_a11y_close_stickers))
                    .performClick()
                composeRule.waitForIdle()
                stickerStyle = null
            }

            // ==================== attachment options ====================

            value("attachmentPopupStyle") {
                // The composer overwrites three of this style's fields on the way through
                // (cornerRadius, itemPaddingVertical, startIconTint) to match the View
                // toolkit, so the entry moves a field that is passed through untouched.
                // The menu composes into its own window, which the decor-view bitmap does
                // not include, so what is asserted is a measurable consequence of the
                // style rather than its pixels.
                openAttachmentPopup()
                val cameraTitle = string(R.string.cometchat_camera)
                val plain = heightOf(cameraTitle)
                popupStyle = defaultPopupStyle!!.copy(
                    itemTextStyle = defaultPopupStyle!!.itemTextStyle.copy(fontSize = 28.sp),
                )
                composeRule.waitForIdle()
                val large = heightOf(cameraTitle)
                assertTrue("$large should exceed $plain", large > plain)
                popupStyle = null
                composeRule.waitForIdle()
            }

            optionEntry("hideCameraOption", string(R.string.cometchat_camera), { hideCameraOption = it }, { viewModel.showCameraOption.value })

            optionEntry("hideImageOption", string(R.string.cometchat_attach_image), { hideImageOption = it }, { viewModel.showImageOption.value })

            optionEntry("hideVideoOption", string(R.string.cometchat_attach_video), { hideVideoOption = it }, { viewModel.showVideoOption.value })

            optionEntry("hideAudioOption", string(R.string.cometchat_attach_audio), { hideAudioOption = it }, { viewModel.showAudioOption.value })

            optionEntry("hideFileOption", string(R.string.cometchat_attach_document), { hideFileOption = it }, { viewModel.showFileOption.value })

            optionEntry("hidePollOption", string(R.string.cometchat_poll), { hidePollOption = it }, { viewModel.showPollOption.value })

            optionEntry("hideCollaborativeDocumentOption", string(R.string.cometchat_collaborative_doc), { hideCollaborativeDocumentOption = it }, { viewModel.showCollaborativeDocumentOption.value })

            optionEntry("hideCollaborativeWhiteboardOption", string(R.string.cometchat_collaborative_whiteboard), { hideCollaborativeWhiteboardOption = it }, { viewModel.showCollaborativeWhiteboardOption.value })

            callback("onCameraClick") {
                clickOption(string(R.string.cometchat_camera))
                assertTrue("the camera option should reach the host's handler", cameraClicked)
            }

            callback("onImageClick") {
                clickOption(string(R.string.cometchat_attach_image))
                assertTrue(imageClicked)
            }

            callback("onVideoClick") {
                clickOption(string(R.string.cometchat_attach_video))
                assertTrue(videoClicked)
            }

            callback("onAudioClick") {
                clickOption(string(R.string.cometchat_attach_audio))
                assertTrue(audioClicked)
            }

            callback("onDocumentClick") {
                clickOption(string(R.string.cometchat_attach_document))
                assertTrue(documentClicked)
            }

            callback("onPollClick") {
                clickOption(string(R.string.cometchat_poll))
                assertTrue(pollClicked)
            }

            callback("onCollaborativeDocumentClick") {
                clickOption(string(R.string.cometchat_collaborative_doc))
                assertTrue(documentBoardClicked)
            }

            callback("onCollaborativeWhiteboardClick") {
                clickOption(string(R.string.cometchat_collaborative_whiteboard))
                assertTrue(whiteboardClicked)
            }

            // ==================== staged attachments ====================
            // Everything below needs a tile in the tray, which is why it runs last: a
            // staged batch gates the send button and hides the voice button.

            value("enableMultipleAttachments") {
                viewModel.stageAttachments(listOf(stagedInput()))
                composeRule.waitForIdle()
                assertEquals(
                    "a staged attachment should show up as a tile",
                    1,
                    nodesWithCd(tileDescription(UPLOADING_LABEL)),
                )
                multipleAttachments = false
                composeRule.waitForIdle()
                assertEquals(
                    "with multi-attachment off there is no tray to stage into",
                    0,
                    nodesWithCd(tileDescription(UPLOADING_LABEL)),
                )
                multipleAttachments = true
                composeRule.waitForIdle()
            }

            value("attachmentTrayStyle") {
                trayStyle = defaultTrayStyle!!.copy(backgroundColor = MAGENTA)
                composeRule.waitForIdle()
                assertTrue(
                    "the tray's own backgroundColor should be on screen",
                    MAGENTA_ARGB in paintedColours(),
                )
                trayStyle = null
                composeRule.waitForIdle()
            }

            value("attachmentTileStyle") {
                // Forwarded to every tile. The staged file is a document on purpose: its
                // tile is a card whose background is painted outright, where a media
                // tile's placeholder is drawn under a thumbnail layer.
                tileStyle = defaultTileStyle!!.copy(cardBackgroundColor = CYAN)
                composeRule.waitForIdle()
                assertTrue(
                    "tileStyle should reach the tiles, not stop at the tray",
                    CYAN_ARGB in paintedColours(),
                )
                tileStyle = null
                composeRule.waitForIdle()
            }

            callback("onError") {
                val fileId = viewModel.attachmentTiles.value.first().fileId
                val rejection = CometChatException("FILE_TOO_LARGE", REJECTION_REASON)
                uploadListener!!.onFileError(fileId, rejection)
                composeRule.waitForIdle()
                assertEquals(
                    "an SDK rejection should surface on the host's error handler",
                    "FILE_TOO_LARGE",
                    lastError?.code,
                )
            }

            value("attachmentErrorAlertStyle") {
                // The rejected tile is left over from onError above; tapping it raises the
                // transient alert this style dresses.
                composeRule.onNodeWithContentDescription(tileDescription(REJECTED_LABEL)).performClick()
                composeRule.waitForIdle()
                val plain = heightOf(REJECTION_REASON)
                errorAlertStyle = defaultErrorAlertStyle!!.copy(
                    textStyle = defaultErrorAlertStyle!!.textStyle.copy(fontSize = 32.sp),
                )
                composeRule.waitForIdle()
                val large = heightOf(REJECTION_REASON)
                assertTrue(
                    "the alert composes into its own window, so this asserts a measurable " +
                        "consequence of the style: $large should exceed $plain",
                    large > plain,
                )
                errorAlertStyle = null
                composeRule.waitForIdle()
            }
        }

        val props = matrix.evaluate() + waivedProps()
        val cov = Denominator.summarize(props)
        val uncovered = props.filter { !it.covered && !it.waived }.map { it.name }

        println("  [messagecomposer compose prop matrix] ${cov.covered}/${cov.total} (${cov.waived} waived)")
        if (uncovered.isNotEmpty()) println("  [messagecomposer] NOT covered: $uncovered")

        assertEquals("every exercised prop should apply cleanly", cov.total, cov.covered)
        assertEquals(100.0, cov.percent, 0.0)
        assertEquals("the whole parameter list, minus modifier", 56, cov.total + cov.waived)
    }

    /**
     * Six props no entry above can honestly claim, every one of them a defect this matrix
     * exists to surface rather than a limit of the test.
     *
     * `attachmentOptions` and `onAttachmentOptionClick` are unreachable together. The
     * composer forwards the list to the ViewModel from a `LaunchedEffect`, but the popup
     * builds its menu inside a `remember` keyed only on the eight option-visibility flows —
     * the custom list is read, never keyed. The effect runs after that `remember` has
     * already captured an empty list, so a host's custom option never reaches the menu and
     * the callback that fires from it can never be invoked. (Measured: with an option
     * passed from the first composition the menu shows 0 of them; toggling any visibility
     * flag afterwards re-keys the `remember` and the same option appears. The fixture
     * above passes one exactly as a host would.) The View composer's `setAttachmentOptions`
     * has no such gap.
     *
     * `maxLines`, `layoutMode` and `disableSoundForMessages` are **dead parameters**: all
     * three are declared and documented on the public composable and never read in its
     * body. `maxLines` promises to cap the input's height, `layoutMode` promises a
     * single-row versus two-row layout, `disableSoundForMessages` promises to mute the
     * outgoing-message sound — and each is passed straight past every use site. There is
     * no assertion an entry could make about them that would not be asserting the defect.
     * The View composer honours all three (`setLayoutMode` switches the row layout,
     * `setShowFormattingToolbar` depends on it), so this is a Compose/View divergence as
     * well as three dead params.
     *
     *
     * `onStickerSelected` fires only when a sticker in the keyboard panel is tapped, and
     * the panel populates itself from an SDK extension fetch through a ViewModel the
     * composer creates internally and does not expose. The panel opens here (see the
     * `stickerKeyboardStyle` entry) but never holds a sticker to tap, so the callback is
     * unreachable from this layer. [com.cometchat.uikit.compose.presentation.stickerkeyboard.CometChatStickerKeyboardComposePropMatrixTest]
     * covers the same click on the keyboard itself, where the ViewModel is a parameter.
     */
    private fun waivedProps(): List<Prop> = listOf(
        Prop(OWNER, "attachmentOptions", PropKind.VALUE, waived = true),
        Prop(OWNER, "onAttachmentOptionClick", PropKind.CALLBACK, waived = true),
        Prop(OWNER, "maxLines", PropKind.VALUE, waived = true),
        Prop(OWNER, "layoutMode", PropKind.VALUE, waived = true),
        Prop(OWNER, "disableSoundForMessages", PropKind.VALUE, waived = true),
        Prop(OWNER, "onStickerSelected", PropKind.CALLBACK, waived = true),
    )
}
