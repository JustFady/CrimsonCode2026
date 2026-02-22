package org.crimsoncode2026.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rick.com.lib.kksafe.KSafe
import rick.com.lib.kksafe.createKSafe
import platform.Foundation.NSUUID

/**
 * iOS implementation of SecureStorage using KSafe
 *
 * Provides encrypted key-value storage backed by iOS Keychain.
 */
actual class SecureStorage actual constructor() {

    private val ksafe: KSafe by lazy {
        createKSafe(
            fileName = "secure_storage",
        )
    }

    actual override suspend fun getString(key: String): String? = withContext(Dispatchers.Default) {
        return@withContext ksafe.getString(key)
    }

    actual override suspend fun putString(key: String, value: String) = withContext(Dispatchers.Default) {
        ksafe.putString(key, value)
    }

    actual override suspend fun remove(key: String) = withContext(Dispatchers.Default) {
        ksafe.remove(key)
    }

    actual override suspend fun clear() = withContext(Dispatchers.Default) {
        ksafe.clear()
    }
}

/**
 * iOS implementation of createSecureStorage factory
 */
actual fun createSecureStorage(): SecureStorage {
    return SecureStorage()
}
