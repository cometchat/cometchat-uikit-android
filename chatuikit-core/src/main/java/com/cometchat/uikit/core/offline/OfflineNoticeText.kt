package com.cometchat.uikit.core.offline

import android.content.Context
import androidx.annotation.RestrictTo
import com.cometchat.uikit.core.R

/**
 * The one-line text both UI stacks show for an [OfflineNotice] (W4, ENG-41089). Public so the UI
 * modules can call it, but [RestrictTo] `LIBRARY_GROUP` — internal to the kit. Every string is
 * translated in all shipped locales (W1).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object OfflineNoticeText {

    /** The text for [notice], resolved from [context]'s resources. */
    @JvmStatic
    public fun resolve(context: Context, notice: OfflineNotice): String = when (notice) {
        OfflineNotice.QueueFull -> context.getString(R.string.cometchat_queue_full)
        OfflineNotice.FileTooLargeOffline -> context.getString(R.string.cometchat_file_too_large_offline)
        OfflineNotice.SendingTryAgain -> context.getString(R.string.cometchat_sending_try_again)
        is OfflineNotice.UnsentRemoved -> context.getString(R.string.cometchat_unsent_message_removed)
        is OfflineNotice.MessagesNotSent ->
            context.resources.getQuantityString(R.plurals.cometchat_messages_not_sent, notice.count, notice.count)
        is OfflineNotice.UnsentLost ->
            context.resources.getQuantityString(R.plurals.cometchat_unsent_messages_lost, notice.count, notice.count)
        OfflineNotice.UnsentMaybeLost -> context.getString(R.string.cometchat_unsent_messages_maybe_lost)
    }
}
