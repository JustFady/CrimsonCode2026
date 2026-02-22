package org.crimsoncode2026.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import maplibre.compose.CameraPosition

/**
 * Map controls overlay with zoom and center buttons.
 *
 * Provides:
 * - Zoom in button
 * - Zoom out button
 * - Center on user location button
 * - Positioned at bottom-right corner by default
 *
 * @param onZoomIn Callback when zoom in is pressed
 * @param onZoomOut Callback when zoom out is pressed
 * @param onCenterUser Callback when center user button is pressed
 * @param modifier Modifier for the controls container
 * @param showCenterButton Whether to show center button (default: true)
 */
@Composable
fun MapControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCenterUser: () -> Unit,
    modifier: Modifier = Modifier,
    showCenterButton: Boolean = true
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zoom out button
            MapControlButton(
                onClick = onZoomOut,
                icon = Icons.Default.Remove,
                contentDescription = "Zoom out"
            )

            // Zoom in button
            MapControlButton(
                onClick = onZoomIn,
                icon = Icons.Default.Add,
                contentDescription = "Zoom in"
            )

            if (showCenterButton) {
                Spacer(modifier = Modifier.width(4.dp))

                // Center on user location button
                MapControlButton(
                    onClick = onCenterUser,
                    icon = Icons.Default.CenterFocusStrong,
                    contentDescription = "Center on your location"
                )
            }
        }
    }
}

/**
 * Individual map control button.
 *
 * Circular icon button with Material3 styling for map controls.
 *
 * @param onClick Callback when button is pressed
 * @param icon Icon to display
 * @param contentDescription Accessibility description
 * @param modifier Modifier for the button
 */
@Composable
private fun MapControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Map interaction configuration.
 *
 * Defines how users can interact with the map.
 *
 * @property enableZoom Allow pinch and button zoom
 * @property enablePan Allow drag to pan the map
 * @property enableRotate Allow two-finger rotation
 * @property enablePitch Allow two-finger tilt
 * @property enableTilt Allow map tilt gesture
 */
data class MapInteractionConfig(
    val enableZoom: Boolean = true,
    val enablePan: Boolean = true,
    val enableRotate: Boolean = true,
    val enablePitch: Boolean = false,
    val enableTilt: Boolean = false
)

/**
 * Default map interaction settings.
 */
val DefaultMapInteraction = MapInteractionConfig(
    enableZoom = true,
    enablePan = true,
    enableRotate = false,
    enablePitch = false,
    enableTilt = false
)

/**
 * Map camera bounds configuration.
 *
 * Defines limits for camera movement.
 *
 * @property minZoom Minimum zoom level allowed
 * @property maxZoom Maximum zoom level allowed
 * @property minLatitude Minimum latitude boundary (optional)
 * @property maxLatitude Maximum latitude boundary (optional)
 * @property minLongitude Minimum longitude boundary (optional)
 * @property maxLongitude Maximum longitude boundary (optional)
 */
data class MapCameraBounds(
    val minZoom: Double = 3.0,
    val maxZoom: Double = 18.0,
    val minLatitude: Double? = null,
    val maxLatitude: Double? = null,
    val minLongitude: Double? = null,
    val maxLongitude: Double? = null
)

/**
 * Default camera bounds for USA region.
 */
val UsaCameraBounds = MapCameraBounds(
    minZoom = 3.0,
    maxZoom = 18.0,
    minLatitude = 15.0,
    maxLatitude = 55.0,
    minLongitude = -125.0,
    maxLongitude = -65.0
)

/**
 * Camera animation duration configuration.
 *
 * @property zoomIn Duration for zoom in animation
 * @property zoomOut Duration for zoom out animation
 * @property pan Duration for pan animation
 * @property centerUser Duration for center on user animation
 */
data class CameraAnimationDuration(
    val zoomIn: Long = 300L,
    val zoomOut: Long = 300L,
    val pan: Long = 500L,
    val centerUser: Long = 800L
)

/**
 * Default camera animation durations in milliseconds.
 */
val DefaultCameraAnimation = CameraAnimationDuration(
    zoomIn = 300L,
    zoomOut = 300L,
    pan = 500L,
    centerUser = 800L
)

/**
 * Calculate new zoom level with bounds checking.
 *
 * @param currentZoom Current zoom level
 * @param delta Zoom change amount (positive = zoom in, negative = zoom out)
 * @param bounds Camera bounds with min/max zoom
 * @return New zoom level within bounds
 */
fun calculateNewZoom(
    currentZoom: Double,
    delta: Double,
    bounds: MapCameraBounds = UsaCameraBounds
): Double {
    return (currentZoom + delta)
        .coerceIn(bounds.minZoom, bounds.maxZoom)
}

/**
 * Standard zoom increment/decrement step.
 */
const val ZOOM_STEP = 1.0

/**
 * Minimum zoom level (world view).
 */
const val MIN_ZOOM = 3.0

/**
 * Maximum zoom level (street level).
 */
const val MAX_ZOOM = 18.0

/**
 * Default zoom level (city/regional view).
 */
const val DEFAULT_ZOOM = 10.0
