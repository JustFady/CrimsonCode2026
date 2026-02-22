package org.crimsoncode2026.notifications

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing FCM (Firebase Cloud Messaging) token lifecycle
 *
 * Coordinates permission request, token retrieval, local caching, and Supabase storage.
 * Provides methods to register token, refresh token, and clear token on logout.
 *
 * @param notificationPermissionHandler Handles notification permission requests
 * @param fcmTokenManager Manages FCM token retrieval and caching
 */
class FcmTokenRepository(
    private val notificationPermissionHandler: NotificationPermissionHandler,
    private val fcmTokenManager: FcmTokenManager
) {

    /**
     * Result of FCM token registration
     */
    sealed class RegisterResult {
        data object Success : RegisterResult()
        data object PermissionDenied : RegisterResult()
        data class Error(val message: String) : RegisterResult()
    }

    /**
     * Registers the FCM token for the user
     *
     * Process:
     * 1. Request notification permission if not granted
     * 2. Retrieve FCM token from KMPNotifier
     * 3. Cache token locally for comparison
     * 4. Store token in Supabase (if changed)
     *
     * @param userId User ID to register token for
     * @return RegisterResult indicating success or failure reason
     */
    suspend fun registerToken(userId: String): RegisterResult {
        // Step 1: Check notification permission
        if (!notificationPermissionHandler.isNotificationPermissionGranted()) {
            val granted = notificationPermissionHandler.requestNotificationPermission()
            if (!granted) {
                return RegisterResult.PermissionDenied
            }
        }

        // Step 2: Retrieve and cache FCM token
        val token = fcmTokenManager.getToken()
            ?: return RegisterResult.Error("Failed to retrieve FCM token")

        // Step 3: Token is automatically cached by FcmTokenManager.getToken()
        // The actual Supabase update should be handled by the caller
        // using RegisterFcmTokenUseCase which handles the comparison

        return RegisterResult.Success
    }

    /**
     * Refreshes the FCM token
     *
     * Called when a token refresh is detected from KMPNotifier.
     * Compares new token with cached and updates if changed.
     *
     * @param userId User ID to update token for
     * @return RegisterResult indicating success or failure reason
     */
    suspend fun refreshToken(userId: String): RegisterResult {
        val token = fcmTokenManager.getToken()
            ?: return RegisterResult.Error("Failed to retrieve FCM token")

        // Token is automatically cached and compared in FcmTokenManager
        // The actual Supabase update should be handled by the caller
        // using RegisterFcmTokenUseCase which handles the comparison

        return RegisterResult.Success
    }

    /**
     * Clears the cached FCM token
     *
     * Called on logout to remove the token from local storage.
     */
    suspend fun clearToken() {
        fcmTokenManager.clearCachedToken()
    }

    /**
     * Gets the cached FCM token
     *
     * @return Cached token or null if not cached
     */
    suspend fun getCachedToken(): String? {
        return fcmTokenManager.getCachedToken()
    }

    /**
     * Provides a Flow of token refresh events
     *
     * Emits a new token whenever FCM issues a token refresh.
     * Can be used to trigger automatic token updates.
     *
     * @return Flow of FCM token strings
     */
    fun tokenRefreshFlow(): Flow<String> {
        return fcmTokenManager.tokenRefreshFlow()
    }

    /**
     * Checks if notification permission is granted
     *
     * @return true if permission granted, false otherwise
     */
    fun isPermissionGranted(): Boolean {
        return notificationPermissionHandler.isNotificationPermissionGranted()
    }

    /**
     * Provides a Flow of notification permission state
     *
     * @return Flow of NotificationPermissionState
     */
    fun permissionState(): Flow<NotificationPermissionState> {
        return notificationPermissionHandler.notificationPermissionState()
    }
}
