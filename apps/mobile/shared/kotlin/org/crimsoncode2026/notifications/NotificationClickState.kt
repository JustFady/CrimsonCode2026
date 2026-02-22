package org.crimsoncode2026.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Shared state for notification click events
 *
 * Provides a singleton-like mechanism for passing notification click
 * events from platform-specific code (MainActivity, iOSApp) to the
 * Compose App navigation layer.
 *
 * Android: MainActivity extracts data from intent extras
 * iOS: iOSApp handles userNotificationCenter callbacks
 */
object NotificationClickState {
    private val _notificationClickEvents = MutableSharedFlow<NotificationClickEvent>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val notificationClickEvents: SharedFlow<NotificationClickEvent> = _notificationClickEvents.asSharedFlow()

    /**
     * Emit a notification click event
     *
     * Called from platform-specific code when a notification is tapped.
     *
     * @param event The notification click event with event ID and deep link
     */
    fun emitNotificationClick(event: NotificationClickEvent) {
        _notificationClickEvents.tryEmit(event)
    }
}
