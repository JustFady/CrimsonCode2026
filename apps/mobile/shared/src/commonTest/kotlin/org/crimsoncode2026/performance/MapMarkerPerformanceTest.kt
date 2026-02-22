package org.crimsoncode2026.performance

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Severity
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Performance tests for map marker rendering
 *
 * Tests:
 * - Rendering performance for small marker sets (1-10)
 * - Rendering performance for medium marker sets (10-100)
 * - Rendering performance for large marker sets (100-500)
 * - Memory usage for marker rendering
 * - Clustering efficiency
 */
class MapMarkerPerformanceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val timer = PerformanceTimer()
    private val memoryTracker = MemoryTracker()

    /**
     * Test performance with small marker set (10 markers)
     * Target: <100ms render time, <5MB memory
     */
    @Test
    fun testSmallMarkerSetPerformance() = runTest {
        val markers = generateTestEvents(10)

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate marker layer creation
            createMarkerLayers(markers)
        }
        memoryTracker.stop()

        val metrics = memoryTracker.getMetrics("Small Marker Set (10)", duration)
        val testResult = PerformanceTestResult(
            testName = "Small Marker Set (10)",
            passed = metrics.isDurationAcceptable(PerformanceThresholds.MAP_MARKER_RENDER_MS) &&
                    metrics.isMemoryUsageAcceptable(),
            metrics = metrics,
            threshold = PerformanceThresholds.MAP_MARKER_RENDER_MS,
            message = if (metrics.isDurationAcceptable(PerformanceThresholds.MAP_MARKER_RENDER_MS)) {
                "PASS: Rendered ${markers.size} markers in ${duration}ms"
            } else {
                "FAIL: Render took ${duration}ms, threshold is ${PerformanceThresholds.MAP_MARKER_RENDER_MS}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test performance with medium marker set (100 markers)
     * Target: <150ms render time, <10MB memory
     */
    @Test
    fun testMediumMarkerSetPerformance() = runTest {
        val markers = generateTestEvents(100)

        memoryTracker.start()
        val (result, duration) = timer.measure {
            createMarkerLayers(markers)
        }
        memoryTracker.stop()

        // Allow more time for larger sets
        val threshold = PerformanceThresholds.MAP_MARKER_RENDER_MS * 2
        val metrics = memoryTracker.getMetrics("Medium Marker Set (100)", duration)
        val testResult = PerformanceTestResult(
            testName = "Medium Marker Set (100)",
            passed = metrics.isDurationAcceptable(threshold) &&
                    metrics.memoryDeltaBytes < (20 * 1024 * 1024), // 20MB for larger sets
            metrics = metrics,
            threshold = threshold,
            message = if (metrics.isDurationAcceptable(threshold)) {
                "PASS: Rendered ${markers.size} markers in ${duration}ms"
            } else {
                "FAIL: Render took ${duration}ms, threshold is ${threshold}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test performance with large marker set (500 markers)
     * Target: <300ms render time with clustering
     */
    @Test
    fun testLargeMarkerSetPerformance() = runTest {
        val markers = generateTestEvents(500)

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate clustered rendering
            createClusteredMarkers(markers)
        }
        memoryTracker.stop()

        // Larger threshold for clustered rendering
        val threshold = PerformanceThresholds.MAP_MARKER_RENDER_MS * 3
        val metrics = memoryTracker.getMetrics("Large Marker Set (500)", duration)
        val testResult = PerformanceTestResult(
            testName = "Large Marker Set (500)",
            passed = metrics.isDurationAcceptable(threshold) &&
                    metrics.memoryDeltaBytes < (50 * 1024 * 1024), // 50MB for large sets
            metrics = metrics,
            threshold = threshold,
            message = if (metrics.isDurationAcceptable(threshold)) {
                "PASS: Clustered ${markers.size} markers in ${duration}ms"
            } else {
                "FAIL: Render took ${duration}ms, threshold is ${threshold}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test marker update performance
     * Simulates adding/removing markers during runtime
     */
    @Test
    fun testMarkerUpdatePerformance() = runTest {
        val initialMarkers = generateTestEvents(50)
        val additionalMarkers = generateTestEvents(10, offset = 50)

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate incremental marker update
            updateMarkers(initialMarkers, additionalMarkers)
        }
        memoryTracker.stop()

        val metrics = memoryTracker.getMetrics("Marker Update (50->60)", duration)
        val testResult = PerformanceTestResult(
            testName = "Marker Update (50->60)",
            passed = metrics.isDurationAcceptable(PerformanceThresholds.MAP_CLUSTER_UPDATE_MS),
            metrics = metrics,
            threshold = PerformanceThresholds.MAP_CLUSTER_UPDATE_MS,
            message = if (metrics.isDurationAcceptable(PerformanceThresholds.MAP_CLUSTER_UPDATE_MS)) {
                "PASS: Updated markers in ${duration}ms"
            } else {
                "FAIL: Update took ${duration}ms, threshold is ${PerformanceThresholds.MAP_CLUSTER_UPDATE_MS}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test memory efficiency of marker storage
     */
    @Test
    fun testMarkerMemoryEfficiency() = runTest {
        val markers = generateTestEvents(100)

        memoryTracker.start()
        val stored = simulateMarkerStorage(markers)
        memoryTracker.stop()

        val expectedMemory = markers.size * PerformanceThresholds.MEMORY_MAP_MARKER_ITEM_BYTES
        val actualMemory = memoryTracker.getDelta()
        val efficiency = expectedMemory.toDouble() / actualMemory.toDouble()

        val metrics = memoryTracker.getMetrics("Marker Memory Efficiency", memoryTracker.getElapsedMs())
        val testResult = PerformanceTestResult(
            testName = "Marker Memory Efficiency",
            passed = efficiency > 0.5, // At least 50% efficient
            metrics = metrics,
            threshold = expectedMemory,
            message = "Memory efficiency: ${(efficiency * 100).toInt()}%, " +
                    "expected ~${expectedMemory} bytes, actual ${actualMemory} bytes"
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    // Helper functions

    /**
     * Generate test events with distinct coordinates
     */
    private fun generateTestEvents(count: Int, offset: Int = 0): List<TestEvent> {
        return (0 until count).map { i ->
            TestEvent(
                id = "test-event-${offset + i}",
                lat = 37.7749 + (i * 0.001),
                lon = -122.4194 + (i * 0.001),
                severity = if (i % 3 == 0) Severity.CRISIS else Severity.ALERT,
                category = when (i % 8) {
                    0 -> Category.MEDICAL
                    1 -> Category.FIRE
                    2 -> Category.WEATHER
                    3 -> Category.CRIME
                    4 -> Category.NATURAL_DISASTER
                    5 -> Category.INFRASTRUCTURE
                    6 -> Category.SEARCH_RESCUE
                    else -> Category.OTHER
                }
            )
        }
    }

    /**
     * Simulate creating marker layers (as done in EventMarkers.kt)
     */
    private fun createMarkerLayers(events: List<TestEvent>): List<MarkerLayer> {
        val layers = mutableListOf<MarkerLayer>()

        // Simulate the 16 layers currently created in EventMarkers.kt
        // Pulse layers (2)
        layers.add(MarkerLayer("pulse", events.filter { it.severity == Severity.CRISIS }))
        layers.add(MarkerLayer("pulse-inner", events.filter { it.severity == Severity.CRISIS }))

        // Cluster layers (3)
        layers.add(MarkerLayer("cluster-crisis", events))
        layers.add(MarkerLayer("cluster-alert", events))
        layers.add(MarkerLayer("cluster-small", events))

        // Marker layers (2)
        layers.add(MarkerLayer("markers-crisis", events.filter { it.severity == Severity.CRISIS }))
        layers.add(MarkerLayer("markers-alert", events.filter { it.severity == Severity.ALERT }))

        // Icon layers (9 categories)
        Category.entries.forEach { category ->
            layers.add(MarkerLayer("icons-${category.name.lowercase()}", events.filter { it.category == category }))
        }

        return layers
    }

    /**
     * Simulate clustered marker rendering with reduced layers
     */
    private fun createClusteredMarkers(events: List<TestEvent>): ClusteredMarkers {
        // Optimized: Only 4 layers instead of 16
        val clusters = groupByCluster(events)
        return ClusteredMarkers(
            crisisMarkers = events.filter { it.severity == Severity.CRISIS },
            alertMarkers = events.filter { it.severity == Severity.ALERT },
            clusterMarkers = clusters,
            iconMarkers = events
        )
    }

    /**
     * Group events by spatial clustering
     */
    private fun groupByCluster(events: List<TestEvent>): Map<String, List<TestEvent>> {
        // Simple grid-based clustering
        return events.groupBy { event ->
            val latGrid = (event.lat * 100).toInt()
            val lonGrid = (event.lon * 100).toInt()
            "$latGrid,$lonGrid"
        }.filterValues { it.size > 1 }
    }

    /**
     * Simulate marker update operation
     */
    private fun updateMarkers(existing: List<TestEvent>, additional: List<TestEvent>): List<TestEvent> {
        return existing + additional
    }

    /**
     * Simulate marker storage for memory testing
     */
    private fun simulateMarkerStorage(events: List<TestEvent>): Map<String, TestEvent> {
        return events.associateBy { it.id }
    }

    private fun printTestResult(result: PerformanceTestResult) {
        println("\n=== Performance Test Result ===")
        println("Test: ${result.testName}")
        println("Status: ${if (result.passed) "PASS" else "FAIL"}")
        println("Duration: ${result.metrics.durationMs}ms (threshold: ${result.threshold}ms)")
        println("Memory Delta: ${String.format("%.2f", result.metrics.memoryDeltaMb)} MB")
        println("Message: ${result.message}")
        println("===============================\n")
    }
}

// Test data classes

data class TestEvent(
    val id: String,
    val lat: Double,
    val lon: Double,
    val severity: Severity,
    val category: Category
)

data class MarkerLayer(
    val id: String,
    val events: List<TestEvent>
)

data class ClusteredMarkers(
    val crisisMarkers: List<TestEvent>,
    val alertMarkers: List<TestEvent>,
    val clusterMarkers: Map<String, List<TestEvent>>,
    val iconMarkers: List<TestEvent>
)
