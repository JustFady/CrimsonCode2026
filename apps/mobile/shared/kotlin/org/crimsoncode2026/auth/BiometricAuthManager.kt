package org.crimsoncode2026.auth

import dev.icerock.moko.biometry.BiometryAuthenticator
import dev.icerock.moko.biometry.BiometryAuthenticatorFactory
import dev.icerock.moko.biometry.BiometryResult
import dev.icerock.moko.biometry.BiometryType

/**
 * Result of biometric authentication operation
 */
sealed class BiometricResult {
    /**
     * Biometric authentication succeeded
     */
    data object Success : BiometricResult()

    /**
     * Biometric authentication failed (user cancelled, not recognized, etc.)
     */
    data object Failed : BiometricResult()

    /**
     * Error occurred during biometric operation
     * @param message Human-readable error description
     * @param cause Original exception if available
     */
    data class Error(val message: String, val cause: Throwable? = null) : BiometricResult()

    /**
     * Biometric feature is not available on this device
     */
    data object NotAvailable : BiometricResult()
}

/**
 * Result of checking biometric availability
 */
sealed class BiometricAvailability {
    /**
     * Biometric authentication is available
     * @param type The type of biometric available (face, fingerprint, etc.)
     */
    data class Available(val type: BiometryType) : BiometricAvailability()

    /**
     * Biometric authentication is not available
     * @param reason Description of why it's not available
     */
    data class NotAvailable(val reason: String) : BiometricAvailability()
}

/**
 * Biometric Authentication Manager
 *
 * Provides a clean interface for biometric authentication operations
 * with proper error handling. Abstracts the moko-biometry API details.
 *
 * Usage:
 * ```kotlin
 * val manager = BiometricAuthManager()
 *
 * // Check if biometrics are available
 * when (val availability = manager.checkAvailability()) {
 *     is BiometricAvailability.Available -> {
 *         // Perform authentication
 *         val result = manager.authenticate(
 *             title = "Unlock App",
 *             reason = "Authenticate to continue"
 *         )
 *         if (result is BiometricResult.Success) { ... }
 *     }
 *     is BiometricAvailability.NotAvailable -> {
 *         // Handle unavailable biometrics
 *     }
 * }
 * ```
 */
class BiometricAuthManager(
    private val biometryAuthenticator: BiometryAuthenticator = BiometryAuthenticatorFactory().createBiometryAuthenticator()
) {

    /**
     * Check if biometric authentication is available on this device
     * @return BiometricAvailability indicating availability and type
     */
    suspend fun checkAvailability(): BiometricAvailability {
        return try {
            val result = biometryAuthenticator.checkBiometryAvailability()

            when (result) {
                is BiometryResult.Available -> {
                    BiometricAvailability.Available(result.type)
                }
                is BiometryResult.NotAvailable -> {
                    BiometricAvailability.NotAvailable(getNotAvailableReason(result))
                }
                is BiometryResult.Failure -> {
                    BiometricAvailability.NotAvailable(result.error.message ?: "Biometrics unavailable")
                }
            }
        } catch (e: Exception) {
            BiometricAvailability.NotAvailable(e.message ?: "Error checking biometrics")
        }
    }

    /**
     * Perform biometric authentication
     *
     * @param requestTitle Title shown in the biometric prompt dialog
     * @param requestReason Reason shown explaining why authentication is needed
     * @param failureButtonText Text for the cancel/fallback button (default: "Cancel")
     * @param allowDeviceCredentials Whether to allow device passcode as fallback (default: true)
     * @return BiometricResult indicating authentication outcome
     */
    suspend fun authenticate(
        requestTitle: String,
        requestReason: String,
        failureButtonText: String = "Cancel",
        allowDeviceCredentials: Boolean = true
    ): BiometricResult {
        return try {
            val isSuccess = biometryAuthenticator.checkBiometryAuthentication(
                requestTitle = requestTitle,
                requestReason = requestReason,
                failureButtonText = failureButtonText,
                allowDeviceCredentials = allowDeviceCredentials
            )

            if (isSuccess) {
                BiometricResult.Success
            } else {
                BiometricResult.Failed
            }
        } catch (e: Exception) {
            BiometricResult.Error(
                message = getErrorMessage(e),
                cause = e
            )
        }
    }

    /**
     * Get user-friendly error message from exception
     */
    private fun getErrorMessage(e: Exception): String {
        val message = e.message ?: "Unknown error"

        return when {
            message.contains("User canceled", ignoreCase = true) -> "Authentication was cancelled"
            message.contains("User fallback", ignoreCase = true) -> "Using fallback authentication"
            message.contains("Not enrolled", ignoreCase = true) -> "No biometric enrolled. Please set up Face ID or Touch ID"
            message.contains("Locked out", ignoreCase = true) -> "Too many failed attempts. Please use your device passcode"
            message.contains("System cancel", ignoreCase = true) -> "Authentication was interrupted"
            message.contains("Not available", ignoreCase = true) -> "Biometric authentication is not available"
            else -> "Authentication failed. Please try again"
        }
    }

    /**
     * Get human-readable reason for biometric unavailability
     */
    private fun getNotAvailableReason(result: BiometryResult.NotAvailable): String {
        return when {
            result.reason.contains("No hardware", ignoreCase = true) -> "This device does not support biometrics"
            result.reason.contains("Not enrolled", ignoreCase = true) -> "No biometric enrolled. Please set up Face ID or Touch ID in device settings"
            result.reason.contains("Locked out", ignoreCase = true) -> "Biometrics are temporarily locked. Please use your device passcode"
            result.reason.contains("Permission", ignoreCase = true) -> "Biometric permission not granted"
            else -> result.reason
        }
    }

    /**
     * Check if biometric authentication is likely to succeed
     * Returns true if biometrics are available and enrolled
     */
    suspend fun canAuthenticate(): Boolean {
        return when (checkAvailability()) {
            is BiometricAvailability.Available -> true
            else -> false
        }
    }
}
