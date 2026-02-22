package org.crimsoncode2026.performance

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Severity
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Performance tests for event list scrolling and filtering
 *
 * Tests:
 * - Loading and displaying 100 items
 * - Scroll performance (simulated)
 * - Filter operations
 * - Memory usage for large lists
 */
class EventListPerformanceTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val timer = PerformanceTimer()
    private val memoryTracker = MemoryTracker()

    /**
     * Test list loading performance for 100 items
     * Target: <200ms load time, <10MB memory
     */
    @Test
    fun testListLoad100ItemsPerformance() = runTest {
        val items = generateTestEventItems(100)

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate LazyColumn items rendering
            simulateListRendering(items)
        }
        memoryTracker.stop()

        val metrics = memoryTracker.getMetrics("List Load (100 items)", duration)
        val testResult = PerformanceTestResult(
            testName = "List Load (100 items)",
            passed = metrics.isDurationAcceptable(PerformanceThresholds.LIST_LOAD_100_ITEMS_MS) &&
                    metrics.isMemoryUsageAcceptable(),
            metrics = metrics,
            threshold = PerformanceThresholds.LIST_LOAD_100_ITEMS_MS,
            message = if (metrics.isDurationAcceptable(PerformanceThresholds.LIST_LOAD_100_ITEMS_MS)) {
                "PASS: Loaded ${items.size} items in ${duration}ms"
            } else {
                "FAIL: Load took ${duration}ms, threshold is ${PerformanceThresholds.LIST_LOAD_100_ITEMS_MS}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test list loading performance for 500 items (stress test)
     */
    @Test
    fun testListLoad500ItemsPerformance() = runTest {
        val items = generateTestEventItems(500)

        memoryTracker.start()
        val (result, duration) = timer.measure {
            simulateListRendering(items)
        }
        memoryTracker.stop()

        // Allow more time for larger lists
        val threshold = PerformanceThresholds.LIST_LOAD_100_ITEMS_MS * 3
        val testResult = PerformanceTestResult(
            testName = "List Load (500 items)",
            passed = metrics.isDurationAcceptable(threshold) &&
                    metrics.memoryDeltaBytes < (30 * 1024 * 1024), // 30MB
            metrics = metrics,
            threshold = threshold,
            message = if (metrics.isDurationAcceptable(threshold)) {
                "PASS: Loaded ${items.size} items in ${duration}ms"
            } else {
                "FAIL: Load took ${duration}ms, threshold is ${threshold}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test filter performance
     * Simulates filtering by severity, category, and broadcast type
     */
    @Test
    fun testFilterPerformance() = runTest {
        val items = generateTestEventItems(200)

        memoryTracker.start()
        val (result, duration) = timer.measure {
            // Simulate the 3 filter operations in EventListView
            applyAllFilters(items)
        }
        memoryTracker.stop()

        val metrics = memoryTracker.getMetrics("Filter (200 items)", duration)
        val testResult = PerformanceTestResult(
            testName = "Filter (200 items)",
            passed = metrics.isDurationAcceptable(PerformanceThresholds.LIST_FILTER_MS),
            metrics = metrics,
            threshold = PerformanceThresholds.LIST_FILTER_MS,
            message = if (metrics.isDurationAcceptable(PerformanceThresholds.LIST_FILTER_MS)) {
                "PASS: Filtered ${items.size} items in ${duration}ms"
            } else {
                "FAIL: Filter took ${duration}ms, threshold is ${PerformanceThresholds.LIST_FILTER_MS}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test repeated filter operations (simulates user changing filters)
     */
    @Test
    fun testRepeatedFilterPerformance() = runTest {
        val items = generateTestEventItems(200)
        val iterations = 20

        memoryTracker.start()
        timer.start()
        repeat(iterations) {
            applyAllFilters(items)
        }
        timer.stop()
        memoryTracker.stop()

        val avgDuration = timer.getElapsedMs() / iterations
        val metrics = memoryTracker.getMetrics("Repeated Filter (20x)", timer.getElapsedMs())
        val testResult = PerformanceTestResult(
            testName = "Repeated Filter (20x)",
            passed = avgDuration <= PerformanceThresholds.LIST_FILTER_MS,
            metrics = metrics,
            threshold = PerformanceThresholds.LIST_FILTER_MS,
            message = if (avgDuration <= PerformanceThresholds.LIST_FILTER_MS) {
                "PASS: Average filter time ${avgDuration}ms over $iterations iterations"
            } else {
                "FAIL: Average filter time ${avgDuration}ms, threshold is ${PerformanceThresholds.LIST_FILTER_MS}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test scroll performance (simulated by measuring item access time)
     * Target: <16ms per frame (60fps)
     */
    @Test
    fun testScrollPerformance() = runTest {
        val items = generateTestEventItems(100)

        memoryTracker.start()
        timer.start()
        // Simulate scrolling through visible items
        val visibleItems = simulateScrolling(items, windowSize = 10)
        timer.stop()
        memoryTracker.stop()

        val durationPerFrame = timer.getElapsedMs() / visibleItems.size
        val metrics = memoryTracker.getMetrics("Scroll (100 items)", timer.getElapsedMs())
        val testResult = PerformanceTestResult(
            testName = "Scroll (100 items)",
            passed = durationPerFrame <= PerformanceThresholds.LIST_SCROLL_FPS_MS,
            metrics = metrics,
            threshold = PerformanceThresholds.LIST_SCROLL_FPS_MS,
            message = if (durationPerFrame <= PerformanceThresholds.LIST_SCROLL_FPS_MS) {
                "PASS: ${durationPerFrame}ms per frame (~${(1000.0 / durationPerFrame).toInt()}fps)"
            } else {
                "FAIL: ${durationPerFrame}ms per frame, threshold is ${PerformanceThresholds.LIST_SCROLL_FPS_MS}ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    /**
     * Test list item composition performance
     */
    @Test
    fun testListItemCompositionPerformance() = runTest {
        val items = generateTestEventItems(50)

        memoryTracker.start()
        val (result, duration) = timer.measure {
            items.forEach { item ->
                // Simulate item composition
                composeListItem(item)
            }
        }
        memoryTracker.stop()

        val avgTimePerItem = duration / items.size
        val metrics = memoryTracker.getMetrics("Item Composition (50)", duration)
        val testResult = PerformanceTestResult(
            testName = "Item Composition (50)",
            passed = avgTimePerItem <= 5, // 5ms per item max
            metrics = metrics,
            threshold = 5,
            message = if (avgTimePerItem <= 5) {
                "PASS: Average composition time ${avgTimePerItem}ms per item"
            } else {
                "FAIL: Average composition time ${avgTimePerItem}ms per item, threshold is 5ms"
            }
        )

        printTestResult(testResult)
        assertTrue(testResult.passed, testResult.message)
    }

    // Helper functions

    /**
     * Generate test event items
     */
    private fun generateTestEventItems(count: Int): List<TestEventItem> {
        return (0 until count).map { i ->
            TestEventItem(
                id = "test-item-$i",
                severity = if (i % 3 == 0) Severity.CRISIS else Severity.ALERT,
                category = Category.entries[i % Category.entries.size],
                isPrivate = i % 2 == 0,
                description = "Test event description for item $i",
                createdAt = System.currentTimeMillis() - (i * 60000L)
            )
        }
    }

    /**
     * Simulate LazyColumn rendering
     */
    private fun simulateListRendering(items: List<TestEventItem>): List<ListItem> {
        return items.map { item ->
            ListItem(
                id = item.id,
                severity = item.severity,
                category = item.category,
                description = item.description,
                timeAgo = formatTimeAgo(item.createdAt)
            )
        }
    }

    /**
     * Simulate the 3 filter operations from EventListView.kt
     */
    private fun applyAllFilters(items: List<TestEventItem>): Map<String, List<TestEventItem>> {
        // Severity filter
        val severityFiltered = items.filter { true } // All for now

        // Category filter
        val categoryFiltered = severityFiltered.filter { true } // All for now

        // Broadcast type filter
        val privateEvents = categoryFiltered.filter { it.isPrivate }
        val publicEvents = categoryFiltered.filter { !it.isPrivate }

        return mapOf(
            "private" to privateEvents,
            "public" to publicEvents
        )
    }

    /**
     * Simulate scrolling by accessing items in a sliding window
     */
    private fun simulateScrolling(items: List<TestEventItem>, windowSize: Int): List<ListItem> {
        val result = mutableListOf<ListItem>()
        for (i in 0..items.size step windowSize) {
            val endIndex = minOf(i + windowSize, items.size)
            val window = items.subList(i, endIndex)
            window.forEach { item ->
                result.add(ListItem(
                    id = item.id,
                    severity = item.severity,
                    category = item.category,
                    description = item.description,
                    timeAgo = formatTimeAgo(item.createdAt)
                ))
            }
        }
        return result
    }

    /**
     * Simulate composing a single list item
     */
    private fun composeListItem(item: TestEventItem): String {
        return buildString {
            append(item.severity.name)
            append(" ")
            append(item.category.displayName)
            append(" ")
            append(item.description.take(60))
        }
    }

    /**
     * Format timestamp for display
     */
    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestamp
        val diffMinutes = diffMs / (1000 * 60)

        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes.toInt()}m ago"
            else -> "${(diffMinutes / 60).toInt()}h ago"
        }
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

data class TestEventItem(
    val id: String,
    val severity: Severity,
    val category: Category,
    val isPrivate: Boolean,
    val description: String,
    val createdAt: Long
)

data class ListItem(
    val id: String,
    val severity: Severity,
    val category: Category,
    val description: String,
    val timeAgo: String
)
