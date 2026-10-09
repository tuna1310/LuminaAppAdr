package com.lumina.manualcamera.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LuminaAmber,
    secondary = LuminaGreen,
    tertiary = LuminaBlue,
    background = LuminaBlack,
    surface = LuminaDarkSurface,
    onPrimary = LuminaBlack,
    onSecondary = LuminaBlack,
    onTertiary = LuminaBlack,
    onBackground = LuminaTextPrimary,
    onSurface = LuminaTextPrimary
)

@Composable
fun LuminaTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = LuminaBlack.toArgb()
                window.navigationBarColor = LuminaBlack.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
