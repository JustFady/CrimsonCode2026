package org.crimsoncode2026.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import maplibre.compose.CameraPosition
import maplibre.compose.MapLibreMap
import maplibre.compose.rememberCameraPositionState
import maplibre.compose.rememberMapLibreStyle

/**
 * OpenStreetMap map view using MapLibre Compose.
 *
 * Features:
 * - OpenStreetMap tiles (no API key required)
 * - Configurable camera position
 * - Map gestures enabled (zoom, pan, rotate)
 * - Optional content slot for markers and other map overlays
 *
 * @param modifier Modifier for map view
 * @param initialZoom Initial zoom level (default: 10.0)
 * @param initialLatitude Initial center latitude (default: 39.8283 - approximate USA center)
 * @param initialLongitude Initial center longitude (default: -98.5795 - approximate USA center)
 * @param onMapReady Callback when map is fully loaded and ready for interaction
 * @param onCameraChanged Callback when camera position changes (optional)
 * @param content Optional composable content to display on map (e.g., markers, layers)
 */
@Composable
fun MapView(
    modifier: Modifier = Modifier,
    initialZoom: Double = 10.0,
    initialLatitude: Double = 39.8283,
    initialLongitude: Double = -98.5795,
    onMapReady: () -> Unit = {},
    onCameraChanged: (CameraPosition) -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val cameraPositionState = rememberCameraPositionState(
        initialPosition = CameraPosition(
            center = maplibre.compose.LatLng(
                latitude = initialLatitude,
                longitude = initialLongitude
            ),
            zoom = initialZoom,
            pitch = 0.0,
            bearing = 0.0
        )
    )

    val style = rememberMapLibreStyle(
        styleUrl = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
    )

    MapLibreMap(
        style = style,
        cameraPositionState = cameraPositionState,
        modifier = modifier.fillMaxSize(),
        onMapLoaded = onMapReady,
        onCameraMove = { camera ->
            onCameraChanged(CameraPosition(
                center = camera.center,
                zoom = camera.zoom,
                pitch = camera.pitch,
                bearing = camera.bearing
            ))
        }
    ) {
        content()
    }
}

/**
 * Map camera position data class.
 *
 * Represents current camera state on map.
 *
 * @property center Center coordinate of map view
 * @property zoom Current zoom level
 * @property pitch Camera pitch angle in degrees (0 = top-down, 60 = max)
 * @property bearing Camera bearing/rotation angle in degrees
 */
data class CameraPosition(
    val center: maplibre.compose.LatLng,
    val zoom: Double,
    val pitch: Double,
    val bearing: Double
)

/**
 * Map bounds for determining visible area.
 *
 * @property north Latitude of north edge
 * @property south Latitude of south edge
 * @property east Longitude of east edge
 * @property west Longitude of west edge
 */
data class MapBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
)

/**
 * Calculates approximate map bounds in miles from center.
 *
 * Rough calculation based on zoom level (valid at equator, approximate elsewhere).
 * For hackathon MVP, this provides sufficient radius calculation for 50-mile rule.
 *
 * @param center Center coordinate
 * @param zoom Current zoom level
 * @return Approximate bounds in degrees
 */
fun calculateMapBoundsFromZoom(
    center: maplibre.compose.LatLng,
    zoom: Double
): MapBounds {
    // Approximate degrees per pixel at given zoom level
    // At zoom=0, one tile covers the whole world (~360 degrees)
    // Each zoom level doubles precision
    val degreesPerPixel = 360.0 / (256.0 * Math.pow(2.0, zoom))

    // Assume 1000px viewport width for calculation
    val halfViewportDegrees = degreesPerPixel * 500.0

    return MapBounds(
        north = center.latitude + halfViewportDegrees,
        south = center.latitude - halfViewportDegrees,
        east = center.longitude + halfViewportDegrees,
        west = center.longitude - halfViewportDegrees
    )
}
