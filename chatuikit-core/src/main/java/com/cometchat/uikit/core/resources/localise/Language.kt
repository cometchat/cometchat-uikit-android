package com.cometchat.uikit.core.resources.localise

import androidx.annotation.StringDef

/**
 * Language codes supported by CometChat UIKit.
 * Use these constants with [CometChatLocalize.setLocale] to set the app language.
 */
public object Language {
    public const val ENGLISH: String = "en"
    public const val SPANISH: String = "es"
    public const val FRENCH: String = "fr"
    public const val GERMAN: String = "de"
    public const val PORTUGUESE: String = "pt"
    public const val ITALIAN: String = "it"
    public const val RUSSIAN: String = "ru"
    public const val CHINESE: String = "zh"
    public const val JAPANESE: String = "ja"
    public const val KOREAN: String = "ko"
    public const val ARABIC: String = "ar"
    public const val HINDI: String = "hi"
    public const val TURKISH: String = "tr"
    public const val DUTCH: String = "nl"
    public const val POLISH: String = "pl"
    public const val SWEDISH: String = "sv"
    public const val HUNGARIAN: String = "hu"
    public const val MALAY: String = "ms"
    public const val LITHUANIAN: String = "lt"

    /**
     * Annotation for language code validation.
     */
    @StringDef(
        ENGLISH, SPANISH, FRENCH, GERMAN, PORTUGUESE, ITALIAN,
        RUSSIAN, CHINESE, JAPANESE, KOREAN, ARABIC, HINDI,
        TURKISH, DUTCH, POLISH, SWEDISH, HUNGARIAN, MALAY, LITHUANIAN
    )
    @Retention(AnnotationRetention.SOURCE)
    public annotation class Code
}
