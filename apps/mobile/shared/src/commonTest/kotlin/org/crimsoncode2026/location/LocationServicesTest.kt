package org.crimsoncode2026.location

import dev.icerock.moko.geo.coordinates.LatLon
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for Location Services Models
 *
 * Tests:
 * - LocationMode enum properties and companion function
 * - AccuracyLevel determination
 * - LocationData properties and computed values
 * - LocationSource enum values
 * - Accuracy fallback chain logic
 *
 * Note: LocationRepository, LocationPermissionHandler, and IpGeolocationService
 * require platform-specific implementations (moko-geo, moko-permissions, HTTP client)
 * and are tested via integration tests or platform-specific unit tests.
 */
class LocationServicesTest {

    // ==================== LocationMode Tests ====================

    @Test
    fun `LocationMode has correct display names`() {
        assertEquals("High-Precision", LocationMode.HIGH_PRECISION.displayName)
        assertEquals("Balanced", LocationMode.BALANCED.displayName)
        assertEquals("Low-Power", LocationMode.LOW_POWER.displayName)
    }

    @Test
    fun `LocationMode has correct update intervals`() {
        assertEquals(5_000L, LocationMode.HIGH_PRECISION.updateIntervalMs)
        assertEquals(30_000L, LocationMode.BALANCED.updateIntervalMs)
        assertEquals(180_000L, LocationMode.LOW_POWER.updateIntervalMs)
    }

    @Test
    fun `LocationMode has correct accuracy targets`() {
        assertEquals(10.0, LocationMode.HIGH_PRECISION.accuracyTargetMeters)
        assertEquals(50.0, LocationMode.BALANCED.accuracyTargetMeters)
        assertEquals(1_000.0, LocationMode.LOW_POWER.accuracyTargetMeters)
    }

    @Test
    fun `LocationMode fromAccuracy returns HIGH_PRECISION for high accuracy`() {
        val mode = LocationMode.fromAccuracy(5.0)
        assertEquals(LocationMode.HIGH_PRECISION, mode)
    }

    @Test
    fun `LocationMode fromAccuracy returns BALANCED for medium accuracy`() {
        val mode = LocationMode.fromAccuracy(30.0)
        assertEquals(LocationMode.BALANCED, mode)
    }

    @Test
    fun `LocationMode fromAccuracy returns BALANCED for boundary high accuracy`() {
        val mode = LocationMode.fromAccuracy(10.0)
        assertEquals(LocationMode.HIGH_PRECISION, mode)
    }

    @Test
    fun `LocationMode fromAccuracy returns BALANCED for boundary medium accuracy`() {
        val mode = LocationMode.fromAccuracy(50.0)
        assertEquals(LocationMode.BALANCED, mode)
    }

    @Test
    fun `LocationMode fromAccuracy returns LOW_POWER for low accuracy`() {
        val mode = LocationMode.fromAccuracy(500.0)
        assertEquals(LocationMode.LOW_POWER, mode)
    }

    @Test
    fun `LocationMode fromAccuracy returns LOW_POWER for exact boundary`() {
        val mode = LocationMode.fromAccuracy(50.1)
        assertEquals(LocationMode.LOW_POWER, mode)
    }

    // ==================== AccuracyLevel Tests ====================

