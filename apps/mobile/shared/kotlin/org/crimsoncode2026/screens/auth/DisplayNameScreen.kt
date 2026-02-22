package org.crimsoncode2026.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

private const val MAX_DISPLAY_NAME_LENGTH = 100
private const val MIN_DISPLAY_NAME_LENGTH = 2

/**
 * Display name entry screen for new user registration
 * Display name is shown to contacts for private events
 *
 * @param onSave Callback when user submits valid display name
 */
@Composable
fun DisplayNameScreen(
    onSave: (String) -> Unit
) {
    var nameValue by remember { mutableStateOf(TextFieldValue()) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Choose Your Name",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This name will be shown to your contacts",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = nameValue,
                onValueChange = { newValue ->
                    if (newValue.text.length <= MAX_DISPLAY_NAME_LENGTH) {
                        nameValue = TextFieldValue(
                            text = newValue.text,
                            selection = TextRange(newValue.text.length)
                        )
                        error = null
                    }
                },
                label = { Text("Display Name") },
                placeholder = { Text("Enter your name") },
                isError = error != null,
                supportingText = {
                    val currentLength = nameValue.text.length
                    Text(
                        text = when {
                            error != null -> error ?: ""
                            currentLength > MAX_DISPLAY_NAME_LENGTH - 10 -> "$currentLength/$MAX_DISPLAY_NAME_LENGTH"
                            else -> "At least $MIN_DISPLAY_NAME_LENGTH characters"
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val name = nameValue.text.trim()
                    when {
                        name.isEmpty() -> error = "Please enter your display name"
                        name.length < MIN_DISPLAY_NAME_LENGTH -> error = "Name must be at least $MIN_DISPLAY_NAME_LENGTH characters"
                        name.isBlank() -> error = "Name cannot be only spaces"
                        else -> {
                            isLoading = true
                            onSave(name)
                        }
                    }
                },
                enabled = !isLoading && nameValue.text.trim().length >= MIN_DISPLAY_NAME_LENGTH,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Saving..." else "Continue")
            }
        }
    }
}
