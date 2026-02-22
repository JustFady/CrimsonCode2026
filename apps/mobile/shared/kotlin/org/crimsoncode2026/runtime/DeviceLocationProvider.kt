package org.crimsoncode2026.runtime

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null
)

expect object DeviceLocationProvider {
    suspend fun getCurrentLocation(): DeviceLocation?
}
