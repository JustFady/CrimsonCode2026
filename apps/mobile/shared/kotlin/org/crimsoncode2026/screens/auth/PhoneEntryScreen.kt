package org.crimsoncode2026.screens.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val PRIMARY_COLOR = Color(0xFFDC2626) // Red for emergency theme
private val SURFACE_COLOR = Color(0xFFF8FAFC)

/**
 * Phone number entry screen with E.164 USA format validation
 * Phone format: +1 XXX XXX-XXXX
 *
 * @param onSendOtp Callback when user clicks Send OTP button with valid phone number
 */
@Composable
fun PhoneEntryScreen(
    onSendOtp: (String) -> Unit
) {
    var phoneValue by remember { mutableStateOf(TextFieldValue(text = "+1 ")) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Logo/Icon Section
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = PRIMARY_COLOR
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone Icon",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Emergency Response",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Subtitle
            Text(
                text = "Enter your phone number to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone Input
            OutlinedTextField(
                value = phoneValue,
                onValueChange = { newValue ->
                    val formatted = formatPhoneNumber(newValue.text)
                    val newCursorPosition = calculateCursorPosition(
                        oldText = phoneValue.text,
                        newText = newValue.text,
                        formattedText = formatted,
                        oldCursorPosition = phoneValue.selection.start
                    )
                    phoneValue = TextFieldValue(
                        text = formatted,
                        selection = TextRange(min(newCursorPosition, formatted.length))
                    )
                    error = null
                },
                label = { Text("Phone Number") },
                placeholder = { Text("+1 (555) 123-4567") },
                isError = error != null,
                supportingText = { error?.let { Text(it, color = Color(0xFFDC2626)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SURFACE_COLOR,
                    unfocusedContainerColor = SURFACE_COLOR,
                    errorContainerColor = Color(0xFFFEF2F2),
                    focusedIndicatorColor = PRIMARY_COLOR,
                    unfocusedIndicatorColor = Color(0xFFCBD5E1),
                    errorIndicatorColor = Color(0xFFDC2626)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Send OTP Button
            Button(
                onClick = {
                    val phone = phoneValue.text.replace(Regex("[^+\\d]"), "")
                    if (isValidUSAPhoneNumber(phone)) {
                        isLoading = true
                        onSendOtp(phone)
                    } else {
                        error = "Please enter a valid US phone number"
                    }
                },
                enabled = !isLoading && isValidUSAPhoneNumber(phoneValue.text.replace(Regex("[^+\\d]"), "")),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PRIMARY_COLOR,
                    disabledContainerColor = PRIMARY_COLOR.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    Text(
                        "Sending...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        "Send Verification Code",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Terms text
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "By continuing, you agree to our Terms of Service",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

/**
 * Format phone number to E.164 USA format
 * Input: +15551234567
 * Output: +1 (555) 123-4567
 */
private fun formatPhoneNumber(input: String): String {
    val digits = input.replace(Regex("[^\\d]"), "")

    return when {
        digits.isEmpty() -> "+1 "
        digits.length <= 3 -> "+1 ($digits"
        digits.length <= 6 -> "+1 (${digits.take(3)}) ${digits.drop(3)}"
        else -> "+1 (${digits.take(3)}) ${digits.substring(3, 6)}-${digits.takeLast(4)}"
    }
}

/**
 * Calculate new cursor position after formatting
 */
private fun calculateCursorPosition(
    oldText: String,
    newText: String,
    formattedText: String,
    oldCursorPosition: Int
): Int {
    val oldDigits = oldText.replace(Regex("[^\\d]"), "")
    val newDigits = newText.replace(Regex("[^\\d]"), "")

    if (newDigits.length >= oldDigits.length) {
        // User added a character, move forward
        val addedPosition = newDigits.length - oldDigits.length
        return (formattedText.length).coerceAtMost(oldCursorPosition + addedPosition + 2)
    } else {
        // User deleted a character, move backward
        return (formattedText.length).coerceAtLeast(0)
    }
}

/**
 * Validate E.164 USA phone number
 * Must start with +1 and have exactly 11 digits total (country code + 10 digits)
 */
private fun isValidUSAPhoneNumber(phone: String): Boolean {
    val digits = phone.replace(Regex("[^\\d]"), "")
    return phone.startsWith("+1") && digits.length == 11
}
