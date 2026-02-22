package org.crimsoncode2026.storage

/**
 * User preferences storage interface
 *
 * Platform-specific implementation for storing user settings/preferences.
 * Settings are stored locally on device (not in database) as per spec.
 * - UserDefaults on iOS
 * - SharedPreferences/DataStore on Android
 *
 * Stored preferences:
 * - publicAlertOptOut: Boolean - User opted out of public alerts
 * - notificationsEnabled: Boolean - Master notification toggle
 * - crisisAlertsEnabled: Boolean - Crisis alert notifications
 * - warningAlertsEnabled: Boolean - Warning alert notifications
 * - privateAlertsEnabled: Boolean - Private event notifications
 * - vibrationEnabled: Boolean - Vibration for notifications
 * - highPrecisionLocation: Boolean - High precision location mode
 * - clearedEventIds: Set<String> - Event IDs user has cleared from their list (local device cache)
 */
interface PreferencesStorage {

    /**
     * Public alert opt-out preference
     * User can opt-out of receiving all public alerts.
     */
    suspend fun getPublicAlertOptOut(): Boolean
    suspend fun setPublicAlertOptOut(value: Boolean)

    /**
     * Master notification toggle
     * Enable/disable all push notifications.
     */
    suspend fun getNotificationsEnabled(): Boolean
    suspend fun setNotificationsEnabled(value: Boolean)

    /**
     * Crisis alerts enabled
     * High priority emergency notifications.
     */
    suspend fun getCrisisAlertsEnabled(): Boolean
    suspend fun setCrisisAlertsEnabled(value: Boolean)

    /**
     * Warning alerts enabled
     * Standard priority alert notifications.
     */
    suspend fun getWarningAlertsEnabled(): Boolean
    suspend fun setWarningAlertsEnabled(value: Boolean)

    /**
     * Private alerts enabled
     * Notifications for private emergency events.
     */
    suspend fun getPrivateAlertsEnabled(): Boolean
    suspend fun setPrivateAlertsEnabled(value: Boolean)

    /**
     * Vibration enabled
     * Vibrate on notification receipt.
     */
    suspend fun getVibrationEnabled(): Boolean
    suspend fun setVibrationEnabled(value: Boolean)

    /**
     * High precision location mode
     * Use GPS for high accuracy at expense of battery.
     */
    suspend fun getHighPrecisionLocation(): Boolean
    suspend fun setHighPrecisionLocation(value: Boolean)

    /**
     * Get all preferences as a map
     * Useful for debugging or bulk operations.
     */
    suspend fun getAllPreferences(): Map<String, Boolean>

    /**
     * Clear all preferences
     * Reset to default values.
     */
    suspend fun clearAll()

    /**
     * Get cleared event IDs
     * Returns set of event IDs that user has cleared from their list.
     */
    suspend fun getClearedEventIds(): Set<String>

    /**
     * Add an event ID to cleared events set
     */
    suspend fun addClearedEventId(eventId: String)

    /**
     * Remove an event ID from cleared events set
     * Useful if event needs to be re-shown (e.g., event was cleared in error).
     */
    suspend fun removeClearedEventId(eventId: String)

    /**
     * Clear all cleared event IDs
     * Used when user wants to see all events again.
     */
    suspend fun clearClearedEventIds()
}

/**
 * Factory function for creating PreferencesStorage
 * Platform-specific implementation provided by expect/actual
 */
expect fun createPreferencesStorage(): PreferencesStorage
