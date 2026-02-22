package org.crimsoncode2026.performance

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Severity
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Performance tests for event creation latency
 *
 * Tests:
 * - Event validation time
 * - Event serialization time
 * - Event submission preparation time
 * - Memory usage for event objects
 */
class EventCreationPerformanceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val timer = PerformanceTimer()
    private val memoryTracker = MemoryTracker()

    /**
     * Test event validation performance
     * Target: <10ms for validation
     */
    @Test
    fun testEventValidationPerformance() = runTest {
        val event = createTestEvent()

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate validation checks
            validateEvent(event)
        }
        memoryTracker.stop()

        val metrics = memoryTracker.getMetrics("Event Validation", duration)
        val testResult = PerformanceTestResult(
            testName = "Event Validation",
            passed = metrics.isDurationAcceptable(10),
            metrics = metrics,
            threshold = 10,
            message = if (metrics.isDurationAcceptable(10)) {
                "PASS: Validated event in ${duration}ms"
            } else {
                "FAIL: Validation took ${duration}ms, threshold is 10ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test event serialization performance
     * Target: <50ms for JSON serialization
     */
    @Test
    fun testEventSerializationPerformance() = runTest {
        val event = createTestEvent()

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate JSON serialization
            json.encodeToString(TestEvent.serializer(), event)
        }
        memoryTracker.stop()

        val metrics = memoryTracker.getMetrics("Event Serialization", duration)
        val testResult = PerformanceTestResult(
            testName = "Event Serialization",
            passed = metrics.isDurationAcceptable(50),
            metrics = metrics,
            threshold = 50,
            message = if (metrics.isDurationAcceptable(50)) {
                "PASS: Serialized event in ${duration}ms"
            } else {
                "FAIL: Serialization took ${duration}ms, threshold is 50ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test full event creation flow performance
     * Target: <2000ms (2 seconds) total
     */
    @Test
    fun testFullEventCreationFlowPerformance() = runTest {
        val eventData = createTestEventData()

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate the full flow:
            // 1. Validate inputs
            validateEventData(eventData)
            // 2. Create event object
            val event = createEventFromData(eventData)
            // 3. Prepare for submission
            prepareEventForSubmission(event)
            // 4. Serialize
            json.encodeToString(TestEvent.serializer(), event)
        }
        memoryTracker.stop()

        val metrics = memoryTracker.getMetrics("Full Event Creation Flow", duration)
        val testResult = PerformanceTestResult(
            testName = "Full Event Creation Flow",
            passed = metrics.isDurationAcceptable(PerformanceThresholds.EVENT_CREATE_SUBMIT_MS),
            metrics = metrics,
            threshold = PerformanceThresholds.EVENT_CREATE_SUBMIT_MS,
            message = if (metrics.isDurationAcceptable(PerformanceThresholds.EVENT_CREATE_SUBMIT_MS)) {
                "PASS: Created event in ${duration}ms"
            } else {
                "FAIL: Creation took ${duration}ms, threshold is ${PerformanceThresholds.EVENT_CREATE_SUBMIT_MS}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test event query performance
     * Target: <1000ms for querying events
     */
    @Test
    fun testEventQueryPerformance() = runTest {
        val events = generateTestEvents(200)

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate querying events by bounds
            queryEventsByBounds(events, bounds = TestBounds(37.7, 37.9, -122.5, -122.3))
        }
        memoryTracker.stop()

        val metrics = memoryTracker.getMetrics("Event Query", duration)
        val testResult = PerformanceTestResult(
            testName = "Event Query (200 items)",
            passed = metrics.isDurationAcceptable(PerformanceThresholds.EVENT_QUERY_MS),
            metrics = metrics,
            threshold = PerformanceThresholds.EVENT_QUERY_MS,
            message = if (metrics.isDurationAcceptable(PerformanceThresholds.EVENT_QUERY_MS)) {
                "PASS: Queried ${result.size} events in ${duration}ms"
            } else {
                "FAIL: Query took ${duration}ms, threshold is ${PerformanceThresholds.EVENT_QUERY_MS}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test memory efficiency of event storage
     */
    @Test
    fun testEventMemoryEfficiency() = runTest {
        val events = generateTestEvents(100)

        memoryTracker.start()
        val stored = simulateEventStorage(events)
        memoryTracker.stop()

        val expectedMemory = events.size * 1024 // 1KB per event estimate
        val actualMemory = memoryTracker.getDelta()
        val efficiency = expectedMemory.toDouble() / actualMemory.toDouble()

        val metrics = memoryTracker.getMetrics("Event Memory Efficiency", memoryTracker.getElapsedMs())
        val testResult = PerformanceTestResult(
            testName = "Event Memory Efficiency",
            passed = efficiency > 0.3, // At least 30% efficient
            metrics = metrics,
            threshold = expectedMemory,
            message = "Memory efficiency: ${(efficiency * 100).toInt()}%, " +
                    "expected ~${expectedMemory} bytes, actual ${actualMemory} bytes"
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test cleared events filtering performance
     */
    @Test
    fun testClearedEventsFilterPerformance() = runTest {
        val events = generateTestEvents(500)
        val clearedIds = (0 until 50).map { "cleared-$it" }.toSet()

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate filtering out cleared events
            filterClearedEvents(events, clearedIds)
        }
        memoryTracker.stop()

        val metrics = memoryTracker.getMetrics("Cleared Events Filter", duration)
        val testResult = PerformanceTestResult(
            testName = "Cleared Events Filter (500 items)",
            passed = metrics.isDurationAcceptable(50),
            metrics = metrics,
            threshold = 50,
            message = if (metrics.isDurationAcceptable(50)) {
                "PASS: Filtered ${result.size} of ${events.size} events in ${duration}ms"
            } else {
                "FAIL: Filter took ${duration}ms, threshold is 50ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    // Helper functions

    private fun createTestEvent(): TestEvent {
        return TestEvent(
            id = "test-event-1",
            creatorId = "test-user",
            severity = Severity.CRISIS,
            category = Category.MEDICAL,
            lat = 37.7749,
            lon = -122.4194,
            locationOverride = null,
            broadcastType = "PUBLIC",
            description = "This is a test event description for performance testing",
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (48 * 60 * 60 * 1000),
            deletedAt = null
        )
    }

    private fun createTestEventData(): EventData {
        return EventData(
            severity = Severity.CRISIS,
            category = Category.MEDICAL,
            broadcastType = "PUBLIC",
            description = "Test event for performance measurement",
            lat = 37.7749,
            lon = -122.4194
        )
    }

    private fun generateTestEvents(count: Int): List<TestEvent> {
        return (0 until count).map { i ->
            TestEvent(
                id = "event-$i",
                creatorId = "user-$i",
                severity = if (i % 3 == 0) Severity.CRISIS else Severity.ALERT,
                category = Category.entries[i % Category.entries.size],
                lat = 37.7 + (i * 0.0001),
                lon = -122.4 + (i * 0.0001),
                locationOverride = null,
                broadcastType = if (i % 2 == 0) "PUBLIC" else "PRIVATE",
                description = "Event $i description",
                isAnonymous = true,
                createdAt = System.currentTimeMillis() - (i * 60000L),
                expiresAt = System.currentTimeMillis() + (48 * 60 * 60 * 1000),
                deletedAt = null
            )
        }
    }

    private fun validateEvent(event: TestEvent): Boolean {
        return event.id.isNotEmpty() &&
                event.severity in listOf(Severity.ALERT, Severity.CRISIS) &&
                event.category in Category.entries &&
                event.lat in -90.0..90.0 &&
                event.lon in -180.0..180.0 &&
                event.description.isNotEmpty() &&
                event.description.length <= 500
    }

    private fun validateEventData(data: EventData): Boolean {
        return data.severity != null &&
                data.category != null &&
                data.broadcastType != null &&
                data.lat in -90.0..90.0 &&
                data.lon in -180.0..180.0
    }

    private fun createEventFromData(data: EventData): TestEvent {
        return TestEvent(
            id = "generated-${System.currentTimeMillis()}",
            creatorId = "test-user",
            severity = data.severity!!,
            category = data.category!!,
            lat = data.lat,
            lon = data.lon,
            locationOverride = null,
            broadcastType = data.broadcastType!!,
            description = data.description,
            isAnonymous = true,
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (48 * 60 * 60 * 1000),
            deletedAt = null
        )
    }

    private fun prepareEventForSubmission(event: TestEvent): Map<String, Any> {
        return mapOf(
            "severity" to event.severity.name,
            "category" to event.category.name,
            "broadcast_type" to event.broadcastType,
            "description" to event.description,
            "lat" to event.lat,
            "lon" to event.lon,
            "is_anonymous" to event.isAnonymous
        )
    }

    private fun queryEventsByBounds(events: List<TestEvent>, bounds: TestBounds): List<TestEvent> {
        return events.filter { event ->
            event.lat >= bounds.minLat && event.lat <= bounds.maxLat &&
                    event.lon >= bounds.minLon && event.lon <= bounds.maxLon
        }
    }

    private fun filterClearedEvents(events: List<TestEvent>, clearedIds: Set<String>): List<TestEvent> {
        return events.filterNot { it.id in clearedIds }
    }

    private fun simulateEventStorage(events: List<TestEvent>): Map<String, TestEvent> {
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

@kotlinx.serialization.Serializable
data class TestEvent(
    val id: String,
    val creatorId: String,
    val severity: Severity,
    val category: Category,
    val lat: Double,
    val lon: Double,
    val locationOverride: String?,
    val broadcastType: String,
    val description: String,
    val isAnonymous: Boolean,
    val createdAt: Long,
    val expiresAt: Long,
    val deletedAt: Long?
)

data class EventData(
    val severity: Severity?,
    val category: Category?,
    val broadcastType: String?,
    val description: String,
    val lat: Double,
    val lon: Double
)

data class TestBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)
