package org.crimsoncode2026.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiConnected
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.network.NetworkInfo
import org.crimsoncode2026.network.NetworkStatus

/**
 * Network status indicator banner
 *
 * Shows when network is offline or experiencing issues.
 * Provides retry action when network is available.
 */
@Composable
fun NetworkStatusBanner(
    networkInfo: NetworkInfo,
    isRetrying: Boolean = false,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !networkInfo.isOnline || isRetrying,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically(),
        modifier = modifier.fillMaxWidth()
    ) {
        val (message, icon, containerColor, contentColor) = when {
            isRetrying -> Triple(
                "Retrying...",
                Icons.Default.Refresh,
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer
            )
            !networkInfo.isOnline -> Triple(
                "No internet connection",
                Icons.Default.SignalWifiOff,
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
            else -> Triple(
                "Network issue",
                Icons.Default.CloudOff,
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRetrying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = contentColor
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }

                if (onRetry != null && !isRetrying) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = contentColor
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

/**
 * Small network status indicator icon
 *
 * Shows online/offline status with appropriate icon.
 */
@Composable
fun NetworkStatusIcon(
    networkInfo: NetworkInfo,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val icon = if (networkInfo.isOnline) {
            Icons.Default.SignalWifiConnected
        } else {
            Icons.Default.SignalWifiOff
        }
        val tint = if (networkInfo.isOnline) {
            Color(0xFF10B981) // Green for online
        } else {
            MaterialTheme.colorScheme.error
        }

        Icon(
            imageVector = icon,
            contentDescription = if (networkInfo.isOnline) "Online" else "Offline",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Retry action button with loading state
 *
 * For use in error cards where retry is needed.
 */
@Composable
fun RetryButton(
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Retry",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text("Retry")
    }
}

/**
 * Network status message for display in UI
 */
@Composable
fun NetworkStatusMessage(networkInfo: NetworkInfo): String {
    return when (networkInfo.status) {
        NetworkStatus.ONLINE -> "Connected"
        NetworkStatus.OFFLINE -> "No internet connection"
        NetworkStatus.UNKNOWN -> "Checking connection..."
    }
}
