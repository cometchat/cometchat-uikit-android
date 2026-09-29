package com.cometchat.uikit.compose.preview.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.uikit.compose.presentation.groupmembers.style.CometChatGroupMembersStyle
import com.cometchat.uikit.compose.presentation.groupmembers.ui.CometChatGroupMembers
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.factory.CometChatGroupMembersViewModelFactory
import com.cometchat.uikit.core.viewmodel.CometChatGroupMembersViewModel
import com.cometchat.uikit.compose.preview.data.repository.PreviewGroupMembersRepository
import com.cometchat.uikit.compose.preview.domain.PreviewMockData
import com.cometchat.uikit.compose.theme.CometChatTheme

@Composable
private fun rememberPreviewGroupMembersViewModel(
    simulateError: Boolean = false,
    simulateEmpty: Boolean = false
): CometChatGroupMembersViewModel {
    val factory = remember(simulateError, simulateEmpty) {
        CometChatGroupMembersViewModelFactory(
            repository = PreviewGroupMembersRepository(
                simulateError = simulateError,
                simulateEmpty = simulateEmpty
            ),
            enableListeners = false
        )
    }
    return viewModel(factory = factory)
}

private val previewGroup = PreviewMockData.createMockGroup(name = "Team Chat", membersCount = 6)

@Preview(showBackground = true, name = "GroupMembers - Default")
@Composable
public fun PreviewGroupMembersDefault() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm)
    }
}

@Preview(showBackground = true, name = "GroupMembers - Selection - Single")
@Composable
public fun PreviewGroupMembersSingleSelection() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, selectionMode = UIKitConstants.SelectionMode.SINGLE)
    }
}

@Preview(showBackground = true, name = "GroupMembers - Selection - Multiple")
@Composable
public fun PreviewGroupMembersMultipleSelection() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, selectionMode = UIKitConstants.SelectionMode.MULTIPLE)
    }
}

@Preview(showBackground = true, name = "GroupMembers - No Toolbar")
@Composable
public fun PreviewGroupMembersNoToolbar() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, hideToolbar = true)
    }
}

@Preview(showBackground = true, name = "GroupMembers - No Search")
@Composable
public fun PreviewGroupMembersNoSearch() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, hideSearch = true)
    }
}

@Preview(showBackground = true, name = "GroupMembers - No User Status")
@Composable
public fun PreviewGroupMembersNoUserStatus() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, hideUserStatus = true)
    }
}

@Preview(showBackground = true, name = "GroupMembers - With Back Button")
@Composable
public fun PreviewGroupMembersWithBackButton() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, hideBackButton = false, onBackPress = { })
    }
}

@Preview(showBackground = true, name = "GroupMembers - With Separators")
@Composable
public fun PreviewGroupMembersWithSeparators() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, hideSeparator = false)
    }
}

@Preview(showBackground = true, name = "GroupMembers - Kick Disabled")
@Composable
public fun PreviewGroupMembersKickDisabled() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, disableKick = true)
    }
}

@Preview(showBackground = true, name = "GroupMembers - All Permissions Disabled")
@Composable
public fun PreviewGroupMembersAllPermissionsDisabled() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, disableKick = true, disableBan = true, disableChangeScope = true)
    }
}

@Preview(showBackground = true, name = "GroupMembers - Custom Title")
@Composable
public fun PreviewGroupMembersCustomTitle() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, title = "Team Members")
    }
}

@Preview(showBackground = true, name = "GroupMembers - Custom Search Placeholder")
@Composable
public fun PreviewGroupMembersCustomSearchPlaceholder() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, searchPlaceholderText = "Find members...")
    }
}

@Preview(showBackground = true, name = "GroupMembers - Custom Background")
@Composable
public fun PreviewGroupMembersCustomBackground() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(
            group = previewGroup,
            viewModel = vm,
            style = CometChatGroupMembersStyle.default(backgroundColor = Color(0xFFF5F5F5))
        )
    }
}

@Preview(showBackground = true, name = "GroupMembers - Comprehensive")
@Composable
public fun PreviewGroupMembersComprehensive() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(
            group = PreviewMockData.createMockGroup(
                name = "Engineering Team",
                groupType = CometChatConstants.GROUP_TYPE_PRIVATE,
                membersCount = 12
            ),
            viewModel = vm,
            hideBackButton = false,
            searchPlaceholderText = "Search members...",
            onBackPress = { },
            onItemClick = { }
        )
    }
}

@Preview(showBackground = true, name = "GroupMembers - Minimal")
@Composable
public fun PreviewGroupMembersMinimal() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, hideToolbar = true, hideSearch = true, hideSeparator = true)
    }
}

@Preview(showBackground = true, name = "GroupMembers - Exclude Owner")
@Composable
public fun PreviewGroupMembersExcludeOwner() {
    CometChatTheme {
        val vm = rememberPreviewGroupMembersViewModel()
        CometChatGroupMembers(group = previewGroup, viewModel = vm, excludeOwner = true)
    }
}
