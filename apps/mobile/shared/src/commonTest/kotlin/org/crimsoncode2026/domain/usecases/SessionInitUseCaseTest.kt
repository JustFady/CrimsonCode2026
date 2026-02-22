package org.crimsoncode2026.domain.usecases

import kotlinx.coroutines.test.runTest
import org.crimsoncode2026.auth.AuthRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for SessionInitUseCase
 *
 * Tests session initialization flow and authentication state determination.
 */
class SessionInitUseCaseTest {

    private lateinit var mockAuthRepository: MockAuthRepository
    private lateinit var mockSessionManager: MockUserSessionManager
    private lateinit var sessionInitUseCase: SessionInitUseCase

    @BeforeTest
    fun setup() {
        mockAuthRepository = MockAuthRepository()
        mockSessionManager = MockUserSessionManager()
        sessionInitUseCase = SessionInitUseCase(mockAuthRepository, mockSessionManager)
    }

    @AfterTest
    fun teardown() {
        mockAuthRepository.clear()
    }

    // ==================== NeedsOtpLogin Tests ====================

    @Test
    fun `invoke returns NeedsOtpLogin when no refresh token`() = runTest {
        // Arrange - no refresh token stored

        // Act
        val result = sessionInitUseCase()

        // Assert
        assertTrue(
            result is SessionInitResult.NeedsOtpLogin,
            "Should require OTP login when no refresh token"
        )
    }

    @Test
    fun `invoke returns NeedsOtpLogin when refresh token is empty string`() = runTest {
        // Arrange
        mockAuthRepository.setRefreshToken("")

        // Act
        val result = sessionInitUseCase()

        // Assert
        assertTrue(
            result is SessionInitResult.NeedsOtpLogin,
            "Empty refresh token should require OTP login"
        )
    }

    @Test
    fun `invoke returns NeedsOtpLogin when refresh token is blank`() = runTest {
        // Arrange
        mockAuthRepository.setRefreshToken("   ")

        // Act
        val result = sessionInitUseCase()

        // Assert
        assertTrue(
            result is SessionInitResult.NeedsOtpLogin,
            "Blank refresh token should require OTP login"
        )
    }

    @Test
    fun `invoke returns NeedsOtpLogin when refresh token exists but no session`() = runTest {
        // Arrange
        mockAuthRepository.setRefreshToken("valid_refresh_token")
        mockSessionManager.setCurrentUserId(null)

        // Act
        val result = sessionInitUseCase()

        // Assert
        assertTrue(
            result is SessionInitResult.NeedsOtpLogin,
            "Should require OTP login when no valid session"
        )
    }

    @Test
    fun `invoke clears invalid refresh token`() = runTest {
        // Arrange
        mockAuthRepository.setRefreshToken("invalid_token")
        mockSessionManager.setCurrentUserId(null)

        // Act
        sessionInitUseCase()

        // Assert
        val storedToken = mockAuthRepository.getRefreshToken()
        assertNull(
            storedToken,
            "Invalid refresh token should be cleared"
        )
    }

    @Test
    fun `invoke handles storage exception gracefully`() = runTest {
        // Arrange
        mockAuthRepository.setGetTokenException(true)

        // Act
        val result = sessionInitUseCase()

        // Assert
        assertTrue(
            result is SessionInitResult.Error,
            "Should return error on storage exception"
        )
        val error = result as SessionInitResult.Error
        assertEquals("Failed to check session", error.message)
    }

    @Test
    fun `invoke handles clear token exception gracefully`() = runTest {
        // Arrange
        mockAuthRepository.setRefreshToken("token_to_clear")
        mockSessionManager.setCurrentUserId(null)
        mockAuthRepository.setClearTokenException(true)

        // Act
        val result = sessionInitUseCase()

        // Assert
        assertTrue(
            result is SessionInitResult.NeedsOtpLogin,
            "Should continue to OTP login even if clear fails"
        )
    }

    // ==================== NeedsBiometricUnlock Tests ====================

    @Test
    fun `invoke returns NeedsBiometricUnlock with valid session`() = runTest {
        // Arrange
        val userId = "user-12345"
        mockAuthRepository.setRefreshToken("valid_refresh_token")
        mockSessionManager.setCurrentUserId(userId)

        // Act
        val result = sessionInitUseCase()

        // Assert
        assertTrue(
            result is SessionInitResult.NeedsBiometricUnlock,
            "Should require biometric unlock with valid session"
        )
        val unlockResult = result as SessionInitResult.NeedsBiometricUnlock
        assertEquals(userId, unlockResult.userId, "User ID should match")
    }

    @Test
    fun `invoke preserves refresh token on valid session`() = runTest {
        // Arrange
        val refreshToken = "preserve_this_token_123"
        mockAuthRepository.setRefreshToken(refreshToken)
        mockSessionManager.setCurrentUserId("user-67890")

        // Act
        sessionInitUseCase()

        // Assert
        val storedToken = mockAuthRepository.getRefreshToken()
        assertEquals(refreshToken, storedToken, "Refresh token should be preserved")
    }

