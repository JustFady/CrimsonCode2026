package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.data.User
import org.crimsoncode2026.data.UserRepository

/**
 * Use case for updating user's last active timestamp
 *
 * Updates the last_active_at timestamp when user is active in the app.
 * Used for activity tracking and cleanup of inactive accounts.
 *
 * Should be called:
 * - On app open after successful authentication
 * - Periodically during active usage (e.g., every 5-10 minutes)
 *
 * @param userRepository Repository for user data operations
 */
class UpdateLastActiveUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Execute the use case
     * @param userId User ID to update
     * @return Result with updated User or error
     */
    suspend operator fun invoke(userId: String): Result<User> {
        return userRepository.updateLastActive(userId)
    }
}
