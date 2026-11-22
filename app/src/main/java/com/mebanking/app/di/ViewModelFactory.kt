package com.mebanking.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mebanking.app.ui.account.AccountDetailViewModel
import com.mebanking.app.ui.auth.LoginViewModel
import com.mebanking.app.ui.bills.BillPaymentViewModel
import com.mebanking.app.ui.chatbot.ChatbotViewModel
import com.mebanking.app.ui.home.HomeViewModel
import com.mebanking.app.ui.lifestyle.FlightBookingViewModel
import com.mebanking.app.ui.lifestyle.HotelBookingViewModel
import com.mebanking.app.ui.navigation.BranchLocatorViewModel
import com.mebanking.app.ui.transfer.TransferViewModel

class ViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(
                    authService = container.authService,
                    awaitDatabaseReady = { container.databaseReady.await() },
                ) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    repository = container.accountRepository,
                    sessionManager = container.sessionManager,
                ) as T
            }
            modelClass.isAssignableFrom(BranchLocatorViewModel::class.java) -> {
                BranchLocatorViewModel(
                    repository = container.branchNavigationRepository,
                ) as T
            }
            modelClass.isAssignableFrom(TransferViewModel::class.java) -> {
                TransferViewModel(
                    transactionService = container.transactionService,
                    accountRepository = container.accountRepository,
                    sessionManager = container.sessionManager,
                ) as T
            }
            modelClass.isAssignableFrom(BillPaymentViewModel::class.java) -> {
                BillPaymentViewModel(
                    simulatorService = container.simulatorService,
                    accountRepository = container.accountRepository,
                    sessionManager = container.sessionManager,
                ) as T
            }
            modelClass.isAssignableFrom(ChatbotViewModel::class.java) -> {
                ChatbotViewModel(
                    chatbotService = container.chatbotService,
                ) as T
            }
            modelClass.isAssignableFrom(AccountDetailViewModel::class.java) -> {
                AccountDetailViewModel(
                    accountService = container.accountService,
                    sessionManager = container.sessionManager,
                ) as T
            }
            modelClass.isAssignableFrom(FlightBookingViewModel::class.java) -> {
                FlightBookingViewModel(
                    flightRepository = container.flightRepository,
                    sessionManager = container.sessionManager,
                ) as T
            }
            modelClass.isAssignableFrom(HotelBookingViewModel::class.java) -> {
                HotelBookingViewModel(
                    hotelRepository = container.hotelRepository,
                    sessionManager = container.sessionManager,
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
