package org.crimsoncode2026.location

import dev.icerock.moko.geo.coordinates.LatLon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration tests for LocationModels
 */
class LocationModelsTest {

    @Test
    fun `LocationData accuracy level returns HIGH when accuracy under 10 meters`() {
        val location = LocationData(
            coordinates = LatLon(37.7749, -122.4194),
            accuracyMeters = 5.0
        )

        assertEquals(AccuracyLevel.HIGH, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracy level returns GOOD when accuracy between 10 and 50 meters`() {
        val location = LocationData(
            coordinates = LatLon(37.7749, -122.4194),
            accuracyMeters = 30.0
        )

        assertEquals(AccuracyLevel.GOOD, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracy level returns FAIR when accuracy between 50 and 100 meters`() {
        val location = LocationData(
            coordinates = LatLon(37.7749, -122.4194),
            accuracyMeters = 75.0
        )

        assertEquals(AccuracyLevel.FAIR, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracy level returns LOW when accuracy over 100 meters`() {
        val location = LocationData(
            coordinates = LatLon(37.7749, -122.4194),
            accuracyMeters = 150.0
        )

        assertEquals(AccuracyLevel.LOW, location.accuracyLevel)
    }

    @Test
    fun `LocationData accuracy level returns UNKNOWN when accuracy is null`() {
        val location = LocationData(
            coordinates = LatLon(37.7749, -122.4194),
            accuracyMeters = null
        )

        assertEquals(AccuracyLevel.UNKNOWN, location.accuracyLevel)
    }

    @Test
    fun `LocationData timestamp is set to current time by default`() {
        val before = System.currentTimeMillis()
        val location = LocationData(
            coordinates = LatLon(37.7749, -122.4194),
            accuracyMeters = 10.0
        )
        val after = System.currentTimeMillis()

        assert(location.timestamp in before..after)
    }

    @Test
    fun `LocationData source defaults to UNKNOWN`() {
        val location = LocationData(
            coordinates = LatLon(37.7749, -122.4194),
            accuracyMeters = 10.0
        )

        assertEquals(LocationSource.UNKNOWN, location.source)
    }

    @Test
    fun `LocationMode HIGH_PRECISION has correct config`() {
        assertEquals("High-Precision", LocationMode.HIGH_PRECISION.displayName)
        assertEquals(5_000L, LocationMode.HIGH_PRECISION.updateIntervalMs)
        assertEquals(10.0, LocationMode.HIGH_PRECISION.accuracyTargetMeters)
    }

    @Test
    fun `LocationMode BALANCED has correct config`() {
        assertEquals("Balanced", LocationMode.BALANCED.displayName)
        assertEquals(30_000L, LocationMode.BALANCED.updateIntervalMs)
        assertEquals(50.0, LocationMode.BALANCED.accuracyTargetMeters)
    }

    @Test
    fun `LocationMode LOW_POWER has correct config`() {
        assertEquals("Low-Power", LocationMode.LOW_POWER.displayName)
        assertEquals(180_000L, LocationMode.LOW_POWER.updateIntervalMs)
        assertEquals(1_000.0, LocationMode.LOW_POWER.accuracyTargetMeters)
    }

    @Test
    fun `LocationMode fromAccuracy returns HIGH_PRECISION for under 10 meters`() {
        assertEquals(LocationMode.HIGH_PRECISION, LocationMode.fromAccuracy(5.0))
    }

    @Test
    fun `LocationMode fromAccuracy returns BALANCED for 10 to 50 meters`() {
        assertEquals(LocationMode.BALANCED, LocationMode.fromAccuracy(30.0))
    }

    @Test
    fun `LocationMode fromAccuracy returns LOW_POWER for over 50 meters`() {
        assertEquals(LocationMode.LOW_POWER, LocationMode.fromAccuracy(100.0))
    }
}
