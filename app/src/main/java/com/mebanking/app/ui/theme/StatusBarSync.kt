package com.mebanking.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Synchronizes the system status bar icon brightness (light vs dark icons)
 * with the underlying screen background to ensure high contrast and legibility.
 *
 * @param darkIcons true for dark status bar icons (on light backgrounds),
 *                  false for light/white status bar icons (on dark backgrounds).
 */
@Composable
fun SynchronizedStatusBar(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                try {
                    val window = activity.window
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = darkIcons
                } catch (_: Exception) {
                    // Gracefully ignore on environments without window insets controller
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
