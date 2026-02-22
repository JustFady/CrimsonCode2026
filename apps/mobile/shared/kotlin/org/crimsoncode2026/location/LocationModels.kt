package org.crimsoncode2026.location

import dev.icerock.moko.geo.coordinates.LatLon

/**
 * Location accuracy level for UI visualization
 */
enum class AccuracyLevel {
    HIGH,      // < 10 meters (Green)
    GOOD,      // 10-50 meters (Yellow)
    FAIR,      // 50-100 meters (Orange)
    LOW,       // > 100 meters (Red)
    UNKNOWN    // Accuracy not available
}

/**
 * Location mode with associated update interval and accuracy target
 */
enum class LocationMode(val displayName: String, val updateIntervalMs: Long, val accuracyTargetMeters: Double) {
    HIGH_PRECISION(
        displayName = "High-Precision",
        updateIntervalMs = 5_000L,      // 5 seconds
        accuracyTargetMeters = 10.0       // Under 10 meters
    ),
    BALANCED(
        displayName = "Balanced",
        updateIntervalMs = 30_000L,       // 30 seconds
        accuracyTargetMeters = 50.0       // Under 50 meters
    ),
    LOW_POWER(
        displayName = "Low-Power",
        updateIntervalMs = 180_000L,      // 2-5 minutes (using 3 minutes)
        accuracyTargetMeters = 1_000.0   // Under 1 kilometer
    );

    companion object {
        fun fromAccuracy(accuracyMeters: Double): LocationMode {
            return when {
                accuracyMeters <= HIGH_PRECISION.accuracyTargetMeters -> HIGH_PRECISION
                accuracyMeters <= BALANCED.accuracyTargetMeters -> BALANCED
                else -> LOW_POWER
            }
        }
    }
}

/**
 * Location data with coordinates and accuracy information
 */
data class LocationData(
    val coordinates: LatLon,
    val accuracyMeters: Double?,
    val timestamp: Long = System.currentTimeMillis(),
    val source: LocationSource = LocationSource.UNKNOWN
) {
    val accuracyLevel: AccuracyLevel
        get() = when {
            accuracyMeters == null -> AccuracyLevel.UNKNOWN
            accuracyMeters < 10.0 -> AccuracyLevel.HIGH
            accuracyMeters < 50.0 -> AccuracyLevel.GOOD
            accuracyMeters < 100.0 -> AccuracyLevel.FAIR
            else -> AccuracyLevel.LOW
        }
}

/**
 * Source of the location data
 */
enum class LocationSource {
    GPS,
    WIFI,
    CELLULAR,
    IP,
    MANUAL,
    UNKNOWN
}
