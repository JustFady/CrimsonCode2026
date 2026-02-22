package org.crimsoncode2026.location

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Compose state holder for location updates
 *
 * Provides a stable state that can be observed by Compose UI components.
 * Handles location tracking state and exposes location data updates.
 */
@Stable
class LocationState(
    private val locationRepository: LocationRepository,
    scope: CoroutineScope
) {
    /**
     * Flow of location data exposed as StateFlow for Compose observation
     */
    val locationData: StateFlow<LocationData?> = locationRepository.locationUpdates
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = locationRepository.lastKnownLocation
        )

    /**
     * Current location mode
     */
    val mode: LocationMode
        get() = locationRepository.currentMode

    /**
     * Whether location tracking is currently active
     */
    val isTracking: Boolean
        get() = locationRepository.isTracking

    /**
     * Last known location (null if not available)
     */
    val lastKnownLocation: LocationData?
        get() = locationRepository.lastKnownLocation

    /**
     * Start location tracking with the specified mode
     */
    fun startTracking(mode: LocationMode = LocationMode.BALANCED) {
        locationRepository.startTracking(mode)
    }

    /**
     * Stop location tracking
     */
    fun stopTracking() {
        locationRepository.stopTracking()
    }

    /**
     * Change the current location mode
     */
    fun changeMode(mode: LocationMode) {
        locationRepository.changeMode(mode)
    }

    /**
     * Request a single location update
     */
    suspend fun requestSingleLocation(mode: LocationMode = LocationMode.BALANCED): LocationData? {
        return locationRepository.requestSingleLocation(mode)
    }

    /**
     * Request location with fallback chain
     *
     * Fallback chain: GPS -> WiFi -> Cellular -> IP -> Manual
     *
     * @param mode The location mode to use for this request
     * @return LocationData if any source is available, null otherwise
     */
    suspend fun requestLocationWithFallback(mode: LocationMode = LocationMode.BALANCED): LocationData? {
        return locationRepository.requestLocationWithFallback(mode)
    }

    /**
     * Set a manual location (e.g., from map pinning)
     *
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param accuracyMeters Optional accuracy estimate in meters
     */
    fun setManualLocation(latitude: Double, longitude: Double, accuracyMeters: Double? = null) {
        locationRepository.setManualLocation(latitude, longitude, accuracyMeters)
    }

    /**
     * Set a manual location from LatLon coordinates
     *
     * @param coordinates LatLon coordinates
     * @param accuracyMeters Optional accuracy estimate in meters
     */
    fun setManualLocation(coordinates: dev.icerock.moko.geo.coordinates.LatLon, accuracyMeters: Double? = null) {
        locationRepository.setManualLocation(coordinates, accuracyMeters)
    }

    /**
     * Clear the manual location and return to GPS-based tracking
     */
    fun clearManualLocation() {
        locationRepository.clearManualLocation()
    }

    /**
     * Check if manual location is currently active
     */
    val isManualLocationActive: Boolean
        get() = locationRepository.isManualLocationActive

    /**
     * Accuracy level of the current location
     */
    val accuracyLevel: AccuracyLevel?
        get() = lastKnownLocation?.accuracyLevel

    /**
     * Current accuracy in meters (null if not available)
     */
    val accuracyMeters: Double?
        get() = lastKnownLocation?.accuracyMeters
}
