package com.mebanking.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.ui.account.AccountDetailScreen
import com.mebanking.app.ui.auth.LoginScreen
import com.mebanking.app.ui.bills.BillPaymentScreen
import com.mebanking.app.ui.chatbot.ChatbotScreen
import com.mebanking.app.ui.home.HomeScreen
import com.mebanking.app.ui.lifestyle.CinemaBookingScreen
import com.mebanking.app.ui.lifestyle.FlightBookingScreen
import com.mebanking.app.ui.lifestyle.HotelBookingScreen
import com.mebanking.app.ui.lifestyle.ShopScreen
import com.mebanking.app.ui.lifestyle.TopupScreen
import com.mebanking.app.ui.navigation.BranchLocatorScreen
import com.mebanking.app.ui.transfer.TransferScreen

/**
 * Root composable hosting authentication gating and top-level Compose
 * navigation for the MEbanking application.
 *
 * Financial screens are only reachable after [SessionState.Authenticated], and
 * every route relies on the real domain service for authorization, so a deep
 * link can never bypass role or ownership checks.
 */
@Composable
fun MEbankingApp() {
    val container = (LocalContext.current.applicationContext as MEbankingApplication).container
    val sessionState by container.authService.sessionState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (val state = sessionState) {
            is SessionState.Authenticated -> AuthenticatedApp(
                isCustomer = state.user.role == Role.CUSTOMER,
                onLogout = { container.sessionManager.logout() },
            )
            else -> LoginScreen()
        }
    }
}

@Composable
private fun AuthenticatedApp(
    isCustomer: Boolean,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToBranchLocator = {
                    navController.navigate("branch_locator") { launchSingleTop = true }
                },
                onNavigateToTransfer = {
                    if (isCustomer) {
                        navController.navigate("transfer") { launchSingleTop = true }
                    }
                },
                onNavigateToBills = {
                    if (isCustomer) {
                        navController.navigate("bills") { launchSingleTop = true }
                    }
                },
                onNavigateToChatbot = {
                    navController.navigate("chatbot") { launchSingleTop = true }
                },
                onNavigateToAccountDetail = { accountId ->
                    navController.navigate("account_detail/$accountId") { launchSingleTop = true }
                },
                onNavigateToFlights = {
                    if (isCustomer) {
                        navController.navigate("flight_booking") { launchSingleTop = true }
                    }
                },
                onNavigateToHotels = {
                    if (isCustomer) {
                        navController.navigate("hotel_booking") { launchSingleTop = true }
                    }
                },
                onNavigateToCinema = {
                    if (isCustomer) {
                        navController.navigate("cinema_booking") { launchSingleTop = true }
                    }
                },
                onNavigateToShop = {
                    if (isCustomer) {
                        navController.navigate("shop") { launchSingleTop = true }
                    }
                },
                onNavigateToTopup = {
                    if (isCustomer) {
                        navController.navigate("topup") { launchSingleTop = true }
                    }
                },
                onLogout = onLogout,
                isCustomer = isCustomer,
            )
        }
        composable("flight_booking") {
            FlightBookingScreen(onBack = { navController.popBackStack() })
        }
        composable("hotel_booking") {
            HotelBookingScreen(onBack = { navController.popBackStack() })
        }
        composable("cinema_booking") {
            CinemaBookingScreen(onBack = { navController.popBackStack() })
        }
        composable("shop") {
            ShopScreen(onBack = { navController.popBackStack() })
        }
        composable("topup") {
            TopupScreen(onBack = { navController.popBackStack() })
        }
        composable("branch_locator") {
            BranchLocatorScreen(onBack = { navController.popBackStack() })
        }
        composable("transfer") {
            TransferScreen(onBack = { navController.popBackStack() })
        }
        composable("bills") {
            BillPaymentScreen(onBack = { navController.popBackStack() })
        }
        composable("chatbot") {
            ChatbotScreen(
                onBack = { navController.popBackStack() },
                onNavigateToTarget = { target ->
                    when (target) {
                        "transfer" -> if (isCustomer) {
                            navController.navigate("transfer") { launchSingleTop = true }
                        }
                        "bills" -> if (isCustomer) {
                            navController.navigate("bills") { launchSingleTop = true }
                        }
                        "branches" -> navController.navigate("branch_locator") { launchSingleTop = true }
                        "home" -> navController.popBackStack("home", inclusive = false)
                    }
                },
            )
        }
        composable("account_detail/{accountId}") { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId")?.toLongOrNull()
            if (accountId != null) {
                AccountDetailScreen(
                    accountId = accountId,
                    onBack = { navController.popBackStack() },
                )
            } else {
                AccountDetailScreen(
                    accountId = -1L,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
