package com.cometchat.uikit.compose.preview.presentation.ui.shared

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.presentation.shared.toolbar.CometChatToolbar
import com.cometchat.uikit.compose.presentation.shared.toolbar.CometChatToolbarStyle
import com.cometchat.uikit.compose.theme.CometChatTheme

// ============================================================================
// SECTION 1: DEFAULT PREVIEWS
// ============================================================================

/**
 * Preview showing the default toolbar.
 */
@Preview(showBackground = true, name = "Toolbar - Default")
@Composable
public fun PreviewToolbarDefault() {
    CometChatTheme {
        CometChatToolbar(title = "Chats")
    }
}

/**
 * Preview showing toolbar with back icon.
 */
@Preview(showBackground = true, name = "Toolbar - With Back Icon")
@Composable
public fun PreviewToolbarWithBackIcon() {
    CometChatTheme {
        CometChatToolbar(
            title = "Chats",
            hideBackIcon = false,
            onNavigationClick = { }
        )
    }
}

/**
 * Preview showing toolbar with back icon hidden.
 */
@Preview(showBackground = true, name = "Toolbar - No Back Icon")
@Composable
public fun PreviewToolbarNoBackIcon() {
    CometChatTheme {
        CometChatToolbar(
            title = "Chats",
            hideBackIcon = true
        )
    }
}

// ============================================================================
// SECTION 2: ACTION BUTTONS PREVIEWS
// ============================================================================

/**
 * Preview showing toolbar with action buttons.
 */
@Preview(showBackground = true, name = "Toolbar - With Actions")
@Composable
public fun PreviewToolbarWithActions() {
    CometChatTheme {
        CometChatToolbar(
            title = "Chats",
            hideBackIcon = true,
            actions = {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.cometchat_a11y_search),
                        tint = CometChatTheme.colorScheme.iconTintPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cometchat_a11y_more),
                        tint = CometChatTheme.colorScheme.iconTintPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )
    }
}

// ============================================================================
// SECTION 3: SELECTION MODE PREVIEWS
// ============================================================================

/**
 * Preview showing toolbar in selection mode.
 */
@Preview(showBackground = true, name = "Toolbar - Selection Mode")
@Composable
public fun PreviewToolbarSelectionMode() {
    CometChatTheme {
        CometChatToolbar(
            title = "Chats",
            selectionMode = true,
            selectionCount = 3,
            onDiscardSelection = { },
            onSubmitSelection = { }
        )
    }
}

/**
 * Preview showing toolbar in selection mode with zero items.
 */
@Preview(showBackground = true, name = "Toolbar - Selection Mode - Zero")
@Composable
public fun PreviewToolbarSelectionModeZero() {
    CometChatTheme {
        CometChatToolbar(
            title = "Chats",
            selectionMode = true,
            selectionCount = 0,
            onDiscardSelection = { },
            onSubmitSelection = { }
        )
    }
}

// ============================================================================
// SECTION 4: STYLE CUSTOMIZATION PREVIEWS
// ============================================================================

/**
 * Preview showing toolbar with custom background color.
 */
@Preview(showBackground = true, name = "Toolbar - Custom Background")
@Composable
public fun PreviewToolbarCustomBackground() {
    CometChatTheme {
        CometChatToolbar(
            title = "Chats",
            hideBackIcon = true,
            style = CometChatToolbarStyle.default(
                backgroundColor = Color(0xFFF0F4FF)
            )
        )
    }
}

/**
 * Preview showing toolbar with custom title color.
 */
@Preview(showBackground = true, name = "Toolbar - Custom Title Color")
@Composable
public fun PreviewToolbarCustomTitleColor() {
    CometChatTheme {
        CometChatToolbar(
            title = "Chats",
            hideBackIcon = true,
            style = CometChatToolbarStyle.default(
                titleTextColor = CometChatTheme.colorScheme.primary
            )
        )
    }
}
