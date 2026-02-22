package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.data.User
import org.crimsoncode2026.data.UserRepository

/**
 * Use case for updating user's display name
 *
 * Updates the display name in the Users table, which is shown
 * to contacts for private events. Historical events will reflect
 * the new name when displayed.
 *
 * @param UserRepository Repository for user data operations
 */
class UpdateDisplayNameUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Execute the use case
     * @param userId User ID to update
     * @param displayName New display name (shown to contacts)
     * @return Result with updated User or error
     */
    suspend operator fun invoke(
        userId: String,
        displayName: String
    ): Result<User> {
        return userRepository.updateDisplayName(userId, displayName)
    }
}
