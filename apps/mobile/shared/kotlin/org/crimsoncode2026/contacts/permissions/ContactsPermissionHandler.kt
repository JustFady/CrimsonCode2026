package org.crimsoncode2026.contacts.permissions

import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Handles contacts permissions for the app
 *
 * Uses moko-permissions for cross-platform permission management.
 * Required for importing device contacts as emergency contacts.
 */
class ContactsPermissionHandler(
    private val permissionsController: PermissionsController
) {

    /**
     * Checks if contacts permission is granted
     */
    fun isContactsPermissionGranted(): Boolean {
        return permissionsController.isPermissionGranted(Permission.CONTACTS)
    }

    /**
     * Provides a Flow of permission state for contacts
     */
    fun contactsPermissionState(): Flow<ContactsPermissionState> {
        return permissionsController.observePermission(Permission.CONTACTS)
            .map { state ->
                when {
                    state.granted -> ContactsPermissionState.Granted
                    state.shouldShowRationale -> ContactsPermissionState.DeniedShouldShowRationale
                    else -> ContactsPermissionState.Denied
                }
            }
    }

    /**
     * Requests contacts permission
     * @return true if permission granted, false otherwise
     */
    suspend fun requestContactsPermission(): Boolean {
        return try {
            permissionsController.providePermission(Permission.CONTACTS)
            isContactsPermissionGranted()
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
 * Represents the current state of contacts permission
 */
sealed class ContactsPermissionState {
    data object Granted : ContactsPermissionState()
    data object Denied : ContactsPermissionState()
    data object DeniedShouldShowRationale : ContactsPermissionState()
}
