package org.crimsoncode2026.notifications

/**
 * iOS implementation of NotificationChannelProvider
 *
 * iOS doesn't use notification channels, so this returns an empty string.
 */
actual object NotificationChannelProvider {
    /**
     * Get the notification channel ID for a severity level on iOS.
     *
     * iOS doesn't use notification channels, returns empty string.
     *
     * @param isCrisis True for CRISIS severity, false for ALERT (unused on iOS)
     * @return Empty string (iOS doesn't use channels)
     */
    actual fun getChannelId(isCrisis: Boolean): String = ""
}
