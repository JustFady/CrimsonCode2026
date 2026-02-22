package org.crimsoncode2026.runtime

import platform.Foundation.NSUserDefaults

actual object RuntimeSessionStore {
    private const val KEY_ACCESS = "cc_runtime_access_token"
    private const val KEY_USER = "cc_runtime_user_id"
    private const val KEY_PHONE = "cc_runtime_phone_number"

    actual fun load(): PersistedSession {
        val d = NSUserDefaults.standardUserDefaults
        return PersistedSession(
            accessToken = d.stringForKey(KEY_ACCESS) ?: "",
            userId = d.stringForKey(KEY_USER) ?: "",
            phoneNumber = d.stringForKey(KEY_PHONE) ?: ""
        )
    }

    actual fun save(session: PersistedSession) {
        val d = NSUserDefaults.standardUserDefaults
        d.setObject(session.accessToken, forKey = KEY_ACCESS)
        d.setObject(session.userId, forKey = KEY_USER)
        d.setObject(session.phoneNumber, forKey = KEY_PHONE)
    }

    actual fun clear() {
        val d = NSUserDefaults.standardUserDefaults
        d.removeObjectForKey(KEY_ACCESS)
        d.removeObjectForKey(KEY_USER)
        d.removeObjectForKey(KEY_PHONE)
    }
}
