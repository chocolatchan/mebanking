package com.mebanking.app.data.local.seed

import com.mebanking.app.data.local.AppDatabase
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.BillEntity
import com.mebanking.app.data.local.entity.BranchEntity
import com.mebanking.app.data.local.entity.ChatIntentEntity
import com.mebanking.app.data.local.entity.ExternalBankEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.BillStatus
import com.mebanking.app.domain.model.BillType
import com.mebanking.app.domain.model.Role

/**
 * Idempotent database seeder for populating initial offline demo data and test fixtures.
 *
 * Security Notice:
 * All pre-populated users, accounts and bills are demo fixtures for the offline
 * course build. They are intentionally seeded with public demo credentials and
 * MUST NOT be shipped as production customer data. Passwords are stored as
 * PBKDF2WithHmacSHA256 hashes (never plaintext); see [PasswordHasher].
 */
object DatabaseSeeder {

    object TestFixtures {
        const val CUSTOMER_ID = 1L
        const val OFFICER_ID = 2L
        const val CUSTOMER_USERNAME = "customer1"
        const val OFFICER_USERNAME = "officer1"
        const val TEST_PASSWORD = "Password@123"

        val customerPasswordHash: String by lazy {
            PasswordHasher.hashPassword(TEST_PASSWORD)
        }

        val officerPasswordHash: String by lazy {
            PasswordHasher.hashPassword(TEST_PASSWORD)
        }

        fun initialUsers(): List<UserEntity> = listOf(
            UserEntity(
                id = CUSTOMER_ID,
                username = CUSTOMER_USERNAME,
                passwordHash = customerPasswordHash,
                fullName = "Nguyễn Văn Khách",
                email = "khach.nguyen@example.com",
                phone = "0901234567",
                role = Role.CUSTOMER,
                kycVerified = true,
                createdAt = 1700000000000L,
            ),
            UserEntity(
                id = OFFICER_ID,
                username = OFFICER_USERNAME,
                passwordHash = officerPasswordHash,
                fullName = "Trần Thị Cán Bộ",
                email = "canbo.tran@mebanking.com",
                phone = "0987654321",
                role = Role.OFFICER,
                kycVerified = true,
                createdAt = 1700000000000L,
            ),
        )

        fun initialAccounts(): List<AccountEntity> = listOf(
            AccountEntity(
                id = 1L,
                userId = CUSTOMER_ID,
                accountNumber = "100123456789",
                accountType = AccountType.CHECKING,
                balance = 50_000_000L, // 50,000,000 VND
                currency = "VND",
                interestRate = 0.0,
                status = "ACTIVE",
                createdAt = 1700000000000L,
            ),
            AccountEntity(
                id = 2L,
                userId = CUSTOMER_ID,
                accountNumber = "200123456789",
                accountType = AccountType.SAVING,
                balance = 100_000_000L, // 100,000,000 VND
                currency = "VND",
                interestRate = 5.8,
                status = "ACTIVE",
                createdAt = 1700000000000L,
            ),
            AccountEntity(
                id = 3L,
                userId = CUSTOMER_ID,
                accountNumber = "300123456789",
                accountType = AccountType.MORTGAGE,
                balance = 0L,
                currency = "VND",
                interestRate = 8.5,
                status = "ACTIVE",
                loanAmount = 500_000_000L,
                monthlyPayment = 15_000_000L,
                payCycle = "MONTHLY",
                createdAt = 1700000000000L,
            ),
        )

        fun initialExternalBanks(): List<ExternalBankEntity> = listOf(
            ExternalBankEntity(
                id = 1L,
                bankCode = "VCB",
                bankName = "Ngân hàng TMCP Ngoại thương Việt Nam",
                shortName = "Vietcombank",
                bin = "970436",
                isSupported = true,
            ),
            ExternalBankEntity(
                id = 2L,
                bankCode = "BIDV",
                bankName = "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam",
                shortName = "BIDV",
                bin = "970418",
                isSupported = true,
            ),
            ExternalBankEntity(
                id = 3L,
                bankCode = "CTG",
                bankName = "Ngân hàng TMCP Công thương Việt Nam",
                shortName = "VietinBank",
                bin = "970415",
                isSupported = true,
            ),
            ExternalBankEntity(
                id = 4L,
                bankCode = "TCB",
                bankName = "Ngân hàng TMCP Kỹ thương Việt Nam",
                shortName = "Techcombank",
                bin = "970407",
                isSupported = true,
            ),
            ExternalBankEntity(
                id = 5L,
                bankCode = "MB",
                bankName = "Ngân hàng TMCP Quân đội",
                shortName = "MBBank",
                bin = "970422",
                isSupported = true,
            ),
        )

