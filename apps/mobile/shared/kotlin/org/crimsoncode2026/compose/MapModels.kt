package org.crimsoncode2026.compose

/**
 * Camera position for map view
 *
 * @param latitude Center latitude
 * @param longitude Center longitude
 * @param zoom Zoom level (typically 1-20, where 20 is most detailed)
 */
data class CameraPosition(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double = 14.0
)

/**
 * Map bounds for querying visible area
 *
 * @param north Northern boundary (maximum latitude)
 * @param south Southern boundary (minimum latitude)
 * @param east Eastern boundary (maximum longitude)
 * @param west Western boundary (minimum longitude)
 */
data class MapBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
) {
    /**
     * Get center point of bounds
     */
    val center: CameraPosition
        get() = CameraPosition(
            latitude = (north + south) / 2,
            longitude = (east + west) / 2
        )

    companion object {
        /**
         * Create default bounds centered on a point with default radius
         */
        fun centeredOn(latitude: Double, longitude: Double, radiusDegrees: Double = 0.5): MapBounds {
            return MapBounds(
                north = latitude + radiusDegrees,
                south = latitude - radiusDegrees,
                east = longitude + radiusDegrees,
                west = longitude - radiusDegrees
            )
        }
    }
}
