package com.example.money.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light Theme Colors
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),         // Money green
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F6CA), // Light mint container
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF1976D2),        // Trustworthy blue
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary = Color(0xFF6200EA),         // Rich purple for premium features
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8DEF8),
    onTertiaryContainer = Color(0xFF4A148C),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF78909C)
)

// Dark Theme Colors
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF66BB6A),         // Lighter money green for dark mode
    onPrimary = Color(0xFF1B5E20),
    primaryContainer = Color(0xFF2E7D32),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = Color(0xFF64B5F6),        // Lighter blue for dark mode
    onSecondary = Color(0xFF0D47A1),
    secondaryContainer = Color(0xFF1565C0),
    onSecondaryContainer = Color(0xFFBBDEFB),
    tertiary = Color(0xFFB388FF),         // Lighter purple for dark mode
    onTertiary = Color(0xFF4A148C),
    tertiaryContainer = Color(0xFF6200EA),
    onTertiaryContainer = Color(0xFFE8DEF8),
    error = Color(0xFFEF5350),
    onError = Color(0xFFB71C1C),
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color(0xFFFFEBEE),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF90A4AE)
)

@Composable
fun MoneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

// Extension properties for easy access to spacing
val MaterialTheme.spacing: Spacing
    @Composable
    get() = LocalSpacing.current