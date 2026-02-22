package org.crimsoncode2026.domain

import io.github.jan-tennert.supabase.auth.User as SupabaseUser
import io.github.jan-tennert.supabase.auth.Auth
import io.github.jan-tennert.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.crimsoncode2026.auth.DeviceIdProvider
import org.crimsoncode2026.data.User
import org.crimsoncode2026.data.UserRepository

/**
 * Result of device binding validation
 */
sealed class DeviceBindingResult {
    /**
     * Device is correctly bound to the user
     */
    data object Valid : DeviceBindingResult()

    /**
     * User exists but bound to a different device
     * @param currentDeviceId The device ID currently bound to the user
     */
    data class DifferentDevice(val currentDeviceId: String) : DeviceBindingResult()

    /**
     * User not found in database
     */
    data object UserNotFound : DeviceBindingResult()
}

/**
 * User Session Manager
 *
 * Handles user session lifecycle including:
 * - Getting current authenticated user from Supabase Auth
 * - Checking device binding (one-device-per-phone enforcement)
 * - Rebinding device ID on new device login
 * - Updating last active timestamp
 *
 * @param auth Supabase Auth client
 * @param userRepository Repository for user data operations
 */
class UserSessionManager(
    private val auth: Auth,
    private val userRepository: UserRepository
) {

    /**
     * Get the current authenticated user from Supabase Auth
     * @return Auth user or null if not authenticated
     */
    fun getCurrentAuthUser(): SupabaseUser? {
        return auth.currentUserOrNull()
    }

    /**
     * Flow of current authentication state
     * @return Flow emitting the current auth user (null when signed out)
     */
    fun authStateFlow(): Flow<SupabaseUser?> {
        return auth.sessionStatus.map { status ->
            when (status) {
                is io.github.jan-tennert.supabase.auth.status.SessionStatus.Authenticated -> {
                    auth.currentUserOrNull()
                }
                else -> null
            }
        }
    }

    /**
     * Get the authenticated user's ID
     * @return User ID or null if not authenticated
     */
    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    /**
     * Validate device binding for the current authenticated user
     *
     * Checks if the user's database record is bound to the current device.
     * If bound to a different device, this indicates the account is being
     * used from a new device and requires device rebinding.
     *
     * @return DeviceBindingResult indicating validation status
     */
    suspend fun validateDeviceBinding(): DeviceBindingResult {
        val userId = getCurrentUserId() ?: return DeviceBindingResult.UserNotFound

        val user = userRepository.getUserById(userId).getOrNull()
            ?: return DeviceBindingResult.UserNotFound

        val currentDeviceId = DeviceIdProvider.getDeviceId()

        return if (user.deviceId == currentDeviceId) {
            DeviceBindingResult.Valid
        } else {
            DeviceBindingResult.DifferentDevice(user.deviceId)
        }
    }

    /**
     * Rebind user to current device
     *
     * Updates the user's device_id to the current device's ID.
     * Called when user logs in from a new device (hackathon behavior).
     *
     * @param userId User ID to rebind
     * @return Result with updated User or error
     */
    suspend fun rebindToDevice(userId: String): Result<User> {
        val currentDeviceId = DeviceIdProvider.getDeviceId()
        return userRepository.updateDeviceId(userId, currentDeviceId)
    }

    /**
     * Update user's last active timestamp
     *
     * Should be called on app open and periodically during active usage.
     *
     * @return Result with updated User or error
     */
    suspend fun updateLastActive(): Result<User> {
        val userId = getCurrentUserId() ?: return Result.failure(
            IllegalStateException("No authenticated user")
        )

        return userRepository.updateLastActive(userId)
    }

    /**
     * Check if user is currently authenticated
     * @return true if authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean {
        return auth.currentUserOrNull() != null
    }

    /**
     * Sign out the current user
     *
     * Clears the Supabase Auth session.
     * Device ID is kept (only cleared on explicit logout or app reinstall).
     */
    suspend fun signOut() {
        auth.signOut()
    }
}
