package org.crimsoncode2026.screens.eventcreation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Description input screen for event creation wizard step 5.
 *
 * Features:
 * - Text field for additional event details
 * - Character count display (max 500 characters per spec)
 * - Auto-focus on text field
 * - Emits entered description callback
 *
 * @param onDescriptionEntered Callback when user submits description
 * @param onCancel Callback when user cancels input
 * @param initialDescription Initial description text (defaults to empty)
 */
@Composable
fun DescriptionInputScreen(
    onDescriptionEntered: (String) -> Unit,
    onCancel: () -> Unit = {},
    initialDescription: String = ""
) {
    val focusRequester = remember { FocusRequester() }
    var textValue by remember { mutableStateOf(TextFieldValue(initialDescription)) }

    // Auto-focus text field on load
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            DescriptionInputHeader(onCancel = onCancel)

            Spacer(modifier = Modifier.height(16.dp))

            // Description input field
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        // Enforce max 500 character limit per spec
                        if (newValue.text.length <= 500) {
                            textValue = newValue
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Describe the emergency situation...") },
                    label = { Text("Description") },
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when {
                                    textValue.text.isEmpty() -> "Required"
                                    textValue.text.length > 450 -> "Approaching character limit"
                                    else -> "Optional additional details"
                                },
                                color = when {
                                    textValue.text.isEmpty() -> MaterialTheme.colorScheme.error
                                    textValue.text.length > 450 -> MaterialTheme.colorScheme.warning
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = "${textValue.text.length}/500",
                                color = when {
                                    textValue.text.length >= 500 -> MaterialTheme.colorScheme.error
                                    textValue.text.length > 450 -> MaterialTheme.colorScheme.warning
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    },
                    isError = textValue.text.isEmpty(),
                    maxLines = 10,
                    minLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = when {
                            textValue.text.isEmpty() -> MaterialTheme.colorScheme.error
                            textValue.text.length >= 500 -> MaterialTheme.colorScheme.error
                            textValue.text.length > 450 -> MaterialTheme.colorScheme.warning
                            else -> MaterialTheme.colorScheme.primary
                        },
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Help text card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Tips for a good description:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Be specific about the type of emergency\n• Include any relevant details about the location\n• Mention if people are injured or in danger\n• Note any hazards to be aware of",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continue button
            Button(
                onClick = { onDescriptionEntered(textValue.text) },
                enabled = textValue.text.isNotBlank() && textValue.text.length <= 500,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (textValue.text.isEmpty()) "Enter a description" else "Continue")
            }
        }
    }
}

/**
 * Header for description input screen
 */
@Composable
private fun DescriptionInputHeader(onCancel: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }
            Text(
                text = "Add Description",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Provide additional details about the emergency",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
