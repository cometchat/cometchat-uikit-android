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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.core.BannedGroupMembersRequest
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Action
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.R
import com.cometchat.sampleapp.compose.navigation.RouteEntities
import com.cometchat.uikit.compose.presentation.shared.defaultstates.CometChatEmptyState
import com.cometchat.uikit.compose.presentation.shared.defaultstates.CometChatErrorState
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
    var error by remember { mutableStateOf<String?>(null) }
    // With offline on, a read that fails offline with nothing shown is the offline state, and it
    // reads again once on reconnect; any other failure is the error state. Offline off keeps
    // today's screen.
    val offlineOn = remember { runCatching { CometChat.isOfflineEnabled() }.getOrDefault(false) }
    var offlineError by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }

    // Fetch group data first: the copy Group Info held, else the server's
    LaunchedEffect(groupId) {
        RouteEntities.getGroup(groupId, object : CometChat.CallbackListener<Group>() {
            override fun onSuccess(fetchedGroup: Group) {
                group = fetchedGroup
            }

            override fun onError(e: CometChatException) {
                isLoading = false
                onBackPress()
            }
        })
    }

    // Then fetch banned members — two-phase: a non-empty cached page paints first, the server
    // page replaces it. An error after a cached paint keeps the cached rows (offline).
    val loadedGuid = group?.guid
    LaunchedEffect(loadedGuid, reloadKey) {
        val loadedGroup = group ?: return@LaunchedEffect
        var cachedShown = false
        fetchBannedMembers(
            group = loadedGroup,
            onCached = { members ->
                cachedShown = true
                bannedMembers = members.filter { it.uid !in unbannedUids }
                isLoading = false
            },
            onSuccess = { members ->
                bannedMembers = members.filter { it.uid !in unbannedUids }
                error = null
                isLoading = false
            },
            onError = { exception ->
                if (!cachedShown) {
                    offlineError = exception.code == CometChatConstants.Errors.ERROR_INTERNET_UNAVAILABLE
                    error = exception.message
                    isLoading = false
                }
            }
        )
    }

    // The offline state reloads once when the connection returns.
    val showError = offlineOn && error != null && bannedMembers.isEmpty()
    val reloadOnConnect = showError && offlineError
    DisposableEffect(groupId, reloadOnConnect) {
        val reconnectId = "BannedMembersScreen_reconnect_$groupId"
        if (reloadOnConnect) {
            CometChat.addConnectionListener(reconnectId, object : CometChat.ConnectionListener {
                override fun onConnected() {
                    CometChat.removeConnectionListener(reconnectId)
                    error = null
                    isLoading = true
                    reloadKey++
                }
                override fun onConnecting() = Unit
                override fun onDisconnected() = Unit
                override fun onFeatureThrottled() = Unit
                override fun onConnectionError(e: CometChatException?) = Unit
            })
        }
        onDispose { if (reloadOnConnect) CometChat.removeConnectionListener(reconnectId) }
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
            showError && offlineError -> {
                // Offline with nothing shown: the offline state, no Retry.
                CometChatEmptyState(
                    title = stringResource(com.cometchat.uikit.core.R.string.cometchat_connect_to_load_members),
                    subtitle = ""
                )
            }
            showError -> {
                CometChatErrorState(
                    onRetry = {
                        error = null
                        isLoading = true
                        reloadKey++
                    }
                )
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
    onCached: ((List<GroupMember>) -> Unit)? = null,
    onSuccess: (List<GroupMember>) -> Unit,
    onError: (CometChatException) -> Unit
) {
    val request = BannedGroupMembersRequest.BannedGroupMembersRequestBuilder(group.guid)
        .setLimit(30)
        .build()

    // Two-phase read (BEH-SNAP-07). [onCached] gets a non-empty cached page (an empty one — a cache
    // miss — is skipped, so the caller keeps loading until the server answers); omit it to take the
    // server page only, as the realtime refetches do. Both phases deliver one row per uid.
    request.fetchNext(object : CometChat.BannedGroupMemberFetchListener() {
        override fun onCachedBannedGroupMembers(members: List<GroupMember>?) {
            if (onCached == null || members.isNullOrEmpty()) return
            onCached(members.distinctBy { it.uid })
        }

        override fun onServerBannedGroupMembers(members: List<GroupMember>?) {
            onSuccess(members.orEmpty().distinctBy { it.uid })
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
