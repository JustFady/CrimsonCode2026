package org.crimsoncode2026.storage

import kotlinx.coroutines.test.runTest
import org.crimsoncode2026.storage.SecureStorage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for SecureStorage interface
 *
 * Tests the encrypted storage operations on both Android and iOS platforms.
 * Since SecureStorage is an expect/actual interface, these tests
 * verify the contract behavior is consistent.
 *
 * Note: These tests use a mock/stub implementation since platform-specific
 * implementations require platform-specific dependencies (Android Context, iOS Keychain).
 * The mock simulates the expected behavior.
 */
class SecureStorageTest {

    private lateinit var storage: TestSecureStorage

    @BeforeTest
    fun setup() {
        storage = TestSecureStorage()
    }

    @AfterTest
    fun teardown() {
        storage.clear()
    }

    @Test
    fun `getString returns null when key does not exist`() = runTest {
        // Act
        val result = storage.getString("nonexistent_key")

        // Assert
        assertNull(result)
    }

    @Test
    fun `getString returns value when key exists`() = runTest {
        // Arrange
        storage.putString("test_key", "test_value")

        // Act
        val result = storage.getString("test_key")

        // Assert
        assertNotNull(result)
        assertEquals("test_value", result)
    }

    @Test
    fun `putString stores value for key`() = runTest {
        // Act
        storage.putString("storage_key", "storage_value")

        // Assert
        val retrieved = storage.getString("storage_key")
        assertEquals("storage_value", retrieved)
    }

    @Test
    fun `putString overwrites existing value`() = runTest {
        // Arrange
        storage.putString("overwrite_key", "original_value")
        storage.putString("overwrite_key", "new_value")

        // Act
        val result = storage.getString("overwrite_key")

        // Assert
        assertEquals("new_value", result)
    }

    @Test
    fun `putString stores empty string`() = runTest {
        // Act
        storage.putString("empty_key", "")

        // Assert
        val result = storage.getString("empty_key")
        assertEquals("", result)
    }

    @Test
    fun `putString stores special characters`() = runTest {
        // Arrange - special characters that should be safely stored
        val specialValue = "!@#$%^&*()_+-=[]{}|;':\",./<>?~`"

        // Act
        storage.putString("special_key", specialValue)

        // Assert
        val result = storage.getString("special_key")
        assertEquals(specialValue, result)
    }

    @Test
    fun `putString stores long string`() = runTest {
        // Arrange - simulate storing a long value (e.g., JWT token)
        val longValue = "a".repeat(1000)

        // Act
        storage.putString("long_key", longValue)

        // Assert
        val result = storage.getString("long_key")
        assertEquals(longValue, result)
        assertEquals(1000, result?.length)
    }

    @Test
    fun `remove removes existing key`() = runTest {
        // Arrange
        storage.putString("remove_key", "value_to_remove")

        // Act
        storage.remove("remove_key")
        val result = storage.getString("remove_key")

        // Assert
        assertNull(result)
    }

    @Test
    fun `remove does not throw when key does not exist`() = runTest {
        // Act - removing a non-existent key should not throw
        storage.remove("nonexistent_remove_key")

        // Assert - no exception thrown
        assertTrue(true) // If we reach here, no exception was thrown
    }

    @Test
    fun `clear removes all stored values`() = runTest {
        // Arrange
        storage.putString("key1", "value1")
        storage.putString("key2", "value2")
        storage.putString("key3", "value3")

        // Act
        storage.clear()

        // Assert
        assertNull(storage.getString("key1"))
        assertNull(storage.getString("key2"))
        assertNull(storage.getString("key3"))
    }

    @Test
    fun `clear does not throw when storage is empty`() = runTest {
        // Act - clearing empty storage should not throw
        storage.clear()

        // Assert - no exception thrown
        assertTrue(true)
    }

    @Test
    fun `multiple keys can be stored and retrieved independently`() = runTest {
        // Arrange & Act
        storage.putString("key_a", "value_a")
        storage.putString("key_b", "value_b")
        storage.putString("key_c", "value_c")

        // Assert - each key retrieves correct value
        assertEquals("value_a", storage.getString("key_a"))
        assertEquals("value_b", storage.getString("key_b"))
        assertEquals("value_c", storage.getString("key_c"))
    }

    @Test
    fun `same key with different case returns null`() = runTest {
        // Arrange - KSafe is typically case-sensitive
        storage.putString("CaseSensitiveKey", "value")

        // Act
        val result = storage.getString("casesensitivekey")

        // Assert
        assertNull(result)
    }

    @Test
    fun `getString after remove returns null`() = runTest {
        // Arrange
        storage.putString("remove_test_key", "test_value")
        storage.remove("remove_test_key")

        // Act
        val result = storage.getString("remove_test_key")

        // Assert
        assertNull(result)
    }

    @Test
    fun `operations are idempotent for same values`() = runTest {
        // Arrange
        storage.putString("idempotent_key", "same_value")
        val first = storage.getString("idempotent_key")

        // Act - put same value again
        storage.putString("idempotent_key", "same_value")
        val second = storage.getString("idempotent_key")

        // Assert
        assertEquals(first, second)
    }

    /**
     * Mock implementation of SecureStorage for testing
     *
     * In-memory implementation that simulates the expected behavior
     * without requiring platform-specific dependencies.
     */
    class TestSecureStorage : SecureStorage {
        private val store = mutableMapOf<String, String>()

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
}
