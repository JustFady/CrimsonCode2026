package org.crimsoncode2026.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.maplibre.compose.camera.CameraPosition as MlCameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import kotlin.math.pow

/**
 * Lightweight wrapper around MapLibre Compose adapted to the currently installed
 * `org.maplibre.compose` API.
 *
 * We keep the app's previous signature so other screens can be restored incrementally.
 */
@Composable
fun MapView(
    modifier: Modifier = Modifier,
    initialZoom: Double = 10.0,
    initialLatitude: Double = 39.8283,
    initialLongitude: Double = -98.5795,
    onMapReady: () -> Unit = {},
    onCameraChanged: (CameraPosition) -> Unit = {},
    targetLocation: Pair<Double, Double>? = null,
    targetZoom: Double? = null,
    content: @Composable () -> Unit = {}
) {
    val baseStyle = remember {
        BaseStyle.Json(
            """
            {
              "version": 8,
              "name": "CrimsonCode OSM Raster",
              "sources": {
                "osm": {
                  "type": "raster",
                  "tiles": [
                    "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
                  ],
                  "tileSize": 256,
                  "attribution": "© OpenStreetMap contributors"
                }
              },
              "layers": [
                {
                  "id": "osm-raster",
                  "type": "raster",
                  "source": "osm",
                  "minzoom": 0,
                  "maxzoom": 19
                }
              ]
            }
            """.trimIndent()
        )
    }

    val cameraState = rememberCameraState(
        firstPosition = MlCameraPosition(
            target = Position(longitude = initialLongitude, latitude = initialLatitude),
            zoom = initialZoom
        )
    )

    LaunchedEffect(targetLocation, targetZoom) {
        targetLocation?.let { (lat, lon) ->
            cameraState.position = cameraState.position.copy(
                target = Position(longitude = lon, latitude = lat),
                zoom = targetZoom ?: cameraState.position.zoom
            )
        }
    }

    LaunchedEffect(cameraState.position) {
        val p = cameraState.position
        onCameraChanged(
            CameraPosition(
                center = LatLng(
                    latitude = p.target.latitude,
                    longitude = p.target.longitude
                ),
                zoom = p.zoom,
                pitch = p.tilt,
                bearing = p.bearing
            )
        )
    }

    MaplibreMap(
        modifier = modifier.fillMaxSize(),
        baseStyle = baseStyle,
        cameraState = cameraState,
        onMapLoadFinished = onMapReady
    ) {
        content()
    }
}

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

data class CameraPosition(
    val center: LatLng,
    val zoom: Double,
    val pitch: Double,
    val bearing: Double
)

data class MapBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
)

fun calculateMapBoundsFromZoom(center: LatLng, zoom: Double): MapBounds {
    val degreesPerPixel = 360.0 / (256.0 * 2.0.pow(zoom))
    val halfViewportDegrees = degreesPerPixel * 500.0
    return MapBounds(
        north = center.latitude + halfViewportDegrees,
        south = center.latitude - halfViewportDegrees,
        east = center.longitude + halfViewportDegrees,
        west = center.longitude - halfViewportDegrees
    )
}
