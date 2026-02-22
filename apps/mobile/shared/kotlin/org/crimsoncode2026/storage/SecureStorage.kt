package org.crimsoncode2026.storage

/**
 * Secure encrypted storage interface
 *
 * Platform-specific implementation providing encrypted key-value storage.
 * Used for sensitive data like device IDs, tokens, etc.
 */
interface SecureStorage {

    /**
     * Get string value for key
     * @return Value if exists, null otherwise
     */
    suspend fun getString(key: String): String?

    /**
     * Put string value for key
     */
    suspend fun putString(key: String, value: String)

    /**
     * Remove value for key
     */
    suspend fun remove(key: String)

    /**
     * Clear all values
     */
    suspend fun clear()
}

/**
 * Factory function for creating SecureStorage
 * Platform-specific implementation provided by expect/actual
 */
expect fun createSecureStorage(): SecureStorage
