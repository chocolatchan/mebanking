package com.mebanking.app.domain.service

import com.mebanking.app.data.local.dao.AccountDao
import com.mebanking.app.data.local.dao.UserDao
import com.mebanking.app.data.local.entity.AccountEntity
import com.mebanking.app.data.local.entity.UserEntity
import com.mebanking.app.data.local.security.AndroidKeystoreNationalIdProtector
import com.mebanking.app.data.local.security.NationalIdProtector
import com.mebanking.app.data.local.security.PasswordHasher
import com.mebanking.app.domain.model.AccountType
import com.mebanking.app.domain.model.DomainError
import com.mebanking.app.domain.model.Role
import com.mebanking.app.domain.model.User
import com.mebanking.app.domain.service.face.FaceQualityValidator

data class RegisterCustomerRequest(
    val username: String,
    val rawPassword: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val nationalId: String,
)

data class RegistrationResult(
    val user: User,
    val accountNumber: String,
    val initialBalance: Long,
)

interface CustomerRegistrationService {
    suspend fun registerCustomerWithBiometrics(
        request: RegisterCustomerRequest,
        faceFrameBytes: ByteArray,
    ): Result<RegistrationResult>
}

class DefaultCustomerRegistrationService(
    private val userDao: UserDao,
    private val accountDao: AccountDao,
    private val faceRecognitionService: FaceRecognitionService,
    private val passwordHasher: PasswordHasher = PasswordHasher,
    private val nationalIdProtector: NationalIdProtector = AndroidKeystoreNationalIdProtector(),
) : CustomerRegistrationService {

    private val usernameRegex = Regex("^[a-zA-Z0-9_]{4,32}$")
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$")
    private val phoneRegex = Regex("^0[0-9]{9}$")
    private val nationalIdRegex = Regex("^[0-9]{12}$")

    override suspend fun registerCustomerWithBiometrics(
        request: RegisterCustomerRequest,
        faceFrameBytes: ByteArray,
    ): Result<RegistrationResult> = try {
        val trimmedUsername = request.username.trim()
        val trimmedName = request.fullName.trim()
        val trimmedEmail = request.email.trim()
        val trimmedPhone = request.phone.trim()
        val trimmedNationalId = request.nationalId.trim()

        // 1. Validate Form Fields
        if (!usernameRegex.matches(trimmedUsername)) {
            Result.failure(DomainError.InvalidData("Tên đăng nhập phải gồm 4-32 ký tự chữ, số hoặc gạch dưới"))
        } else if (request.rawPassword.length < 8) {
            Result.failure(DomainError.InvalidData("Mật khẩu phải có độ dài tối thiểu 8 ký tự"))
        } else if (trimmedName.length < 2) {
            Result.failure(DomainError.InvalidData("Họ và tên phải có tối thiểu 2 ký tự"))
        } else if (!emailRegex.matches(trimmedEmail)) {
            Result.failure(DomainError.InvalidData("Định dạng email không hợp lệ"))
        } else if (!phoneRegex.matches(trimmedPhone)) {
            Result.failure(DomainError.InvalidData("Số điện thoại phải gồm 10 chữ số bắt đầu bằng 0"))
        } else if (!nationalIdRegex.matches(trimmedNationalId)) {
            Result.failure(DomainError.InvalidData("Số CCCD phải gồm đúng 12 chữ số"))
        } else if (userDao.getByUsername(trimmedUsername) != null) {
            // 2. Validate Uniqueness
            Result.failure(DomainError.InvalidData("Tên đăng nhập '$trimmedUsername' đã được sử dụng"))
        } else {
            val protectedId = nationalIdProtector.protect(trimmedNationalId)
                ?: throw DomainError.SystemError("Không thể mã hóa CCCD bảo mật trên thiết bị")

            if (userDao.getByNationalIdHash(protectedId.lookupHash) != null) {
                Result.failure(DomainError.InvalidData("Số CCCD này đã được đăng ký tài khoản khác"))
            } else {
                // 3. Pre-inference Face Quality Validation
                val qualityResult = FaceQualityValidator.validateFrame(faceFrameBytes)
                if (qualityResult.isFailure) {
                    Result.failure(
                        DomainError.InvalidData(
                            qualityResult.exceptionOrNull()?.message ?: "Chất lượng ảnh khuôn mặt không đạt chuẩn eKYC",
                        ),
                    )
                } else {
                    // 4. Create User Entity with kycVerified = true
                    val passwordHash = passwordHasher.hashPasswordV1(request.rawPassword)
                    val userEntity = UserEntity(
                        username = trimmedUsername,
                        passwordHash = passwordHash,
                        fullName = trimmedName,
                        email = trimmedEmail,
                        phone = trimmedPhone,
                        role = Role.CUSTOMER,
                        kycVerified = true,
                        nationalIdCipher = protectedId.cipherText,
                        nationalIdHash = protectedId.lookupHash,
                        createdAt = System.currentTimeMillis(),
                    )

                    val newUserId = userDao.insert(userEntity)

                    // 5. Enroll Biometric Face Template
                    val enrollResult = faceRecognitionService.enrollFace(newUserId, faceFrameBytes)
                    if (enrollResult.isFailure) {
                        // Rollback newly inserted user
                        val createdUser = userDao.getById(newUserId)
                        if (createdUser != null) {
                            userDao.delete(createdUser)
                        }
                        Result.failure(
                            DomainError.SystemError(
                                enrollResult.exceptionOrNull()?.message ?: "Không thể trích xuất hoặc lưu trữ mẫu sinh trắc học",
                            ),
                        )
                    } else {
                        // 6. Automatically Create Default Checking Account with Welcome Bonus (50,000 VND)
                        val initialBonus = 50_000L
                        val accountNumber = "100" + String.format("%07d", newUserId)
                        accountDao.insert(
                            AccountEntity(
                                userId = newUserId,
                                accountNumber = accountNumber,
                                accountType = AccountType.CHECKING,
                                balance = initialBonus,
                                currency = "VND",
                                status = "ACTIVE",
                            ),
                        )

                        val domainUser = userEntity.toDomainModel().copy(
                            id = newUserId,
                            nationalId = trimmedNationalId,
                        )

                        Result.success(
                            RegistrationResult(
                                user = domainUser,
                                accountNumber = accountNumber,
                                initialBalance = initialBonus,
                            ),
                        )
                    }
                }
            }
        }
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
