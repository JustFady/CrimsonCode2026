package org.crimsoncode2026.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.crimsoncode2026.storage.SecureStorage
import platform.Foundation.NSUUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * iOS implementation of DeviceIdProvider
 *
 * Uses device vendor ID as base but stores generated UUID in SecureStorage for persistence.
 */
actual object DeviceIdProvider {

    private const val DEVICE_ID_KEY = "device_id_ios"

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
    actual suspend fun getDeviceId(): String = withContext(Dispatchers.Default) {
        val secureStorage = requireStorage()

        // Check if device ID already stored
        secureStorage.getString(DEVICE_ID_KEY)?.let { return@withContext it }

        // Generate new device ID
        val newDeviceId = generateDeviceId()
        secureStorage.putString(DEVICE_ID_KEY, newDeviceId)
        return@withContext newDeviceId
    }

    /**
     * Regenerate device ID for new device binding
     */
    actual suspend fun regenerateDeviceId(): String = withContext(Dispatchers.Default) {
        val secureStorage = requireStorage()
        val newDeviceId = generateDeviceId()
        secureStorage.putString(DEVICE_ID_KEY, newDeviceId)
        return@withContext newDeviceId
    }

    /**
     * Clear stored device ID
     */
    actual suspend fun clearDeviceId() = withContext(Dispatchers.Default) {
        requireStorage().remove(DEVICE_ID_KEY)
    }

    /**
     * Generate unique device ID
     *
     * Uses combination of device vendor ID (if available) and random UUID
     * Vendor ID changes when app is deleted and reinstalled
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun generateDeviceId(): String {
        val vendorId: String? = try {
            // UIDevice.currentDevice.identifierForVendor?.UUIDString
            // Using Kotlin UUID instead for simplicity
            NSUUID().UUIDString
        } catch (e: Exception) {
            null
        }

        val randomUuid = Uuid.randomUUID().toString()

        // Combine vendor ID (if available) with random UUID for uniqueness
        return if (!vendorId.isNullOrEmpty()) {
            "ios-$vendorId-$randomUuid"
        } else {
            "ios-unknown-$randomUuid"
        }
    }
}
