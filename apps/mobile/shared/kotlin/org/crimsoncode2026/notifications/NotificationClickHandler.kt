package org.crimsoncode2026.notifications

/**
 * Notification click handler
 *
 * Platform-specific implementation for handling notification clicks.
 *
 * Android: MainActivity extracts data from intent extras
 * iOS: Called from Swift delegate when user taps notification
 */
expect object NotificationClickHandler {

    /**
     * Handle notification click with event data
     *
     * Called when a notification is tapped by the user.
     * Extracts event ID and other metadata from the notification.
     *
     * @param eventId Event ID from the notification
     * @param severityStr Severity string (ALERT/CRISIS)
     * @param category Event category
     * @param deepLink Deep link URL
     */
    fun handleNotificationClick(
        eventId: String,
        severityStr: String?,
        category: String?,
        deepLink: String?
    )
}
