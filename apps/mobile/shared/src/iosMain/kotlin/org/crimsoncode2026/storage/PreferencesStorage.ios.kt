package org.crimsoncode2026.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of PreferencesStorage using NSUserDefaults
 *
 * Stores user preferences locally on device.
 * Not encrypted like SecureStorage since these are user preferences, not sensitive data.
 */
actual class PreferencesStorage actual constructor() {

    private val userDefaults: NSUserDefaults by lazy {
        NSUserDefaults.standardUserDefaults
    }

    companion object {
        private const val KEY_PUBLIC_ALERT_OPT_OUT = "public_alert_opt_out"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_CRISIS_ALERTS_ENABLED = "crisis_alerts_enabled"
        private const val KEY_WARNING_ALERTS_ENABLED = "warning_alerts_enabled"
        private const val KEY_PRIVATE_ALERTS_ENABLED = "private_alerts_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_HIGH_PRECISION_LOCATION = "high_precision_location"
        private const val KEY_CLEARED_EVENT_IDS = "cleared_event_ids"
        private val json = Json { ignoreUnknownKeys = true }
    }

    actual override suspend fun getPublicAlertOptOut(): Boolean = withContext(Dispatchers.Default) {
        userDefaults.boolForKey(KEY_PUBLIC_ALERT_OPT_OUT)
    }

    actual override suspend fun setPublicAlertOptOut(value: Boolean) = withContext(Dispatchers.Default) {
        userDefaults.setBool(value, forKey = KEY_PUBLIC_ALERT_OPT_OUT)
    }

    actual override suspend fun getNotificationsEnabled(): Boolean = withContext(Dispatchers.Default) {
        userDefaults.boolForKey(KEY_NOTIFICATIONS_ENABLED)
    }

    actual override suspend fun setNotificationsEnabled(value: Boolean) = withContext(Dispatchers.Default) {
        userDefaults.setBool(value, forKey = KEY_NOTIFICATIONS_ENABLED)
    }

    actual override suspend fun getCrisisAlertsEnabled(): Boolean = withContext(Dispatchers.Default) {
        userDefaults.boolForKey(KEY_CRISIS_ALERTS_ENABLED)
    }

    actual override suspend fun setCrisisAlertsEnabled(value: Boolean) = withContext(Dispatchers.Default) {
        userDefaults.setBool(value, forKey = KEY_CRISIS_ALERTS_ENABLED)
    }

    actual override suspend fun getWarningAlertsEnabled(): Boolean = withContext(Dispatchers.Default) {
        userDefaults.boolForKey(KEY_WARNING_ALERTS_ENABLED)
    }

    actual override suspend fun setWarningAlertsEnabled(value: Boolean) = withContext(Dispatchers.Default) {
        userDefaults.setBool(value, forKey = KEY_WARNING_ALERTS_ENABLED)
    }

    actual override suspend fun getPrivateAlertsEnabled(): Boolean = withContext(Dispatchers.Default) {
        userDefaults.boolForKey(KEY_PRIVATE_ALERTS_ENABLED)
    }

    actual override suspend fun setPrivateAlertsEnabled(value: Boolean) = withContext(Dispatchers.Default) {
        userDefaults.setBool(value, forKey = KEY_PRIVATE_ALERTS_ENABLED)
    }

    actual override suspend fun getVibrationEnabled(): Boolean = withContext(Dispatchers.Default) {
        userDefaults.boolForKey(KEY_VIBRATION_ENABLED)
    }

    actual override suspend fun setVibrationEnabled(value: Boolean) = withContext(Dispatchers.Default) {
        userDefaults.setBool(value, forKey = KEY_VIBRATION_ENABLED)
    }

    actual override suspend fun getHighPrecisionLocation(): Boolean = withContext(Dispatchers.Default) {
        userDefaults.boolForKey(KEY_HIGH_PRECISION_LOCATION)
    }

    actual override suspend fun setHighPrecisionLocation(value: Boolean) = withContext(Dispatchers.Default) {
        userDefaults.setBool(value, forKey = KEY_HIGH_PRECISION_LOCATION)
    }

    actual override suspend fun getAllPreferences(): Map<String, Boolean> = withContext(Dispatchers.Default) {
        mapOf(
            KEY_PUBLIC_ALERT_OPT_OUT to userDefaults.boolForKey(KEY_PUBLIC_ALERT_OPT_OUT),
            KEY_NOTIFICATIONS_ENABLED to userDefaults.boolForKey(KEY_NOTIFICATIONS_ENABLED),
            KEY_CRISIS_ALERTS_ENABLED to userDefaults.boolForKey(KEY_CRISIS_ALERTS_ENABLED),
            KEY_WARNING_ALERTS_ENABLED to userDefaults.boolForKey(KEY_WARNING_ALERTS_ENABLED),
            KEY_PRIVATE_ALERTS_ENABLED to userDefaults.boolForKey(KEY_PRIVATE_ALERTS_ENABLED),
            KEY_VIBRATION_ENABLED to userDefaults.boolForKey(KEY_VIBRATION_ENABLED),
            KEY_HIGH_PRECISION_LOCATION to userDefaults.boolForKey(KEY_HIGH_PRECISION_LOCATION)
        )
    }

    actual override suspend fun clearAll() = withContext(Dispatchers.Default) {
        userDefaults.synchronize()
        val keys = listOf(
            KEY_PUBLIC_ALERT_OPT_OUT,
            KEY_NOTIFICATIONS_ENABLED,
            KEY_CRISIS_ALERTS_ENABLED,
            KEY_WARNING_ALERTS_ENABLED,
            KEY_PRIVATE_ALERTS_ENABLED,
            KEY_VIBRATION_ENABLED,
            KEY_HIGH_PRECISION_LOCATION,
            KEY_CLEARED_EVENT_IDS
        )
        keys.forEach { key ->
            userDefaults.removeObjectForKey(key)
        }
    }

    actual override suspend fun getClearedEventIds(): Set<String> = withContext(Dispatchers.Default) {
        val clearedIdsJson = userDefaults.stringForKey(KEY_CLEARED_EVENT_IDS)
        if (clearedIdsJson != null) {
            try {
                json.decodeFromString<List<String>>(clearedIdsJson).toSet()
            } catch (e: Exception) {
                emptySet()
            }
        } else {
            emptySet()
        }
    }

    actual override suspend fun addClearedEventId(eventId: String) = withContext(Dispatchers.Default) {
        val currentIds = getClearedEventIds().toMutableSet()
        currentIds.add(eventId)
        userDefaults.setObject(json.encodeToString(currentIds.toList()), forKey = KEY_CLEARED_EVENT_IDS)
    }

    actual override suspend fun removeClearedEventId(eventId: String) = withContext(Dispatchers.Default) {
        val currentIds = getClearedEventIds().toMutableSet()
        currentIds.remove(eventId)
        userDefaults.setObject(json.encodeToString(currentIds.toList()), forKey = KEY_CLEARED_EVENT_IDS)
    }

    actual override suspend fun clearClearedEventIds() = withContext(Dispatchers.Default) {
        userDefaults.removeObjectForKey(KEY_CLEARED_EVENT_IDS)
    }
}

/**
 * iOS implementation of createPreferencesStorage factory
 */
actual fun createPreferencesStorage(): PreferencesStorage {
    return PreferencesStorage()
}
