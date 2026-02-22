package org.crimsoncode2026.network

import kotlinx.coroutines.CoroutineScope

/**
 * Factory function to create platform-specific NetworkMonitor
 */
expect fun createNetworkMonitor(scope: CoroutineScope): NetworkMonitor
