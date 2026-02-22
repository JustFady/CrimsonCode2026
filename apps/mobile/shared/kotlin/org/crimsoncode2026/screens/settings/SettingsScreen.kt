package org.crimsoncode2026.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Settings screen placeholder
 *
 * User settings and preferences will be implemented here.
 * Per spec, the following are stored locally:
 * - Notification preferences (master toggle, crisis, warning, private alerts, vibration)
 * - Location preferences (high precision mode)
 * - Account settings (display name, logout)
 *
 * Public alert opt-out is also stored locally per spec.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Settings - Coming Soon")
    }
}
