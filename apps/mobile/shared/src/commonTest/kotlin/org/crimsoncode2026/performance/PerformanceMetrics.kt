package org.crimsoncode2026.performance

import kotlinx.serialization.Serializable

/**
 * Performance metrics captured during testing
 */
@Serializable
data class PerformanceMetrics(
    val testName: String,
    val durationMs: Long,
    val memoryBeforeBytes: Long,
    val memoryAfterBytes: Long,
    val memoryDeltaBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Memory difference in MB
     */
    val memoryDeltaMb: Double
        get() = memoryDeltaBytes / (1024.0 * 1024.0)

    /**
     * Check if memory usage is acceptable (<10MB for single operation)
     */
    fun isMemoryUsageAcceptable(): Boolean = memoryDeltaBytes < (10 * 1024 * 1024)

    /**
     * Check if duration is acceptable (<2s for operations, <100ms for UI updates)
     */
    fun isDurationAcceptable(thresholdMs: Long): Boolean = durationMs <= thresholdMs
}

/**
 * Performance thresholds for different operations
 */
object PerformanceThresholds {
    // Map operations
    const val MAP_MARKER_RENDER_MS = 100L
    const val MAP_CLUSTER_UPDATE_MS = 150L
    const val MAP_ZOOM_PAN_MS = 50L

    // List operations
    const val LIST_SCROLL_FPS_MS = 16L // 60fps target
    const val LIST_FILTER_MS = 50L
    const val LIST_LOAD_100_ITEMS_MS = 200L

    // Event operations
    const val EVENT_CREATE_SUBMIT_MS = 2000L
    const val EVENT_QUERY_MS = 1000L

    // Realtime operations
    const val REALTIME_SUBSCRIBE_MS = 500L
    const val REALTIME_MESSAGE_PROCESS_MS = 10L

    // Memory thresholds
    const val MEMORY_SINGLE_OPERATION_MB = 10.0
    const val MEMORY_SESSION_MB = 50.0
    const val MEMORY_MAP_MARKER_ITEM_BYTES = 500 // bytes per marker
}

/**
 * Memory tracker for performance testing
 */
class MemoryTracker {
    private var startMemory: Long = 0
    private var endMemory: Long = 0

    /**
     * Start tracking memory
     */
    fun start() {
        startMemory = getUsedMemory()
    }

    /**
     * Stop tracking memory
     */
    fun stop() {
        endMemory = getUsedMemory()
    }

    /**
     * Get memory delta in bytes
     */
    fun getDelta(): Long = endMemory - startMemory

    /**
     * Get approximate used memory in bytes
     * Note: This is a common implementation that works across platforms
     */
    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    /**
     * Get metrics for a test
     */
    fun getMetrics(testName: String, durationMs: Long): PerformanceMetrics {
        return PerformanceMetrics(
            testName = testName,
            durationMs = durationMs,
            memoryBeforeBytes = startMemory,
            memoryAfterBytes = endMemory,
            memoryDeltaBytes = getDelta()
        )
    }
}

/**
 * Performance timer utility
 */
class PerformanceTimer {
    private var startTime: Long = 0
    private var endTime: Long = 0

    /**
     * Start the timer
     */
    fun start() {
        startTime = System.nanoTime()
    }

    /**
     * Stop the timer
     */
    fun stop() {
        endTime = System.nanoTime()
    }

    /**
     * Get elapsed time in milliseconds
     */
    fun getElapsedMs(): Long = (endTime - startTime) / 1_000_000

    /**
     * Get elapsed time in nanoseconds
     */
    fun getElapsedNs(): Long = endTime - startTime

    /**
     * Time a block of code and return duration
     */
    inline fun <T> measure(block: () -> T): Pair<T, Long> {
        start()
        val result = block()
        stop()
        return result to getElapsedMs()
    }

    /**
     * Time a suspend block of code
     */
    suspend inline fun <T> measureSuspend(block: suspend () -> T): Pair<T, Long> {
        start()
        val result = block()
        stop()
        return result to getElapsedMs()
    }
}

/**
 * Performance test result
 */
data class PerformanceTestResult(
    val testName: String,
    val passed: Boolean,
    val metrics: PerformanceMetrics,
    val threshold: Long,
    val message: String
)
