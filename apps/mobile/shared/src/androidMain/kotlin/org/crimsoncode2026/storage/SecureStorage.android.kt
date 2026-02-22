package org.crimsoncode2026.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.crimsoncode2026.auth.ContextProvider
import rick.com.lib.kksafe.KSafe
import rick.com.lib.kksafe.createKSafe

/**
 * Android implementation of SecureStorage using KSafe
 *
 * Provides encrypted key-value storage backed by Android's EncryptedSharedPreferences.
 */
actual class SecureStorage actual constructor(private val context: Context) {

    private val ksafe: KSafe by lazy {
        createKSafe(
            context = context,
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

    companion object {
        /**
         * Factory method to create SecureStorage using ContextProvider
         * Convenience method for DI setup
         */
        fun create(): SecureStorage {
            return SecureStorage(ContextProvider.getContext())
        }
    }
}

/**
 * Android implementation of createSecureStorage factory
 */
actual fun createSecureStorage(): SecureStorage {
    return SecureStorage.create()
}
