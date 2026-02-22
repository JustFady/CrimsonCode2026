package org.crimsoncode2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import org.crimsoncode2026.auth.ContextProvider
import org.crimsoncode2026.notifications.NotificationChannelInitializer
import org.crimsoncode2026.notifications.NotificationClickEvent
import org.crimsoncode2026.notifications.NotificationClickState
import org.crimsoncode2026.data.Severity

class MainActivity : ComponentActivity() {

    companion object {
        /**
         * Payload data keys for notification data
         */
        private const val KEY_EVENT_ID = "event_id"
        private const val KEY_SEVERITY = "severity"
        private const val KEY_CATEGORY = "category"
        private const val KEY_DEEP_LINK = "deep_link"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ContextProvider for secure storage access
        ContextProvider.initialize(this)

        // Initialize Android notification channels
        NotificationChannelInitializer.initializeChannels(this)

        // Handle notification click from intent
        handleNotificationClickIntent(intent)

        enableEdgeToEdge()
        setContent {
            // Remove when https://issuetracker.google.com/issues/364713509 is fixed
            LaunchedEffect(isSystemInDarkTheme()) {
                enableEdgeToEdge()
            }
            App()
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        // Handle notification click from new intent
        handleNotificationClickIntent(intent)
    }

    /**
     * Handle notification click from intent
     *
     * Extracts event data from intent extras and emits to shared state.
     */
    private fun handleNotificationClickIntent(intent: android.content.Intent?) {
        if (intent == null || intent.extras == null) {
            return
        }

        val extras = intent.extras ?: return
        val eventId = extras.getString(KEY_EVENT_ID)

        if (eventId.isNullOrEmpty()) {
            return
        }

        val deepLink = extras.getString(KEY_DEEP_LINK)
        val severityStr = extras.getString(KEY_SEVERITY)
        val category = extras.getString(KEY_CATEGORY)

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
