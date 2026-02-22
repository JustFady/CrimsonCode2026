package org.crimsoncode2026.runtime

actual object DeviceLocationProvider {
    actual suspend fun getCurrentLocation(): DeviceLocation? = null
}
