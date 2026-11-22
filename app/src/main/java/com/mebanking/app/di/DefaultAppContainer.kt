package com.mebanking.app.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.data.local.dao.BillDao
import com.mebanking.app.data.local.dao.BranchDao
import com.mebanking.app.data.local.dao.ChatIntentDao
import com.mebanking.app.data.local.dao.ExternalBankDao
import com.mebanking.app.data.local.dao.OtpCodeDao
import com.mebanking.app.data.local.dao.lifestyle.CinemaDao
import com.mebanking.app.data.local.dao.lifestyle.FlightDao
import com.mebanking.app.data.local.dao.lifestyle.HotelDao
import com.mebanking.app.data.local.dao.TransactionDao
import com.mebanking.app.data.local.dao.UserDao
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.data.local.seed.DatabaseSeeder
import com.mebanking.app.data.local.seed.LifestyleDatabaseSeeder
import com.mebanking.app.data.navigation.RoadGraphLoader
import com.mebanking.app.data.repository.AccountRepository
import com.mebanking.app.data.repository.AuthRepository
import com.mebanking.app.data.repository.BranchNavigationRepository
import com.mebanking.app.data.repository.ChatbotRepository
import com.mebanking.app.data.repository.DefaultAccountRepository
import com.mebanking.app.data.repository.DefaultAuthRepository
import com.mebanking.app.data.repository.DefaultChatbotRepository
import com.mebanking.app.data.repository.DefaultSimulatorRepository
import com.mebanking.app.data.repository.DefaultTransactionRepository
import com.mebanking.app.data.repository.SimulatorRepository
import com.mebanking.app.data.repository.TransactionRepository
import com.mebanking.app.data.repository.lifestyle.CinemaRepository
import com.mebanking.app.data.repository.lifestyle.DefaultCinemaRepository
import com.mebanking.app.data.repository.lifestyle.DefaultFlightRepository
import com.mebanking.app.data.repository.lifestyle.DefaultHotelRepository
import com.mebanking.app.data.repository.lifestyle.FlightRepository
import com.mebanking.app.data.repository.lifestyle.HotelRepository
import com.mebanking.app.domain.service.AccountService
import com.mebanking.app.domain.service.AuthService
import com.mebanking.app.domain.service.ChatbotService
import com.mebanking.app.domain.service.DefaultAccountService
import com.mebanking.app.domain.service.DefaultAuthService
import com.mebanking.app.domain.service.DefaultChatbotService
import com.mebanking.app.domain.service.DefaultFaceRecognitionService
import com.mebanking.app.domain.service.DefaultOfficerCustomerService
import com.mebanking.app.domain.service.DefaultSessionManager
import com.mebanking.app.domain.service.DefaultSimulatorService
import com.mebanking.app.domain.service.DefaultTransactionAuthenticationPolicy
import com.mebanking.app.domain.service.DefaultTransactionService
import com.mebanking.app.domain.service.FaceRecognitionService
import com.mebanking.app.domain.service.OfficerCustomerService
import com.mebanking.app.domain.service.SessionManager
import com.mebanking.app.domain.service.SimulatorService
import com.mebanking.app.domain.service.TransactionAuthenticationPolicy
import com.mebanking.app.domain.service.TransactionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

