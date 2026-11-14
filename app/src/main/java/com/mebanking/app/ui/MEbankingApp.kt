package com.mebanking.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mebanking.app.ui.home.HomeScreen

/**
 * Root composable. Add app-level navigation (e.g. Navigation Compose) here as
 * the feature set grows.
 */
@Composable
fun MEbankingApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        HomeScreen()
    }
}