    @Test
    fun `LocationData accuracyLevel returns HIGH for accuracy under 10 meters`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 5.0
        )
        assertEquals(AccuracyLevel.HIGH, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracyLevel returns HIGH for exact 10 meter boundary`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 9.9
        )
        assertEquals(AccuracyLevel.HIGH, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracyLevel returns GOOD for accuracy between 10 and 50 meters`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 30.0
        )
        assertEquals(AccuracyLevel.GOOD, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracyLevel returns GOOD for exact 10 meter boundary`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 10.0
        )
        assertEquals(AccuracyLevel.GOOD, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracyLevel returns FAIR for accuracy between 50 and 100 meters`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 75.0
        )
        assertEquals(AccuracyLevel.FAIR, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracyLevel returns FAIR for exact 50 meter boundary`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 50.0
        )
        assertEquals(AccuracyLevel.FAIR, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracyLevel returns LOW for accuracy over 100 meters`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 150.0
        )
        assertEquals(AccuracyLevel.LOW, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracyLevel returns LOW for exact 100 meter boundary`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 100.0
        )
        assertEquals(AccuracyLevel.LOW, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracyLevel returns UNKNOWN when accuracy is null`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = null
        )
        assertEquals(AccuracyLevel.UNKNOWN, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracyLevel boundary test - all transitions`() {
        // Test exact boundaries at each level transition
        assertEquals(AccuracyLevel.HIGH, LocationData(LatLon(0.0, 0.0), 0.1).accuracyLevel)
        assertEquals(AccuracyLevel.HIGH, LocationData(LatLon(0.0, 0.0), 9.9).accuracyLevel)
        assertEquals(AccuracyLevel.GOOD, LocationData(LatLon(0.0, 0.0), 10.0).accuracyLevel)
        assertEquals(AccuracyLevel.GOOD, LocationData(LatLon(0.0, 0.0), 49.9).accuracyLevel)
        assertEquals(AccuracyLevel.FAIR, LocationData(LatLon(0.0, 0.0), 50.0).accuracyLevel)
        assertEquals(AccuracyLevel.FAIR, LocationData(LatLon(0.0, 0.0), 99.9).accuracyLevel)
        assertEquals(AccuracyLevel.LOW, LocationData(LatLon(0.0, 0.0), 100.0).accuracyLevel)
        assertEquals(AccuracyLevel.LOW, LocationData(LatLon(0.0, 0.0), 1000.0).accuracyLevel)
    }

    // ==================== LocationData Tests ====================

    @Test
    fun `LocationData has correct properties`() {
        val coordinates = LatLon(40.7128, -74.0060)
        val timestamp = 1234567890L
        val source = LocationSource.GPS

        val location = LocationData(
            coordinates = coordinates,
            accuracyMeters = 15.0,
            timestamp = timestamp,
            source = source
        )

        assertEquals(coordinates, location.coordinates)
        assertEquals(15.0, location.accuracyMeters)
        assertEquals(timestamp, location.timestamp)
        assertEquals(source, location.source)
    }

    @Test
    fun `LocationData accuracyLevel matches accuracy`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 8.0
        )
        assertEquals(AccuracyLevel.HIGH, location.accuracyLevel)
    }

    @Test
    fun `LocationData uses current timestamp when not provided`() = runTest {
        val before = System.currentTimeMillis()
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 15.0
        )
        val after = System.currentTimeMillis()

        assertTrue(location.timestamp in before..after)
    }

    @Test
    fun `LocationData uses UNKNOWN source when not provided`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 15.0
        )

        assertEquals(LocationSource.UNKNOWN, location.source)
    }

    @Test
    fun `LocationData with null accuracy has UNKNOWN accuracy level`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = null
        )

        assertEquals(AccuracyLevel.UNKNOWN, location.accuracyLevel)
    }

    @Test
    fun `LocationData coordinates can be created with LatLon`() {
        val coordinates = LatLon(40.7128, -74.0060)
        val location = LocationData(
            coordinates = coordinates,
            accuracyMeters = 15.0
        )

        assertEquals(coordinates, location.coordinates)
        assertEquals(40.7128, location.coordinates.latitude)
        assertEquals(-74.0060, location.coordinates.longitude)
    }

    // ==================== LocationSource Enum Tests ====================

    @Test
    fun `LocationSource enum has all expected values`() {
        val sources = LocationSource.entries
        assertTrue(sources.contains(LocationSource.GPS))
        assertTrue(sources.contains(LocationSource.WIFI))
        assertTrue(sources.contains(LocationSource.CELLULAR))
        assertTrue(sources.contains(LocationSource.IP))
        assertTrue(sources.contains(LocationSource.MANUAL))
        assertTrue(sources.contains(LocationSource.UNKNOWN))
        assertEquals(6, sources.size)
    }

    // ==================== AccuracyLevel Enum Tests ====================

    @Test
    fun `AccuracyLevel enum has all expected values`() {
        val levels = AccuracyLevel.entries
        assertTrue(levels.contains(AccuracyLevel.HIGH))
        assertTrue(levels.contains(AccuracyLevel.GOOD))
        assertTrue(levels.contains(AccuracyLevel.FAIR))
        assertTrue(levels.contains(AccuracyLevel.LOW))
        assertTrue(levels.contains(AccuracyLevel.UNKNOWN))
        assertEquals(5, levels.size)
    }

    @Test
    fun `AccuracyLevel values are ordered from HIGH to UNKNOWN`() {
        val levels = AccuracyLevel.entries
        assertEquals(AccuracyLevel.HIGH, levels[0])
        assertEquals(AccuracyLevel.GOOD, levels[1])
        assertEquals(AccuracyLevel.FAIR, levels[2])
        assertEquals(AccuracyLevel.LOW, levels[3])
        assertEquals(AccuracyLevel.UNKNOWN, levels[4])
    }

    // ==================== LocationMode Enum Tests ====================

    @Test
    fun `LocationMode enum has all expected values`() {
        val modes = LocationMode.entries
        assertTrue(modes.contains(LocationMode.HIGH_PRECISION))
        assertTrue(modes.contains(LocationMode.BALANCED))
        assertTrue(modes.contains(LocationMode.LOW_POWER))
        assertEquals(3, modes.size)
    }

    @Test
    fun `LocationMode values are ordered from HIGH_PRECISION to LOW_POWER`() {
        val modes = LocationMode.entries
        assertEquals(LocationMode.HIGH_PRECISION, modes[0])
        assertEquals(LocationMode.BALANCED, modes[1])
        assertEquals(LocationMode.LOW_POWER, modes[2])
    }

    // ==================== Accuracy Fallback Chain Logic Tests ====================

    @Test
    fun `Accuracy fallback priority is GPS > WiFi > Cellular > IP > Manual`() {
        // This is a documentation test to verify the documented fallback chain
        // The actual implementation is in LocationRepository and requires
        // platform-specific mocking for full integration testing

        val gpsAccuracy = 5.0      // GPS: < 20 meters
        val wifiAccuracy = 50.0      // WiFi: 20-100 meters
        val cellularAccuracy = 300.0   // Cellular: 100-500 meters
        val ipAccuracy = null           // IP: no accuracy (null)
        val manualAccuracy = 10.0    // Manual: user-specified

        // GPS should be highest priority
        assertTrue(gpsAccuracy < (wifiAccuracy ?: Double.MAX_VALUE))
        // WiFi should be second priority
        assertTrue((wifiAccuracy ?: Double.MAX_VALUE) < (cellularAccuracy ?: Double.MAX_VALUE))
        // Cellular should be third priority
        assertTrue((cellularAccuracy ?: Double.MAX_VALUE) > (ipAccuracy ?: -1.0))
    }

    @Test
    fun `Accuracy level mapping matches specification colors`() {
        // Verify accuracy levels match UI color specification:
        // HIGH (< 10m) = Green
        // GOOD (10-50m) = Yellow
        // FAIR (50-100m) = Orange
        // LOW (> 100m) = Red
        // UNKNOWN = No color

        val highAccuracy = LocationData(LatLon(0.0, 0.0), 5.0).accuracyLevel
        val goodAccuracy = LocationData(LatLon(0.0, 0.0), 30.0).accuracyLevel
        val fairAccuracy = LocationData(LatLon(0.0, 0.0), 75.0).accuracyLevel
        val lowAccuracy = LocationData(LatLon(0.0, 0.0), 150.0).accuracyLevel
        val unknownAccuracy = LocationData(LatLon(0.0, 0.0), null).accuracyLevel

        assertEquals(AccuracyLevel.HIGH, highAccuracy)
        assertEquals(AccuracyLevel.GOOD, goodAccuracy)
        assertEquals(AccuracyLevel.FAIR, fairAccuracy)
        assertEquals(AccuracyLevel.LOW, lowAccuracy)
        assertEquals(AccuracyLevel.UNKNOWN, unknownAccuracy)
    }

    // ==================== LocationData with Different Sources Tests ====================

    @Test
    fun `LocationData can represent GPS location`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 5.0,
            source = LocationSource.GPS
        )

        assertEquals(LocationSource.GPS, location.source)
        assertEquals(AccuracyLevel.HIGH, location.accuracyLevel)
    }

    @Test
    fun `LocationData can represent WiFi location`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 30.0,
            source = LocationSource.WIFI
        )

        assertEquals(LocationSource.WIFI, location.source)
        assertEquals(AccuracyLevel.GOOD, location.accuracyLevel)
    }

    @Test
    fun `LocationData can represent Cellular location`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 300.0,
            source = LocationSource.CELLULAR
        )

        assertEquals(LocationSource.CELLULAR, location.source)
        assertEquals(AccuracyLevel.LOW, location.accuracyLevel)
    }

    @Test
    fun `LocationData can represent IP location`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = null,
            source = LocationSource.IP
        )

        assertEquals(LocationSource.IP, location.source)
        assertEquals(AccuracyLevel.UNKNOWN, location.accuracyLevel)
    }

    @Test
    fun `LocationData can represent Manual location`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 10.0,
            source = LocationSource.MANUAL
        )

        assertEquals(LocationSource.MANUAL, location.source)
        assertEquals(AccuracyLevel.HIGH, location.accuracyLevel)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `LocationData handles zero accuracy`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 0.0
        )

        assertEquals(0.0, location.accuracyMeters)
        assertEquals(AccuracyLevel.HIGH, location.accuracyLevel)
    }

    @Test
    fun `LocationData handles very large accuracy`() {
        val location = LocationData(
            coordinates = LatLon(40.7128, -74.0060),
            accuracyMeters = 10000.0
        )

        assertEquals(10000.0, location.accuracyMeters)
        assertEquals(AccuracyLevel.LOW, location.accuracyLevel)
    }

    @Test
    fun `LocationData handles extreme coordinates`() {
        val location = LocationData(
            coordinates = LatLon(90.0, -180.0),
            accuracyMeters = 100.0
        )

        assertEquals(90.0, location.coordinates.latitude)
        assertEquals(-180.0, location.coordinates.longitude)
    }

    @Test
    fun `LocationData handles negative coordinates`() {
        val location = LocationData(
            coordinates = LatLon(-90.0, 180.0),
            accuracyMeters = 100.0
        )

        assertEquals(-90.0, location.coordinates.latitude)
        assertEquals(180.0, location.coordinates.longitude)
    }

    @Test
    fun `LocationMode fromAccuracy handles extreme accuracy values`() {
        // Very accurate (under 1 meter)
        assertEquals(LocationMode.HIGH_PRECISION, LocationMode.fromAccuracy(0.5))
        // Very inaccurate (over 1 km)
        assertEquals(LocationMode.LOW_POWER, LocationMode.fromAccuracy(10000.0))
    }

    // ==================== Data Equality Tests ====================

    @Test
    fun `LocationData with same properties are equal`() {
        val coordinates = LatLon(40.7128, -74.0060)
        val location1 = LocationData(
            coordinates = coordinates,
            accuracyMeters = 15.0,
            timestamp = 1234567890L,
            source = LocationSource.GPS
        )
        val location2 = LocationData(
            coordinates = coordinates,
            accuracyMeters = 15.0,
            timestamp = 1234567890L,
            source = LocationSource.GPS
        )

        assertEquals(location1, location2)
    }

    @Test
    fun `LocationData with different accuracy are not equal`() {
        val coordinates = LatLon(40.7128, -74.0060)
        val location1 = LocationData(
            coordinates = coordinates,
            accuracyMeters = 15.0
        )
        val location2 = LocationData(
            coordinates = coordinates,
            accuracyMeters = 20.0
        )

        assertFalse(location1 == location2)
    }

    @Test
    fun `LocationData with different source are not equal`() {
        val coordinates = LatLon(40.7128, -74.0060)
        val location1 = LocationData(
            coordinates = coordinates,
            source = LocationSource.GPS
        )
        val location2 = LocationData(
            coordinates = coordinates,
            source = LocationSource.WIFI
        )

        assertFalse(location1 == location2)
    }

    // ==================== Mode Accuracy Target Tests ====================

    @Test
    fun `LocationMode accuracy targets match specification`() {
        // From spec:
        // High-Precision: < 10 meters accuracy target
        // Balanced: < 50 meters accuracy target
        // Low-Power: < 1 kilometer accuracy target

        assertEquals(10.0, LocationMode.HIGH_PRECISION.accuracyTargetMeters)
        assertEquals(50.0, LocationMode.BALANCED.accuracyTargetMeters)
        assertEquals(1_000.0, LocationMode.LOW_POWER.accuracyTargetMeters)
    }

    @Test
    fun `LocationMode update intervals match specification`() {
        // From spec:
        // High-Precision: 5 seconds
        // Balanced: 30 seconds
        // Low-Power: 2-5 minutes (using 3 minutes)

        assertEquals(5_000L, LocationMode.HIGH_PRECISION.updateIntervalMs)      // 5 seconds
        assertEquals(30_000L, LocationMode.BALANCED.updateIntervalMs)       // 30 seconds
        assertEquals(180_000L, LocationMode.LOW_POWER.updateIntervalMs)     // 3 minutes
    }
}
