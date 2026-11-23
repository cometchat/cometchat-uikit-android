package com.cometchat.sampleapp.kotlin.utils

import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.google.gson.Gson

/**
 * Carries a user or group between screens as an intent extra, so a screen opens on the object the
 * caller already holds (offline too: `CometChat.getUser`/`getGroup` read only the server), then
 * refreshes it. Same encoding as `MessagesActivity`: user JSON, Gson for a group.
 */
object PassedEntity {

    fun encode(user: User): String = user.toJson().toString()

    fun encode(group: Group): String = Gson().toJson(group)

    fun decodeUser(json: String?): User? =
        json?.let { runCatching { User.fromJson(it) }.getOrNull() }

    fun decodeGroup(json: String?): Group? =
        json?.let { runCatching { Gson().fromJson(it, Group::class.java) }.getOrNull() }

    /**
     * Copies the server's user into [shown], the instance a screen and its views already hold, so
     * they read the fresh state without being re-bound.
     */
    fun applyFresh(shown: User, fresh: User) {
        shown.name = fresh.name
        shown.avatar = fresh.avatar
        shown.link = fresh.link
        shown.role = fresh.role
        shown.metadata = fresh.metadata
        shown.status = fresh.status
        shown.statusMessage = fresh.statusMessage
        shown.lastActiveAt = fresh.lastActiveAt
        shown.isHasBlockedMe = fresh.isHasBlockedMe
        shown.isBlockedByMe = fresh.isBlockedByMe
        shown.tags = fresh.tags
        shown.deactivatedAt = fresh.deactivatedAt
    }

    /**
     * Copies the server's group into [shown], including the fields a copy taken from a message lacks
     * (`hasJoined`, scope, owner, member count), so scope-based options use the server's state.
     */
    fun applyFresh(shown: Group, fresh: Group) {
        shown.name = fresh.name
        shown.icon = fresh.icon
        shown.description = fresh.description
        shown.owner = fresh.owner
        shown.groupType = fresh.groupType
        shown.metadata = fresh.metadata
        shown.updatedAt = fresh.updatedAt
        shown.setHasJoined(fresh.isJoined)
        shown.joinedAt = fresh.joinedAt
        shown.scope = fresh.scope
        shown.membersCount = fresh.membersCount
        shown.tags = fresh.tags
    }
}
