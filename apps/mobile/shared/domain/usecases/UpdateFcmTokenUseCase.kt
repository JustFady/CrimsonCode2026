package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.data.User
import org.crimsoncode2026.data.UserRepository

/**
 * Use case for updating user's FCM push notification token
 *
 * Stores the Firebase Cloud Messaging token for push notification targeting.
 * The token should be updated whenever FCM provides a new token.
 *
 * @param userRepository Repository for user data operations
 */
class UpdateFcmTokenUseCase(
    private val userRepository: UserRepository
) {
    /**
     * Execute the use case
     * @param userId User ID to update
     * @param fcmToken New FCM token from Firebase
     * @return Result with updated User or error
     */
    suspend operator fun invoke(
        userId: String,
        fcmToken: String
    ): Result<User> {
        return userRepository.updateFcmToken(userId, fcmToken)
    }
}
