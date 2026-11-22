package com.mebanking.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Sand95,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Gold40,
    onSecondary = Sand95,
    background = Sand95,
    onBackground = Sand10,
    surface = Sand95,
    onSurface = Sand10,
    surfaceVariant = Sand90,
    onSurfaceVariant = Sand20,
    error = Error40,
    onError = Sand95,
    errorContainer = Error90,
    onErrorContainer = Error40,
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Gold80,
    onSecondary = Sand20,
    background = Sand10,
    onBackground = Sand90,
    surface = Sand10,
    onSurface = Sand90,
    surfaceVariant = Sand20,
    onSurfaceVariant = Sand90,
    error = Error80,
    onError = Error40,
    errorContainer = Error40,
    onErrorContainer = Error90,
)

/**
 * Brand theme for the app.
 *
 * Set [dynamicColor] to true to let Android 12+ derive the palette from the
 * user's wallpaper instead of the banking brand colors.
 */
@Composable
fun MEbankingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
