package org.crimsoncode2026.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.crimsoncode2026.auth.ContextProvider
import eu.anifantakis.lib.ksafe.KSafe

/**
 * Android implementation of SecureStorage using KSafe
 *
 * Provides encrypted key-value storage backed by Android's EncryptedSharedPreferences.
 */
class AndroidSecureStorage(private val context: Context) : org.crimsoncode2026.storage.SecureStorage {

    private val ksafe: KSafe by lazy {
        KSafe(
            context = context,
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

    companion object {
        /**
         * Factory method to create SecureStorage using ContextProvider
         * Convenience method for DI setup
         */
        fun create(): org.crimsoncode2026.storage.SecureStorage {
            return AndroidSecureStorage(ContextProvider.getContext())
        }
    }
}

/**
 * Android implementation of createSecureStorage factory
 */
actual fun createSecureStorage(): SecureStorage {
    return AndroidSecureStorage.create()
}