    // ==================== clearSession Tests ====================

    @Test
    fun `clearSession clears refresh token`() = runTest {
        // Arrange
        mockAuthRepository.setRefreshToken("token_to_clear")

        // Act
        sessionInitUseCase.clearSession()

        // Assert
        val storedToken = mockAuthRepository.getRefreshToken()
        assertNull(storedToken, "Refresh token should be cleared")
    }

    @Test
    fun `clearSession signs out session manager`() = runTest {
        // Arrange

        // Act
        sessionInitUseCase.clearSession()

        // Assert
        assertTrue(
            mockSessionManager.signOutCalled,
            "Session manager signOut should be called"
        )
    }

    @Test
    fun `clearSession handles auth repo exceptions gracefully`() = runTest {
        // Arrange
        mockAuthRepository.setClearTokenException(true)

        // Act
        sessionInitUseCase.clearSession()

        // Assert
        assertTrue(mockSessionManager.signOutCalled, "Should still call signOut")
    }

    @Test
    fun `clearSession handles session manager exceptions gracefully`() = runTest {
        // Arrange
        mockSessionManager.setSignOutException(true)

        // Act
        sessionInitUseCase.clearSession() // Should not throw

        // Assert - no exception thrown
        assertTrue(true)
    }

    // ==================== Session Lifecycle Tests ====================

    @Test
    fun `complete session lifecycle works`() = runTest {
        // Step 1: Initial state - no session
        val initialResult = sessionInitUseCase()
        assertTrue(initialResult is SessionInitResult.NeedsOtpLogin, "Initially needs OTP")

        // Step 2: Login (simulated by setting token and user)
        val userId = "user-lifecycle-test"
        mockAuthRepository.setRefreshToken("lifecycle_token")
        mockSessionManager.setCurrentUserId(userId)

        // Step 3: Check after login
        val loggedInResult = sessionInitUseCase()
        assertTrue(loggedInResult is SessionInitResult.NeedsBiometricUnlock, "Should be ready")
        assertEquals(userId, (loggedInResult as SessionInitResult.NeedsBiometricUnlock).userId)

        // Step 4: Logout
        sessionInitUseCase.clearSession()
        assertNull(mockAuthRepository.getRefreshToken(), "Token cleared")

        // Step 5: Back to initial state
        val loggedOutResult = sessionInitUseCase()
        assertTrue(loggedOutResult is SessionInitResult.NeedsOtpLogin, "Back to needing OTP")
    }

    @Test
    fun `session persists across use case instances`() = runTest {
        // Arrange
        val userId = "user-persist-test"
        mockAuthRepository.setRefreshToken("persist_token")
        mockSessionManager.setCurrentUserId(userId)

        // Act - check with same use case
        val firstResult = sessionInitUseCase()
        assertTrue(firstResult is SessionInitResult.NeedsBiometricUnlock)

        // Assert - session still valid
        val secondResult = sessionInitUseCase()
        assertTrue(secondResult is SessionInitResult.NeedsBiometricUnlock)
        assertEquals(userId, (secondResult as SessionInitResult.NeedsBiometricUnlock).userId)
    }

    // ==================== Mock Implementations ====================

    class MockAuthRepository : AuthRepository {
        private var refreshToken: String? = null
        private var getTokenException = false
        private var clearTokenException = false

        fun setRefreshToken(token: String?) {
            refreshToken = token
        }

        fun setGetTokenException(shouldThrow: Boolean) {
            getTokenException = shouldThrow
        }

        fun setClearTokenException(shouldThrow: Boolean) {
            clearTokenException = shouldThrow
        }

        fun clear() {
            refreshToken = null
            getTokenException = false
            clearTokenException = false
        }

        override suspend fun sendOtp(phone: String): AuthResult {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override suspend fun verifyOtp(phone: String, token: String): AuthResult {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override suspend fun signOut(): AuthResult {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override fun getCurrentSession(): UserSession? = null

        override fun getCurrentUser(): UserInfo? = null

        override fun isAuthenticated(): Boolean = false

        override suspend fun storeRefreshToken(refreshToken: String) {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override suspend fun getRefreshToken(): String? {
            if (getTokenException) {
                throw Exception("Storage error")
            }
            return refreshToken
        }

        override suspend fun clearRefreshToken() {
            if (clearTokenException) {
                throw Exception("Clear error")
            }
            refreshToken = null
        }
    }

    class MockUserSessionManager : UserSessionManager(null, null) {
        private var currentUserId: String? = null
        var signOutCalled = false
        private var signOutException = false

        fun setCurrentUserId(userId: String?) {
            currentUserId = userId
        }

        fun setSignOutException(shouldThrow: Boolean) {
            signOutException = shouldThrow
        }

        fun getCurrentUserId(): String? = currentUserId

        override suspend fun updateLastActive(): Result<User> {
            throw UnsupportedOperationException("Not implemented in mock")
        }

        override suspend fun signOut() {
            if (signOutException) {
                throw Exception("Sign out error")
            }
            signOutCalled = true
        }
    }
}
