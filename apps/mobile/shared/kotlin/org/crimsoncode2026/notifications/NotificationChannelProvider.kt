package org.crimsoncode2026.notifications

/**
 * Platform-specific provider for notification channel configuration.
 * Android uses channels, iOS does not.
 */
expect object NotificationChannelProvider {
    /**
     * Get the notification channel ID for a severity level.
     *
     * On Android: Returns the appropriate channel ID for the severity.
     * On iOS: Returns empty string (iOS doesn't use channels).
     *
     * @param isCrisis True for CRISIS severity, false for ALERT
     * @return Channel ID (Android) or empty string (iOS)
     */
    fun getChannelId(isCrisis: Boolean): String
}
