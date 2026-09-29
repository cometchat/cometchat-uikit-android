package com.cometchat.uikit.compose.presentation.shared.defaultstates

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cometchat.uikit.compose.R

/**
 * Reusable error state composable for list components.
 * Displays icon, title, subtitle, and retry button when fetching fails.
 *
 * @param onRetry Callback when retry button is clicked
 * @param modifier Modifier to be applied to the component
 * @param style Style configuration for the component
 * @param title Title text to display
 * @param subtitle Subtitle text to display
 */
@Composable
public fun CometChatErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    style: CometChatErrorStateStyle = CometChatErrorStateStyle.default(),
    title: String = "Something went wrong",
    subtitle: String = "Unable to load data. Please try again."
) {
    val cdHoist2 = stringResource(R.string.cometchat_a11y_error_2, title, subtitle)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(style.backgroundColor)
            .padding(32.dp)
            .semantics {
                contentDescription = cdHoist2
                liveRegion = LiveRegionMode.Assertive
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val cdHoist1 = stringResource(R.string.cometchat_a11y_loading, style.retryButtonText)
        // Icon
        style.icon?.let { icon ->
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(style.iconSize),
                tint = style.iconTint
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Title
        Text(
            text = title,
            style = style.titleTextStyle,
            color = style.titleTextColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = subtitle,
            style = style.subtitleTextStyle,
            color = style.subtitleTextColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Retry button
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = style.retryButtonBackgroundColor
            ),
            modifier = Modifier
                .focusable()
                .semantics {
                    contentDescription = cdHoist1
                    role = Role.Button
                }
        ) {
            Text(
                text = style.retryButtonText,
                style = style.retryButtonTextStyle,
                color = style.retryButtonTextColor
            )
        }
    }
}
