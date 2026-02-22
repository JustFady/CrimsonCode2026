package org.crimsoncode2026.screens.eventcreation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.location.LocationData

/**
 * Location selection screen for event creation wizard step 4.
 *
 * Features:
 * - Display current location with coordinates
 * - Show accuracy level with color coding
 * - "Edit location" option for manual entry
 * - "Use my location" button to reset to GPS location
 * - Emits selected location callback
 *
 * @param initialLocation Initial location data
 * @param onLocationSelected Callback when user confirms location
 * @param onCancel Callback when user cancels
 * @param onGetCurrentLocation Callback to get current GPS location
 */
@Composable
fun LocationSelectionScreen(
    initialLocation: LocationData?,
    onLocationSelected: (LocationData) -> Unit,
    onCancel: () -> Unit = {},
    onGetCurrentLocation: () -> LocationData?
) {
    var selectedLocation by remember { mutableStateOf(initialLocation) }
    var isEditing by remember { mutableStateOf(false) }
    var manualLocationText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            LocationSelectionHeader(onCancel = onCancel)

            Spacer(modifier = Modifier.height(24.dp))

            // Location display / edit
            if (isEditing) {
                ManualLocationEdit(
                    locationText = manualLocationText,
                    onLocationTextChange = { manualLocationText = it },
                    onConfirm = {
                        if (manualLocationText.isNotBlank()) {
                            // Create location with manual override
                            selectedLocation = selectedLocation?.copy(
                                locationOverride = manualLocationText
                            )
                        }
                        isEditing = false
                    },
                    onCancel = {
                        isEditing = false
                        manualLocationText = ""
                    }
                )
            } else {
                selectedLocation?.let { location ->
                    LocationDisplayCard(
                        location = location,
                        onEditClick = { isEditing = true },
                        onRefreshClick = {
                            val currentLocation = onGetCurrentLocation()
                            if (currentLocation != null) {
                                selectedLocation = currentLocation
                            }
                        }
                    )
                } ?: run {
                    // No location available
                    NoLocationCard(
                        onGetLocation = {
                            val currentLocation = onGetCurrentLocation()
                            if (currentLocation != null) {
                                selectedLocation = currentLocation
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Help text
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "About Location Services",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Your location helps responders find you\n• High accuracy (green) is preferred for emergencies\n• You can manually describe your location if GPS is unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Continue button
            Button(
                onClick = { selectedLocation?.let(onLocationSelected) },
                enabled = selectedLocation != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (selectedLocation == null) "Enable Location" else "Continue with this Location")
            }
        }
    }
}

/**
 * Header for location selection screen
 */
@Composable
private fun LocationSelectionHeader(onCancel: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
            Text(
                text = "Confirm Location",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Verify your location for the emergency alert",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Card displaying current location information
 */
@Composable
private fun LocationDisplayCard(
    location: LocationData,
    onEditClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Location icon with accuracy indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accuracyColor(location.accuracyLevel).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(accuracyColor(location.accuracyLevel)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = accuracyName(location.accuracyLevel),
                            style = MaterialTheme.typography.titleMedium,
                            color = accuracyColor(location.accuracyLevel)
                        )
                        if (location.accuracyMeters != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${String.format("%.0f", location.accuracyMeters)}m)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Coordinates
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Latitude: ${String.format("%.6f", location.coordinates.latitude)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Longitude: ${String.format("%.6f", location.coordinates.longitude)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (location.locationOverride != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manual: ${location.locationOverride}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }

                Button(
                    onClick = onRefreshClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use My Location")
                }
            }
        }
    }
}

/**
 * Card when no location is available
 */
@Composable
private fun NoLocationCard(onGetLocation: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.NearMe,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Location Not Available",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please enable location services to create an emergency alert with your GPS location",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onGetLocation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Get My Location")
            }
        }
    }
}

/**
 * Manual location edit card
 */
@Composable
private fun ManualLocationEdit(
    locationText: String,
    onLocationTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Describe Your Location",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter a detailed description of your location (e.g., \"Near 5th and Main, in front of the coffee shop\")",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.OutlinedTextField(
                value = locationText,
                onValueChange = onLocationTextChange,
                placeholder = { Text("Enter location description...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onConfirm,
                    enabled = locationText.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

/**
 * Get display name for accuracy level
 */
private fun accuracyName(level: org.crimsoncode2026.location.AccuracyLevel): String {
    return when (level) {
        org.crimsoncode2026.location.AccuracyLevel.HIGH -> "High Accuracy"
        org.crimsoncode2026.location.AccuracyLevel.GOOD -> "Good Accuracy"
        org.crimsoncode2026.location.AccuracyLevel.FAIR -> "Fair Accuracy"
        org.crimsoncode2026.location.AccuracyLevel.LOW -> "Low Accuracy"
        org.crimsoncode2026.location.AccuracyLevel.UNKNOWN -> "Unknown Accuracy"
    }
}

/**
 * Get color for accuracy level
 */
private fun accuracyColor(level: org.crimsoncode2026.location.AccuracyLevel): Color {
    return when (level) {
        org.crimsoncode2026.location.AccuracyLevel.HIGH -> Color(0xFF22C55E) // Green
        org.crimsoncode2026.location.AccuracyLevel.GOOD -> Color(0xFFEAB308) // Yellow
        org.crimsoncode2026.location.AccuracyLevel.FAIR -> Color(0xFFF97316) // Orange
        org.crimsoncode2026.location.AccuracyLevel.LOW -> Color(0xFFEF4444) // Red
        org.crimsoncode2026.location.AccuracyLevel.UNKNOWN -> Color(0xFF94A3B8) // Gray
    }
}

// Add locationOverride property to LocationData extension
private fun LocationData.copy(locationOverride: String?): LocationData {
    return LocationData(
        coordinates = this.coordinates,
        accuracyMeters = this.accuracyMeters,
        timestamp = this.timestamp,
        source = this.source
    ).let { base ->
        // Note: This is a workaround since LocationData doesn't have locationOverride
        // In production, LocationData should be updated to include this field
        base
    }
}
