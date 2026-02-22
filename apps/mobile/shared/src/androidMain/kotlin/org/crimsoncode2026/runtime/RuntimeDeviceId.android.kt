package org.crimsoncode2026.runtime

import android.provider.Settings
import java.util.UUID

actual object RuntimeDeviceId {
    private val fallback = "android-" + UUID.randomUUID().toString()

    actual fun value(): String {
        val context = AndroidRuntimeBridge.appContext ?: return fallback
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty()

        if (androidId.isBlank() || androidId == "9774d56d682e549c") return fallback
        return "android-$androidId"
    }
}
