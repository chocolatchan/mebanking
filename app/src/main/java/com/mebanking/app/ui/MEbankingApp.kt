package com.mebanking.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mebanking.app.ui.home.HomeScreen
import com.mebanking.app.ui.navigation.BranchLocatorScreen

/**
 * Root composable hosting top-level Compose navigation for the MEbanking application.
 */
@Composable
fun MEbankingApp() {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavHost(
            navController = navController,
            startDestination = "home",
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToBranchLocator = {
                        navController.navigate("branch_locator")
                    },
                )
            }
            composable("branch_locator") {
                BranchLocatorScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
