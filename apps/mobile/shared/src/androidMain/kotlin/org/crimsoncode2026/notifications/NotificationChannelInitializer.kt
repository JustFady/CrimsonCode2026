package org.crimsoncode2026.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

/**
 * Android Notification Channel Initializer
 *
 * Creates notification channels required by the app.
 * Channels must be created before showing any notifications on Android 8.0+.
 *
 * Spec requirements:
 * - Emergency Alerts: High importance
 * - Alerts: Default importance
 */
object NotificationChannelInitializer {

    /**
     * Channel IDs - must be unique and constant
     */
    const val CHANNEL_EMERGENCY_ALERTS = "emergency_alerts"
    const val CHANNEL_ALERTS = "alerts"

    /**
     * Light colors for severity-based styling
     */
    private const val COLOR_CRISIS = 0xFFFF0000.toInt() // Red
    private const val COLOR_ALERT = 0xFFFFA500.toInt() // Orange

    /**
     * Vibration patterns
     *
     * Crisis: Aggressive pattern - short, short, short, long repeat
     * Alert: Standard pattern - single long vibration
     */
    private val VIBRATION_PATTERN_CRISIS = longArrayOf(0, 100, 100, 100, 100, 500)
    private val VIBRATION_PATTERN_ALERT = longArrayOf(0, 500)

    /**
     * Channel names (shown to users in Settings)
     */
    private const val CHANNEL_NAME_EMERGENCY_ALERTS = "Emergency Alerts"
    private const val CHANNEL_NAME_ALERTS = "Alerts"

    /**
     * Channel descriptions (shown to users in Settings)
     */
    private const val CHANNEL_DESC_EMERGENCY_ALERTS = "Critical emergency notifications requiring immediate attention"
    private const val CHANNEL_DESC_ALERTS = "Standard emergency alerts and warnings"

    /**
     * Initialize all notification channels
     *
     * This should be called once during app startup, typically in MainActivity.onCreate().
     * Channels are created on Android 8.0+ (API 26+). On earlier versions,
     * this method safely does nothing.
     *
     * @param context Application context
     */
    fun initializeChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = context.getSystemService<NotificationManager>()
            ?: return

        // Create Emergency Alerts channel (High importance)
        createEmergencyAlertsChannel(notificationManager)

        // Create Alerts channel (Default importance)
        createAlertsChannel(notificationManager)
    }

    /**
     * Create Emergency Alerts channel
     *
     * Importance: HIGH
     * - Makes sound and appears on screen
     * - Can interrupt user with sound/vibration
     * - Red light color for visual indicator
     * - Aggressive vibration pattern for urgency
     */
    private fun createEmergencyAlertsChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_EMERGENCY_ALERTS,
            CHANNEL_NAME_EMERGENCY_ALERTS,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC_EMERGENCY_ALERTS
            enableLights(true)
            lightColor = COLOR_CRISIS
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN_CRISIS
        }

        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create Alerts channel
     *
     * Importance: DEFAULT
     * - Makes sound but does not visually interrupt
     * - Shows in notification shade
     * - Orange light color for visual indicator
     * - Standard vibration pattern
     */
    private fun createAlertsChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ALERTS,
            CHANNEL_NAME_ALERTS,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC_ALERTS
            enableLights(true)
            lightColor = COLOR_ALERT
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN_ALERT
        }

        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Get the appropriate channel ID for a severity level
     *
     * @param isCrisis True for CRISIS severity, false for ALERT
     * @return Channel ID to use for notification
     */
    fun getChannelId(isCrisis: Boolean): String {
        return if (isCrisis) CHANNEL_EMERGENCY_ALERTS else CHANNEL_ALERTS
    }
}