        fun initialBranches(): List<BranchEntity> = listOf(
            BranchEntity(
                id = 1L,
                name = "Chi nhánh Quận 1 - Hội Sở",
                address = "123 Đường Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh",
                latitude = 10.7769,
                longitude = 106.7009,
                phone = "02838221234",
                openingHours = "08:00 - 17:00 (T2 - T6)",
                hasAtm = true,
            ),
            BranchEntity(
                id = 2L,
                name = "Chi nhánh Quận 7 - Tân Phong",
                address = "19 Đường Nguyễn Hữu Thọ, Phường Tân Phong, Quận 7, TP. Hồ Chí Minh",
                latitude = 10.7326,
                longitude = 106.6992,
                phone = "02837751234",
                openingHours = "08:00 - 17:00 (T2 - T6)",
                hasAtm = true,
            ),
            BranchEntity(
                id = 3L,
                name = "ATM Chợ Bến Thành",
                address = "Cửa Nam Chợ Bến Thành, Quận 1, TP. Hồ Chí Minh",
                latitude = 10.7725,
                longitude = 106.6980,
                phone = "19001234",
                openingHours = "24/7",
                hasAtm = true,
            ),
        )

        fun initialBills(): List<BillEntity> = listOf(
            BillEntity(
                id = 1L,
                userId = CUSTOMER_ID,
                provider = "EVN HCMC",
                billType = BillType.ELECTRICITY,
                amount = 1_250_000L,
                dueDate = 1701000000000L,
                status = BillStatus.UNPAID,
                paidAt = null,
            ),
            BillEntity(
                id = 2L,
                userId = CUSTOMER_ID,
                provider = "SAWACO",
                billType = BillType.WATER,
                amount = 180_000L,
                dueDate = 1701000000000L,
                status = BillStatus.UNPAID,
                paidAt = null,
            ),
            BillEntity(
                id = 3L,
                userId = CUSTOMER_ID,
                provider = "VNPT Telecom",
                billType = BillType.INTERNET,
                amount = 330_000L,
                dueDate = 1700500000000L,
                status = BillStatus.PAID,
                paidAt = 1700400000000L,
            ),
        )

        fun initialChatIntents(): List<ChatIntentEntity> = listOf(
            ChatIntentEntity(
                id = 1L,
                intentCode = "transfer_money",
                patterns = listOf("chuyển tiền", "bắn tiền", "chuyển khoản", "transfer", "gui tien", "chuyen tien"),
                responseTemplate = "Bạn muốn chuyển tiền đến tài khoản nội bộ MEbanking hay chuyển liên ngân hàng 24/7?",
                targetScreen = "transfer",
                priority = 10,
            ),
            ChatIntentEntity(
                id = 2L,
                intentCode = "pay_bills",
                patterns = listOf("thanh toán hóa đơn", "đóng tiền điện", "đóng tiền nước", "nạp cước", "bill", "hóa đơn", "hoa don", "tiền điện", "tiền nước", "thanh toán"),
                responseTemplate = "MEbanking hỗ trợ thanh toán hóa đơn điện, nước, internet và nạp tiền điện thoại. Vui lòng chọn dịch vụ.",
                targetScreen = "bills",
                priority = 9,
            ),
            ChatIntentEntity(
                id = 3L,
                intentCode = "check_balance",
                patterns = listOf("số dư", "kiểm tra số dư", "tài khoản còn bao nhiêu", "balance", "xem tien", "so du", "tài khoản"),
                responseTemplate = "Bạn có thể kiểm tra số dư chi tiết của từng tài khoản thanh toán và tiết kiệm ngay trên trang chủ.",
                targetScreen = "home",
                priority = 8,
            ),
            ChatIntentEntity(
                id = 4L,
                intentCode = "nearest_atm",
                patterns = listOf("cây atm gần nhất", "tìm atm", "chi nhánh ở đâu", "branch", "dia diem", "rút tiền", "rut tien", "atm", "chi nhánh", "chi nhanh"),
                responseTemplate = "Mở bản đồ ngoại tuyến để tìm chi nhánh và cây ATM MEbanking gần bạn nhất với chỉ đường đi bộ.",
                targetScreen = "branches",
                priority = 7,
            ),
        )
    }

    /**
     * Seeds initial fixtures into the database idempotently.
     * Uses fixed IDs and INSERT OR IGNORE so running multiple times causes no duplicates,
     * no constraint violations, and no overwriting of modified user balances.
     */
    suspend fun seed(db: AppDatabase) {
        db.userDao().insertAll(TestFixtures.initialUsers())
        db.accountDao().insertAll(TestFixtures.initialAccounts())
        db.externalBankDao().insertAll(TestFixtures.initialExternalBanks())
        db.branchDao().insertAll(TestFixtures.initialBranches())
        db.billDao().insertAll(TestFixtures.initialBills())
        db.chatIntentDao().insertAll(TestFixtures.initialChatIntents())
    }
}
