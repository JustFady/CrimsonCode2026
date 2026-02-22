package org.crimsoncode2026.domain.usecases

import io.github.jan-tennert.supabase.auth.auth
import kotlinx.coroutines.flow.first
import org.crimsoncode2026.auth.AuthRepository
import org.crimsoncode2026.auth.DeviceIdProvider
import org.crimsoncode2026.data.User
import org.crimsoncode2026.data.UserRepository
import org.crimsoncode2026.domain.UserSessionManager
import org.crimsoncode2026.storage.SecureStorage

/**
 * Result of user registration
 */
sealed class RegistrationResult {
    data object Success : RegistrationResult()
    data class Error(val message: String) : RegistrationResult()
}

/**
 * Registration Use Case
 * Orchestrates the complete user registration flow:
 * 1. Verify OTP via AuthRepository
 * 2. Get device ID from DeviceIdProvider
 * 3. Create or update user in database
 * 4. Store refresh token in SecureStorage
 */
class RegisterUserUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val deviceIdProvider: DeviceIdProvider,
    private val secureStorage: SecureStorage,
    private val userSessionManager: UserSessionManager
) {

    companion object {
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val PHONE_NUMBER_KEY = "phone_number"
    }

    /**
     * Complete user registration flow
     * @param phoneNumber Phone number in E.164 format
     * @param displayName User's display name
     * @param otpToken 6-digit OTP code
     * @return RegistrationResult
     */
    suspend operator fun invoke(
        phoneNumber: String,
        displayName: String,
        otpToken: String
    ): RegistrationResult {
        // Step 1: Verify OTP
        when (val verifyResult = authRepository.verifyOtp(phoneNumber, otpToken)) {
            is org.crimsoncode2026.auth.AuthResult.Error -> {
                return RegistrationResult.Error(verifyResult.message)
            }
            is org.crimsoncode2026.auth.AuthResult.Success -> {
                // Continue to next steps
            }
        }

        // Step 2: Get device ID
        val deviceId = try {
            deviceIdProvider.getDeviceId()
        } catch (e: Exception) {
            return RegistrationResult.Error("Failed to get device ID")
        }

        // Step 3: Get authenticated user info from Supabase
        val currentUserId = userSessionManager.getCurrentUserId()
            ?: return RegistrationResult.Error("Authentication failed")

        // Step 4: Create or update user in database
        val user = User(
            id = currentUserId,
            phoneNumber = phoneNumber,
            displayName = displayName,
            deviceId = deviceId,
            platform = getPlatformName(),
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis()
        )

        when (val existingUser = userRepository.getUserByPhoneNumber(phoneNumber)) {
            is Result.Success -> {
                if (existingUser.getOrNull() != null) {
                    // User exists, update device ID and display name
                    userRepository.updateDeviceId(currentUserId, deviceId)
                    userRepository.updateDisplayName(currentUserId, displayName)
                } else {
                    // New user, create record
                    userRepository.createUser(user)
                }
            }
            is Result.Failure -> {
                userRepository.createUser(user)
            }
        }

        // Step 5: Store refresh token in SecureStorage
        try {
            authRepository.getCurrentSession()?.refreshToken?.let { refreshToken ->
                secureStorage.putString(REFRESH_TOKEN_KEY, refreshToken)
            }
            secureStorage.putString(PHONE_NUMBER_KEY, phoneNumber)
        } catch (e: Exception) {
            return RegistrationResult.Error("Failed to store session")
        }

        return RegistrationResult.Success
    }

    /**
     * Get platform name for user record
     */
    private fun getPlatformName(): String {
        return if (isAndroid()) "ANDROID" else "IOS"
    }

    /**
     * Check if running on Android
     */
    private fun isAndroid(): Boolean {
        return System.getProperty("java.vm.name")?.contains("Android") ?: false ||
                System.getProperty("os.name")?.equals("Linux", ignoreCase = true) ?: false
    }
}
