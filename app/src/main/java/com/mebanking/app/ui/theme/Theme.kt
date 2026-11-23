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
    primary = VcbGreenPrimary,
    onPrimary = Sand95,
    primaryContainer = VcbGreenContainer,
    onPrimaryContainer = VcbGreenDark,
    secondary = BankGoldPrimary,
    onSecondary = Sand95,
    background = BankLightBg,
    onBackground = TextPrimaryLight,
    surface = BankLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = Sand90,
    onSurfaceVariant = TextSecondaryLight,
    error = Error40,
    onError = Sand95,
    errorContainer = Error90,
    onErrorContainer = Error40,
)

private val DarkColorScheme = darkColorScheme(
    primary = VcbGreenLight,
    onPrimary = BankDarkBg,
    primaryContainer = VcbGreenDark,
    onPrimaryContainer = VcbGreenContainer,
    secondary = BankGoldLight,
    onSecondary = BankDarkBg,
    background = BankDarkBg,
    onBackground = Sand90,
    surface = BankDarkSurface,
    onSurface = Sand90,
    surfaceVariant = BankDarkCard,
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

    SynchronizedStatusBar(darkIcons = !darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
