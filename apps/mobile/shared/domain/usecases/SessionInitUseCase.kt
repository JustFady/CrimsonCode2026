package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.domain.UserSessionManager
import org.crimsoncode2026.storage.SecureStorage

/**
 * Result of session initialization check
 */
sealed class SessionInitResult {
    /**
     * No session exists - user must log in with OTP
     */
    data object NeedsOtpLogin : SessionInitResult()

    /**
     * Valid session exists - user must authenticate with biometrics
     */
    data class NeedsBiometricUnlock(val userId: String) : SessionInitResult()

    /**
     * Session is ready to use (no biometric unlock required in this flow)
     */
    data class SessionReady(val userId: String) : SessionInitResult()

    /**
     * Error occurred during initialization
     */
    data class Error(val message: String) : SessionInitResult()
}

/**
 * Session Initialization Use Case
 * Determines what authentication flow to show on app launch:
 * 1. Check for existing refresh token in encrypted storage
 * 2. Validate session via UserSessionManager
 * 3. Return appropriate flow (OTP login or biometric unlock)
 */
class SessionInitUseCase(
    private val secureStorage: SecureStorage,
    private val userSessionManager: UserSessionManager
) {

    companion object {
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    /**
     * Initialize session and determine authentication flow
     * @return SessionInitResult indicating next step
     */
    suspend operator fun invoke(): SessionInitResult {
        // Step 1: Check for existing refresh token in encrypted storage
        val refreshToken = try {
            secureStorage.getString(REFRESH_TOKEN_KEY)
        } catch (e: Exception) {
            return SessionInitResult.Error("Failed to check session")
        }

        // Step 2: If no refresh token, need OTP login
        if (refreshToken.isNullOrEmpty()) {
            return SessionInitResult.NeedsOtpLogin
        }

        // Step 3: Check if Supabase has a valid session
        val userId = userSessionManager.getCurrentUserId()

        // Step 4: No valid session in Supabase Auth, need OTP login
        if (userId == null) {
            // Clear invalid stored token
            try {
                secureStorage.remove(REFRESH_TOKEN_KEY)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            return SessionInitResult.NeedsOtpLogin
        }

        // Step 5: Valid session exists, need biometric unlock
        return SessionInitResult.NeedsBiometricUnlock(userId)
    }

    /**
     * Clear stored session (logout)
     */
    suspend fun clearSession() {
        try {
            secureStorage.remove(REFRESH_TOKEN_KEY)
            userSessionManager.signOut()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}
