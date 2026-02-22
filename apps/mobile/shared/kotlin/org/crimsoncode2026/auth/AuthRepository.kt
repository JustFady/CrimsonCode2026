package org.crimsoncode2026.auth

import io.github.jan-tennert.supabase.auth.Auth
import io.github.jan-tennert.supabase.auth.auth
import io.github.jan-tennert.supabase.auth.OtpType
import io.github.jan-tennert.supabase.auth.user.UserInfo
import io.github.jan-tennert.supabase.auth.user.UserSession
import org.crimsoncode2026.storage.SecureStorage

/**
 * Result type for authentication operations
 */
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String, val cause: Exception? = null) : AuthResult()
}

/**
 * Repository interface for authentication operations
 * Handles Supabase Auth phone OTP flow
 */
interface AuthRepository {

    /**
     * Send OTP code to phone number
     * @param phone Phone number in E.164 format (e.g., +15551234567)
     * @return AuthResult.Success on success, AuthResult.Error on failure
     */
    suspend fun sendOtp(phone: String): AuthResult

    /**
     * Verify OTP code and sign in
     * @param phone Phone number in E.164 format
     * @param token 6-digit OTP code received via SMS
     * @return AuthResult.Success on success, AuthResult.Error on failure
     */
    suspend fun verifyOtp(phone: String, token: String): AuthResult

    /**
     * Sign out current user
     * @return AuthResult.Success on success, AuthResult.Error on failure
     */
    suspend fun signOut(): AuthResult

    /**
     * Get current user session if authenticated
     * @return UserSession or null if not authenticated
     */
    fun getCurrentSession(): UserSession?

    /**
     * Get current user info if authenticated
     * @return UserInfo or null if not authenticated
     */
    fun getCurrentUser(): UserInfo?

    /**
     * Check if user is currently authenticated
     * @return true if authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean

    /**
     * Store refresh token in encrypted storage
     * Called after successful OTP verification to maintain 30-day session persistence
     * @param refreshToken The refresh token to store
     */
    suspend fun storeRefreshToken(refreshToken: String)

    /**
     * Get stored refresh token from encrypted storage
     * @return Refresh token if exists, null otherwise
     */
    suspend fun getRefreshToken(): String?

    /**
     * Clear stored refresh token
     * Called on logout
     */
    suspend fun clearRefreshToken()
}

/**
 * Supabase implementation of AuthRepository
 */
class AuthRepositoryImpl(
    private val auth: Auth,
    private val secureStorage: SecureStorage
) : AuthRepository {

    companion object {
        private const val REFRESH_TOKEN_KEY = "refresh_token"
    }

    override suspend fun sendOtp(phone: String): AuthResult = try {
        auth.signInWith(Otp) {
            this.phone = phone
        }
        AuthResult.Success
    } catch (e: Exception) {
        AuthResult.Error(
            message = getErrorMessage(e),
            cause = e
        )
    }

    override suspend fun verifyOtp(phone: String, token: String): AuthResult = try {
        auth.verifyOTP(
            type = OtpType.Phone,
            phone = phone,
            token = token
        )

        // Store refresh token for 30-day session persistence
        auth.currentSessionOrNull()?.let { session ->
            session.refreshToken?.let { refreshToken ->
                secureStorage.putString(REFRESH_TOKEN_KEY, refreshToken)
            }
        }

        AuthResult.Success
    } catch (e: Exception) {
        AuthResult.Error(
            message = getErrorMessage(e),
            cause = e
        )
    }

    override suspend fun signOut(): AuthResult = try {
        auth.signOut()
        // Clear stored refresh token on logout
        secureStorage.remove(REFRESH_TOKEN_KEY)
        AuthResult.Success
    } catch (e: Exception) {
        AuthResult.Error(
            message = getErrorMessage(e),
            cause = e
        )
    }

    override fun getCurrentSession(): UserSession? {
        return auth.currentSessionOrNull()
    }

    override fun getCurrentUser(): UserInfo? {
        return auth.currentUserOrNull()
    }

    override fun isAuthenticated(): Boolean {
        return auth.currentUserOrNull() != null
    }

    override suspend fun storeRefreshToken(refreshToken: String) {
        secureStorage.putString(REFRESH_TOKEN_KEY, refreshToken)
    }

    override suspend fun getRefreshToken(): String? {
        return secureStorage.getString(REFRESH_TOKEN_KEY)
    }

    override suspend fun clearRefreshToken() {
        secureStorage.remove(REFRESH_TOKEN_KEY)
    }

    /**
     * Get user-friendly error message from exception
     * Maps Supabase Auth errors to readable messages
     */
    private fun getErrorMessage(e: Exception): String {
        val message = e.message ?: "Unknown error occurred"

        return when {
            message.contains("Invalid login credentials") -> "Invalid verification code"
            message.contains("Email not confirmed") -> "Please verify your email first"
            message.contains("User already registered") -> "Phone number already registered"
            message.contains("Phone verification failed") -> "Invalid verification code"
            message.contains("OTP has expired") -> "Verification code expired. Please request a new one"
            message.contains("Too many requests") -> "Too many attempts. Please try again later"
            message.contains("Network", ignoreCase = true) -> "Network error. Please check your connection"
            else -> "Authentication failed. Please try again"
        }
    }
}
