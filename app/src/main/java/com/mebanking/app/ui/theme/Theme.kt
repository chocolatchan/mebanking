package com.mebanking.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PastelPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PastelPurpleContainer,
    onPrimaryContainer = PastelPurpleDark,
    secondary = Color(0xFF8E7AB5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3EEFA),
    onSecondaryContainer = PastelPurpleDark,
    background = BankLightBg,
    onBackground = TextPrimaryLight,
    surface = BankLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = BankLightCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = PastelPurpleBorder,
    outlineVariant = Color(0xFFEBE6F3),
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error40,
)

private val DarkColorScheme = darkColorScheme(
    primary = PastelPurpleLight,
    onPrimary = Color(0xFF24163E),
    primaryContainer = Color(0xFF4D3873),
    onPrimaryContainer = Color(0xFFEDE7F6),
    secondary = Color(0xFFBBA7DC),
    onSecondary = Color(0xFF2A1C44),
    secondaryContainer = Color(0xFF382D4A),
    onSecondaryContainer = Color(0xFFF1E9FA),
    background = BankDarkBg,
    onBackground = Color(0xFFECE7F0),
    surface = BankDarkSurface,
    onSurface = Color(0xFFECE7F0),
    surfaceVariant = BankDarkCard,
    onSurfaceVariant = Color(0xFFCAC4D2),
    outline = Color(0xFF554E60),
    outlineVariant = Color(0xFF3A3445),
    error = Error80,
    onError = Error40,
    errorContainer = Error40,
    onErrorContainer = Error90,
)

/**
 * Brand theme for the app with Pastel Purple palette and clean flat styling.
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
