package com.cometchat.sampleapp.compose.ui.groups

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cometchat.chat.core.BannedGroupMembersRequest
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.presentation.shared.defaultstates.CometChatEmptyState
import com.cometchat.uikit.compose.presentation.users.ui.CometChatUsersListItem
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatGroupEvent

/**
 * Full-screen composable for viewing and managing banned members.
 *
 * Displays a list of banned members with the option to unban them,
 * matching the master-app-jetpack behavior.
 *
 * @param groupId The GUID of the group to show banned members for
 * @param onBackPress Callback when back button is pressed
 */
@Composable
fun BannedMembersScreen(
    groupId: String,
    onBackPress: () -> Unit
) {
    val colorScheme = CometChatTheme.colorScheme
    val typography = CometChatTheme.typography

    var group by remember { mutableStateOf<Group?>(null) }
    var bannedMembers by remember { mutableStateOf<List<GroupMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // ENG-39698: BannedGroupMembersRequest serves a stale local cache after an unban, so a
    // just-unbanned member can reappear on a refetch. Track members unbanned this session and
    // filter them out; a re-ban clears the uid so a genuinely re-banned member shows again.
    val unbannedUids = remember { mutableSetOf<String>() }

    // Fetch group data first
    LaunchedEffect(groupId) {
        CometChat.getGroup(groupId, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(fetchedGroup: Group) {
                group = fetchedGroup
                // Then fetch banned members
                fetchBannedMembers(
                    group = fetchedGroup,
                    onSuccess = { members ->
                        bannedMembers = members.filter { it.uid !in unbannedUids }
                        isLoading = false
                    },
                    onError = {
                        isLoading = false
                    }
                )
            }

            override fun onError(e: CometChatException) {
                isLoading = false
                onBackPress()
            }
        })
    }

    // ENG-39698: keep the list live for bans/unbans done through the kit (this user) and
    // elsewhere (another admin/device). The UIKit event bus fires immediately; the SDK group
    // listener covers out-of-app changes.
    val currentGroup = group
    LaunchedEffect(currentGroup) {
        if (currentGroup == null) return@LaunchedEffect
        CometChatEvents.groupEvents.collect { event ->
            when (event) {
                is CometChatGroupEvent.MemberBanned ->
                    if (event.group.guid == currentGroup.guid) {
                        unbannedUids.remove(event.user.uid)
                        fetchBannedMembers(currentGroup, onSuccess = { members ->
                            bannedMembers = members.filter { it.uid !in unbannedUids }
                        }, onError = {})
                    }
                is CometChatGroupEvent.MemberUnbanned ->
                    if (event.group.guid == currentGroup.guid) {
                        unbannedUids.add(event.user.uid)
                        bannedMembers = bannedMembers.filter { it.uid != event.user.uid }
                    }
                else -> {}
            }
        }
    }
    DisposableEffect(currentGroup) {
        if (currentGroup == null) return@DisposableEffect onDispose { }
        val listenerId = "BannedMembersScreen_${currentGroup.guid}"
        CometChat.addGroupListener(listenerId, object : CometChat.GroupListener() {
            override fun onGroupMemberBanned(action: Action, bannedUser: User, bannedBy: User, bannedFrom: Group) {
                if (bannedFrom.guid == currentGroup.guid) {
                    unbannedUids.remove(bannedUser.uid)
                    fetchBannedMembers(currentGroup, onSuccess = { members ->
                        bannedMembers = members.filter { it.uid !in unbannedUids }
                    }, onError = {})
                }
            }

            override fun onGroupMemberUnbanned(action: Action, unbannedUser: User, unbannedBy: User, unbannedFrom: Group) {
                if (unbannedFrom.guid == currentGroup.guid) {
                    unbannedUids.add(unbannedUser.uid)
                    bannedMembers = bannedMembers.filter { it.uid != unbannedUser.uid }
                }
            }
        })
        onDispose { CometChat.removeGroupListener(listenerId) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.backgroundColor1)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Custom Toolbar
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPress) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.iconTintPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Banned Members",
                    style = typography.heading2Bold,
                    color = colorScheme.textColorPrimary
                )
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = colorScheme.strokeColorLight
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colorScheme.primary
                    )
                }
            }
            bannedMembers.isEmpty() -> {
                CometChatEmptyState(
                    title = "No Banned Members",
                    subtitle = "There are no banned members in this group"
                )
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(bannedMembers, key = { it.uid }) { member ->
                        BannedMemberListItem(
                            member = member,
                            group = group,
                            onUnban = {
                                unbannedUids.add(member.uid)
                                bannedMembers = bannedMembers.filter { it.uid != member.uid }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Item for displaying a banned member with unban option.
 */
@Composable
private fun BannedMemberListItem(
    member: GroupMember,
    group: Group?,
    onUnban: () -> Unit
) {
    CometChatUsersListItem(
        user = member,
        onItemClick = { /* No action */ },
        trailingView = {
            IconButton(
                onClick = {
                    group?.let { g ->
                        unbanMember(
                            group = g,
                            member = member,
                            onSuccess = onUnban,
                            onError = { exception ->
                                Log.e("BannedMembersScreen", "Failed to unban: ${exception.message}")
                            }
                        )
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cometchat_ic_close),
                    contentDescription = "Unban",
                    tint = CometChatTheme.colorScheme.errorColor
                )
            }
        }
    )
}

/**
 * Fetch banned members from the group.
 */
private fun fetchBannedMembers(
    group: Group,
    onSuccess: (List<GroupMember>) -> Unit,
    onError: (CometChatException) -> Unit
) {
    val request = BannedGroupMembersRequest.BannedGroupMembersRequestBuilder(group.guid)
        .setLimit(30)
        .build()

    request.fetchNext(object : CometChat.CallbackListener<List<GroupMember>>() {
        override fun onSuccess(members: List<GroupMember>?) {
            onSuccess(members ?: emptyList())
        }

        override fun onError(exception: CometChatException?) {
            exception?.let { onError(it) }
        }
    })
}

/**
 * Unban a member from the group.
 */
private fun unbanMember(
    group: Group,
    member: GroupMember,
    onSuccess: () -> Unit,
    onError: (CometChatException) -> Unit
) {
    CometChat.unbanGroupMember(
        member.uid,
        group.guid,
        object : CometChat.CallbackListener<String>() {
            override fun onSuccess(result: String?) {
                Log.d("BannedMembersScreen", "Member unbanned successfully")
                onSuccess()
            }

            override fun onError(exception: CometChatException?) {
                exception?.let { onError(it) }
            }
        }
    )
}
