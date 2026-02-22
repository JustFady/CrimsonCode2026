package org.crimsoncode2026.location

import dev.icerock.moko.geo.LocationTracker
import dev.icerock.moko.geo.Precision
import dev.icerock.moko.geo.compose.LocationTrackerFactory
import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.merge

/**
 * Repository for managing location updates using moko-geo
 *
 * Provides location updates with configurable modes for different
 * accuracy targets and battery usage levels.
 *
 * Fallback chain: GPS -> WiFi -> Cellular -> IP -> Manual
 */
class LocationRepository(
    private val permissionsController: PermissionsController,
    private val ipGeolocationService: IpGeolocationService? = null
) {
    private val tracker: LocationTracker = LocationTrackerFactory(permissionsController).createLocationTracker()

    private var _currentMode: LocationMode = LocationMode.BALANCED

    /**
     * Enable IP geolocation fallback
     */
    var enableIpFallback: Boolean = true
        set(value) {
            field = value && ipGeolocationService != null
        }

    // Manual location flow for user-set location on map
    private val _manualLocationFlow = MutableSharedFlow<LocationData>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val manualLocationFlow = _manualLocationFlow.asSharedFlow()

    /**
     * Manual location set by user (null if not set)
     */
    var manualLocation: LocationData? = null
        private set

    /**
     * Current active location mode
     */
    val currentMode: LocationMode
        get() = _currentMode

    /**
     * Flow of location data updates
     * Merges tracker location updates with manual location updates
     */
    val locationUpdates: Flow<LocationData>
        get() = merge(
            tracker.locationFlow.map { geoLocation ->
                LocationData(
                    coordinates = geoLocation.coordinates,
                    accuracyMeters = geoLocation.horizontalAccuracy,
                    timestamp = geoLocation.timestamp,
                    source = determineSource(geoLocation)
                )
            },
            manualLocationFlow
        )

    /**
     * Get last known location, if available
     * Returns manual location if set, otherwise returns GPS-based location
     */
    val lastKnownLocation: LocationData?
        get() = manualLocation ?: tracker.location?.let { geoLocation ->
            LocationData(
                coordinates = geoLocation.coordinates,
                accuracyMeters = geoLocation.horizontalAccuracy,
                timestamp = geoLocation.timestamp,
                source = determineSource(geoLocation)
            )
        }

    /**
     * Start tracking location with the specified mode
     *
     * @param mode The location mode to use for tracking
     */
    fun startTracking(mode: LocationMode = _currentMode) {
        _currentMode = mode
        tracker.startTracking(precision = mode.toPrecision())
    }

    /**
     * Stop tracking location
     */
    fun stopTracking() {
        tracker.stopTracking()
    }

    /**
     * Change the location mode during active tracking
     *
     * @param mode The new location mode to use
     */
    fun changeMode(mode: LocationMode) {
        _currentMode = mode
        // If already tracking, restart with new precision
        if (tracker.isTracking) {
            tracker.stopTracking()
            tracker.startTracking(precision = mode.toPrecision())
        }
    }

    /**
     * Request a single location update with specified mode
     *
     * @param mode The location mode to use for this request
     * @return LocationData if available, null otherwise
     */
    suspend fun requestSingleLocation(mode: LocationMode = _currentMode): LocationData? {
        val geoLocation = tracker.requestLocation(precision = mode.toPrecision())
        return geoLocation?.let {
            LocationData(
                coordinates = it.coordinates,
                accuracyMeters = it.horizontalAccuracy,
                timestamp = it.timestamp,
                source = determineSource(it)
            )
        }
    }

    /**
     * Request location with fallback chain
     *
     * Fallback chain: GPS -> WiFi -> Cellular -> IP -> Manual
     *
     * @param mode The location mode to use for this request
     * @return LocationData if any source is available, null otherwise
     */
    suspend fun requestLocationWithFallback(mode: LocationMode = _currentMode): LocationData? {
        // 1. Try GPS (via native location services which handle GPS/WiFi/Cellular automatically)
        val geoLocation = tracker.requestLocation(precision = mode.toPrecision())
        if (geoLocation != null) {
            return LocationData(
                coordinates = geoLocation.coordinates,
                accuracyMeters = geoLocation.horizontalAccuracy,
                timestamp = geoLocation.timestamp,
                source = determineSource(geoLocation)
            )
        }

        // 2. Try IP geolocation as fallback (if enabled and service is available)
        if (enableIpFallback && ipGeolocationService != null) {
            val ipLocation = ipGeolocationService.getIpLocation()
            if (ipLocation != null) {
                return ipLocation
            }
        }

        // 3. Return manual location if set
        return manualLocation
    }

    /**
     * Set a manual location (e.g., from map pinning)
     *
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param accuracyMeters Optional accuracy estimate in meters
     */
    fun setManualLocation(latitude: Double, longitude: Double, accuracyMeters: Double? = null) {
        manualLocation = LocationData(
            coordinates = dev.icerock.moko.geo.coordinates.LatLon(latitude, longitude),
            accuracyMeters = accuracyMeters,
            timestamp = System.currentTimeMillis(),
            source = LocationSource.MANUAL
        )
        // Emit the manual location to the flow
        _manualLocationFlow.tryEmit(manualLocation!!)
    }

    /**
     * Set a manual location from LatLon coordinates
     *
     * @param coordinates LatLon coordinates
     * @param accuracyMeters Optional accuracy estimate in meters
     */
    fun setManualLocation(coordinates: dev.icerock.moko.geo.coordinates.LatLon, accuracyMeters: Double? = null) {
        manualLocation = LocationData(
            coordinates = coordinates,
            accuracyMeters = accuracyMeters,
            timestamp = System.currentTimeMillis(),
            source = LocationSource.MANUAL
        )
        _manualLocationFlow.tryEmit(manualLocation!!)
    }

    /**
     * Clear the manual location and return to GPS-based tracking
     */
    fun clearManualLocation() {
        manualLocation = null
    }

    /**
     * Check if manual location is currently active
     */
    val isManualLocationActive: Boolean
        get() = manualLocation != null

    /**
     * Check if location tracking is currently active
     */
    val isTracking: Boolean
        get() = tracker.isTracking

    /**
     * Convert LocationMode to moko-geo Precision
     */
    private fun LocationMode.toPrecision(): Precision {
        return when (this) {
            LocationMode.HIGH_PRECISION -> Precision.High
            LocationMode.BALANCED -> Precision.Balanced
            LocationMode.LOW_POWER -> Precision.Low
        }
    }

    /**
     * Determine the location source from geo location data
     * Note: moko-geo doesn't directly expose source, using defaults based on mode
     */
    private fun determineSource(geoLocation: dev.icerock.moko.geo.Location): LocationSource {
        // In a real implementation, platform-specific APIs would provide this info
        // For now, infer based on accuracy and current mode
        val accuracy = geoLocation.horizontalAccuracy ?: return LocationSource.UNKNOWN
        return when {
            accuracy < 20.0 -> LocationSource.GPS
            accuracy < 100.0 -> LocationSource.WIFI
            accuracy < 500.0 -> LocationSource.CELLULAR
            else -> LocationSource.IP
        }
    }
}
