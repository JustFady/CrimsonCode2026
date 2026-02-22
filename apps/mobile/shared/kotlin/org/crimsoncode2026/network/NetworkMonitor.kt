package org.crimsoncode2026.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Network monitoring service
 *
 * Provides real-time network status and connectivity information.
 * Platform-specific implementations handle connectivity detection.
 */
interface NetworkMonitor {

    /**
     * Current network status as a flow
     */
    val networkStatus: StateFlow<NetworkInfo>

    /**
     * Current network info (synchronous)
     */
    val currentNetworkInfo: NetworkInfo
        get() = networkStatus.value

    /**
     * Check if network is available
     */
    val isOnline: Boolean
        get() = currentNetworkInfo.isOnline

    /**
     * Start monitoring network status
     */
    fun startMonitoring()

    /**
     * Stop monitoring network status
     */
    fun stopMonitoring()
}