class DefaultAppContainer(
    private val context: Context,
) : AppContainer {

    private val initializationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val database: AppDatabase by lazy {
        AppDatabase.buildDatabase(context)
    }

    /**
     * Seeds the offline demo fixtures exactly like the E2E harness. The login
     * screen awaits this before authenticating so a cold first launch cannot
     * race the fixture insert on the IO dispatcher.
     */
    override val databaseReady: Deferred<Unit> = initializationScope.async {
        DatabaseSeeder.seed(database)
        LifestyleDatabaseSeeder.seed(database)
    }

    override val accountDao: AccountDao by lazy { database.accountDao() }
    override val userDao: UserDao by lazy { database.userDao() }
    override val transactionDao: TransactionDao by lazy { database.transactionDao() }
    override val billDao: BillDao by lazy { database.billDao() }
    override val externalBankDao: ExternalBankDao by lazy { database.externalBankDao() }
    override val branchDao: BranchDao by lazy { database.branchDao() }
    override val chatIntentDao: ChatIntentDao by lazy { database.chatIntentDao() }
    override val otpCodeDao: OtpCodeDao by lazy { database.otpCodeDao() }
    override val flightDao: FlightDao by lazy { database.flightDao() }
    override val hotelDao: HotelDao by lazy { database.hotelDao() }
    override val cinemaDao: CinemaDao by lazy { database.cinemaDao() }

    override val passwordHasher: PasswordHasher = PasswordHasher

    override val accountRepository: AccountRepository by lazy {
        DefaultAccountRepository(accountDao)
    }

    override val branchNavigationRepository: BranchNavigationRepository by lazy {
        BranchNavigationRepository(
            branchDao = branchDao,
            roadGraphLoader = RoadGraphLoader(context),
        )
    }

    override val authRepository: AuthRepository by lazy {
        DefaultAuthRepository(userDao)
    }

    override val sessionManager: SessionManager by lazy {
        DefaultSessionManager(authRepository = authRepository, passwordHasher = passwordHasher)
    }

    override val authService: AuthService by lazy {
        DefaultAuthService(
            sessionManager = sessionManager,
            authRepository = authRepository,
            passwordHasher = passwordHasher,
        )
    }

    override val officerCustomerService: OfficerCustomerService by lazy {
        DefaultOfficerCustomerService(
            userDao = userDao,
            accountDao = accountDao,
            sessionManager = sessionManager,
            passwordHasher = passwordHasher,
        )
    }

    override val accountService: AccountService by lazy {
        DefaultAccountService(
            accountRepository = accountRepository,
            sessionManager = sessionManager,
        )
    }

    override val faceRecognitionService: FaceRecognitionService by lazy {
        DefaultFaceRecognitionService(context = context)
    }

    override val transactionRepository: TransactionRepository by lazy {
        DefaultTransactionRepository(
            transactionDao = transactionDao,
            accountDao = accountDao,
            database = database,
        )
    }

    override val simulatorRepository: SimulatorRepository by lazy {
        DefaultSimulatorRepository(
            externalBankDao = externalBankDao,
            billDao = billDao,
            accountDao = accountDao,
            transactionDao = transactionDao,
            database = database,
        )
    }

    override val chatbotRepository: ChatbotRepository by lazy {
        DefaultChatbotRepository(
            chatIntentDao = chatIntentDao,
        )
    }

    override val flightRepository: FlightRepository by lazy {
        DefaultFlightRepository(flightDao = flightDao)
    }

    override val hotelRepository: HotelRepository by lazy {
        DefaultHotelRepository(hotelDao = hotelDao)
    }

    override val cinemaRepository: CinemaRepository by lazy {
        DefaultCinemaRepository(cinemaDao = cinemaDao)
    }

    override val authenticationPolicy: TransactionAuthenticationPolicy by lazy {
        DefaultTransactionAuthenticationPolicy(
            faceRecognitionService = faceRecognitionService,
        )
    }

    override val transactionService: TransactionService by lazy {
        DefaultTransactionService(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            accountDao = accountDao,
            transactionDao = transactionDao,
            authenticationPolicy = authenticationPolicy,
            sessionManager = sessionManager,
        )
    }

    override val simulatorService: SimulatorService by lazy {
        DefaultSimulatorService(
            simulatorRepository = simulatorRepository,
            accountRepository = accountRepository,
            authenticationPolicy = authenticationPolicy,
            sessionManager = sessionManager,
        )
    }

    override val chatbotService: ChatbotService by lazy {
        DefaultChatbotService(
            chatbotRepository = chatbotRepository,
        )
    }

    override val viewModelFactory: ViewModelProvider.Factory by lazy {
        ViewModelFactory(this)
    }
}
