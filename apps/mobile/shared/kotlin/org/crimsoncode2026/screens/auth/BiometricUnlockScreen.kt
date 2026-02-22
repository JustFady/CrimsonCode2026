package org.crimsoncode2026.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.auth.BiometricAuthManager
import org.crimsoncode2026.auth.BiometricResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Biometric unlock screen
 * Prompts user for biometric authentication (fingerprint, face ID, etc.)
 * On success, proceeds to main app
 *
 * @param onUnlockSuccess Callback when biometric authentication succeeds
 * @param onLogout Callback when user chooses to logout (use phone login instead)
 */
@Composable
fun BiometricUnlockScreen(
    onUnlockSuccess: () -> Unit,
    onLogout: () -> Unit
) {
    var isAuthenticating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val biometricAuthManager: BiometricAuthManager by inject()

    // Auto-trigger biometric prompt on screen load
    LaunchedEffect(Unit) {
        isAuthenticating = true
        val result = biometricAuthManager.authenticate(
            requestTitle = "Unlock Emergency Response",
            requestReason = "Authenticate to access the app",
            failureButtonText = "Cancel",
            allowDeviceCredentials = true
        )

        when (result) {
            is BiometricResult.Success -> {
                onUnlockSuccess()
            }
            is BiometricResult.Failed -> {
                isAuthenticating = false
                error = "Biometric authentication failed"
            }
            is BiometricResult.Error -> {
                isAuthenticating = false
                error = result.message
            }
            is BiometricResult.NotAvailable -> {
                isAuthenticating = false
                error = "Biometric authentication not available"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = rememberBiometricIcon(),
                contentDescription = "Biometric",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isAuthenticating) "Authenticating..." else "Unlock with Biometrics",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Use your fingerprint or face to unlock",
                style = MaterialTheme.typography.bodyLarge
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isAuthenticating) {
                CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedButton(
                onClick = onLogout,
                enabled = !isAuthenticating
            ) {
                Text("Use Phone Number Instead")
            }
        }
    }
}

/**
 * Get appropriate biometric icon based on available biometric type
 * This is a placeholder - actual implementation would check BiometryType
 */
@Composable
private fun rememberBiometricIcon(): androidx.compose.ui.graphics.vector.ImageVector {
    // Placeholder - in production, check BiometricAvailability.BiometryType
    // For now, returning a generic icon
    return androidx.compose.material.icons.Icons.Default.Fingerprint
}
