package org.crimsoncode2026.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.crimsoncode2026.auth.ContextProvider

/**
 * Android implementation of PreferencesStorage using SharedPreferences
 *
 * Stores user preferences locally on device.
 * Not encrypted like SecureStorage since these are user preferences, not sensitive data.
 */
class AndroidPreferencesStorage() : org.crimsoncode2026.storage.PreferencesStorage {

    private val prefs by lazy {
        ContextProvider.getContext().getSharedPreferences(
            "user_preferences",
            Context.MODE_PRIVATE
        )
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

    override suspend fun getPublicAlertOptOut(): Boolean = withContext(Dispatchers.Default) {
        prefs.getBoolean(KEY_PUBLIC_ALERT_OPT_OUT, false)
    }

    override suspend fun setPublicAlertOptOut(value: Boolean) = withContext(Dispatchers.Default) {
        prefs.edit().putBoolean(KEY_PUBLIC_ALERT_OPT_OUT, value).apply()
    }

    override suspend fun getNotificationsEnabled(): Boolean = withContext(Dispatchers.Default) {
        prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    override suspend fun setNotificationsEnabled(value: Boolean) = withContext(Dispatchers.Default) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()
    }

    override suspend fun getCrisisAlertsEnabled(): Boolean = withContext(Dispatchers.Default) {
        prefs.getBoolean(KEY_CRISIS_ALERTS_ENABLED, true)
    }

    override suspend fun setCrisisAlertsEnabled(value: Boolean) = withContext(Dispatchers.Default) {
        prefs.edit().putBoolean(KEY_CRISIS_ALERTS_ENABLED, value).apply()
    }

    override suspend fun getWarningAlertsEnabled(): Boolean = withContext(Dispatchers.Default) {
        prefs.getBoolean(KEY_WARNING_ALERTS_ENABLED, true)
    }

    override suspend fun setWarningAlertsEnabled(value: Boolean) = withContext(Dispatchers.Default) {
        prefs.edit().putBoolean(KEY_WARNING_ALERTS_ENABLED, value).apply()
    }

    override suspend fun getPrivateAlertsEnabled(): Boolean = withContext(Dispatchers.Default) {
        prefs.getBoolean(KEY_PRIVATE_ALERTS_ENABLED, true)
    }

    override suspend fun setPrivateAlertsEnabled(value: Boolean) = withContext(Dispatchers.Default) {
        prefs.edit().putBoolean(KEY_PRIVATE_ALERTS_ENABLED, value).apply()
    }

    override suspend fun getVibrationEnabled(): Boolean = withContext(Dispatchers.Default) {
        prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
    }

    override suspend fun setVibrationEnabled(value: Boolean) = withContext(Dispatchers.Default) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()
    }

    override suspend fun getHighPrecisionLocation(): Boolean = withContext(Dispatchers.Default) {
        prefs.getBoolean(KEY_HIGH_PRECISION_LOCATION, false)
    }

    override suspend fun setHighPrecisionLocation(value: Boolean) = withContext(Dispatchers.Default) {
        prefs.edit().putBoolean(KEY_HIGH_PRECISION_LOCATION, value).apply()
    }

    override suspend fun getAllPreferences(): Map<String, Boolean> = withContext(Dispatchers.Default) {
        mapOf(
            KEY_PUBLIC_ALERT_OPT_OUT to prefs.getBoolean(KEY_PUBLIC_ALERT_OPT_OUT, false),
            KEY_NOTIFICATIONS_ENABLED to prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true),
            KEY_CRISIS_ALERTS_ENABLED to prefs.getBoolean(KEY_CRISIS_ALERTS_ENABLED, true),
            KEY_WARNING_ALERTS_ENABLED to prefs.getBoolean(KEY_WARNING_ALERTS_ENABLED, true),
            KEY_PRIVATE_ALERTS_ENABLED to prefs.getBoolean(KEY_PRIVATE_ALERTS_ENABLED, true),
            KEY_VIBRATION_ENABLED to prefs.getBoolean(KEY_VIBRATION_ENABLED, true),
            KEY_HIGH_PRECISION_LOCATION to prefs.getBoolean(KEY_HIGH_PRECISION_LOCATION, false)
        )
    }

    override suspend fun clearAll() = withContext(Dispatchers.Default) {
        prefs.edit().clear().apply()
    }

    override suspend fun getClearedEventIds(): Set<String> = withContext(Dispatchers.Default) {
        val clearedIdsJson = prefs.getString(KEY_CLEARED_EVENT_IDS, null)
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

    override suspend fun addClearedEventId(eventId: String) = withContext(Dispatchers.Default) {
        val currentIds = getClearedEventIds().toMutableSet()
        currentIds.add(eventId)
        prefs.edit()
            .putString(KEY_CLEARED_EVENT_IDS, json.encodeToString(currentIds.toList()))
            .apply()
    }

    override suspend fun removeClearedEventId(eventId: String) = withContext(Dispatchers.Default) {
        val currentIds = getClearedEventIds().toMutableSet()
        currentIds.remove(eventId)
        prefs.edit()
            .putString(KEY_CLEARED_EVENT_IDS, json.encodeToString(currentIds.toList()))
            .apply()
    }

    override suspend fun clearClearedEventIds() = withContext(Dispatchers.Default) {
        prefs.edit().remove(KEY_CLEARED_EVENT_IDS).apply()
    }
}

/**
 * Android implementation of createPreferencesStorage factory
 */
actual fun createPreferencesStorage(): PreferencesStorage {
    return AndroidPreferencesStorage()
}
