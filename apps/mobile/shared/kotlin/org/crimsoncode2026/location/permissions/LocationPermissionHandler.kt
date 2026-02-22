package org.crimsoncode2026.location.permissions

import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Handles location permissions for the app
 *
 * Uses moko-permissions for cross-platform permission management.
 * Only foreground location permission is used in MVP (background not required).
 */
class LocationPermissionHandler(
    private val permissionsController: PermissionsController
) {

    /**
     * Checks if foreground location permission is granted
     */
    fun isLocationPermissionGranted(): Boolean {
        return permissionsController.isPermissionGranted(Permission.LOCATION)
    }

    /**
     * Provides a Flow of permission state for location
     */
    fun locationPermissionState(): Flow<LocationPermissionState> {
        return permissionsController.observePermission(Permission.LOCATION)
            .map { state ->
                when {
                    state.granted -> LocationPermissionState.Granted
                    state.shouldShowRationale -> LocationPermissionState.DeniedShouldShowRationale
                    else -> LocationPermissionState.Denied
                }
            }
    }

    /**
     * Requests foreground location permission
     * @return true if permission granted, false otherwise
     */
    suspend fun requestLocationPermission(): Boolean {
        return try {
            permissionsController.providePermission(Permission.LOCATION)
            isLocationPermissionGranted()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens app settings (for when user has permanently denied permission)
     * This is platform-specific and will be implemented via expect/actual if needed
     */
    fun openAppSettings() {
        // moko-permissions doesn't provide a cross-platform settings API
        // Platform-specific implementation would go here if needed
    }
}

/**
 * Represents the current state of location permission
 */
sealed class LocationPermissionState {
    data object Granted : LocationPermissionState()
    data object Denied : LocationPermissionState()
    data object DeniedShouldShowRationale : LocationPermissionState()
}
