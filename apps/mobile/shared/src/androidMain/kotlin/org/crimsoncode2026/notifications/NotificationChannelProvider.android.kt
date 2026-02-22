package org.crimsoncode2026.notifications

/**
 * Android implementation of NotificationChannelProvider
 */
actual object NotificationChannelProvider {
    /**
     * Get the notification channel ID for a severity level on Android.
     *
     * @param isCrisis True for CRISIS severity, false for ALERT
     * @return Channel ID to use for notification
     */
    actual fun getChannelId(isCrisis: Boolean): String {
        return NotificationChannelInitializer.getChannelId(isCrisis)
    }
}
