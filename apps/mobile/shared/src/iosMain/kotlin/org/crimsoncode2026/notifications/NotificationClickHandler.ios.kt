package org.crimsoncode2026.notifications

import org.crimsoncode2026.data.Severity

/**
 * iOS implementation of NotificationClickHandler
 *
 * Emits notification clicks to NotificationClickState when user taps a notification.
 *
 * This is called from Swift code (iOSApp.swift) when a notification is tapped.
 * KmpNotifier handles the underlying notification display and delegate setup.
 */
actual object NotificationClickHandler {

    /**
     * Handle notification click with event data
     *
     * Called from Swift when a notification is tapped.
     * Emits the click event to NotificationClickState.
     *
     * @param eventId Event ID from the notification
     * @param severityStr Severity string (ALERT/CRISIS)
     * @param category Event category
     * @param deepLink Deep link URL
     */
    actual fun handleNotificationClick(
        eventId: String,
        severityStr: String?,
        category: String?,
        deepLink: String?
    ) {
        val severity = Severity.fromValue(severityStr) ?: Severity.ALERT
        val clickEvent = NotificationClickEvent(
            eventId = eventId,
            deepLinkUrl = deepLink ?: "crimsoncode://event/$eventId",
            severity = severity,
            category = category ?: "Unknown"
        )

        // Emit to shared state for App.kt to handle
        NotificationClickState.emitNotificationClick(clickEvent)
    }
}
