package com.cometchat.uikit.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

public object CometChatTheme {

    public val colorScheme: CometChatColorScheme
        @Composable @ReadOnlyComposable get() = LocalColorScheme.current

    public val typography: CometChatTypography
        @Composable @ReadOnlyComposable get() = LocalTypography.current

    public val shapes: Shapes
        @Composable @ReadOnlyComposable get() = LocalShapes.current

}