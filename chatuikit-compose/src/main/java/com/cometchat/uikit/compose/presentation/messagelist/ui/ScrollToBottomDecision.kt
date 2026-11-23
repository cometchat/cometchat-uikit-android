package com.cometchat.uikit.compose.presentation.messagelist.ui

import com.cometchat.chat.models.BaseMessage

/**
 * Collision-free identity of the bottom (last) message, used to distinguish an append from a
 * prepend.
 *
 * The naive `id` is not enough: an optimistic own-sent message has `id == 0` until the server
 * acks it (it carries only a `muid` + `sentAt`), so **every** un-acked send shares `id == 0`
 * and they collide. `muid` alone is also insufficient — incoming server messages carry no
 * `muid`. A composite keyed on whichever is present ("id:…" once acked, "muid:…" while pending)
 * is stable and collision-free across both cases, and it flips exactly once when a pending
 * message acks (muid:… → id:…).
 */
internal fun bottomMessageIdentity(message: BaseMessage?): String? =
    message?.let { if (it.id > 0) "id:${it.id}" else "muid:${it.muid}" }

/**
 * Pure decision for whether a message-list update should auto-scroll to the bottom.
 *
 * Extracted from the message-list auto-scroll `LaunchedEffect(messages.size)` so the branch
 * logic can be unit-tested without a LazyColumn / Compose runtime. The key correctness rule
 * this encodes is the **append-vs-prepend** distinction:
 *
 * - An **append** (a genuinely new message at the bottom) grows the item count AND changes
 *   the id of the last/bottom message.
 * - A **prepend** (an older page loaded via pagination) also grows the item count, but the
 *   last/bottom message id is unchanged. It must NOT auto-scroll — otherwise a fast fling up
 *   that triggers the older-page fetch snaps the list right back to the bottom.
 *
 * `isAppend` is a strict subset of the old `newCount > previousCount` condition, so this only
 * ever *removes* scroll calls for prepends and never suppresses a scroll for a real append.
 *
 * Behaviour matches the View (kotlin) stack's decision one-for-one.
 *
 * @param previousCount item count before the update.
 * @param newCount item count after the update.
 * @param previousLastId collision-free identity of the bottom (last) message before the update
 *   (see [bottomMessageIdentity]), or null if empty.
 * @param newLastId collision-free identity of the bottom (last) message after the update
 *   (see [bottomMessageIdentity]), or null if empty.
 * @param wasAtBottom whether the user was at (or within 2 of) the bottom before the update.
 * @param scrollToBottomOnNewMessage the component's configured auto-scroll policy.
 * @param lastIsOwn whether the new bottom message is the logged-in user's own message.
 * @param lastVisiblePos last visible item index before the update (for the within-5 check).
 */
internal fun shouldScrollToBottomOnUpdate(
    previousCount: Int,
    newCount: Int,
    previousLastId: String?,
    newLastId: String?,
    wasAtBottom: Boolean,
    scrollToBottomOnNewMessage: Boolean,
    lastIsOwn: Boolean,
    lastVisiblePos: Int
): Boolean {
    // Append = list grew AND the newest/bottom message changed. A prepend (pagination) grows
    // the count but leaves the bottom message unchanged -> NOT an append -> do not scroll.
    val isAppend = newCount > previousCount && newLastId != previousLastId
    if (!isAppend) return false

    return if (wasAtBottom) {
        if (scrollToBottomOnNewMessage) {
            // Always scroll to bottom when the policy is enabled.
            true
        } else {
            // Only scroll if the user is within 5 items of the bottom.
            previousCount == 0 || (previousCount - 1) - lastVisiblePos < 5
        }
    } else {
        // Even if wasAtBottom is false (stale due to keyboard/compose-area resize), always
        // scroll to bottom for the logged-in user's own messages.
        lastIsOwn
    }
}
