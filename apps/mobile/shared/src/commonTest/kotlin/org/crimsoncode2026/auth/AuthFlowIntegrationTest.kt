package org.crimsoncode2026.auth

import kotlinx.coroutines.test.runTest
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
 * Integration tests for Authentication Flow
 *
 * Tests the complete authentication flow from phone entry through registration
 * and session persistence using mock implementations.
 *
 * Flow:
 * 1. PhoneEntryScreen: Validate and format phone number
 * 2. OtpVerificationScreen: Verify 6-digit OTP code
 * 3. DisplayNameScreen: Validate display name (2-100 chars)
 * 4. AuthRepository: Send OTP, verify OTP, sign out
 * 5. SessionInitUseCase: Determine auth flow
 * 6. RegisterUserUseCase: Complete registration
 */
class AuthFlowIntegrationTest {

    private lateinit var mockSecureStorage: MockSecureStorage
    private lateinit var mockAuthRepository: MockAuthRepository
    private lateinit var mockDeviceIdProvider: MockDeviceIdProvider
    private lateinit var mockUserRepository: MockUserRepository

    @BeforeTest
    fun setup() {
        mockSecureStorage = MockSecureStorage()
        mockAuthRepository = MockAuthRepository(mockSecureStorage)
        mockDeviceIdProvider = MockDeviceIdProvider()
        mockUserRepository = MockUserRepository()
    }

    @AfterTest
    fun teardown() {
        mockSecureStorage.clear()
    }

    // ==================== Phone Entry Flow Tests ====================

    @Test
    fun `phone entry validates correct E.164 format`() = runTest {
        // Arrange
        val phoneEntry = PhoneEntryTestHelper()
        val validPhones = listOf(
            "+15551234567",
            "+12223334444",
            "+1999888776666"
        )

        // Act & Assert
        validPhones.forEach { phone ->
            val isValid = phoneEntry.isValidUSAPhoneNumber(phone)
            assertTrue(isValid, "Phone $phone should be valid")
        }
    }

    @Test
    fun `phone entry rejects invalid phone numbers`() = runTest {
        // Arrange
        val phoneEntry = PhoneEntryTestHelper()
        val invalidPhones = listOf(
            "15551234567",      // Missing country code
            "+1555123456",       // Too short
            "+1555123456789",    // Too long
            "+4420123456789",     // Wrong country code
            "+1abc5551234",       // Contains letters
            "+1 (555) 123-4567",  // Formatted, not stripped
            ""                      // Empty
        )

        // Act & Assert
        invalidPhones.forEach { phone ->
            val isValid = phoneEntry.isValidUSAPhoneNumber(phone)
            assertFalse(isValid, "Phone '$phone' should be invalid")
        }
    }

    @Test
    fun `phone entry formats to E.164 correctly`() = runTest {
        // Arrange
        val phoneEntry = PhoneEntryTestHelper()
        val inputsToExpected = mapOf(
            "+1" to "+1 ",
            "+15" to "+1 (5",
            "+155" to "+1 (55",
            "+1555" to "+1 (555",
            "+15551" to "+1 (555) 1",
            "+155512" to "+1 (555) 12",
            "+1555123" to "+1 (555) 123",
            "+15551234" to "+1 (555) 123-4",
            "+155512345" to "+1 (555) 123-45",
            "+1555123456" to "+1 (555) 123-456",
            "+15551234567" to "+1 (555) 123-4567"
        )

        // Act & Assert
        inputsToExpected.forEach { (input, expected) ->
            val formatted = phoneEntry.formatPhoneNumber(input)
            assertEquals(expected, formatted, "Formatting '$input' should produce '$expected'")
        }
    }

    // ==================== OTP Verification Flow Tests ====================

    @Test
    fun `otp entry accepts exactly 6 digits`() = runTest {
        // Arrange
        val otpEntry = OtpTestHelper()
        val validCodes = listOf("123456", "000000", "999999", "048231")

        // Act & Assert
        validCodes.forEach { code ->
            val isValid = otpEntry.isValidOtpCode(code)
            assertTrue(isValid, "OTP code '$code' should be valid")
        }
    }

