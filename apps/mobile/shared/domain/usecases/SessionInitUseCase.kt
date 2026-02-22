package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.auth.AuthRepository
import org.crimsoncode2026.domain.UserSessionManager

/**
 * Result of session initialization check
 */
sealed class SessionInitResult {
    /**
     * No session exists - user must log in with OTP
     */
    data object NeedsOtpLogin : SessionInitResult()

    /**
     * Session is ready to use
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
 * 3. Return appropriate flow (OTP login or session ready)
 */
class SessionInitUseCase(
    private val authRepository: AuthRepository,
    private val userSessionManager: UserSessionManager
) {

    /**
     * Initialize session and determine authentication flow
     * @return SessionInitResult indicating next step
     */
    suspend operator fun invoke(): SessionInitResult {
        // Step 1: Check for existing refresh token in encrypted storage
        val refreshToken = try {
            authRepository.getRefreshToken()
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
                authRepository.clearRefreshToken()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
            return SessionInitResult.NeedsOtpLogin
        }

        // Step 5: Valid session exists, proceed to main app
        return SessionInitResult.SessionReady(userId)
    }

    /**
     * Clear stored session (logout)
     */
    suspend fun clearSession() {
        try {
            authRepository.clearRefreshToken()
            userSessionManager.signOut()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}
