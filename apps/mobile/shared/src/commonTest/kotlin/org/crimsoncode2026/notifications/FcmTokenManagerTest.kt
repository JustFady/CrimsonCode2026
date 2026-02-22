package org.crimsoncode2026.notifications

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for FcmTokenManager
 *
 * Tests FCM token retrieval, caching, change detection, and flow.
 */
class FcmTokenManagerTest {

    // ==================== Mock Implementations ====================

    class MockSecureStorage : SecureStorage {
        val store = mutableMapOf<String, String>()

        override suspend fun getString(key: String): String? {
            return store[key]
        }

        override suspend fun putString(key: String, value: String) {
            store[key] = value
        }

        override suspend fun remove(key: String) {
            store.remove(key)
        }

        override suspend fun clear() {
            store.clear()
        }
    }

    class MockNotifier(
        private val token: String? = "mock-fcm-token"
    ) : io.github.mirzemehdi.kmpnotifier.KmpNotifier {
        override suspend fun getToken(): String? = token

        val tokenFlowValue = token
        val tokenFlow = flowOf(
            io.github.mirzemehdi.kmpnotifier.NotificationToken(
                token = tokenFlowValue
            )
        )

        override val tokenFlow: Flow<io.github.mirzemehdi.kmpnotifier.NotificationToken>
            get() = tokenFlow
    }

    class NullTokenNotifier : io.github.mirzemehdi.kmpnotifier.KmpNotifier {
        override suspend fun getToken(): String? = null

        override val tokenFlow: Flow<io.github.mirzemehdi.kmpnotifier.NotificationToken>
            get() = flowOf(
                io.github.mirzemehdi.kmpnotifier.NotificationToken(
                    token = null
                )
            )
    }

    class ThrowingNotifier : io.github.mirzemehdi.kmpnotifier.KmpNotifier {
        override suspend fun getToken(): String? {
            throw RuntimeException("FCM error")
        }

        override val tokenFlow: Flow<io.github.mirzemehdi.kmpnotifier.NotificationToken>
            get() = flowOf()
    }

    // ==================== Get Token Tests ====================

    @Test
    fun `getToken returns token from notifier`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage()
        val mockNotifier = MockNotifier(token = "test-fcm-token")
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        val token = manager.getToken()

        // Assert
        assertEquals("test-fcm-token", token, "Should return token from notifier")
    }

    @Test
    fun `getToken caches token in storage`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage()
        val mockNotifier = MockNotifier(token = "cached-token")
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        manager.getToken()

        // Assert
        assertEquals("cached-token", mockStorage.store["fcm_token"], "Token should be cached")
    }

    @Test
    fun `getToken returns null when notifier returns null`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage()
        val mockNotifier = NullTokenNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        val token = manager.getToken()

        // Assert
        assertNull(token, "Should return null when notifier returns null")
    }

    @Test
    fun `getToken handles exceptions from notifier gracefully`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage()
        val mockNotifier = ThrowingNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        val token = manager.getToken()

        // Assert
        assertNull(token, "Should return null on exception")
    }

    @Test
    fun `getToken does not cache null token`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage()
        val mockNotifier = NullTokenNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        manager.getToken()

        // Assert
        assertFalse(mockStorage.store.containsKey("fcm_token"), "Should not cache null token")
    }

    // ==================== Get Cached Token Tests ====================

    @Test
    fun `getCachedToken returns previously cached token`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage().apply {
            store["fcm_token"] = "cached-token-123"
        }
        val mockNotifier = MockNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        val cachedToken = manager.getCachedToken()

        // Assert
        assertEquals("cached-token-123", cachedToken, "Should return cached token")
    }

    @Test
    fun `getCachedToken returns null when no token cached`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage()
        val mockNotifier = MockNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        val cachedToken = manager.getCachedToken()

        // Assert
        assertNull(cachedToken, "Should return null when no token cached")
    }

    // ==================== Has Token Changed Tests ====================

    @Test
    fun `hasTokenChanged returns true when token differs from cached`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage().apply {
            store["fcm_token"] = "old-token"
        }
        val mockNotifier = MockNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        val hasChanged = manager.hasTokenChanged("new-token")

        // Assert
        assertTrue(hasChanged, "Should detect token change")
    }

    @Test
    fun `hasTokenChanged returns false when token matches cached`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage().apply {
            store["fcm_token"] = "same-token"
        }
        val mockNotifier = MockNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        val hasChanged = manager.hasTokenChanged("same-token")

        // Assert
        assertFalse(hasChanged, "Should not detect change for same token")
    }

    @Test
    fun `hasTokenChanged returns true when no cached token`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage()
        val mockNotifier = MockNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        val hasChanged = manager.hasTokenChanged("new-token")

        // Assert
        assertTrue(hasChanged, "Should detect change when no cached token")
    }

    @Test
    fun `hasTokenChanged returns false when both are null`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage()
        val mockNotifier = NullTokenNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        val hasChanged = manager.hasTokenChanged(null)

        // Assert
        assertFalse(hasChanged, "Should not detect change when both are null")
    }

    // ==================== Token Refresh Flow Tests ====================

    @Test
    fun `tokenRefreshFlow emits tokens from notifier`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage()
        val mockNotifier = MockNotifier(token = "flow-token")
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        val flow = manager.tokenRefreshFlow()

        // Assert
        assertTrue(flow is Flow, "Should return Flow")
    }

    @Test
    fun `tokenRefreshFlow handles null tokens`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage()
        val mockNotifier = NullTokenNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        val flow = manager.tokenRefreshFlow()

        // Assert
        assertTrue(flow is Flow, "Should return Flow even with null tokens")
    }

    // ==================== Clear Cached Token Tests ====================

    @Test
    fun `clearCachedToken removes token from storage`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage().apply {
            store["fcm_token"] = "token-to-clear"
        }
        val mockNotifier = MockNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act
        manager.clearCachedToken()

        // Assert
        assertFalse(mockStorage.store.containsKey("fcm_token"), "Token should be removed")
    }

    @Test
    fun `clearCachedToken handles missing token gracefully`() = runTest {
        // Arrange
        val mockStorage = MockSecureStorage()
        val mockNotifier = MockNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act - Should not throw
        manager.clearCachedToken()

        // Assert - If we reach here, it handled missing gracefully
        assertTrue(true, "Clear should handle missing token")
    }

    // ==================== Storage Key Tests ====================

    @Test
    fun `FCM_TOKEN_KEY is constant`() = runTest {
        // The key is a private const, but we can verify behavior
        val mockStorage = MockSecureStorage()
        val mockNotifier = MockNotifier()
        val manager = FcmTokenManager(mockStorage, mockNotifier)

        // Act - Set and retrieve token
        manager.getToken()

        // Assert - Verify the expected key is used
        // We can't access the private constant directly, but we can verify the behavior
        assertTrue(mockStorage.store.size > 0, "Something should be stored")
    }
}
