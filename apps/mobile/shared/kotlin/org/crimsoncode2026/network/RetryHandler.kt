package org.crimsoncode2026.network

import kotlinx.coroutines.delay

/**
 * Configuration for retry behavior
 */
data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 30000,
    val backoffMultiplier: Double = 2.0,
    val retryableErrors: Set<String> = DEFAULT_RETRYABLE_ERRORS
) {
    companion object {
        val DEFAULT_RETRYABLE_ERRORS = setOf(
            "NetworkException",
            "SocketTimeoutException",
            "ConnectTimeoutException",
            "UnknownHostException",
            "SocketException",
            "EOFException",
            "IOException",
            "SSLException",
            "HTTP 429", // Rate limit
            "HTTP 502", // Bad gateway
            "HTTP 503", // Service unavailable
            "HTTP 504"  // Gateway timeout
        )
    }
}

/**
 * Result of a retry operation
 */
sealed class RetryResult<out T> {
    data class Success<T>(val value: T, val attempts: Int) : RetryResult<T>()
    data class Failure<T>(val error: Throwable, val attempts: Int) : RetryResult<T>()
}

/**
 * Determines if an error is retryable
 */
private fun isRetryableError(error: Throwable, config: RetryConfig): Boolean {
    val errorMessage = error.message ?: ""
    val errorClassName = error::class.simpleName ?: ""

    // Check against known retryable error types
    return config.retryableErrors.any { pattern ->
        errorMessage.contains(pattern, ignoreCase = true) ||
        errorClassName.contains(pattern, ignoreCase = true)
    }
}

/**
 * Calculate delay for retry attempt using exponential backoff
 */
private fun calculateDelay(attempt: Int, config: RetryConfig): Long {
    val delay = (config.initialDelayMs * kotlin.math.pow(config.backoffMultiplier, (attempt - 1).toDouble())).toLong()
    return minOf(delay, config.maxDelayMs)
}

/**
 * Retry handler for suspend functions with exponential backoff
 *
 * Retry configuration:
 * - Exponential backoff: delay increases with each attempt
 * - Max delay caps at config.maxDelayMs
 * - Only retries on network-related errors
 *
 * Usage:
 * ```kotlin
 * suspend fun createEvent(event: Event): Result<Event> {
 *     return retryHandler.retry(config) {
 *         eventRepository.createEvent(event)
 *     }
 * }
 * ```
 */
class RetryHandler(
    private val config: RetryConfig = RetryConfig()
) {

    /**
     * Execute a suspend function with retry logic
     *
     * @param block The suspend function to execute
     * @return RetryResult with success value or failure error
     */
    suspend fun <T> retry(block: suspend () -> T): RetryResult<T> {
        var lastError: Throwable? = null

        for (attempt in 1..config.maxAttempts) {
            try {
                val result = block()
                return RetryResult.Success(result, attempt)
            } catch (e: Exception) {
                lastError = e

                // If this was the last attempt or error is not retryable, fail immediately
                if (attempt >= config.maxAttempts || !isRetryableError(e, config)) {
                    return RetryResult.Failure(e, attempt)
                }

                // Calculate delay and wait before retry
                val delay = calculateDelay(attempt, config)
                delay(delay)
            }
        }

        return RetryResult.Failure(
            lastError ?: Exception("Unknown error"),
            config.maxAttempts
        )
    }

    /**
     * Execute a suspend function returning Result with retry logic
     *
     * @param block The suspend function to execute
     * @return Result with success value or failure error
     */
    suspend fun <T> retryResult(block: suspend () -> Result<T>): Result<T> {
        var lastError: Throwable? = null

        for (attempt in 1..config.maxAttempts) {
            try {
                val result = block()
                if (result.isSuccess) {
                    return result
                }
                val exception = result.exceptionOrNull()
                if (exception != null) {
                    lastError = exception
                    if (attempt >= config.maxAttempts || !isRetryableError(exception, config)) {
                        return result
                    }
                    val delay = calculateDelay(attempt, config)
                    delay(delay)
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt >= config.maxAttempts || !isRetryableError(e, config)) {
                    return Result.failure(e)
                }
                val delay = calculateDelay(attempt, config)
                delay(delay)
            }
        }

        return Result.failure(
            lastError ?: Exception("Unknown error after ${config.maxAttempts} attempts")
        )
    }
}

/**
 * Convenience function to create a retry handler with custom config
 */
fun retryHandler(config: RetryConfig = RetryConfig()): RetryHandler {
    return RetryHandler(config)
}

/**
 * Convenience function to execute a block with retry
 */
suspend fun <T> withRetry(
    config: RetryConfig = RetryConfig(),
    block: suspend () -> T
): RetryResult<T> {
    return retryHandler(config).retry(block)
}

/**
 * Convenience function to execute a Result-returning block with retry
 */
suspend fun <T> withRetryResult(
    config: RetryConfig = RetryConfig(),
    block: suspend () -> Result<T>
): Result<T> {
    return retryHandler(config).retryResult(block)
}
