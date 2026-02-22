package org.crimsoncode2026.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import eu.anifantakis.lib.ksafe.KSafe

/**
 * iOS implementation of SecureStorage using KSafe
 *
 * Provides encrypted key-value storage backed by iOS Keychain.
 */
class IOSSecureStorage() : org.crimsoncode2026.storage.SecureStorage {

    private val ksafe: KSafe by lazy {
        KSafe(
            fileName = "securestorage",
        )
    }

    override suspend fun getString(key: String): String? = withContext(Dispatchers.Default) {
        return@withContext ksafe.get<String?>(key, null)
    }

    override suspend fun putString(key: String, value: String) = withContext(Dispatchers.Default) {
        ksafe.put(key, value)
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.Default) {
        ksafe.delete(key)
    }

    override suspend fun clear() = withContext(Dispatchers.Default) {
        ksafe.clearAll()
    }
}

/**
 * iOS implementation of createSecureStorage factory
 */
actual fun createSecureStorage(): SecureStorage {
    return IOSSecureStorage()
}
