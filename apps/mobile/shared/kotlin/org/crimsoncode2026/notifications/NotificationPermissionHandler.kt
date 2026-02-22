package org.crimsoncode2026.notifications

import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Handles notification permissions for the app
 *
 * Uses moko-permissions for cross-platform permission management.
 * Required for KMPNotifier/Firebase FCM push notifications.
 */
class NotificationPermissionHandler(
    private val permissionsController: PermissionsController
) {

    /**
     * Checks if notification permission is granted
     */
    fun isNotificationPermissionGranted(): Boolean {
        return permissionsController.isPermissionGranted(Permission.NOTIFICATION)
    }

    /**
     * Provides a Flow of permission state for notifications
     */
    fun notificationPermissionState(): Flow<NotificationPermissionState> {
        return permissionsController.observePermission(Permission.NOTIFICATION)
            .map { state ->
                when {
                    state.granted -> NotificationPermissionState.Granted
                    state.shouldShowRationale -> NotificationPermissionState.DeniedShouldShowRationale
                    else -> NotificationPermissionState.Denied
                }
            }
    }

    /**
     * Requests notification permission
     * @return true if permission granted, false otherwise
     */
    suspend fun requestNotificationPermission(): Boolean {
        return try {
            permissionsController.providePermission(Permission.NOTIFICATION)
            isNotificationPermissionGranted()
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
 * Represents the current state of notification permission
 */
sealed class NotificationPermissionState {
    data object Granted : NotificationPermissionState()
    data object Denied : NotificationPermissionState()
    data object DeniedShouldShowRationale : NotificationPermissionState()
}
