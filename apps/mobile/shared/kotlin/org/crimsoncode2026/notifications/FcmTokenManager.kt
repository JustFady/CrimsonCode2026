package org.crimsoncode2026.notifications

import com.mmk.kmpnotifier.KmpNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.crimsoncode2026.storage.SecureStorage

/**
 * Manages FCM (Firebase Cloud Messaging) token retrieval and caching.
 *
 * Uses KMPNotifier for cross-platform FCM token management.
 * Stores token locally in encrypted storage for comparison and persistence.
 */
class FcmTokenManager(
    private val secureStorage: SecureStorage,
    private val notifier: KmpNotifier
) {

    companion object {
        private const val FCM_TOKEN_KEY = "fcm_token"
    }

    /**
     * Retrieves the current FCM token from KMPNotifier.
     * Also stores the token in encrypted storage for comparison.
     *
     * @return The current FCM token, or null if retrieval fails
     */
    suspend fun getToken(): String? {
        return try {
            val token = notifier.getToken()
            if (token != null) {
                cacheToken(token)
            }
            token
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Retrieves the cached FCM token from encrypted storage.
     *
     * @return The cached FCM token, or null if not cached
     */
    suspend fun getCachedToken(): String? {
        return secureStorage.getString(FCM_TOKEN_KEY)
    }

    /**
     * Checks if the provided token differs from the cached token.
     * Used to determine if a token refresh has occurred.
     *
     * @param newToken The new token to compare
     * @return true if the token has changed, false otherwise
     */
    suspend fun hasTokenChanged(newToken: String): Boolean {
        val cachedToken = getCachedToken()
        return cachedToken != newToken
    }

    /**
     * Provides a Flow of token refresh events from KMPNotifier.
     * Emits a new token whenever FCM issues a token refresh.
     *
     * @return Flow that emits new FCM tokens
     */
    fun tokenRefreshFlow(): Flow<String> {
        return notifier.tokenFlow.map { it.token }
    }

    /**
     * Caches the FCM token in encrypted storage.
     *
     * @param token The FCM token to cache
     */
    private suspend fun cacheToken(token: String) {
        secureStorage.putString(FCM_TOKEN_KEY, token)
    }

    /**
     * Clears the cached FCM token from encrypted storage.
     */
    suspend fun clearCachedToken() {
        secureStorage.remove(FCM_TOKEN_KEY)
    }
}
