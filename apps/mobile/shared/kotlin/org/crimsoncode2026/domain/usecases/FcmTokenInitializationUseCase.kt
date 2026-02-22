package org.crimsoncode2026.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.crimsoncode2026.notifications.FcmTokenRepository
import org.crimsoncode2026.notifications.FcmTokenRepository.RegisterResult

/**
 * Use case for handling FCM token refresh on app launch.
 *
 * On app launch:
 * 1. Checks if cached token exists
 * 2. Listens for token refresh events from KMPNotifier
 * 3. Automatically updates Supabase when token changes
 *
 * Ensures the FCM token stays fresh for push delivery.
 *
 * @param fcmTokenRepository Repository for FCM token management
 * @param registerFcmTokenUseCase Use case for registering token in Supabase
 */
class FcmTokenInitializationUseCase(
    private val fcmTokenRepository: FcmTokenRepository,
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase
) {

    /**
     * Initializes FCM token handling on app launch.
     *
     * This method should be called during app initialization to ensure
     * the FCM token is registered and stays fresh.
     *
     * Process:
     * 1. Check if notification permission is granted
     * 2. Listen for token refresh events
     * 3. Automatically refresh token when it changes
     *
     * @param userId User ID to register FCM token for
     * @return Flow of RegisterResult events for status updates
     */
    operator fun invoke(userId: String): Flow<RegisterResult> {
        // Step 1: Check if notification permission is granted
        if (!fcmTokenRepository.isPermissionGranted()) {
            // Permission not granted, return permission denied
            return kotlinx.coroutines.flow.flowOf(FcmTokenRepository.RegisterResult.PermissionDenied)
        }

        // Step 2: Get the token refresh flow from KMPNotifier
        val tokenRefreshFlow = fcmTokenRepository.tokenRefreshFlow()

        // Step 3: Convert refresh events to registration results
        return tokenRefreshFlow
            .onEach { newToken ->
                // When a new token is emitted, register it in Supabase
                registerFcmTokenUseCase(userId, newToken)
            }
            .map { _ ->
                // Return success to indicate initialization is complete
                FcmTokenRepository.RegisterResult.Success
            }
    }

    /**
     * Performs one-time token refresh on app launch.
     *
     * Unlike invoke(), this performs a single refresh check rather
     * than continuously listening for token changes.
     *
     * @param userId User ID to refresh token for
     * @return RegisterResult indicating success or failure
     */
    suspend fun initializeOnce(userId: String): RegisterResult {
        // Step 1: Check if notification permission is granted
        if (!fcmTokenRepository.isPermissionGranted()) {
            return FcmTokenRepository.RegisterResult.PermissionDenied
        }

        // Step 2: Get cached token to check if we have one
        val cachedToken = fcmTokenRepository.getCachedToken()

        // Step 3: If no cached token, request registration
        if (cachedToken == null) {
            return fcmTokenRepository.registerToken(userId)
        }

        // Step 4: Token exists, refresh to ensure it's still valid
        return fcmTokenRepository.refreshToken(userId)
    }
}
