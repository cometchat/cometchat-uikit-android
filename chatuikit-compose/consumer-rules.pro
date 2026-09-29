# Consumer ProGuard rules for chatuikit-compose

# ENG-38653 (G3): strip gated debug logging from minified consumer builds.
# CometChatLogger.d/i/v calls (and their message-string arguments) are removed
# by R8 when the consuming app builds with minification - debug chatter never
# ships in release APKs. w/e are deliberately KEPT so warnings/errors remain
# available in production via CometChatLogger.enableLog(true).
# Covers both the @JvmStatic bridges and the Kotlin object's instance methods.
-assumenosideeffects class com.cometchat.uikit.core.utils.CometChatLogger {
    public *** d(...);
    public *** i(...);
    public *** v(...);
}

# ENG-38654 (R1): the emoji keyboard deserializes com.cometchat.uikit.compose.presentation.emojikeyboard.model.Emoji /
# EmojiCategory reflectively with Gson (EmojiRepository.loadAndSaveEmojis).
# Keep the model fields and constructors; serialized names come from
# @SerializedName, but R8 must not remove the fields or the no-arg path.
# (ViewBinding/DataBinding generated classes are referenced from code, not by
# name, so they intentionally have no rules here.)
-keepclassmembers class com.cometchat.uikit.compose.presentation.emojikeyboard.model.** {
    <fields>;
    <init>(...);
}
