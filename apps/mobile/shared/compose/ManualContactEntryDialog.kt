package org.crimsoncode2026.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.utils.PhoneNumberUtils

/**
 * Dialog for manual contact entry when contacts permission is denied.
 *
 * Allows users to add emergency contacts manually by entering:
 * - Phone number (required, USA format)
 * - Display name (optional)
 *
 * Validates phone number format before adding to contact list.
 *
 * @param onDismiss Callback when dialog is dismissed
 * @param onContactAdded Callback when valid contact is submitted (phone, name)
 * @param modifier Modifier for the dialog
 */
@Composable
fun ManualContactEntryDialog(
    onDismiss: () -> Unit,
    onContactAdded: (phoneNumber: String, displayName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }

    fun validateAndSubmit() {
        val normalized = PhoneNumberUtils.normalizeToE164(phoneNumber)
        when {
            phoneNumber.isBlank() -> {
                phoneError = "Phone number is required"
            }
            normalized == null -> {
                phoneError = "Invalid USA phone number format"
            }
            else -> {
                phoneError = null
                onContactAdded(normalized, displayName.ifBlank { "Contact" })
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Add Contact Manually",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter the contact's phone number to add them to your emergency list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = it
                        phoneError = null
                    },
                    label = { Text("Phone Number *") },
                    placeholder = { Text("(555) 123-4567") },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name (optional)") },
                    placeholder = { Text("Friend or Family Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Only USA phone numbers supported (+1 country code)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = { validateAndSubmit() },
                    enabled = phoneNumber.isNotBlank() && phoneError == null
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.width(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Contact")
                }
            }
        },
        modifier = modifier
    )
}
