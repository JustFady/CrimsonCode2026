package org.crimsoncode2026

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.first
import org.crimsoncode2026.auth.AuthRepository
import org.crimsoncode2026.screens.auth.BiometricUnlockScreen
import org.crimsoncode2026.screens.auth.DisplayNameScreen
import org.crimsoncode2026.screens.auth.OtpVerificationScreen
import org.crimsoncode2026.screens.auth.PhoneEntryScreen
import org.crimsoncode2026.screens.main.MainScreen
import org.crimsoncode2026.screens.settings.SettingsScreen
import org.crimsoncode2026.domain.usecases.SessionInitUseCase
import org.crimsoncode2026.domain.usecases.SessionInitResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.serialization.Serializable

// Navigation destinations
@Serializable
object SessionInitDestination

@Serializable
object PhoneEntryDestination

@Serializable
data class OtpVerificationDestination(val phoneNumber: String)

@Serializable
object DisplayNameDestination

@Serializable
object BiometricUnlockDestination

@Serializable
object MainDestination

@Serializable
object SettingsDestination

/**
 * Main App composable
 * Handles navigation and authentication flow using SessionInitUseCase
 */
@Composable
fun App() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface {
            val navController: NavHostController = rememberNavController()

            NavHost(navController = navController, startDestination = SessionInitDestination) {
                // Session initialization - determines auth flow
                composable<SessionInitDestination> {
                    SessionInitScreen(
                        onNeedsOtpLogin = {
                            navController.navigate(PhoneEntryDestination) {
                                popUpTo(SessionInitDestination) { inclusive = true }
                            }
                        },
                        onNeedsBiometricUnlock = {
                            navController.navigate(BiometricUnlockDestination) {
                                popUpTo(SessionInitDestination) { inclusive = true }
                            }
                        },
                        onSessionReady = {
                            navController.navigate(MainDestination) {
                                popUpTo(SessionInitDestination) { inclusive = true }
                            }
                        },
                        onError = {
                            // For now, go to phone entry on error
                            navController.navigate(PhoneEntryDestination) {
                                popUpTo(SessionInitDestination) { inclusive = true }
                            }
                        }
                    )
                }

                // Phone entry - start OTP login flow
                composable<PhoneEntryDestination> {
                    PhoneEntryScreen(
                        onSendOtp = { phoneNumber ->
                            navController.navigate(OtpVerificationDestination(phoneNumber))
                        }
                    )
                }

                // OTP verification
                composable<OtpVerificationDestination> { backStackEntry ->
                    val phoneNumber = backStackEntry.toRoute<OtpVerificationDestination>().phoneNumber
                    OtpVerificationScreen(
                        phoneNumber = phoneNumber,
                        onVerify = { otp ->
                            // TODO: Verify OTP via AuthRepository
                            // For now, navigate to display name
                            navController.navigate(DisplayNameDestination) {
                                popUpTo(PhoneEntryDestination) { inclusive = true }
                            }
                        },
                        onResend = {
                            // TODO: Resend OTP via AuthRepository
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // Display name entry for new users
                composable<DisplayNameDestination> {
                    DisplayNameScreen(
                        onSave = { displayName ->
                            // TODO: Save display name via RegisterUserUseCase
                            // Then navigate to main app
                            navController.navigate(MainDestination) {
                                popUpTo(SessionInitDestination) { inclusive = true }
                            }
                        }
                    )
                }

                // Biometric unlock
                composable<BiometricUnlockDestination> {
                    BiometricUnlockScreen(
                        onUnlockSuccess = {
                            navController.navigate(MainDestination) {
                                popUpTo(SessionInitDestination) { inclusive = true }
                            }
                        },
                        onLogout = {
                            navController.navigate(PhoneEntryDestination) {
                                popUpTo(SessionInitDestination) { inclusive = true }
                            }
                        }
                    )
                }

                // Main app screen
                composable<MainDestination> {
                    MainScreen()
                }

                // Settings screen
                composable<SettingsDestination> {
                    SettingsScreen()
                }
            }
        }
    }
}

/**
 * Session initialization screen
 * Uses SessionInitUseCase to determine which auth flow to show
 */
@Composable
private fun SessionInitScreen(
    onNeedsOtpLogin: () -> Unit,
    onNeedsBiometricUnlock: () -> Unit,
    onSessionReady: () -> Unit,
    onError: () -> Unit
) {
    val sessionInitUseCase: SessionInitUseCase by inject()
    var hasHandled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasHandled) {
            when (sessionInitUseCase()) {
                is SessionInitResult.NeedsOtpLogin -> onNeedsOtpLogin()
                is SessionInitResult.NeedsBiometricUnlock -> onNeedsBiometricUnlock()
                is SessionInitResult.SessionReady -> onSessionReady()
                is SessionInitResult.Error -> onError()
            }
            hasHandled = true
        }
    }
}
