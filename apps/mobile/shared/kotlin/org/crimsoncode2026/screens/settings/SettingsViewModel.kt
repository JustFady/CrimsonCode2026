package org.crimsoncode2026.screens.settings

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.crimsoncode2026.storage.PreferencesStorage

/**
 * Settings ViewModel for user preferences state management.
 *
 * Manages:
 * - Notification preferences (master toggle, crisis, warning, private alerts, vibration)
 * - Location preferences (high precision mode)
 * - Public alert opt-out preference
 *
 * All preferences are stored locally on device via PreferencesStorage.
 * Defaults per spec: notifications=true, crisis=true, warning=true, private=true, vibration=true, highPrecision=false
 */
@Stable
class SettingsViewModel(
    private val preferencesStorage: PreferencesStorage,
    private val scope: CoroutineScope
) {
    // Private mutable state for each preference
    private val _notificationsEnabled = MutableStateFlow(true)
    private val _crisisAlertsEnabled = MutableStateFlow(true)
    private val _warningAlertsEnabled = MutableStateFlow(true)
    private val _privateAlertsEnabled = MutableStateFlow(true)
    private val _publicAlertOptOut = MutableStateFlow(false)
    private val _vibrationEnabled = MutableStateFlow(true)
    private val _highPrecisionLocation = MutableStateFlow(false)

    // Public read-only state flows
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    val crisisAlertsEnabled: StateFlow<Boolean> = _crisisAlertsEnabled.asStateFlow()
    val warningAlertsEnabled: StateFlow<Boolean> = _warningAlertsEnabled.asStateFlow()
    val privateAlertsEnabled: StateFlow<Boolean> = _privateAlertsEnabled.asStateFlow()
    val publicAlertOptOut: StateFlow<Boolean> = _publicAlertOptOut.asStateFlow()
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()
    val highPrecisionLocation: StateFlow<Boolean> = _highPrecisionLocation.asStateFlow()

    /**
     * Load all preferences from storage
     * Should be called when ViewModel is initialized
     */
    fun loadPreferences() {
        scope.launch {
            val prefs = preferencesStorage.getAllPreferences()
            _notificationsEnabled.value = prefs["notifications_enabled"] ?: true
            _crisisAlertsEnabled.value = prefs["crisis_alerts_enabled"] ?: true
            _warningAlertsEnabled.value = prefs["warning_alerts_enabled"] ?: true
            _privateAlertsEnabled.value = prefs["private_alerts_enabled"] ?: true
            _publicAlertOptOut.value = prefs["public_alert_opt_out"] ?: false
            _vibrationEnabled.value = prefs["vibration_enabled"] ?: true
            _highPrecisionLocation.value = prefs["high_precision_location"] ?: false
        }
    }

    /**
     * Toggle master notifications setting
     * @param enabled Whether notifications are enabled
     */
    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        scope.launch {
            preferencesStorage.setNotificationsEnabled(enabled)
        }
    }

    /**
     * Toggle crisis alerts setting
     * @param enabled Whether crisis alerts are enabled
     */
    fun toggleCrisisAlerts(enabled: Boolean) {
        _crisisAlertsEnabled.value = enabled
        scope.launch {
            preferencesStorage.setCrisisAlertsEnabled(enabled)
        }
    }

    /**
     * Toggle warning alerts setting
     * @param enabled Whether warning alerts are enabled
     */
    fun toggleWarningAlerts(enabled: Boolean) {
        _warningAlertsEnabled.value = enabled
        scope.launch {
            preferencesStorage.setWarningAlertsEnabled(enabled)
        }
    }

    /**
     * Toggle private alerts setting
     * @param enabled Whether private event alerts are enabled
     */
    fun togglePrivateAlerts(enabled: Boolean) {
        _privateAlertsEnabled.value = enabled
        scope.launch {
            preferencesStorage.setPrivateAlertsEnabled(enabled)
        }
    }

    /**
     * Toggle public alert opt-out setting
     * When true, user opts out of receiving all public alerts
     * @param optedOut Whether user has opted out of public alerts
     */
    fun togglePublicAlertOptOut(optedOut: Boolean) {
        _publicAlertOptOut.value = optedOut
        scope.launch {
            preferencesStorage.setPublicAlertOptOut(optedOut)
        }
    }

    /**
     * Toggle vibration setting for notifications
     * @param enabled Whether vibration is enabled for notifications
     */
    fun toggleVibration(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        scope.launch {
            preferencesStorage.setVibrationEnabled(enabled)
        }
    }

    /**
     * Toggle high precision location mode
     * When true, uses GPS for high accuracy at expense of battery
     * @param enabled Whether high precision location is enabled
     */
    fun toggleHighPrecisionLocation(enabled: Boolean) {
        _highPrecisionLocation.value = enabled
        scope.launch {
            preferencesStorage.setHighPrecisionLocation(enabled)
        }
    }
}