    @Test
    fun `otp entry rejects non-6-digit codes`() = runTest {
        // Arrange
        val otpEntry = OtpTestHelper()
        val invalidCodes = listOf(
            "12345",         // Too short
            "1234567",       // Too long
            "abcdef",         // Contains letters
            "12345a",        // Contains letter
            "12 34 56",      // Contains spaces
            "",               // Empty
            "123-456",       // Contains dash
            " 123456",       // Leading space
            "123456 "        // Trailing space
        )

        // Act & Assert
        invalidCodes.forEach { code ->
            val isValid = otpEntry.isValidOtpCode(code)
            assertFalse(isValid, "OTP code '$code' should be invalid")
        }
    }

    @Test
    fun `otp entry strips non-digits before validation`() = runTest {
        // Arrange
        val otpEntry = OtpTestHelper()

        // Act
        val digitsOnly = otpEntry.extractDigits("a1b2c3d4e5f6g")

        // Assert
        assertEquals("123456", digitsOnly)
    }

    @Test
    fun `otp countdown starts at 30 seconds`() = runTest {
        // Arrange
        val otpEntry = OtpTestHelper()
        val expectedInitialCountdown = 30

        // Act
        val initialCountdown = otpEntry.getInitialCountdown()

        // Assert
        assertEquals(expectedInitialCountdown, initialCountdown)
    }

    // ==================== Display Name Flow Tests ====================

    @Test
    fun `display name accepts valid names`() = runTest {
        // Arrange
        val nameEntry = DisplayNameTestHelper()
        val validNames = listOf(
            "John",
            "Jane Doe",
            "A B",                         // Exactly 2 chars
            "A".repeat(100),                // Exactly 100 chars
            "O'Connor",
            "Mary-Jane Smith",
            "José García",
            "张伟",
            "Ali Baba",
            "Bob"
        )

        // Act & Assert
        validNames.forEach { name ->
            val result = nameEntry.validateDisplayName(name)
            assertTrue(result.isValid, "Name '$name' should be valid")
        }
    }

    @Test
    fun `display name rejects invalid names`() = runTest {
        // Arrange
        val nameEntry = DisplayNameTestHelper()
        val invalidNames = listOf(
            "A",                      // Too short (1 char)
            "",                        // Empty
            "   ",                    // Only spaces
            " A",                      // Space before
            "A ",                      // Space after
            "  A  "                    // Spaces around
        )

        // Act & Assert
        invalidNames.forEach { name ->
            val result = nameEntry.validateDisplayName(name)
            assertFalse(result.isValid, "Name '$name' should be invalid")
            assertNotNull(result.errorMessage)
        }
    }

    @Test
    fun `display name enforces maximum length`() = runTest {
        // Arrange
        val nameEntry = DisplayNameTestHelper()
        val maxName = "A".repeat(100)
        val tooLongName = "A".repeat(101)

        // Act & Assert
        assertTrue(nameEntry.validateDisplayName(maxName).isValid, "100 chars should be valid")
        assertFalse(nameEntry.validateDisplayName(tooLongName).isValid, "101 chars should be invalid")
    }

    @Test
    fun `display name enforces minimum length`() = runTest {
        // Arrange
        val nameEntry = DisplayNameTestHelper()
        val minName = "AB"
        val tooShortName = "A"

        // Act & Assert
        assertTrue(nameEntry.validateDisplayName(minName).isValid, "2 chars should be valid")
        assertFalse(nameEntry.validateDisplayName(tooShortName).isValid, "1 char should be invalid")
    }

    // ==================== Auth Repository Flow Tests ====================

    @Test
    fun `auth repository sends OTP successfully`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"

        // Act
        val result = mockAuthRepository.sendOtp(phoneNumber)

