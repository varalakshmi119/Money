package com.example.money.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Spacing(
    val default: Dp = 16.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val huge: Dp = 48.dp,
    // Specific spacings for cards
    val cardPadding: Dp = 16.dp,
    val cardElevation: Dp = 2.dp,
    val cardCornerRadius: Dp = 12.dp,
    // List item spacings
    val listItemVerticalPadding: Dp = 12.dp,
    val listItemHorizontalPadding: Dp = 16.dp,
    // Button spacings
    val buttonHeight: Dp = 48.dp,
    val buttonCornerRadius: Dp = 24.dp,
    val buttonPadding: Dp = 16.dp,
    // Icon spacings
    val iconSize: Dp = 24.dp,
    val iconPadding: Dp = 8.dp,
    // Screen margins
    val screenHorizontalPadding: Dp = 16.dp,
    val screenVerticalPadding: Dp = 16.dp,
    // Bottom navigation
    val bottomNavHeight: Dp = 56.dp,
    // FAB margins
    val fabMargin: Dp = 16.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() } 