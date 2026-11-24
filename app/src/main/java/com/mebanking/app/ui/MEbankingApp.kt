package com.mebanking.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mebanking.app.MEbankingApplication
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.SessionState
import com.mebanking.app.ui.account.AccountDetailScreen
import com.mebanking.app.ui.auth.LoginScreen
import com.mebanking.app.ui.auth.RegisterScreen
import com.mebanking.app.ui.cash.DepositScreen
import com.mebanking.app.ui.cash.WithdrawScreen
import com.mebanking.app.ui.bills.BillPaymentScreen
import com.mebanking.app.ui.chatbot.ChatbotScreen
import com.mebanking.app.ui.home.HomeScreen
import com.mebanking.app.ui.history.TransactionHistoryScreen
import com.mebanking.app.ui.history.TransactionDetailScreen
import com.mebanking.app.ui.lifestyle.CinemaBookingScreen
import com.mebanking.app.ui.lifestyle.FlightBookingScreen
import com.mebanking.app.ui.lifestyle.HotelBookingScreen
import com.mebanking.app.ui.lifestyle.ShopScreen
import com.mebanking.app.ui.lifestyle.TopupScreen
import com.mebanking.app.ui.navigation.BranchLocatorScreen
import com.mebanking.app.ui.officer.OfficerDashboardScreen
import com.mebanking.app.ui.services.ServicesScreen
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
    var showRegisterScreen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (val state = sessionState) {
            is SessionState.Authenticated -> AuthenticatedApp(
                isCustomer = state.user.role == Role.CUSTOMER,
                onLogout = { container.sessionManager.logout() },
            )
            else -> {
                if (showRegisterScreen) {
                    RegisterScreen(
                        onNavigateBackToLogin = { showRegisterScreen = false },
                    )
                } else {
                    LoginScreen(
                        onNavigateToRegister = { showRegisterScreen = true },
                    )
                }
            }
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
                onNavigateToTransfer = { account, name, amount, note ->
                    if (isCustomer) {
                        if (!account.isNullOrBlank()) {
                            val encAcc = Uri.encode(account)
                            val encName = Uri.encode(name ?: "")
                            val encAmt = Uri.encode(amount ?: "")
                            val encNote = Uri.encode(note ?: "")
                            navController.navigate("transfer?targetAccount=$encAcc&targetName=$encName&targetAmount=$encAmt&targetNote=$encNote") { launchSingleTop = true }
                        } else {
                            navController.navigate("transfer") { launchSingleTop = true }
                        }
                    }
                },
                onNavigateToDeposit = { if (isCustomer) navController.navigate("deposit") { launchSingleTop = true } },
                onNavigateToWithdraw = { if (isCustomer) navController.navigate("withdraw") { launchSingleTop = true } },
                onNavigateToHistory = { if (isCustomer) navController.navigate("transaction_history") { launchSingleTop = true } },
                onNavigateToOfficerDashboard = { if (!isCustomer) navController.navigate("officer_dashboard") { launchSingleTop = true } },
                onNavigateToBills = {
                    if (isCustomer) {
                        navController.navigate("bills") { launchSingleTop = true }
                    }
                },
                onNavigateToServices = {
                    if (isCustomer) {
                        navController.navigate("services") { launchSingleTop = true }
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
        composable(
            route = "transfer?targetAccount={targetAccount}&targetName={targetName}&targetAmount={targetAmount}&targetNote={targetNote}",
            arguments = listOf(
                navArgument("targetAccount") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("targetName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("targetAmount") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("targetNote") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val targetAccount = backStackEntry.arguments?.getString("targetAccount")
            val targetName = backStackEntry.arguments?.getString("targetName")
            val targetAmount = backStackEntry.arguments?.getString("targetAmount")
            val targetNote = backStackEntry.arguments?.getString("targetNote")
            TransferScreen(
                targetAccount = targetAccount,
                targetName = targetName,
                targetAmount = targetAmount,
                targetNote = targetNote,
                onBack = { navController.popBackStack() },
            )
        }
        composable("transfer") {
            TransferScreen(onBack = { navController.popBackStack() })
        }
        composable("deposit") { DepositScreen(onBack = { navController.popBackStack() }) }
        composable("withdraw") { WithdrawScreen(onBack = { navController.popBackStack() }) }
        composable("transaction_history") {
            TransactionHistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenTransaction = { id -> navController.navigate("transaction_detail/$id") { launchSingleTop = true } },
            )
        }
        composable("transaction_detail/{transactionId}") { entry ->
            val transactionId = entry.arguments?.getString("transactionId")?.toLongOrNull() ?: -1L
            TransactionDetailScreen(transactionId = transactionId, onBack = { navController.popBackStack() })
        }
        composable("officer_dashboard") {
            OfficerDashboardScreen(onBack = { navController.popBackStack() })
        }
        composable("bills") {
            BillPaymentScreen(onBack = { navController.popBackStack() })
        }
        composable("services") {
            ServicesScreen(
                onBack = { navController.popBackStack() },
                onNavigateToBills = { navController.navigate("bills") { launchSingleTop = true } },
                onNavigateToTopup = { navController.navigate("topup") { launchSingleTop = true } },
                onNavigateToFlights = { navController.navigate("flight_booking") { launchSingleTop = true } },
                onNavigateToHotels = { navController.navigate("hotel_booking") { launchSingleTop = true } },
                onNavigateToCinema = { navController.navigate("cinema_booking") { launchSingleTop = true } },
                onNavigateToShop = { navController.navigate("shop") { launchSingleTop = true } },
                onNavigateToBranchLocator = { navController.navigate("branch_locator") { launchSingleTop = true } },
                onNavigateToChatbot = { navController.navigate("chatbot") { launchSingleTop = true } },
                onNavigateToTransfer = { navController.navigate("transfer") { launchSingleTop = true } },
            )
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
