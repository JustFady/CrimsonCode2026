package org.crimsoncode2026.auth

import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.crimsoncode2026.storage.SecureStorage
import java.util.UUID

/**
 * Android implementation of DeviceIdProvider
 *
 * Uses Android ID as base but stores generated UUID in SecureStorage for persistence.
 */
actual object DeviceIdProvider {

    private const val DEVICE_ID_KEY = "device_id"
    private const val DEVICE_ID_KEY_ANDROID = "device_id_android"

    private var storage: SecureStorage? = null

    /**
     * Initialize with SecureStorage
     */
    actual fun initialize(storage: SecureStorage) {
        this.storage = storage
    }

    private fun requireStorage(): SecureStorage {
        return storage ?: throw IllegalStateException(
            "DeviceIdProvider must be initialized before use. Call initialize() first."
        )
    }

    /**
     * Get current device ID, generating one if needed
     */
    actual override suspend fun getDeviceId(): String = withContext(Dispatchers.Default) {
        val secureStorage = requireStorage()

        // Check if device ID already stored
        secureStorage.getString(DEVICE_ID_KEY_ANDROID)?.let { return@withContext it }

        // Generate new device ID
        val newDeviceId = generateDeviceId()
        secureStorage.putString(DEVICE_ID_KEY_ANDROID, newDeviceId)
        return@withContext newDeviceId
    }

    /**
     * Regenerate device ID for new device binding
     */
    actual override suspend fun regenerateDeviceId(): String = withContext(Dispatchers.Default) {
        val secureStorage = requireStorage()
        val newDeviceId = generateDeviceId()
        secureStorage.putString(DEVICE_ID_KEY_ANDROID, newDeviceId)
        return@withContext newDeviceId
    }

    /**
     * Clear stored device ID
     */
    actual override suspend fun clearDeviceId() = withContext(Dispatchers.Default) {
        requireStorage().remove(DEVICE_ID_KEY_ANDROID)
    }

    /**
     * Generate unique device ID
     *
     * Uses combination of Android ID (if available) and random UUID
     * Android ID: Persistent across app reinstalls but reset on factory reset
     */
    private fun generateDeviceId(): String {
        val androidId = try {
            Settings.Secure.getString(
                ContextProvider.getContext().contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } catch (e: Exception) {
            null
        }

        val randomUuid = UUID.randomUUID().toString()

        // Combine android ID (if available) with random UUID for uniqueness
        return if (!androidId.isNullOrEmpty() && androidId != "9774d56d682e549c") {
            "android-$androidId-$randomUuid"
        } else {
            "android-unknown-$randomUuid"
        }
    }
}
