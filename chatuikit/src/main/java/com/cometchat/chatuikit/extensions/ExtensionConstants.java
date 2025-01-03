package com.cometchat.chatuikit.extensions;

public class ExtensionConstants {
    private static final String TAG = ExtensionConstants.class.getSimpleName();
    public static final String EXTENSIONS = "extensions";
    public static final String LINK_PREVIEW = "link-preview";
    public static final String SMART_REPLY = "smart-reply";
    public static final String MESSAGE_TRANSLATION = "message-translation";
    public static final String PROFANITY_FILTER = "profanity-filter";
    public static final String IMAGE_MODERATION = "image-moderation";
    public static final String THUMBNAIL_GENERATION = "thumbnail-generation";
    public static final String SENTIMENT_ANALYSIS = "sentiment-analysis";
    public static final String POLLS = "polls";
    public static final String REACTIONS = "reactions";
    public static final String WHITEBOARD = "whiteboard";
    public static final String DOCUMENT = "document";
    public static final String DATA_MASKING = "data-masking";
    public static final String STICKERS = "stickers";
    public static final String XSS_FILTER = "xss-filter";
    public static final String SAVE_MESSAGE = "save-message";
    public static final String PIN_MESSAGE = "pin-message";
    public static final String VOICE_TRANSCRIPTION = "voice-transcription";
    public static final String RICH_MEDIA = "rich-media";
    public static final String MALWARE_SCANNER = "virus-malware-scanner";
    public static final String MENTIONS = "mentions";
    public static final String CUSTOM_STICKERS = "customStickers";
    public static final String DEFAULT_STICKERS = "defaultStickers";
    public static final String STICKER_URL = "stickerUrl";

    public static class ExtensionUrls {
        public static final String REACTION = "/v1/react";
        public static final String STICKERS = "/v1/fetch";
        public static final String DOCUMENT = "/v1/create";
        public static final String WHITEBOARD = "/v1/create";
        public static final String VOTE_POLL = "/v2/vote";
        public static final String CREATE_POLL = "/v2/create";
        public static final String TRANSLATE = "/v2/translate";
    }

    public static class ExtensionType {
        public static final String EXTENSION_POLL = "extension_poll";
        public static final String MEETING = "meeting";
        public static final String LOCATION = "location";
        public static final String STICKER = "extension_sticker";
        public static final String DOCUMENT = "extension_document";
        public static final String WHITEBOARD = "extension_whiteboard";
    }

    public static class ExtensionServerId {
        public static final String THUMBNAIL_GENERATION = "thumbnail-generation";
        public static final String LINK_PREVIEW = "link-preview";
        public static final String STICKERS = "stickers";
        public static final String REACTIONS = "reactions";
        public static final String MESSAGE_TRANSLATION = "message-translation";
        public static final String SMART_REPLIES = "smart-reply";
        public static final String POLLS = "polls";
        public static final String COLLABORATION_WHITEBOARD = "whiteboard";
        public static final String COLLABORATION_DOCUMENT = "document";
        public static final String PROFANITY_FILTER = "profanity-filter";
        public static final String IMAGE_MODERATION = "image-moderation";
        public static final String DATA_MASKING = "data-masking";
    }

    public static class ExtensionRequest {
        public static final String POST = "POST";
        public static final String GET = "GET";
        public static final String DELETE = "DELETE";
        public static final String PUT = "PUT";
    }

    public static class ExtensionJSONField {
        public static final String ID = "id";
        public static final String VOTE = "vote";
        public static final String VOTERS = "voters";
        public static final String AVATAR = "avatar";
        public static final String NAME = "name";
        public static final String OPTIONS = "options";
        public static final String QUESTION = "question";
        public static final String LINK_PREVIEW = "linkPreview";
        public static final String DESCRIPTION = "description";
        public static final String IMAGE = "image";
        public static final String TITLE = "title";
        public static final String URL = "url";
        public static final String FAV_ICON = "favicon";
        public static final String VALUES = "values";
        public static final String MESSAGE_TRANSLATED = "message_translated";
    }
}
