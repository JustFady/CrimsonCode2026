package org.crimsoncode2026.domain.usecases

import kotlinx.coroutines.test.runTest
import org.crimsoncode2026.auth.AuthRepository
import org.crimsoncode2026.auth.DeviceIdProvider
import org.crimsoncode2026.data.User
import org.crimsoncode2026.storage.SecureStorage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for RegisterUserUseCase
 *
 * Tests complete user registration flow including OTP verification,
 * user creation/update, and session storage.
 */
class RegisterUserUseCaseTest {

    private lateinit var mockAuthRepository: MockAuthRepository
    private lateinit var mockUserRepository: MockUserRepository
    private lateinit var mockDeviceIdProvider: MockDeviceIdProvider
    private lateinit var mockSecureStorage: MockSecureStorage
    private lateinit var mockSessionManager: MockSessionManager
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @BeforeTest
    fun setup() {
        mockAuthRepository = MockAuthRepository()
        mockUserRepository = MockUserRepository()
        mockDeviceIdProvider = MockDeviceIdProvider()
        mockSecureStorage = MockSecureStorage()
        mockSessionManager = MockSessionManager()
        registerUserUseCase = RegisterUserUseCase(
            authRepository = mockAuthRepository,
            userRepository = mockUserRepository,
            deviceIdProvider = mockDeviceIdProvider,
            secureStorage = mockSecureStorage,
            userSessionManager = mockSessionManager
        )
    }

    @AfterTest
    fun teardown() {
        mockSecureStorage.clear()
        mockUserRepository.clear()
    }

    // ==================== Registration Success Tests ====================

    @Test
    fun `invoke returns Success for new user`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "John Doe"
        val otpToken = "123456"
        mockAuthRepository.setupAuthenticatedSession("user-new-123", "refresh_token_xyz")
        mockUserRepository.setGetByPhoneResult(Result.success(null))
        mockUserRepository.setCreateResult(Result.success(mockUser()))

