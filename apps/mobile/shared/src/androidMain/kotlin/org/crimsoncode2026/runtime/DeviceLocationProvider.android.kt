package org.crimsoncode2026.runtime

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object AndroidRuntimeBridge {
    var appContext: Context? = null
}

actual object DeviceLocationProvider {
    actual suspend fun getCurrentLocation(): DeviceLocation? {
        val context = AndroidRuntimeBridge.appContext ?: return null
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        val client = LocationServices.getFusedLocationProviderClient(context)
        val priority = if (hasFine) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val lastKnown = suspendCancellableCoroutine<android.location.Location?> { cont ->
            client.lastLocation
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }

        val fresh = suspendCancellableCoroutine<android.location.Location?> { cont ->
            val tokenSource = CancellationTokenSource()
            cont.invokeOnCancellation { tokenSource.cancel() }
            client.getCurrentLocation(priority, tokenSource.token)
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { e ->
                    if (cont.isActive) {
                        cont.resumeWithException(e)
                    }
                }
        }

        val location = when {
            isUsableFreshLocation(fresh) -> fresh
            isUsableCachedLocation(lastKnown) -> lastKnown
            fresh != null -> fresh
            else -> lastKnown
        }

        return location?.let {
            DeviceLocation(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracyMeters = if (it.hasAccuracy()) it.accuracy else null
            )
        }
    }

    private fun isUsableFreshLocation(location: android.location.Location?): Boolean {
        if (location == null) return false
        if (!location.hasAccuracy()) return true
        return location.accuracy <= 80f
    }

    private fun isUsableCachedLocation(location: android.location.Location?): Boolean {
        if (location == null) return false
        val ageMs = kotlin.math.abs(System.currentTimeMillis() - location.time)
        val accuracyOk = !location.hasAccuracy() || location.accuracy <= 150f
        return ageMs <= 2 * 60_000L && accuracyOk
    }
}
