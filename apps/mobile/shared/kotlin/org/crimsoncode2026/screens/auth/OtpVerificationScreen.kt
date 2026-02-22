package org.crimsoncode2026.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val RESEND_DELAY_SECONDS = 30

/**
 * OTP verification screen for 6-digit code entry
 *
 * @param phoneNumber Phone number for display (masked format)
 * @param onVerify Callback when user submits OTP code
 * @param onResend Callback when user requests resend OTP
 * @param onBack Callback when user navigates back
 */
@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit
) {
    var otpValue by remember { mutableStateOf(TextFieldValue()) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(RESEND_DELAY_SECONDS) }
    var canResend by remember { mutableStateOf(false) }

    // Countdown timer for resend
    LaunchedEffect(countdown) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        canResend = true
    }

    val maskedPhone = maskPhoneNumber(phoneNumber)

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
                text = "Enter Verification Code",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "We sent a 6-digit code to",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = maskedPhone,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

            // OTP Input Field
            OutlinedTextField(
                value = otpValue,
                onValueChange = { newValue ->
                    // Only allow digits, max 6
                    val digits = newValue.text.replace(Regex("[^\\d]"), "")
                    if (digits.length <= 6) {
                        otpValue = TextFieldValue(
                            text = digits,
                            selection = TextRange(digits.length)
                        )
                        error = null
                    }
                },
                label = { Text("Verification Code") },
                placeholder = { Text("000000") },
                isError = error != null,
                supportingText = { error?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !isLoading
                ) {
                    Text("Back")
                }

                Button(
                    onClick = {
                        if (otpValue.text.length == 6) {
                            isLoading = true
                            onVerify(otpValue.text)
                        } else {
                            error = "Please enter the 6-digit code"
                        }
                    },
                    enabled = !isLoading && otpValue.text.length == 6,
                    modifier = Modifier.width(160.dp)
                ) {
                    Text(if (isLoading) "Verifying..." else "Verify")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    countdown = RESEND_DELAY_SECONDS
                    canResend = false
                    onResend()
                },
                enabled = canResend && !isLoading,
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text(if (canResend) "Resend Code" else "Resend in ${countdown}s")
            }
        }
    }
}

/**
 * Mask phone number for display
 * +15551234567 -> +1 (***) ***-4567
 */
private fun maskPhoneNumber(phone: String): String {
    val digits = phone.replace(Regex("[^\\d]"), "")
    return if (digits.length == 11) {
        "+1 (***) ***-${digits.takeLast(4)}"
    } else {
        phone
    }
}