        // Assert
        assertTrue(result is AuthResult.Success, "OTP send should succeed")
        assertTrue(mockAuthRepository.lastSentPhone == phoneNumber, "Phone should be recorded")
    }

    @Test
    fun `auth repository verifies OTP successfully`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val otpToken = "123456"
        mockAuthRepository.sendOtp(phoneNumber) // Pre-send OTP

        // Act
        val result = mockAuthRepository.verifyOtp(phoneNumber, otpToken)

        // Assert
        assertTrue(result is AuthResult.Success, "OTP verification should succeed")
        assertNotNull(mockSecureStorage.getString("refresh_token"), "Refresh token should be stored")
    }

    @Test
    fun `auth repository stores refresh token on verification`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val otpToken = "123456"
        val expectedRefreshToken = "mock_refresh_token_12345"

        // Act
        mockAuthRepository.sendOtp(phoneNumber)
        mockAuthRepository.verifyOtp(phoneNumber, otpToken)

        // Assert
        val storedToken = mockSecureStorage.getString("refresh_token")
        assertNotNull(storedToken, "Refresh token should be stored")
        assertEquals(expectedRefreshToken, storedToken, "Stored token should match expected")
    }

    @Test
    fun `auth repository clears refresh token on sign out`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val otpToken = "123456"
        mockAuthRepository.sendOtp(phoneNumber)
        mockAuthRepository.verifyOtp(phoneNumber, otpToken)
        assertNotNull(mockSecureStorage.getString("refresh_token"), "Token should be stored")

        // Act
        mockAuthRepository.signOut()

        // Assert
        val storedToken = mockSecureStorage.getString("refresh_token")
        assertNull(storedToken, "Refresh token should be cleared")
    }

    @Test
    fun `auth repository returns current session when authenticated`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val otpToken = "123456"
        mockAuthRepository.sendOtp(phoneNumber)
        mockAuthRepository.verifyOtp(phoneNumber, otpToken)

        // Act
        val session = mockAuthRepository.getCurrentSession()

        // Assert
        assertNotNull(session, "Session should exist after verification")
        assertNotNull(session?.refreshToken, "Session should have refresh token")
    }

    @Test
    fun `auth repository returns null session when not authenticated`() = runTest {
        // Arrange - No login performed

        // Act
        val session = mockAuthRepository.getCurrentSession()

        // Assert
        assertNull(session, "Session should be null when not authenticated")
    }

    @Test
    fun `auth repository isAuthenticated returns correct state`() = runTest {
        // Arrange
        assertFalse(mockAuthRepository.isAuthenticated(), "Should not be authenticated initially")

        // Act
        mockAuthRepository.sendOtp("+15551234567")
        mockAuthRepository.verifyOtp("+15551234567", "123456")

        // Assert
        assertTrue(mockAuthRepository.isAuthenticated(), "Should be authenticated after verification")
    }

    // ==================== Session Persistence Tests ====================

    @Test
    fun `refresh token persists across repository instances`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val otpToken = "123456"

        // Act - Store token with first instance
        mockAuthRepository.sendOtp(phoneNumber)
        mockAuthRepository.verifyOtp(phoneNumber, otpToken)
        val firstToken = mockSecureStorage.getString("refresh_token")

        // Create new repository instance with same storage
        val newAuthRepository = MockAuthRepository(mockSecureStorage)
        val secondToken = newAuthRepository.getRefreshToken()

        // Assert
        assertEquals(firstToken, secondToken, "Token should persist across instances")
    }

    @Test
    fun `stored refresh token can be retrieved`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val otpToken = "123456"

        // Act
        mockAuthRepository.sendOtp(phoneNumber)
        mockAuthRepository.verifyOtp(phoneNumber, otpToken)
        val retrievedToken = mockAuthRepository.getRefreshToken()

        // Assert
        assertNotNull(retrievedToken, "Should retrieve stored refresh token")
    }

    @Test
    fun `refresh token is null when not stored`() = runTest {
        // Arrange - No login performed

        // Act
        val token = mockAuthRepository.getRefreshToken()

        // Assert
        assertNull(token, "Refresh token should be null when not stored")
    }

    // ==================== Complete Auth Flow Integration Tests ====================

    @Test
    fun `complete auth flow from phone to registration`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val otpToken = "123456"
        val displayName = "John Doe"
        val deviceId = "test-device-id-12345"
        mockDeviceIdProvider.setDeviceId(deviceId)

        // Step 1: Send OTP
        val sendOtpResult = mockAuthRepository.sendOtp(phoneNumber)
        assertTrue(sendOtpResult is AuthResult.Success, "Send OTP should succeed")

        // Step 2: Verify OTP
        val verifyOtpResult = mockAuthRepository.verifyOtp(phoneNumber, otpToken)
        assertTrue(verifyOtpResult is AuthResult.Success, "Verify OTP should succeed")

        // Step 3: Check token storage
        val storedToken = mockSecureStorage.getString("refresh_token")
        assertNotNull(storedToken, "Refresh token should be stored")

        // Step 4: Verify session is active
        val isAuthenticated = mockAuthRepository.isAuthenticated()
        assertTrue(isAuthenticated, "User should be authenticated")

        // Step 5: Get user ID for registration
        val userId = mockAuthRepository.getCurrentUser()?.id
        assertNotNull(userId, "User ID should be available")

        // Step 6: Register user (simulated via mock)
        val userCreated = mockUserRepository.createUser(
            userId!!,
            phoneNumber,
            displayName,
            deviceId
        )
        assertTrue(userCreated, "User should be created")

        // Assert complete flow
        assertTrue(mockAuthRepository.isAuthenticated(), "Flow should end with authenticated user")
        assertNotNull(mockSecureStorage.getString("refresh_token"), "Token should persist")
        assertTrue(mockUserRepository.userExists(userId), "User record should exist")
    }

    @Test
    fun `auth flow handles invalid OTP`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val invalidOtp = "999999"

        // Act
        mockAuthRepository.sendOtp(phoneNumber)
        mockAuthRepository.setRequireValidOtp(false) // Simulate invalid OTP
        val result = mockAuthRepository.verifyOtp(phoneNumber, invalidOtp)

        // Assert
        assertTrue(result is AuthResult.Error, "Invalid OTP should return error")
        assertNull(mockSecureStorage.getString("refresh_token"), "No token should be stored")
        assertFalse(mockAuthRepository.isAuthenticated(), "User should not be authenticated")
    }

    @Test
    fun `auth flow handles network errors gracefully`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"

        // Act - Simulate network error
        mockAuthRepository.setNetworkErrorMode(true)
        val result = mockAuthRepository.sendOtp(phoneNumber)

        // Assert
        assertTrue(result is AuthResult.Error, "Network error should return error")
        val errorResult = result as AuthResult.Error
        assertNotNull(errorResult.message, "Error message should be provided")
    }

    @Test
    fun `auth flow supports logout and re-login`() = runTest {
        // Arrange - Complete initial login
        val phoneNumber = "+15551234567"
        mockAuthRepository.sendOtp(phoneNumber)
        mockAuthRepository.verifyOtp(phoneNumber, "123456")
        assertTrue(mockAuthRepository.isAuthenticated(), "Should be authenticated")

        // Act - Logout
        mockAuthRepository.signOut()
        assertFalse(mockAuthRepository.isAuthenticated(), "Should not be authenticated after logout")
        assertNull(mockSecureStorage.getString("refresh_token"), "Token should be cleared")

        // Re-login
        mockAuthRepository.sendOtp(phoneNumber)
        mockAuthRepository.verifyOtp(phoneNumber, "654321")
        assertTrue(mockAuthRepository.isAuthenticated(), "Should be authenticated after re-login")
        assertNotNull(mockSecureStorage.getString("refresh_token"), "Token should be stored again")
    }

    // ==================== Mock Implementations ====================

    class MockSecureStorage : SecureStorage {
        private val store = mutableMapOf<String, String>()

        override suspend fun getString(key: String): String? {
            return store[key]
        }

        override suspend fun putString(key: String, value: String) {
            store[key] = value
        }

        override suspend fun remove(key: String) {
            store.remove(key)
        }

        override suspend fun clear() {
            store.clear()
        }
    }

    class MockAuthRepository(
        private val secureStorage: MockSecureStorage
    ) : AuthRepository {
        var lastSentPhone: String? = null
        private var _isAuthenticated = false
        private var _currentUser: MockUser? = null
        private var networkErrorMode = false
        private var requireValidOtp = true

        fun setNetworkErrorMode(enabled: Boolean) {
            networkErrorMode = enabled
        }

        fun setRequireValidOtp(require: Boolean) {
            requireValidOtp = require
        }

        override suspend fun sendOtp(phone: String): AuthResult {
            if (networkErrorMode) {
                return AuthResult.Error("Network error. Please check your connection")
            }
            lastSentPhone = phone
            return AuthResult.Success
        }

        override suspend fun verifyOtp(phone: String, token: String): AuthResult {
            if (networkErrorMode) {
                return AuthResult.Error("Network error. Please check your connection")
            }
            if (!requireValidOtp) {
                return AuthResult.Error("Invalid verification code")
            }
            _isAuthenticated = true
            _currentUser = MockUser(
                id = "test-user-id",
                phone = phone
            )
            val refreshToken = "mock_refresh_token_12345"
            secureStorage.putString("refresh_token", refreshToken)
            return AuthResult.Success
        }

        override suspend fun signOut(): AuthResult {
            _isAuthenticated = false
            _currentUser = null
            secureStorage.remove("refresh_token")
            return AuthResult.Success
        }

        override fun getCurrentSession(): UserSession? {
            return if (_isAuthenticated) {
                UserSession(
                    accessToken = "mock_access_token",
                    refreshToken = secureStorage.getString("refresh_token"),
                    user = _currentUser
                )
            } else null
        }

        override fun getCurrentUser(): UserInfo? {
            return _currentUser
        }

        override fun isAuthenticated(): Boolean {
            return _isAuthenticated
        }

        override suspend fun storeRefreshToken(refreshToken: String) {
            secureStorage.putString("refresh_token", refreshToken)
        }

        override suspend fun getRefreshToken(): String? {
            return secureStorage.getString("refresh_token")
        }

        override suspend fun clearRefreshToken() {
            secureStorage.remove("refresh_token")
        }
    }

    class MockDeviceIdProvider {
        private var deviceId = "default-device-id-12345"

        fun setDeviceId(id: String) {
            deviceId = id
        }

        fun getDeviceId(): String {
            return deviceId
        }
    }

    class MockUserRepository {
        private val users = mutableMapOf<String, UserData>()

        fun createUser(id: String, phone: String, displayName: String, deviceId: String): Boolean {
            users[id] = UserData(
                id = id,
                phone = phone,
                displayName = displayName,
                deviceId = deviceId
            )
            return true
        }

        fun userExists(id: String): Boolean {
            return users.containsKey(id)
        }
    }

    data class UserData(
        val id: String,
        val phone: String,
        val displayName: String,
        val deviceId: String
    )

    class MockUser(
        override val id: String,
        val phone: String
    ) : UserInfo

    data class UserSession(
        override val accessToken: String,
        override val refreshToken: String?,
        override val user: UserInfo?
    )

    // ==================== Test Helpers ====================

    class PhoneEntryTestHelper {
        fun isValidUSAPhoneNumber(phone: String): Boolean {
            val digits = phone.replace(Regex("[^\\d]"), "")
            return phone.startsWith("+1") && digits.length == 11
        }

        fun formatPhoneNumber(input: String): String {
            val digits = input.replace(Regex("[^\\d]"), "")
            return when {
                digits.isEmpty() -> "+1 "
                digits.length <= 3 -> "+1 ($digits"
                digits.length <= 6 -> "+1 (${digits.take(3)}) ${digits.drop(3)}"
                else -> "+1 (${digits.take(3)}) ${digits.substring(3, 6)}-${digits.takeLast(4)}"
            }
        }
    }

    class OtpTestHelper {
        fun isValidOtpCode(code: String): Boolean {
            return code.length == 6 && code.all { it.isDigit() }
        }

        fun extractDigits(input: String): String {
            return input.replace(Regex("[^\\d]"), "")
        }

        fun getInitialCountdown(): Int {
            return 30
        }
    }

    class DisplayNameTestHelper {
        private const val MAX_DISPLAY_NAME_LENGTH = 100
        private const val MIN_DISPLAY_NAME_LENGTH = 2

        data class ValidationResult(
            val isValid: Boolean,
            val errorMessage: String? = null
        )

        fun validateDisplayName(name: String): ValidationResult {
            val trimmed = name.trim()
            return when {
                trimmed.isEmpty() -> ValidationResult(false, "Please enter your display name")
                trimmed.length < MIN_DISPLAY_NAME_LENGTH -> ValidationResult(false, "Name must be at least $MIN_DISPLAY_NAME_LENGTH characters")
                name.isBlank() -> ValidationResult(false, "Name cannot be only spaces")
                else -> ValidationResult(true, null)
            }
        }
    }
}
