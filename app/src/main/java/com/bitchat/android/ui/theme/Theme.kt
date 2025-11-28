package com.bitchat.android.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

// Zii Dark Theme - Modern dark design with blue accents
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),        // Bright Zii blue for dark mode
    onPrimary = Color(0xFF0F172A),      // Dark text on blue
    secondary = Color(0xFF3B82F6),      // Standard Zii blue
    onSecondary = Color.White,
    tertiary = Color(0xFF8B5CF6),       // Purple accent
    onTertiary = Color.White,
    background = Color(0xFF0F172A),     // Deep dark slate
    onBackground = Color(0xFFF8FAFC),   // Very light text
    surface = Color(0xFF1E293B),        // Dark surface
    onSurface = Color(0xFFE2E8F0),      // Light gray text
    surfaceVariant = Color(0xFF334155),  // Lighter surface variant
    onSurfaceVariant = Color(0xFFCBD5E1), // Medium gray text
    outline = Color(0xFF64748B),        // Border color
    error = Color(0xFFEF4444),          // Red for errors
    onError = Color.White
)

// Zii Light Theme - Clean light design with blue accents
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3B82F6),        // Zii blue
    onPrimary = Color.White,
    secondary = Color(0xFF1E40AF),      // Darker blue
    onSecondary = Color.White,
    tertiary = Color(0xFF8B5CF6),       // Purple accent
    onTertiary = Color.White,
    background = Color(0xFFFAFAFA),     // Light gray background
    onBackground = Color(0xFF0F172A),   // Dark text
    surface = Color.White,              // White surface
    onSurface = Color(0xFF1E293B),      // Dark text on surface
    surfaceVariant = Color(0xFFF1F5F9), // Light surface variant
    onSurfaceVariant = Color(0xFF475569), // Medium gray text
    outline = Color(0xFFCBD5E1),       // Light border color
    error = Color(0xFFEF4444),          // Red for errors
    onError = Color.White
)

@Composable
fun ZiiTheme(
    darkTheme: Boolean? = null, // null = use user preference
    content: @Composable () -> Unit
) {
    // App-level override from ThemePreferenceManager
    val themePref by ThemePreferenceManager.themeFlow.collectAsState(initial = ThemePreference.Dark)
    val shouldUseDark = when (darkTheme) {
        true -> true
        false -> false
        null -> when (themePref) {
            ThemePreference.Dark -> true
            ThemePreference.Light -> false
            ThemePreference.System -> isSystemInDarkTheme()
        }
    }

    val colorScheme = if (shouldUseDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    if (!shouldUseDark) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = if (!shouldUseDark) {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else 0
            }
            window.navigationBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
