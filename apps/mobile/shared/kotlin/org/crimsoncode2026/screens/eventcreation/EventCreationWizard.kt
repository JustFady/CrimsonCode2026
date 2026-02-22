package org.crimsoncode2026.screens.eventcreation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import org.crimsoncode2026.data.BroadcastType
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.domain.usecases.CreateEventResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.crimsoncode2026.compose.NetworkStatusBanner
import org.crimsoncode2026.network.NetworkInfo

/**
 * Event Creation Wizard - Orchestrates all 6 wizard screens for event creation
 *
 * Features:
 * - Sequential navigation through 6 steps:
 *   1. Category Selection
 *   2. Severity Selection
 *   3. Broadcast Scope
 *   4. Location Confirmation
 *   5. Description Input
 *   6. Review & Submit
 * - Back button for navigation
 * - Returns to main screen on successful submit or cancel
 * - Displays success/error dialogs
 *
 * @param onDismiss Callback when wizard is dismissed (cancel or success)
 */
@Composable
fun EventCreationWizard(onDismiss: () -> Unit) {
    val viewModel: EventCreationViewModel by inject()

    val state by viewModel.state.collectAsState()

    // Handle submit result
    LaunchedEffect(state.submitResult) {
        when (val result = state.submitResult) {
            is CreateEventResult.Success -> {
                // Event created successfully - dismiss wizard
                viewModel.reset()
                onDismiss()
            }
            is CreateEventResult.Error -> {
                // Show error dialog - keep wizard open
            }
            null -> {
                // No result yet - continue
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Back button (shown for all steps except first)
        if (state.currentStep.hasPrevious) {
            IconButton(
                onClick = { viewModel.previousStep() },
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        // Render current step
        when (state.currentStep) {
            WizardStep.CATEGORY -> CategorySelectionScreen(
                onCategorySelected = { category ->
                    viewModel.setCategory(category)
                    viewModel.nextStep()
                },
                onCancel = {
                    viewModel.reset()
                    onDismiss()
                }
            )

            WizardStep.SEVERITY -> SeveritySelectionScreen(
                onSeveritySelected = { severity ->
                    viewModel.setSeverity(severity)
                    viewModel.nextStep()
                },
                onCancel = {
                    viewModel.reset()
                    onDismiss()
                },
                initialSeverity = state.selectedSeverity
            )

            WizardStep.BROADCAST_SCOPE -> BroadcastScopeScreen(
                onBroadcastScopeSelected = { scope ->
                    viewModel.setBroadcastScope(
                        when (scope) {
                            BroadcastType.PUBLIC -> BroadcastScope.PUBLIC
                            BroadcastType.PRIVATE -> BroadcastScope.PRIVATE
                        }
                    )
                    viewModel.nextStep()
                },
                onCancel = {
                    viewModel.reset()
                    onDismiss()
                },
                initialScope = if (state.selectedBroadcastScope.isPublic) {
                    BroadcastType.PUBLIC
                } else {
                    BroadcastType.PRIVATE
                }
            )

            WizardStep.LOCATION -> LocationSelectionScreen(
                initialLocation = state.location,
                onLocationSelected = { location ->
                    viewModel.setLocation(location)
                    viewModel.nextStep()
                },
                onCancel = {
                    viewModel.reset()
                    onDismiss()
                },
                onGetCurrentLocation = {
                    // In a real implementation, this would call LocationRepository
                    // For now, return null to indicate no location available
                    null
                }
            )

            WizardStep.DESCRIPTION -> DescriptionInputScreen(
                onDescriptionEntered = { description ->
                    viewModel.setDescription(description)
                    viewModel.nextStep()
                },
                onCancel = {
                    viewModel.reset()
                    onDismiss()
                },
                initialDescription = state.description
            )

            WizardStep.REVIEW -> {
                val networkInfo by viewModel.networkStatus.collectAsState()

                val reviewData = EventReviewData(
                    category = state.selectedCategory!!,
                    severity = state.selectedSeverity,
                    broadcastScope = if (state.selectedBroadcastScope.isPublic) {
                        BroadcastType.PUBLIC
                    } else {
                        BroadcastType.PRIVATE
                    },
                    location = state.location?.let {
                        "${it.coordinates.latitude}, ${it.coordinates.longitude}"
                    } ?: "Location not set",
                    latitude = state.location?.coordinates?.latitude ?: 0.0,
                    longitude = state.location?.coordinates?.longitude ?: 0.0,
                    description = state.description
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    NetworkStatusBanner(
                        networkInfo = networkInfo,
                        isRetrying = state.isRetrying,
                        onRetry = if (state.submitResult is CreateEventResult.Error) {
                            { viewModel.retrySubmission() }
                        } else null
                    )
                    ReviewSubmitScreen(
                        reviewData = reviewData,
                        onSubmit = { viewModel.submitEvent() },
                        onCancel = {
                            viewModel.reset()
                            onDismiss()
                        },
                        isLoading = state.isSubmitting,
                        errorMessage = (state.submitResult as? CreateEventResult.Error)?.message,
                        onRetry = if (state.submitResult is CreateEventResult.Error) {
                            { viewModel.retrySubmission() }
                        } else null,
                        isRetrying = state.isRetrying
                    )
                }
            }
        }
    }

    // Error dialog
    if (state.submitResult is CreateEventResult.Error) {
        val errorResult = state.submitResult as CreateEventResult.Error
        AlertDialog(
            onDismissRequest = { viewModel.clearSubmitResult() },
            title = { Text("Failed to Create Alert") },
            text = { Text(errorResult.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSubmitResult() }) {
                    Text("OK")
                }
            }
        )
    }

    // Cleanup network monitoring when wizard is dismissed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.cleanup()
        }
    }
}
