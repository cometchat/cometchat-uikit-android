package com.cometchat.uikit.core.formatter

/**
 * Public, stable handle the composer hands to a custom trailing-toolbar button so it can
 * read and mutate the live composer input.
 *
 * Platform-agnostic — expressed only in [String]/[IntRange] and the built-in [RichTextFormat]
 * enum, so both the Jetpack Compose and the Kotlin XML UI kits provide their own implementation
 * (over [RichTextEditorController] and over the composer's `EditText` respectively).
 *
 * The UIKit supplies a live implementation while the composer is mounted; consumers must not
 * retain the reference past the button's lifecycle.
 */
public interface ComposerInputController {

    /** Current plain text of the composer input (no markdown markers). */
    public val text: String

    /** Current selection as `start..end`; `start == end` means a collapsed caret. */
    public val selection: IntRange

    /** True when there is no selected range (caret only). */
    public val isCursorCollapsed: Boolean

    /**
     * Insert [textToInsert] at the caret, replacing any active selection.
     * The caret is moved to the end of the inserted text.
     */
    public fun insertAtCursor(textToInsert: String)

    /**
     * Replace the current selection with [replacement].
     * Degrades to [insertAtCursor] when the caret is collapsed.
     */
    public fun replaceSelection(replacement: String)

    /** Toggle one of the built-in rich-text formats over the current selection/caret. */
    public fun toggleFormat(format: RichTextFormat)

    /**
     * Ranges (`start..end`) occupied by mentions in the current text, so a custom style can
     * skip them. Empty when there are no mentions.
     */
    public fun mentionRanges(): List<IntRange>
}
