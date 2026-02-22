package org.crimsoncode2026.auth

import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for AuthRepository interface
 *
 * Tests the Supabase-based authentication operations using mock implementations.
 */
class AuthRepositoryTest {

    private lateinit var mockSecureStorage: MockSecureStorage
    private lateinit var mockAuth: MockSupabaseAuth
    private lateinit var authRepository: AuthRepositoryImpl

    @BeforeTest
    fun setup() {
        mockSecureStorage = MockSecureStorage()
        mockAuth = MockSupabaseAuth()
        authRepository = AuthRepositoryImpl(mockAuth, mockSecureStorage)
    }

    @AfterTest
    fun teardown() {
        mockSecureStorage.clear()
    }

    // ==================== sendOtp Tests ====================

    @Test
    fun `sendOtp returns Success on valid phone number`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"

        // Act
        val result = authRepository.sendOtp(phoneNumber)

        // Assert
        assertTrue(result is AuthResult.Success, "Send OTP should succeed")
        assertTrue(mockAuth.lastOtpPhone == phoneNumber, "Phone should be recorded")
    }

    @Test
    fun `sendOtp calls signInWith with Otp`() = runTest {
        // Arrange
        val phoneNumber = "+12223334444"

        // Act
        authRepository.sendOtp(phoneNumber)

        // Assert
        assertEquals(phoneNumber, mockAuth.lastOtpPhone, "Sign in should be called with phone")
        assertEquals(OtpType.Phone, mockAuth.lastOtpType, "OTP type should be Phone")
    }

    @Test
    fun `sendOtp returns Error on network failure`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        mockAuth.setSignInFailure(true)

        // Act
        val result = authRepository.sendOtp(phoneNumber)

        // Assert
        assertTrue(result is AuthResult.Error, "Send OTP should fail")
        val error = result as AuthResult.Error
        assertNotNull(error.message, "Error message should be provided")
    }

    @Test
    fun `sendOtp maps network error to user message`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        mockAuth.setSimulatedException(Exception("Network error"))

        // Act
        val result = authRepository.sendOtp(phoneNumber)

        // Assert
        assertTrue(result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertTrue(
            error.message?.contains("Network") == true,
            "Network errors should be mapped to user-friendly message"
        )
    }

    // ==================== verifyOtp Tests ====================

    @Test
    fun `verifyOtp returns Success with valid code`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val otpToken = "123456"
        mockAuth.setupAuthenticatedSession()

        // Act
        val result = authRepository.verifyOtp(phoneNumber, otpToken)

        // Assert
        assertTrue(result is AuthResult.Success, "OTP verification should succeed")
        assertNotNull(mockSecureStorage.getString("refresh_token"), "Refresh token should be stored")
    }

    @Test
    fun `verifyOtp stores refresh token from session`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val otpToken = "123456"
        val expectedRefreshToken = "test_refresh_token_xyz"
        mockAuth.setupAuthenticatedSession(expectedRefreshToken)

        // Act
        authRepository.verifyOtp(phoneNumber, otpToken)

        // Assert
        val storedToken = mockSecureStorage.getString("refresh_token")
        assertEquals(expectedRefreshToken, storedToken, "Refresh token should match session token")
    }

    @Test
    fun `verifyOtp returns Error for invalid code`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val otpToken = "999999"
        mockAuth.setVerifyFailure(true)

        // Act
        val result = authRepository.verifyOtp(phoneNumber, otpToken)

        // Assert
        assertTrue(result is AuthResult.Error, "Invalid OTP should return error")
        val error = result as AuthResult.Error
        assertNotNull(error.message, "Error message should be provided")
    }

    @Test
    fun `verifyOtp maps invalid credentials error`() = runTest {
        // Arrange
        val phoneNumber = "+15551234567"
        val otpToken = "000000"
        mockAuth.setSimulatedException(Exception("Invalid login credentials"))

        // Act
        val result = authRepository.verifyOtp(phoneNumber, otpToken)

        // Assert
        assertTrue(result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals("Invalid verification code", error.message, "Should map to user-friendly message")
    }

    @Test
    fun `verifyOtp maps expired OTP error`() = runTest {
        // Arrange
        mockAuth.setSimulatedException(Exception("OTP has expired"))

        // Act
        val result = authRepository.verifyOtp("+15551234567", "123456")

        // Assert
        assertTrue(result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertTrue(
            error.message?.contains("expired") == true,
            "Should indicate OTP expired"
        )
    }

    @Test
    fun `verifyOtp does not store token on failure`() = runTest {
        // Arrange
        mockAuth.setVerifyFailure(true)

        // Act
        authRepository.verifyOtp("+15551234567", "123456")

        // Assert
        val storedToken = mockSecureStorage.getString("refresh_token")
        assertNull(storedToken, "Token should not be stored on failure")
    }

    // ==================== signOut Tests ====================

    @Test
    fun `signOut returns Success on logout`() = runTest {
        // Arrange
        mockAuth.setupAuthenticatedSession()

        // Act
        val result = authRepository.signOut()

        // Assert
        assertTrue(result is AuthResult.Success, "Sign out should succeed")
    }

    @Test
    fun `signOut clears stored refresh token`() = runTest {
        // Arrange
        mockAuth.setupAuthenticatedSession("test_token")
        authRepository.verifyOtp("+15551234567", "123456")
        assertNotNull(mockSecureStorage.getString("refresh_token"), "Token should be stored")

        // Act
        authRepository.signOut()

        // Assert
        val storedToken = mockSecureStorage.getString("refresh_token")
        assertNull(storedToken, "Refresh token should be cleared on logout")
    }

    @Test
    fun `signOut calls auth signOut`() = runTest {
        // Arrange

        // Act
        authRepository.signOut()

        // Assert
        assertTrue(mockAuth.signOutCalled, "Auth signOut should be called")
    }

    // ==================== getCurrentSession Tests ====================

    @Test
    fun `getCurrentSession returns session when authenticated`() = runTest {
        // Arrange
        val expectedSession = MockUserSession(
            accessToken = "access_token_123",
            refreshToken = "refresh_token_456",
            user = MockUserInfo(id = "user-123", email = null)
        )
        mockAuth.setCurrentSession(expectedSession)

        // Act
        val session = authRepository.getCurrentSession()

        // Assert
        assertNotNull(session, "Session should exist when authenticated")
        assertEquals(expectedSession.accessToken, session?.accessToken)
        assertEquals(expectedSession.refreshToken, session?.refreshToken)
    }

    @Test
    fun `getCurrentSession returns null when not authenticated`() = runTest {
        // Arrange - no session set

        // Act
        val session = authRepository.getCurrentSession()

        // Assert
        assertNull(session, "Session should be null when not authenticated")
    }

    // ==================== getCurrentUser Tests ====================

    @Test
    fun `getCurrentUser returns user when authenticated`() = runTest {
        // Arrange
        val expectedUser = MockUserInfo(id = "user-456", email = null)
        mockAuth.setCurrentUser(expectedUser)

        // Act
        val user = authRepository.getCurrentUser()

        // Assert
        assertNotNull(user, "User should exist when authenticated")
        assertEquals(expectedUser.id, user?.id)
    }

    @Test
    fun `getCurrentUser returns null when not authenticated`() = runTest {
        // Arrange - no user set

        // Act
        val user = authRepository.getCurrentUser()

        // Assert
        assertNull(user, "User should be null when not authenticated")
    }

    // ==================== isAuthenticated Tests ====================

    @Test
    fun `isAuthenticated returns true when user exists`() = runTest {
        // Arrange
        mockAuth.setCurrentUser(MockUserInfo(id = "user-789", email = null))

        // Act
        val authenticated = authRepository.isAuthenticated()

        // Assert
        assertTrue(authenticated, "Should be authenticated when user exists")
    }

    @Test
    fun `isAuthenticated returns false when no user`() = runTest {
        // Arrange - no user set

        // Act
        val authenticated = authRepository.isAuthenticated()

        // Assert
        assertFalse(authenticated, "Should not be authenticated when no user")
    }

    // ==================== Refresh Token Storage Tests ====================

    @Test
    fun `storeRefreshToken saves to secure storage`() = runTest {
        // Arrange
        val token = "stored_refresh_token_abc123"

        // Act
        authRepository.storeRefreshToken(token)

        // Assert
        val retrieved = mockSecureStorage.getString("refresh_token")
        assertEquals(token, retrieved, "Token should be stored correctly")
    }

    @Test
    fun `getRefreshToken retrieves stored token`() = runTest {
        // Arrange
        val token = "retrieved_refresh_token_xyz789"
        mockSecureStorage.putString("refresh_token", token)

        // Act
        val retrieved = authRepository.getRefreshToken()

        // Assert
        assertEquals(token, retrieved, "Should retrieve stored token")
    }

    @Test
    fun `getRefreshToken returns null when not stored`() = runTest {
        // Arrange - no token stored

        // Act
        val token = authRepository.getRefreshToken()

        // Assert
        assertNull(token, "Should return null when token not stored")
    }

    @Test
    fun `clearRefreshToken removes from storage`() = runTest {
        // Arrange
        mockSecureStorage.putString("refresh_token", "token_to_clear")

        // Act
        authRepository.clearRefreshToken()

        // Assert
        val retrieved = mockSecureStorage.getString("refresh_token")
        assertNull(retrieved, "Token should be cleared")
    }

    // ==================== Error Message Mapping Tests ====================

    @Test
    fun `error message maps Too many requests`() = runTest {
        // Arrange
        mockAuth.setSimulatedException(Exception("Too many requests"))

        // Act
        val result = authRepository.sendOtp("+15551234567")

        // Assert
        assertTrue(result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertTrue(
            error.message?.contains("Too many attempts") == true,
            "Should map rate limit error"
        )
    }

    @Test
    fun `error message defaults to generic message`() = runTest {
        // Arrange
        mockAuth.setSimulatedException(Exception("Unknown error"))

        // Act
        val result = authRepository.sendOtp("+15551234567")

        // Assert
        assertTrue(result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals("Authentication failed. Please try again", error.message, "Should use default message")
    }

    @Test
    fun `error message maps Phone verification failed`() = runTest {
        // Arrange
        mockAuth.setSimulatedException(Exception("Phone verification failed"))

        // Act
        val result = authRepository.verifyOtp("+15551234567", "123456")

        // Assert
        assertTrue(result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals("Invalid verification code", error.message, "Should map phone verification failure")
    }

    @Test
    fun `error message maps User already registered`() = runTest {
        // Arrange
        mockAuth.setSimulatedException(Exception("User already registered"))

        // Act
        val result = authRepository.sendOtp("+15551234567")

        // Assert
        assertTrue(result is AuthResult.Error)
        val error = result as AuthResult.Error
        assertEquals("Phone number already registered", error.message, "Should map existing user error")
    }

    // ==================== Mock Implementations ====================

    class MockSecureStorage : SecureStorage {
        private val store = mutableMapOf<String, String>()

        override suspend fun getString(key: String): String? = store[key]
        override suspend fun putString(key: String, value: String) { store[key] = value }
        override suspend fun remove(key: String) { store.remove(key) }
        override suspend fun clear() { store.clear() }
    }

    class MockSupabaseAuth {
        var lastOtpPhone: String? = null
        var lastOtpType: OtpType? = null
        var signInFailure = false
        var verifyFailure = false
        var signOutCalled = false
        var simulatedException: Exception? = null
        var currentSession: MockUserSession? = null
        var currentUser: MockUserInfo? = null

        fun setSignInFailure(failure: Boolean) {
            signInFailure = failure
        }

        fun setVerifyFailure(failure: Boolean) {
            verifyFailure = failure
        }

        fun setSimulatedException(e: Exception?) {
            simulatedException = e
        }

        fun setCurrentSession(session: MockUserSession) {
            currentSession = session
        }

        fun setCurrentUser(user: MockUserInfo) {
            currentUser = user
        }

        fun setupAuthenticatedSession(refreshToken: String = "mock_refresh_token") {
            currentSession = MockUserSession(
                accessToken = "mock_access_token",
                refreshToken = refreshToken,
                user = MockUserInfo(id = "mock_user_id", email = null)
            )
            currentUser = MockUserInfo(id = "mock_user_id", email = null)
        }

        suspend fun signInWith(otpType: OtpType, block: OtpConfig.() -> Unit = {}) {
            if (signInFailure || simulatedException != null) {
                throw simulatedException ?: Exception("Sign in failed")
            }
            val config = OtpConfig().apply(block)
            lastOtpType = otpType
            lastOtpPhone = config.phone
        }

        suspend fun verifyOTP(
            type: OtpType,
            phone: String,
            token: String
        ): MockUserSession {
            if (verifyFailure || simulatedException != null) {
                throw simulatedException ?: Exception("Verification failed")
            }
            setupAuthenticatedSession()
            return currentSession!!
        }

        fun currentSessionOrNull(): MockUserSession? = currentSession
        fun currentUserOrNull(): MockUserInfo? = currentUser

        suspend fun signOut() {
            signOutCalled = true
            currentSession = null
            currentUser = null
        }
    }

    class OtpConfig {
        var phone: String = ""
    }

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
