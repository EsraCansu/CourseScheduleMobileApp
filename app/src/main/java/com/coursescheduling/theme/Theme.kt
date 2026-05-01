package com.coursescheduling.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light colour scheme ──────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Indigo500,
    onPrimary        = White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo900,

    secondary        = Indigo300,
    onSecondary      = White,
    secondaryContainer = Indigo50,
    onSecondaryContainer = Indigo800,

    tertiary         = Indigo600,
    onTertiary       = White,

    background       = Grey100,
    onBackground     = Grey900,

    surface          = White,
    onSurface        = Grey900,
    surfaceVariant   = Grey200,
    onSurfaceVariant = Grey600,

    error            = ErrorRed,
    onError          = White,

    outline          = Grey400
)

// ── Dark colour scheme ───────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = Indigo300,
    onPrimary        = Indigo900,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo50,

    secondary        = Indigo200,
    onSecondary      = Indigo800,
    secondaryContainer = Indigo800,
    onSecondaryContainer = Indigo100,

    tertiary         = Indigo400,
    onTertiary       = Indigo900,

    background       = DarkBackground,
    onBackground     = Grey100,

    surface          = DarkSurface,
    onSurface        = Grey100,
    surfaceVariant   = DarkSurface2,
    onSurfaceVariant = Grey400,

    error            = ErrorRed,
    onError          = White,

    outline          = Grey600
)

// ── Theme composable ─────────────────────────────────────────────────────────
@Composable
fun CourseScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}
