package org.crimsoncode2026.domain.usecases

import org.crimsoncode2026.data.User
import org.crimsoncode2026.notifications.FcmTokenManager

/**
 * Use case for registering FCM token after authentication
 *
 * Retrieves the FCM token from KMPNotifier, compares with cached token,
 * and updates Supabase if the token has changed. This should be called
 * after successful authentication or when token refresh is detected.
 *
 * @param updateFcmTokenUseCase Use case for updating token in Supabase
 * @param fcmTokenManager Manager for FCM token retrieval and caching
 */
class RegisterFcmTokenUseCase(
    private val updateFcmTokenUseCase: UpdateFcmTokenUseCase,
    private val fcmTokenManager: FcmTokenManager
) {
    /**
     * Execute the use case
     * @param userId User ID to register FCM token for
     * @return Result indicating success or error
     */
    suspend operator fun invoke(userId: String): Result<Unit> {
        return try {
            // Step 1: Get the current FCM token from KMPNotifier
            val currentToken = fcmTokenManager.getToken()
                ?: return Result.failure(Exception("Failed to retrieve FCM token"))

            // Step 2: Check if token has changed compared to cached
            if (!fcmTokenManager.hasTokenChanged(currentToken)) {
                // Token hasn't changed, no need to update Supabase
                return Result.success(Unit)
            }

            // Step 3: Token has changed, update in Supabase
            updateFcmTokenUseCase(userId, currentToken)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