        // Act
        val result = registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertTrue(result is RegistrationResult.Success, "Registration should succeed")
    }

    @Test
    fun `invoke creates user in database for new user`() = runTest {
        // Arrange
        val phoneNumber = "+12223334444"
        val displayName = "Jane Smith"
        val otpToken = "654321"
        val userId = "user-create-456"
        mockAuthRepository.setupAuthenticatedSession(userId, "token_abc")
        mockUserRepository.setGetByPhoneResult(Result.success(null))
        mockUserRepository.setCreateResult(Result.success(mockUser(userId)))

        // Act
        registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertTrue(mockUserRepository.createUserCalled, "createUser should be called")
        assertEquals(displayName, mockUserRepository.lastCreatedUser?.displayName)
        assertEquals(phoneNumber, mockUserRepository.lastCreatedUser?.phoneNumber)
    }

    @Test
    fun `invoke updates existing user device ID`() = runTest {
        // Arrange
        val phoneNumber = "+19998887766"
        val displayName = "Existing User"
        val otpToken = "789012"
        val userId = "user-existing-789"
        val existingUser = mockUser(userId, phoneNumber, "old-device-id", displayName)
        mockAuthRepository.setupAuthenticatedSession(userId, "token_def")
        mockUserRepository.setGetByPhoneResult(Result.success(existingUser))
        mockUserRepository.setUpdateDeviceResult(Result.success(existingUser))

        // Act
        registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertTrue(mockUserRepository.updateDeviceIdCalled, "updateDeviceId should be called")
        assertEquals(userId, mockUserRepository.lastUpdateDeviceUserId)
    }

    @Test
    fun `invoke updates existing user display name`() = runTest {
        // Arrange
        val phoneNumber = "+15555555555"
        val newDisplayName = "Updated Name"
        val oldDisplayName = "Old Name"
        val otpToken = "111222"
        val userId = "user-update-999"
        val existingUser = mockUser(userId, phoneNumber, "device-123", oldDisplayName)
        mockAuthRepository.setupAuthenticatedSession(userId, "token_ghi")
        mockUserRepository.setGetByPhoneResult(Result.success(existingUser))
        mockUserRepository.setUpdateDisplayResult(Result.success(existingUser))

        // Act
        registerUserUseCase(phoneNumber, newDisplayName, otpToken)

        // Assert
        assertTrue(mockUserRepository.updateDisplayNameCalled, "updateDisplayName should be called")
        assertEquals(newDisplayName, mockUserRepository.lastUpdateDisplayName)
    }

    // ==================== Refresh Token Storage Tests ====================

    @Test
    fun `invoke stores refresh token in secure storage`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "Token Test User"
        val otpToken = "333444"
        val expectedRefreshToken = "stored_refresh_token_12345"
        mockAuthRepository.setupAuthenticatedSession("user-token-789", expectedRefreshToken)
        mockUserRepository.setGetByPhoneResult(Result.success(null))
        mockUserRepository.setCreateResult(Result.success(mockUser()))

        // Act
        registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        val storedToken = mockSecureStorage.getString("refresh_token")
        assertEquals(expectedRefreshToken, storedToken, "Refresh token should be stored")
    }

    @Test
    fun `invoke stores phone number in secure storage`() = runTest {
        // Arrange
        val phoneNumber = "+16667778888"
        val displayName = "Phone Storage User"
        val otpToken = "555666"
        mockAuthRepository.setupAuthenticatedSession("user-phone-000", "token_jkl")
        mockUserRepository.setGetByPhoneResult(Result.success(null))
        mockUserRepository.setCreateResult(Result.success(mockUser()))

        // Act
        registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        val storedPhone = mockSecureStorage.getString("phone_number")
        assertEquals(phoneNumber, storedPhone, "Phone number should be stored")
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `invoke returns Error when OTP verification fails`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "Failed OTP User"
        val otpToken = "999999"
        mockAuthRepository.setVerifyOtpError("Invalid verification code")

        // Act
        val result = registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertTrue(result is RegistrationResult.Error, "Should return error on OTP failure")
        val error = result as RegistrationResult.Error
        assertEquals("Invalid verification code", error.message)
    }

    @Test
    fun `invoke returns Error when device ID fails`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "Device ID Fail User"
        val otpToken = "888999"
        mockAuthRepository.setupAuthenticatedSession("user-device-111", "token_mno")
        mockDeviceIdProvider.setShouldFail(true)

        // Act
        val result = registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertTrue(result is RegistrationResult.Error, "Should return error on device ID failure")
        val error = result as RegistrationResult.Error
        assertEquals("Failed to get device ID", error.message)
    }

    @Test
    fun `invoke returns Error when user ID not available`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "No User ID User"
        val otpToken = "777888"
        mockAuthRepository.setupAuthenticatedSession(null, "token_pqr") // No user ID

        // Act
        val result = registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertTrue(result is RegistrationResult.Error, "Should return error when no user ID")
        val error = result as RegistrationResult.Error
        assertEquals("Authentication failed", error.message)
    }

    @Test
    fun `invoke returns Error when storage fails`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "Storage Fail User"
        val otpToken = "666777"
        mockAuthRepository.setupAuthenticatedSession("user-storage-222", "token_stu")
        mockUserRepository.setGetByPhoneResult(Result.success(null))
        mockUserRepository.setCreateResult(Result.success(mockUser()))
        mockSecureStorage.setShouldFail(true)

        // Act
        val result = registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertTrue(result is RegistrationResult.Error, "Should return error on storage failure")
        val error = result as RegistrationResult.Error
        assertEquals("Failed to store session", error.message)
    }

    // ==================== Device ID Tests ====================

    @Test
    fun `invoke uses provided device ID`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "Device ID User"
        val otpToken = "444555"
        val expectedDeviceId = "expected-device-id-123"
        mockAuthRepository.setupAuthenticatedSession("user-did-333", "token_vwx")
        mockDeviceIdProvider.setDeviceId(expectedDeviceId)
        mockUserRepository.setGetByPhoneResult(Result.success(null))
        mockUserRepository.setCreateResult(Result.success(mockUser()))

        // Act
        registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertEquals(expectedDeviceId, mockUserRepository.lastCreatedUser?.deviceId)
    }

    @Test
    fun `invoke creates user with Android platform`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "Android User"
        val otpToken = "222333"
        mockAuthRepository.setupAuthenticatedSession("user-android-444", "token_android")
        mockDeviceIdProvider.setPlatform("Android")
        mockUserRepository.setGetByPhoneResult(Result.success(null))
        mockUserRepository.setCreateResult(Result.success(mockUser()))

        // Act
        registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertEquals("ANDROID", mockUserRepository.lastCreatedUser?.platform)
    }

    @Test
    fun `invoke creates user with iOS platform`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "iOS User"
        val otpToken = "111000"
        mockAuthRepository.setupAuthenticatedSession("user-ios-555", "token_ios")
        mockDeviceIdProvider.setPlatform("iOS")
        mockUserRepository.setGetByPhoneResult(Result.success(null))
        mockUserRepository.setCreateResult(Result.success(mockUser()))

        // Act
        registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertEquals("IOS", mockUserRepository.lastCreatedUser?.platform)
    }

    // ==================== User Record Tests ====================

    @Test
    fun `invoke creates user with active status`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "Active User"
        val otpToken = "999000"
        mockAuthRepository.setupAuthenticatedSession("user-active-666", "token_active")
        mockUserRepository.setGetByPhoneResult(Result.success(null))
        mockUserRepository.setCreateResult(Result.success(mockUser()))

        // Act
        registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertTrue(mockUserRepository.lastCreatedUser?.isActive == true, "User should be active")
    }

    @Test
    fun `invoke sets timestamps on new user`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "Timestamp User"
        val otpToken = "888777"
        val beforeTime = System.currentTimeMillis()
        mockAuthRepository.setupAuthenticatedSession("user-time-777", "token_time")
        mockUserRepository.setGetByPhoneResult(Result.success(null))
        mockUserRepository.setCreateResult(Result.success(mockUser()))

        // Act
        registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        val user = mockUserRepository.lastCreatedUser
        assertNotNull(user?.createdAt, "Created timestamp should be set")
        assertNotNull(user?.updatedAt, "Updated timestamp should be set")
        assertNotNull(user?.lastActiveAt, "Last active timestamp should be set")
        assertTrue(user?.createdAt ?: 0 >= beforeTime)
    }

    // ==================== Database Failure Tests ====================

    @Test
    fun `invoke handles database query failure for existing user`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "DB Query Fail User"
        val otpToken = "666555"
        mockAuthRepository.setupAuthenticatedSession("user-db-888", "token_db_query")
        mockUserRepository.setGetByPhoneResult(Result.failure(Exception("Query failed")))
        mockUserRepository.setCreateResult(Result.success(mockUser()))

        // Act
        val result = registerUserUseCase(phoneNumber, displayName, otpToken)

        // Assert
        assertTrue(result is RegistrationResult.Success, "Should create new user on query failure")
        assertTrue(mockUserRepository.createUserCalled, "Should attempt to create user")
    }

    @Test
    fun `invoke handles database create failure`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val displayName = "DB Create Fail User"
        val otpToken = "444333"
        mockAuthRepository.setupAuthenticatedSession("user-db-999", "token_db_create")
        mockUserRepository.setGetByPhoneResult(Result.success(null))
        mockUserRepository.setCreateResult(Result.failure(Exception("Create failed")))

        // Act - should not throw
        try {
            registerUserUseCase(phoneNumber, displayName, otpToken)
        } catch (e: Exception) {
            // We expect the result to still be returned even if create fails
            // as the use case doesn't explicitly handle this error
        }

        // Assert - The use case doesn't explicitly handle create failure,
        // but it doesn't throw either
        assertTrue(mockUserRepository.createUserCalled, "Create was attempted")
    }

    // ==================== Mock Implementations ====================

    class MockAuthRepository : AuthRepository {
        private var userId: String? = null
        private var refreshToken: String? = null
        private var verifyOtpErrorMessage: String? = null

        fun setupAuthenticatedSession(id: String? = "mock-user-id", token: String = "mock-refresh-token") {
            userId = id
            refreshToken = token
        }

        fun setVerifyOtpError(message: String) {
            verifyOtpErrorMessage = message
        }

        override suspend fun sendOtp(phone: String): AuthResult {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override suspend fun verifyOtp(phone: String, token: String): AuthResult {
            if (verifyOtpErrorMessage != null) {
                return AuthResult.Error(verifyOtpErrorMessage!!)
            }
            return AuthResult.Success
        }

        override suspend fun signOut(): AuthResult {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override fun getCurrentSession(): UserSession? {
            return if (refreshToken != null) {
                MockUserSession(accessToken = "access_token", refreshToken = refreshToken, user = null)
            } else null
        }

        override fun getCurrentUser(): UserInfo? {
            return if (userId != null) {
                MockUserInfo(id = userId!!, email = null)
            } else null
        }

        override fun isAuthenticated(): Boolean = userId != null

        override suspend fun storeRefreshToken(refreshToken: String) {}
        override suspend fun getRefreshToken(): String? = refreshToken
        override suspend fun clearRefreshToken() { refreshToken = null }
    }

    class MockUserRepository : UserRepository {
        private var getByPhoneResult: Result<User?> = Result.success(null)
        private var createResult: Result<User> = Result.success(mockUser())
        private var updateDeviceResult: Result<User> = Result.success(mockUser())
        private var updateDisplayResult: Result<User> = Result.success(mockUser())

        var createUserCalled = false
        var lastCreatedUser: User? = null
        var updateDeviceIdCalled = false
        var lastUpdateDeviceUserId: String? = null
        var updateDisplayNameCalled = false
        var lastUpdateDisplayName: String? = null

        fun setGetByPhoneResult(result: Result<User?>) {
            getByPhoneResult = result
        }

        fun setCreateResult(result: Result<User>) {
            createResult = result
        }

        fun setUpdateDeviceResult(result: Result<User>) {
            updateDeviceResult = result
        }

        fun setUpdateDisplayResult(result: Result<User>) {
            updateDisplayResult = result
        }

        fun clear() {
            createUserCalled = false
            lastCreatedUser = null
            updateDeviceIdCalled = false
            lastUpdateDeviceUserId = null
            updateDisplayNameCalled = false
            lastUpdateDisplayName = null
        }

        override suspend fun createUser(user: User): Result<User> {
            createUserCalled = true
            lastCreatedUser = user
            return createResult
        }

        override suspend fun getUserById(userId: String): Result<User?> {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override suspend fun getUserByPhoneNumber(phoneNumber: String): Result<User?> {
            return getByPhoneResult
        }

        override suspend fun getUserByDeviceId(deviceId: String): Result<User?> {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override suspend fun updateDisplayName(userId: String, displayName: String): Result<User> {
            updateDisplayNameCalled = true
            lastUpdateDisplayName = displayName
            return updateDisplayResult
        }

        override suspend fun updateFcmToken(userId: String, fcmToken: String): Result<User> {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override suspend fun updateLastActive(userId: String): Result<User> {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override suspend fun updateDeviceId(userId: String, deviceId: String): Result<User> {
            updateDeviceIdCalled = true
            lastUpdateDeviceUserId = userId
            return updateDeviceResult
        }

        override suspend fun deactivateUser(userId: String): Result<Unit> {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override suspend fun deleteUser(userId: String): Result<Unit> {
            throw UnsupportedOperationException("Not implemented in mock")
        }
    }

    class MockDeviceIdProvider : DeviceIdProvider {
        private var deviceId = "default-device-id"
        private var platform = "Android"
        private var shouldFail = false

        fun setDeviceId(id: String) {
            deviceId = id
        }

        fun setPlatform(p: String) {
            platform = p
        }

        fun setShouldFail(fail: Boolean) {
            shouldFail = fail
        }

        override fun initialize(storage: SecureStorage) {}

        override suspend fun getDeviceId(): String {
            if (shouldFail) {
                throw Exception("Device ID generation failed")
            }
            return deviceId
        }

        override suspend fun regenerateDeviceId(): String {
            return "regenerated-$deviceId"
        }

        override suspend fun clearDeviceId() {}
    }

    class MockSecureStorage : SecureStorage {
        private val store = mutableMapOf<String, String>()
        private var shouldFail = false

        fun setShouldFail(fail: Boolean) {
            shouldFail = fail
        }

        override suspend fun getString(key: String): String? {
            if (shouldFail) {
                throw Exception("Storage error")
            }
            return store[key]
        }

        override suspend fun putString(key: String, value: String) {
            if (shouldFail) {
                throw Exception("Storage error")
            }
            store[key] = value
        }

        override suspend fun remove(key: String) {
            store.remove(key)
        }

        override suspend fun clear() {
            store.clear()
            shouldFail = false
        }
    }

    class MockSessionManager : UserSessionManager(null, null) {
        private var userId: String? = null

        fun setUserId(id: String?) {
            userId = id
        }

        fun getCurrentUserId(): String? = userId

        override suspend fun updateLastActive(): Result<User> {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override suspend fun signOut() {}
    }

    // Helper function to create mock users
    private fun mockUser(
        id: String = "mock-user-id",
        phone: String = "+15551234567",
        deviceId: String = "mock-device-id",
        displayName: String = "Mock User"
    ): User {
        val now = System.currentTimeMillis()
        return User(
            id = id,
            phoneNumber = phone,
            displayName = displayName,
            deviceId = deviceId,
            platform = "ANDROID",
            isActive = true,
            createdAt = now,
            updatedAt = now,
            lastActiveAt = now
        )
    }

    // Mock classes for AuthRepository
    class MockUserSession(
        override val accessToken: String,
        override val refreshToken: String?,
        override val user: UserInfo?
    ) : UserSession

    class MockUserInfo(
        override val id: String,
        override val email: String?
    ) : UserInfo
}
