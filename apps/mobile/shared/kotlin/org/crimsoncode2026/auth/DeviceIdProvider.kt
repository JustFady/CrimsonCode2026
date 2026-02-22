package org.crimsoncode2026.auth

import org.crimsoncode2026.storage.SecureStorage

/**
 * Device ID Provider interface
 *
 * Generates and persists a unique device ID across app installations.
 * The device ID is generated once and stored in secure encrypted storage.
 *
 * Used for:
 * - One-device-per-account enforcement in Users table
 * - Session validation on app open
 * - Rebinding device ID on new device login
 */
expect object DeviceIdProvider {

    /**
     * Initialize the DeviceIdProvider with SecureStorage
     * Must be called before using other methods
     */
    fun initialize(storage: SecureStorage)

    /**
     * Get the current device ID
     * Generates and persists a new ID if none exists
     * @return Device ID string
     */
    suspend fun getDeviceId(): String

    /**
     * Regenerate and store a new device ID
     * Use this when user logs in on a new device (device rebinding)
     * @return New device ID string
     */
    suspend fun regenerateDeviceId(): String

    /**
     * Clear the stored device ID
     * Use this on logout or account deletion
     */
    suspend fun clearDeviceId()
}
