package com.mebanking.app.di

import androidx.lifecycle.ViewModelProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.data.local.dao.BillDao
import com.mebanking.app.data.local.dao.BranchDao
import com.mebanking.app.data.local.dao.ChatIntentDao
import com.mebanking.app.data.local.dao.ExternalBankDao
import com.mebanking.app.data.local.dao.OtpCodeDao
import com.mebanking.app.data.local.dao.TransactionDao
import com.mebanking.app.data.local.dao.UserDao
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.data.repository.AuthRepository
import com.mebanking.app.data.repository.BranchNavigationRepository
import com.mebanking.app.data.repository.ChatbotRepository
import com.mebanking.app.data.repository.SimulatorRepository
import com.mebanking.app.data.repository.TransactionRepository
import com.mebanking.app.domain.service.AccountService
import com.mebanking.app.domain.service.AuthService
import com.mebanking.app.domain.service.ChatbotService
import com.mebanking.app.domain.service.FaceRecognitionService
import com.mebanking.app.domain.service.OfficerCustomerService
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.SimulatorService
import com.mebanking.app.domain.service.TransactionAuthenticationPolicy
import com.mebanking.app.domain.service.TransactionService

interface AppContainer {
    val database: AppDatabase
    val accountDao: AccountDao
    val userDao: UserDao
    val transactionDao: TransactionDao
    val billDao: BillDao
    val externalBankDao: ExternalBankDao
    val branchDao: BranchDao
    val chatIntentDao: ChatIntentDao
    val otpCodeDao: OtpCodeDao
    val passwordHasher: PasswordHasher

    val accountRepository: AccountRepository
    val authRepository: AuthRepository
    val transactionRepository: TransactionRepository
    val simulatorRepository: SimulatorRepository
    val branchNavigationRepository: BranchNavigationRepository
    val chatbotRepository: ChatbotRepository

    val sessionManager: SessionManager
    val authService: AuthService
    val officerCustomerService: OfficerCustomerService
    val accountService: AccountService
    val faceRecognitionService: FaceRecognitionService
    val transactionService: TransactionService
    val simulatorService: SimulatorService
    val chatbotService: ChatbotService
    val authenticationPolicy: TransactionAuthenticationPolicy

    val viewModelFactory: ViewModelProvider.Factory
}
