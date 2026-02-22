package org.crimsoncode2026.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import maplibre.compose.LngLat
import maplibre.compose.MapLibreMap

/**
 * User location marker with accuracy circle visualization.
 *
 * Displays:
 * - Blue transparent circle representing GPS accuracy
 * - Central user position indicator (location pin)
 * - Circle size dynamically scales with accuracy reading
 *
 * Accuracy circle color and size based on accuracy level:
 * - Green: < 10m (high precision)
 * - Yellow: 10-50m (good precision)
 * - Orange: 50-100m (moderate precision)
 * - Red: > 100m (low precision)
 *
 * @param latitude User's current latitude
 * @param longitude User's current longitude
 * @param accuracyInMeters GPS accuracy in meters (null if unavailable)
 * @param modifier Modifier for the marker container
 */
@Composable
fun UserLocationMarker(
    latitude: Double,
    longitude: Double,
    accuracyInMeters: Double?,
    modifier: Modifier = Modifier
) {
    val accuracyColor = when {
        accuracyInMeters == null -> MaterialTheme.colorScheme.outline
        accuracyInMeters < 10.0 -> Color(0xFF4CAF50) // Green
        accuracyInMeters < 50.0 -> Color(0xFFFFC107) // Yellow
        accuracyInMeters < 100.0 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }

    // Convert accuracy in meters to approximate screen pixels
    // This is a rough approximation for visualization purposes
    // In a production app, this should use MapLibre's projection APIs
    val accuracyRadiusDp = when (accuracyInMeters) {
        null -> 0.dp
        else -> (accuracyInMeters / 2.0).coerceIn(10.0, 150.0).dp
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Accuracy circle (outer ring)
        if (accuracyInMeters != null && accuracyInMeters > 0) {
            Box(
                modifier = Modifier
                    .size(accuracyRadiusDp * 2)
                    .clip(CircleShape)
                    .background(
                        color = accuracyColor.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            )
        }

        // Inner accuracy ring
        if (accuracyInMeters != null && accuracyInMeters > 0) {
            Box(
                modifier = Modifier
                    .size(accuracyRadiusDp * 2)
                    .clip(CircleShape)
                    .background(
                        color = Color.Transparent,
                        shape = CircleShape
                    )
            )
        }

        // Center user position indicator
        Box(
            modifier = Modifier
                .offset(x = 0.dp, y = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            UserLocationDot(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Center dot indicator for user location.
 *
 * Displays a pulsing location icon at the center of the accuracy circle.
 *
 * @param color Color for the location indicator
 * @param size Size of the indicator (default 24dp)
 */
@Composable
private fun UserLocationDot(
    color: Color,
    size: Dp = 24.dp
) {
    Icon(
        imageVector = Icons.Default.MyLocation,
        contentDescription = "Your location",
        tint = color,
        modifier = Modifier.size(size)
    )
}

/**
 * Converts MapLibre LngLat to latitude/longitude pair.
 *
 * @param lngLat MapLibre coordinate
 * @return Pair of (latitude, longitude)
 */
fun lngLatToCoordinates(lngLat: maplibre.compose.LatLng): Pair<Double, Double> {
    return lngLat.latitude to lngLat.longitude
}

/**
 * Converts latitude/longitude to MapLibre LngLat.
 *
 * @param latitude Geographic latitude
 * @param longitude Geographic longitude
 * @return MapLibre LatLng coordinate
 */
fun coordinatesToLngLat(latitude: Double, longitude: Double): maplibre.compose.LatLng {
    return maplibre.compose.LatLng(latitude, longitude)
}

/**
 * Gets accuracy color based on GPS accuracy reading.
 *
 * @param accuracyInMeters GPS accuracy in meters (null if unavailable)
 * @return Color corresponding to accuracy level
 */
fun getAccuracyColor(accuracyInMeters: Double?): Color {
    return when {
        accuracyInMeters == null -> MaterialTheme.colorScheme.outline
        accuracyInMeters < 10.0 -> Color(0xFF4CAF50) // Green
        accuracyInMeters < 50.0 -> Color(0xFFFFC107) // Yellow
        accuracyInMeters < 100.0 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

/**
 * Gets accuracy label for display.
 *
 * @param accuracyInMeters GPS accuracy in meters (null if unavailable)
 * @return Human-readable accuracy description
 */
fun getAccuracyLabel(accuracyInMeters: Double?): String {
    return when {
        accuracyInMeters == null -> "Locating..."
        accuracyInMeters < 10.0 -> "High precision"
        accuracyInMeters < 50.0 -> "Good"
        accuracyInMeters < 100.0 -> "Moderate"
        else -> "Low precision"
    }
}
