package com.cometchat.chatuikit.shared.views.messageinput;

import android.text.Editable;

import com.cometchat.chatuikit.shared.spans.NonEditableSpan;

/**
 * CometChatTextWatcher is an abstract class that is used to handle the text
 * change events in the CometChatEditText.
 */
public abstract class CometChatTextWatcher {
    private static final String TAG = CometChatTextWatcher.class.getSimpleName();

    /**
     * Called when the text in the compose box is changed.
     *
     * @param charSequence The changed text.
     * @param start        The start position of the changed text.
     * @param before       The length of the text before the change.
     * @param count        The length of the changed text.
     */
    public abstract void onTextChanged(CharSequence charSequence, int start, int before, int count);

    /**
     * Called before the text in the compose box is changed.
     *
     * @param s     The current text in the compose box.
     * @param start The start position of the changed text.
     * @param count The length of the changed text.
     * @param after The length of the text after the change.
     */
    public abstract void beforeTextChanged(CharSequence s, int start, int count, int after);

    /**
     * Called after the text in the compose box is changed.
     *
     * @param editable The editable text after the change.
     */
    public abstract void afterTextChanged(Editable editable);

    /**
     * Called when the selection in the compose box is changed.
     *
     * @param selStart The start position of the selection.
     * @param selEnd   The end position of the selection.
     */
    public void onSelectionChanged(int selStart, int selEnd) {
    }

    /**
     * Called when a span is deleted from the compose box.
     *
     * @param span The span that was deleted.
     */
    public void onSpanDeleted(NonEditableSpan span) {
    }
}
