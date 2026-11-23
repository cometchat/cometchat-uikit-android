package com.cometchat.uikit.core.offline

import androidx.annotation.RestrictTo
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.domain.model.StreamMessage

/**
 * The only offline helper the **UI modules** (`chatuikit-kotlin`, `chatuikit-compose`) are meant
 * to touch. Kotlin `internal` does not cross a module boundary, so this type is `public` but
 * carries [RestrictTo] `LIBRARY_GROUP` — it is internal to the kit, not part of the customer API.
 * The UI modules get the key mode from here and never read [OfflineGate] themselves (G1).
 *
 * The two stacks identify rows differently today, so there is one helper per stack:
 * - **Compose** uses a `LazyColumn` key → [renderKey].
 * - **Views** uses `DiffUtil.areItemsTheSame` → [isSameItem].
 *
 * Contract: capture the mode **once** when the list binds its ViewModel
 * (`val offlineOn = OfflineUi.keyMode()`) and pass that value on every call for that binding.
 *
 * With `offlineOn == false` both helpers reproduce **exactly** today's identity:
 * - Compose (`CometChatMessageList.kt` `itemsIndexed` key): `id` when non-zero, otherwise
 *   `muid ?: hashCode().toLong()` — so a pending row is never keyed `0`.
 * - Views (`MessageAdapter.MessageDiffCallback.areItemsTheSame`): two [StreamMessage]s match by
 *   non-empty muid, else by reference; a stream and a non-stream never match; anything else
 *   matches on `id`.
 *
 * With `offlineOn == true` the identity is the offline [MessageKey.renderKey], which keeps an own
 * optimistic row stable across its ack. [StreamMessage]s keep today's identity in both modes, and
 * a row with no usable offline key (no server id, no local id, no muid) falls back to today's.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object OfflineUi {

    /**
     * The current offline key mode. Read **once** when a list binds its ViewModel and pass the
     * captured value to [renderKey] / [isSameItem] for the lifetime of that binding, so a flag flip
     * never splits one list between two key schemes.
     */
    @JvmStatic
    public fun keyMode(): Boolean = OfflineGate.isOn()

    /**
     * Compose `LazyColumn` key for a message row.
     *
     * @return with `offlineOn == false`, today's key: `message.id` (a [Long]) when non-zero, else
     * `message.muid` (a [String]) or, when that is null, `message.hashCode().toLong()`. With
     * `offlineOn == true`, the offline render key (a [String]) — see the class KDoc for fallbacks.
     */
    @JvmStatic
    public fun renderKey(message: BaseMessage, offlineOn: Boolean): Any =
        if (offlineOn) offlineKey(message, loggedInUid()) ?: todayComposeKey(message)
        else todayComposeKey(message)

    /**
     * Views `DiffUtil.areItemsTheSame` for two message rows.
     *
     * @return with `offlineOn == false`, exactly today's `MessageDiffCallback.areItemsTheSame`.
     * With `offlineOn == true`, equality of the offline render keys; streams, and rows with no
     * usable offline key, keep today's rule.
     */
    @JvmStatic
    public fun isSameItem(old: BaseMessage, new: BaseMessage, offlineOn: Boolean): Boolean {
        val oldIsStream = old is StreamMessage
        val newIsStream = new is StreamMessage
        if (oldIsStream || newIsStream) return todayViewsSameItem(old, new)
        if (!offlineOn) return todayViewsSameItem(old, new)
        val uid = loggedInUid()
        val oldKey = offlineKey(old, uid)
        val newKey = offlineKey(new, uid)
        return if (oldKey != null && newKey != null) oldKey == newKey else todayViewsSameItem(old, new)
    }

    /** Today's Compose key, verbatim from `CometChatMessageList`. */
    private fun todayComposeKey(message: BaseMessage): Any =
        if (message.id == 0L) message.muid ?: message.hashCode().toLong() else message.id

    /** Today's Views `areItemsTheSame`, verbatim from `MessageAdapter.MessageDiffCallback`. */
    private fun todayViewsSameItem(old: BaseMessage, new: BaseMessage): Boolean {
        val oldIsStream = old is StreamMessage
        val newIsStream = new is StreamMessage
        if (oldIsStream && newIsStream) {
            val oldMuid = old.muid
            val newMuid = new.muid
            return if (!oldMuid.isNullOrEmpty() && !newMuid.isNullOrEmpty()) oldMuid == newMuid else old === new
        }
        if (oldIsStream || newIsStream) return false
        return old.id == new.id
    }

    /**
     * The offline render key, or null when the row has no usable one: a [StreamMessage] (keeps
     * today's identity) or a row whose key would collapse to `s:0` (no server id, local id or muid).
     */
    private fun offlineKey(message: BaseMessage, myUid: String?): String? {
        if (message is StreamMessage) return null
        val key = MessageKey.renderKey(message, myUid)
        return if (key == "s:0") null else key
    }

    private fun loggedInUid(): String? =
        runCatching { CometChat.getLoggedInUser()?.uid }.getOrNull()
}
