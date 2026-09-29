package com.cometchat.uikit.core.constants

/**
 * UIKit constants and enums for configuration and settings.
 */
public object UIKitConstants {

    /**
     * Enum defining the scope for search operations.
     */
    public enum class SearchScope(public val value: String) {
        /**
         * Search within conversations.
         */
        CONVERSATIONS("conversations"),

        /**
         * Search within messages.
         */
        MESSAGES("messages");

        override fun toString(): String = value
    }

    /**
     * Defines the status values for a message.
     */
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.LOCAL_VARIABLE)
    public annotation class MessageStatus {
        public companion object {
            /**
             * The message is in progress.
             */
            public const val IN_PROGRESS: Int = 0

            /**
             * The message was successfully sent.
             */
            public const val SUCCESS: Int = 1

            /**
             * An error occurred while sending the message.
             */
            public const val ERROR: Int = -1
        }
    }

    /**
     * Message header menu options.
     */
    public object MessageHeaderMenuOptions {
        public const val SEARCH: String = "search"
        public const val CONVERSATION_SUMMARY: String = "conversation_summary"
        public const val DETAILS: String = "details"
        public const val PINNED_MESSAGES: String = "pinned_messages"
        public const val SAVED_MESSAGES: String = "saved_messages"
    }

    /**
     * Enum defining search modes.
     */
    public enum class SearchMode {
        MESSAGES, CONVERSATIONS, BOTH, NONE
    }

    /**
     * Enum defining mentions types.
     */
    public enum class MentionsType {
        USERS, USERS_AND_GROUP_MEMBERS
    }

    /**
     * Enum defining call workflows.
     */
    public enum class CallWorkFlow {
        MEETING, DEFAULT
    }

    /**
     * Enum defining mentions visibility.
     */
    public enum class MentionsVisibility {
        USERS_CONVERSATION_ONLY, GROUP_CONVERSATION_ONLY, BOTH
    }

    /**
     * Enum defining formatting types.
     */
    public enum class FormattingType {
        MESSAGE_BUBBLE, MESSAGE_COMPOSER, CONVERSATIONS
    }

    /**
     * Enum defining selection modes.
     */
    public enum class SelectionMode {
        NONE, SINGLE, MULTIPLE
    }

    /**
     * Controls the overall alignment of messages in the list.
     */
    public enum class MessageListAlignment {
        /**
         * Standard alignment: outgoing messages on right, incoming on left.
         */
        STANDARD,

        /**
         * All messages aligned to the left.
         */
        LEFT_ALIGNED
    }

    /**
     * Enum defining message bubble alignments.
     */
    public enum class MessageBubbleAlignment {
        RIGHT, LEFT, CENTER
    }

    /**
     * Controls where the timestamp is displayed in message bubbles.
     */
    public enum class TimeStampAlignment {
        /**
         * Display timestamp in the header view alongside sender name.
         */
        TOP,

        /**
         * Display timestamp in the status info view alongside receipt indicator.
         */
        BOTTOM
    }

    /**
     * Enum defining auxiliary button alignment.
     */
    public enum class AuxiliaryButtonAlignment {
        LEFT, RIGHT
    }

    /**
     * Enum defining states.
     */
    public enum class States {
        LOADING, LOADED, ERROR, EMPTY, NON_EMPTY, INITIAL
    }

    /**
     * Enum defining contacts visibility mode.
     */
    public enum class ContactsVisibilityMode {
        USER, GROUP, USER_AND_GROUP
    }

    /**
     * Enum defining time formats.
     */
    public enum class TimeFormat {
        TWELVE_HOUR, TWENTY_FOUR_HOUR
    }

    /**
     * Enum defining date-time modes.
     */
    public enum class DateTimeMode {
        DATE, TIME, DATE_TIME
    }

    /**
     * Enum defining delete states.
     */
    public enum class DeleteState {
        INITIATED_DELETE, SUCCESS_DELETE, FAILURE_DELETE
    }

    /**
     * Enum defining flag message states.
     */
    public enum class FlagMessageState {
        INITIATED_FLAG, SUCCESS_FLAG, FAILURE_FLAG
    }

    /**
     * Enum defining dialog states.
     */
    public enum class DialogState {
        INITIATED, SUCCESS, FAILURE
    }

    /**
     * Enum defining custom UI positions.
     */
    public enum class CustomUIPosition {
        COMPOSER_TOP, COMPOSER_BOTTOM, MESSAGE_LIST_TOP, MESSAGE_LIST_BOTTOM
    }

    /**
     * Enum defining search filters.
     */
    public enum class SearchFilter(public val value: String) {
        MESSAGES("messages"),
        CONVERSATIONS("conversations"),
        UNREAD("unread"),
        GROUPS("groups"),
        PHOTOS("photos"),
        VIDEOS("videos"),
        LINKS("links"),
        DOCUMENTS("files"),
        AUDIO("audio");

        override fun toString(): String = value
    }

    /**
     * Shared preferences keys.
     */
    public object SharedPreferencesKeys {
        public const val CALL: String = "initiated_call"
        public const val CALL_MESSAGE: String = "call_message"
    }

    /**
     * Prefixes for the tags ViewModels register their SDK listeners under. The full tag appends a
     * per-instance suffix so two live instances of the same screen never collide, and so removal on
     * teardown only detaches that instance's listener.
     */
    public object ListenerTags {
        public const val PINNED_MESSAGES: String = "PinnedMessages"
        public const val SAVED_MESSAGES: String = "SavedMessages"
    }

    /**
     * View tags.
     */
    public object ViewTag {
        public const val INTERNAL_HEADER_VIEW: String = "internal_header_view"
        public const val INTERNAL_STATUS_INFO_VIEW: String = "internal_status_info_view"
        public const val INTERNAL_THREAD_VIEW: String = "internal_thread_view"
        public const val INTERNAL_LEADING_VIEW: String = "internal_leading_view"
        public const val INTERNAL_BOTTOM_VIEW: String = "internal_bottom_view"
    }

    /**
     * Intent string constants.
     */
    public object IntentStrings {
        public const val UID: String = "uid"
        public const val NAME: String = "name"
        public val EXTRA_MIME_DOC: Array<String> = arrayOf(
            "text/plane",
            "image/*",
            "video/*",
            "text/html",
            "application/pdf",
            "application/msword",
            "application/vnd.ms.excel",
            "application/mspowerpoint",
            "application/docs",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip"
        )
        public const val SENT_AT: String = "sent_at"
        public const val MESSAGE_TYPE: String = "message_type"
        public const val INTENT_MEDIA_MESSAGE: String = "intent_media_message"
        public const val URL: String = "url"
        public const val TITLE: String = "title"
        public const val MEDIA_SIZE: String = "media_size"
        public const val STORE_INSTANCE: String = "store_instance"
        public const val PATH: String = "path"
    }

    /**
     * JSON keys.
     */
    public object JSONKeys {
        public const val METADATA: String = "metadata"
        public const val CUSTOM_DATA: String = "customData"
        public const val INFO_TEXT: String = "infoText"
        public const val INJECTED: String = "@injected"
        public const val EXTENSIONS: String = "extensions"
        public const val LINK_PREVIEW: String = "link-preview"
        public const val LINKS: String = "links"

        // Multi-attachment batching (ENG-36737). Cross-platform interop contract shared with
        // iOS/RN/web: a single multi-attachment send is split into one MediaMessage per type, all
        // sharing [BATCH_ID]. Grouping is derived from list-neighbor adjacency on [BATCH_ID] (no
        // index/count keys — matches iOS). A mic-recorded voice note is marked with
        // [AUDIO_TYPE] = [AUDIO_TYPE_VOICE_NOTE] (matches the cross-platform DD / iOS), which routes
        // it to VoiceNoteBubble (always standalone); picker audio has no flag → AudiosBubble.
        // [VOICE_NOTE] is the legacy Bool key — still read for backward compatibility, no longer
        // written.
        public const val BATCH_ID: String = "batchId"
        public const val AUDIO_TYPE: String = "audioType"
        public const val AUDIO_TYPE_VOICE_NOTE: String = "voice_note"
        public const val VOICE_NOTE: String = "voiceNote"
    }

    /**
     * MIME types.
     */
    public object MimeType {
        public const val VIDEO: String = "video"
        public const val OCTET_STREAM: String = "application/octet-stream"
        public const val AUDIO: String = "audio/mpeg"
        public const val PDF: String = "pdf"
        public const val ZIP: String = "zip"
        public const val IMAGE: String = "image"
        public const val CSV: String = "csv"
        public const val RTF: String = "text/rtf"
        public const val DOC: String = "doc"
        public const val XLS: String = "xls"
        public const val PPT: String = "ppt"
        public const val TEXT: String = "text"
        public const val LINK: String = "link"
        public const val GIF_EXTENSION: String = ".gif"

        // Text types
        public const val MIME_CSV: String = "text/comma-separated-values"
        public const val MIME_RTF: String = "text/rtf"
        public const val MIME_DOC: String = "application/msword"
        public const val MIME_XLS: String = "application/vnd.ms-excel"
        public const val MIME_PPT: String = "application/vnd.ms-powerpoint"
        public const val MIME_PDF: String = "application/pdf"

        // Compressed types
        public const val MIME_ZIP: String = "application/zip"
        public const val MIME_ODP: String = "application/vnd.oasis.opendocument.presentation"
        public const val MIME_ODS: String = "application/vnd.oasis.opendocument.spreadsheet"
        public const val MIME_ODT: String = "application/vnd.oasis.opendocument.text"

        // Video types
        public const val MIME_MP4_VIDEO: String = "video/mp4"

        // Audio types
        public const val MIME_MP3_AUDIO: String = "audio/mp3"
        public const val MIME_MPEG_AUDIO: String = "audio/mpeg"

        // Image types
        public const val MIME_JPEG_IMAGE: String = "image/jpeg"
        public const val MIME_PNG_IMAGE: String = "image/png"

        // Default (unknown)
        public const val MIME_UNKNOWN: String = "unknown"
    }

    /**
     * MIME-type keyword fragments, prefixes and file extensions used to resolve a file's display
     * type (the colored file-type icon). Single source of truth for both UIKits — the Compose
     * `FileTypeUtils.getFileType` and the Views `MultiAttachmentUtils.fileIconRes` match against
     * these, so a given file always resolves to the same icon everywhere.
     */
    public object FileTypeMatchers {
        // Reuses [MimeType] constants where the value matches. The DOC / XLS / PPT keyword lists
        // deliberately do NOT use MimeType.DOC / XLS / PPT: as `contains()` fragments those would
        // misclassify — every OOXML MIME contains "officedocument" (so "doc" would match an .xlsx
        // MIME); "msword" / "wordprocessingml" etc. are the discriminating fragments.
        public const val PDF_KEYWORD: String = MimeType.PDF
        public val PDF_EXTENSIONS: List<String> = listOf(".pdf")

        public val DOC_KEYWORDS: List<String> = listOf("msword", "wordprocessingml")
        public val DOC_EXTENSIONS: List<String> = listOf(".doc", ".docx")

        public val XLS_KEYWORDS: List<String> = listOf("spreadsheet", "excel")
        public val XLS_EXTENSIONS: List<String> = listOf(".xls", ".xlsx", ".csv")

        public val PPT_KEYWORDS: List<String> = listOf("presentation", "powerpoint")
        public val PPT_EXTENSIONS: List<String> = listOf(".ppt", ".pptx")

        public val ARCHIVE_KEYWORDS: List<String> = listOf(MimeType.ZIP, "compressed", "archive", "rar", "7z", "tar", "gzip")
        public val ARCHIVE_EXTENSIONS: List<String> = listOf(".zip", ".rar", ".7z", ".tar", ".gz")

        public const val AUDIO_MIME_PREFIX: String = "audio/"
        public val AUDIO_EXTENSIONS: List<String> = listOf(".mp3", ".wav", ".aac", ".m4a", ".ogg", ".flac")

        public const val VIDEO_MIME_PREFIX: String = "video/"
        public val VIDEO_EXTENSIONS: List<String> = listOf(".mp4", ".mov", ".avi", ".mkv", ".webm")

        public const val IMAGE_MIME_PREFIX: String = "image/"
        public val IMAGE_EXTENSIONS: List<String> = listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg")

        public const val TEXT_MIME_PREFIX: String = "text/"
        public val TEXT_EXTENSIONS: List<String> = listOf(".txt", ".rtf", ".md", ".json", ".xml", ".html", ".css", ".js")

        public val LINK_PREFIXES: List<String> = listOf("http://", "https://")
    }

    /**
     * Map IDs.
     */
    public object MapId {
        public const val PARENT_MESSAGE_ID: String = "parentMessageID"
        public const val RECEIVER_ID: String = "receiverID"
        public const val RECEIVER_TYPE: String = "receiverType"
    }

    /**
     * Call options.
     */
    public object CallOption {
        public const val PARTICIPANTS: String = "participants"
        public const val RECORDING: String = "recording"
        public const val CALL_HISTORY: String = "callHistory"
    }

    /**
     * Group member options.
     */
    public object GroupMemberOption {
        public const val KICK: String = "kick"
        public const val BAN: String = "ban"
        public const val UNBAN: String = "unban"
        public const val CHANGE_SCOPE: String = "changeScope"
    }

    /**
     * User status constants.
     */
    public object UserStatus {
        public const val ONLINE: String = "online"
        public const val OFFLINE: String = "offline"
    }

    /**
     * Conversation options.
     */
    public object ConversationOption {
        public const val DELETE: String = "delete"
        public const val PIN: String = "pin"
        public const val UNPIN: String = "unpin"
    }

    /**
     * Conversation types.
     */
    public object ConversationType {
        public const val USERS: String = "user"
        public const val GROUPS: String = "group"
        public const val BOTH: String = "both"
    }

    /**
     * Group types.
     */
    public object GroupType {
        public const val PRIVATE: String = "private"
        public const val PASSWORD: String = "password"
        public const val PUBLIC: String = "public"
    }

    /**
     * Group member scopes.
     */
    public object GroupMemberScope {
        public const val ADMIN: String = "admin"
        public const val MODERATOR: String = "moderator"
        public const val PARTICIPANTS: String = "participant"
    }

    /**
     * Message categories.
     */
    public object MessageCategory {
        public const val MESSAGE: String = "message"
        public const val CUSTOM: String = "custom"
        public const val INTERACTIVE: String = "interactive"
        public const val ACTION: String = "action"
        public const val CALL: String = "call"
        public const val STREAM: String = "stream_message"
        public const val CARD: String = "card"
        public const val AGENTIC: String = "agentic"
    }

    /**
     * Message types.
     */
    public object MessageType {
        public const val TEXT: String = "text"
        public const val FILE: String = "file"
        public const val IMAGE: String = "image"
        public const val AUDIO: String = "audio"
        public const val VIDEO: String = "video"
        public const val STREAM: String = "ai_assistant_stream"
        public const val MEETING: String = "meeting"
        public const val CUSTOM: String = "custom"
        public const val EXTENSION_POLL: String = "extension_poll"
        public const val EXTENSION_STICKER: String = "extension_sticker"
        public const val EXTENSION_DOCUMENT: String = "extension_document"
        public const val EXTENSION_WHITEBOARD: String = "extension_whiteboard"
        public const val EXTENSION_MEETING: String = "meeting"
        public const val CARD: String = "card"
    }

    /**
     * Message template IDs.
     */
    public object MessageTemplateId {
        public const val TEXT: String = "message_text"
        public const val FILE: String = "message_file"
        public const val IMAGE: String = "message_image"
        public const val AUDIO: String = "message_audio"
        public const val VIDEO: String = "message_video"
        public const val GROUP_ACTION: String = "action_group_member"
        public const val FORM: String = "interactive_form"
        public const val SCHEDULER: String = "interactive_scheduler"
        public const val CARD: String = "interactive_card"
        public const val ASSISTANT: String = "agentic_assistant"
        public const val CUSTOM_INTERACTIVE: String = "interactive_customInteractive"
        public const val EXTENSION_POLL: String = "extension_poll"
        public const val EXTENSION_STICKER: String = "extension_sticker"
        public const val EXTENSION_DOCUMENT: String = "extension_document"
        public const val EXTENSION_WHITEBOARD: String = "extension_whiteboard"
        public const val EXTENSION_MEETING: String = "meeting"
        public const val EXTENSION_LOCATION: String = "location"
    }

    /**
     * Call status constants.
     */
    public object CallStatusConstants {
        public const val INITIATED: String = "initiated"
        public const val ONGOING: String = "ongoing"
        public const val REJECTED: String = "rejected"
        public const val CANCELLED: String = "cancelled"
        public const val BUSY: String = "busy"
        public const val UNANSWERED: String = "unanswered"
        public const val ENDED: String = "ended"
    }

    /**
     * Composer actions.
     */
    public object ComposerAction {
        public const val CAMERA: String = "camera"
        public const val IMAGE: String = "image"
        public const val VIDEO: String = "video"
        public const val AUDIO: String = "audio"
        public const val DOCUMENT: String = "document"
    }

    /**
     * Receiver types.
     */
    public object ReceiverType {
        public const val USER: String = "user"
        public const val GROUP: String = "group"
    }

    /**
     * Message options.
     */
    public object MessageOption {
        public const val EDIT: String = "edit"
        public const val DELETE: String = "delete"
        public const val REPLY: String = "reply"
        public const val FORWARD: String = "forward"
        public const val REPLY_PRIVATELY: String = "reply_privately"
        public const val MESSAGE_PRIVATELY: String = "message_privately"
        public const val COPY: String = "copy"
        public const val TRANSLATE: String = "translate"
        public const val MESSAGE_INFORMATION: String = "message_information"
        public const val SHARE: String = "share"
        public const val REPLY_IN_THREAD: String = "reply_in_thread"
        public const val REPLY_TO_MESSAGE: String = "reply_to_message"
        public const val REPORT: String = "report"
        public const val MARK_AS_UNREAD: String = "mark_as_unread"
        public const val REACT: String = "react"
        public const val THREAD_SUBSCRIPTION: String = "thread_subscription"
        public const val PIN: String = "pin"
        public const val UNPIN: String = "unpin"
        public const val SAVE: String = "save"
        public const val UNSAVE: String = "unsave"
    }

    /**
     * Error codes the server returns for a failed pin/save action. Read off
     * `CometChatException.code`; the structured cap (when served) is in `errorParams["limit"]`.
     */
    public object PinSaveErrorCodes {
        public const val PINNED_MESSAGES_LIMIT_EXCEEDED: String = "ERR_PINNED_MESSAGES_LIMIT_EXCEEDED"
        public const val SAVED_MESSAGES_LIMIT_EXCEEDED: String = "ERR_SAVED_MESSAGES_LIMIT_EXCEEDED"
        public const val PERMISSION_DENIED: String = "ERR_PERMISSION_DENIED"

        /** `errorParams` key carrying the cap that was hit. */
        public const val PARAM_LIMIT: String = "limit"
    }


    /**
     * File operations.
     */
    public object Files {
        public const val OPEN: String = "open"
        public const val SHARE: String = "share"
    }

    /**
     * Scheduler constants.
     */
    public object SchedulerConstants {
        public const val AVAILABLE: String = "available"
        public const val OCCUPIED: String = "occupied"
    }

    /**
     * UI elements types.
     */
    public object UIElementsType {
        public const val UI_ELEMENT_TEXT_INPUT: String = "textInput"
        public const val UI_ELEMENT_BUTTON: String = "button"
        public const val UI_ELEMENT_CHECKBOX: String = "checkbox"
        public const val UI_ELEMENT_SPINNER: String = "dropdown"
        public const val UI_ELEMENT_LABEL: String = "label"
        public const val UI_ELEMENT_RADIO_BUTTON: String = "radio"
        public const val UI_ELEMENT_SINGLE_SELECT: String = "singleSelect"
        public const val UI_ELEMENT_DATE_TIME: String = "dateTime"
    }

    /**
     * Calling JSON constants.
     */
    public object CallingJSONConstants {
        public const val CALL_TYPE: String = "callType"
        public const val CALL_SESSION_ID: String = "sessionID"
    }

    /**
     * Moderation constants.
     */
    public object ModerationConstants {
        public const val UNMODERATED: String = "unmoderated"
        public const val PENDING: String = "pending"
        public const val APPROVED: String = "approved"
        public const val DISAPPROVED: String = "disapproved"
    }

    /**
     * AI Assistant event types.
     */
    public object AIAssistantEventType {
        public const val RUN_STARTED: String = "run_started"
        public const val RUN_FINISHED: String = "run_finished"
        public const val TOOL_CALL_START: String = "tool_call_started"
        public const val TOOL_CALL_END: String = "tool_call_ended"
        public const val TEXT_MESSAGE_START: String = "text_message_start"
        public const val TEXT_MESSAGE_END: String = "text_message_end"
    }

    /**
     * AI constants.
     */
    public object AIConstants {
        public const val AGENTIC_USER: String = "@agentic"
        public const val AI_ASSISTANT_EVENT_TYPE: String = "ai_assistant_event_type"
    }

    /**
     * AI Assistant JSON constants.
     */
    public object AIAssistantJsonConstants {
        public const val SUGGESTED_MESSAGES: String = "suggestedMessages"
        public const val GREETING_MESSAGE: String = "greetingMessage"
        public const val INTRODUCTORY_MESSAGE: String = "introductoryMessage"
    }

    /**
     * Utility constants for UIKit operations.
     */
    public object UIKitUtilityConstants {
        /**
         * Composer search query debounce interval in milliseconds.
         */
        public const val COMPOSER_SEARCH_QUERY_INTERVAL: Int = 500

        /**
         * Composer operation debounce interval in milliseconds.
         */
        public const val COMPOSER_OPERATION_INTERVAL: Int = 200

        /**
         * Typing indicator debounce interval in milliseconds.
         */
        public const val TYPING_INDICATOR_DEBOUNCER: Int = 1000
    }
}