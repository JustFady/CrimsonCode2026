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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.navDeepLink
import androidx.navigation.navOptions
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
import org.crimsoncode2026.screens.main.MainMapViewModel
import org.crimsoncode2026.screens.main.MapEvent
import org.crimsoncode2026.screens.settings.SettingsScreen
import org.crimsoncode2026.screens.eventcreation.EventCreationWizard
import org.crimsoncode2026.screens.publicevents.EventListView
import org.crimsoncode2026.screens.publicevents.EventListItem
import org.crimsoncode2026.domain.usecases.SessionInitUseCase
import org.crimsoncode2026.domain.usecases.SessionInitResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.compose.koinInject
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

@Serializable
object EventCreationWizardDestination

@Serializable
object EventListViewDestination

/**
 * Deep link destination for navigating to a specific event from push notifications.
 * Handles crimsoncode://event/{eventId} URL scheme.
 */
@Serializable
data class EventDeepLinkDestination(val eventId: String)

/**
 * Main App composable
 * Handles navigation and authentication flow using SessionInitUseCase
 */
@Composable
fun App() {
    // Shared state for zoom target event ID (persisted across navigation)
    var zoomTargetEventId by rememberSaveable { mutableStateOf<String?>(null) }

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
                    val viewModel: MainMapViewModel = koinInject()

                    // Zoom to event when returning from event list
                    LaunchedEffect(zoomTargetEventId) {
                        zoomTargetEventId?.let { eventId ->
                            viewModel.zoomToEvent(eventId)
                            zoomTargetEventId = null // Clear after zooming
                        }
                    }

                    MainScreen(
                        onNavigateToSettings = {
                            navController.navigate(SettingsDestination)
                        },
                        onCreateEvent = {
                            navController.navigate(EventCreationWizardDestination)
                        },
                        onShowEventList = {
                            navController.navigate(EventListViewDestination)
                        }
                    )
                }

                // Event list view
                composable<EventListViewDestination> {
                    val viewModel: MainMapViewModel = koinInject()
                    val state by viewModel.state.collectAsState()

                    // Convert MapEvent to EventListItem for EventListView
                    val eventListItems = state.loadedEvents.map { mapEvent ->
                        EventListItem(
                            event = mapEvent.event,
                            creator = mapEvent.creator
                        )
                    }

                    EventListView(
                        events = eventListItems,
                        onEventClick = { event ->
                            // Set zoom target and navigate back to map
                            zoomTargetEventId = event.id
                            navController.popBackStack()
                        },
                        onClearAll = {
                            viewModel.clearAllEvents()
                        },
                        onDismiss = {
                            navController.popBackStack()
                        }
                    )
                }

                // Event creation wizard
                composable<EventCreationWizardDestination> {
                    EventCreationWizard(
                        onDismiss = {
                            navController.popBackStack()
                        }
                    )
                }

                // Settings screen
                composable<SettingsDestination> {
                    SettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToLogin = {
                            navController.navigate(PhoneEntryDestination) {
                                popUpTo(MainDestination) { inclusive = true }
                            }
                        }
                    )
                }

                // Deep link handler for push notifications
                // Handles crimsoncode://event/{eventId} URL scheme
                composable<EventDeepLinkDestination>(
                    deepLinks = listOf(
                        navDeepLink(
                            uriPattern = "crimsoncode://event/{eventId}"
                        )
                    )
                ) { backStackEntry ->
                    val eventId = backStackEntry.toRoute<EventDeepLinkDestination>().eventId
                    // Set zoom target and navigate to main map
                    zoomTargetEventId = eventId
                    navController.navigate(MainDestination) {
                        popUpTo(SessionInitDestination) { inclusive = true }
                    }
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
