package com.hikmah.hadits.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Cream,
    primaryContainer = Color(0xFFD6E9DE),
    onPrimaryContainer = Color(0xFF0E372B),
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBC9),
    onSecondaryContainer = Color(0xFF3A0D00),
    tertiary = Color(0xFF7D5C26),
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = Slate,
    outline = Color(0xFF7A837E),
)

private val DarkColors = darkColorScheme(
    primary = ForestDark,
    onPrimary = Color(0xFF06382C),
    primaryContainer = Color(0xFF1D4B3E),
    onPrimaryContainer = Color(0xFFB8E8D4),
    secondary = Color(0xFFFFB791),
    onSecondary = Color(0xFF54200A),
    secondaryContainer = Color(0xFF74351B),
    onSecondaryContainer = Color(0xFFFFDBC9),
    background = Color(0xFF111816),
    onBackground = Color(0xFFE1E8E3),
    surface = Color(0xFF111816),
    onSurface = Color(0xFFE1E8E3),
    surfaceVariant = Color(0xFF3E4944),
    onSurfaceVariant = Color(0xFFC0CAC4),
    outline = Color(0xFF8A948E),
)

@Composable
fun HikmahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val background = colors.background.toArgb()
            window.statusBarColor = background
            window.navigationBarColor = background
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = HikmahTypography,
        content = content,
    )
}
