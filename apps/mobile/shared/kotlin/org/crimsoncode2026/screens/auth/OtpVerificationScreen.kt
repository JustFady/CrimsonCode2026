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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val RESEND_DELAY_SECONDS = 30
private val PRIMARY_COLOR = Color(0xFFDC2626) // Red for emergency theme
private val SURFACE_COLOR = Color(0xFFF8FAFC)

/**
 * Result of OTP verification attempt
 */
sealed class OtpVerificationResult {
    data object Success : OtpVerificationResult()
    data class Error(val message: String) : OtpVerificationResult()
}

/**
 * OTP verification screen for 6-digit code entry
 *
 * @param phoneNumber Phone number for display (masked format)
 * @param onVerify Suspended callback when user submits OTP code - returns verification result
 * @param onResend Callback when user requests resend OTP
 * @param onBack Callback when user navigates back
 * @param onVerificationSuccess Callback when verification succeeds
 */
@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onVerify: suspend (String) -> OtpVerificationResult,
    onResend: () -> Unit,
    onBack: () -> Unit,
    onVerificationSuccess: () -> Unit = {}
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
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button at top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack, enabled = !isLoading) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Back",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Icon Section
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
                        imageVector = Icons.Default.Sms,
                        contentDescription = "SMS Icon",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Title
            Text(
                text = "Verify Your Number",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Instructions
            Text(
                text = "We sent a 6-digit code to",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF64748B)
            )

            Text(
                text = maskedPhone,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                supportingText = { error?.let { Text(it, color = Color(0xFFDC2626)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
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

            // Verify Button
            Button(
                onClick = {
                    if (otpValue.text.length == 6) {
                        isLoading = true
                        error = null
                    } else {
                        error = "Please enter 6-digit code"
                    }
                },
                enabled = !isLoading && otpValue.text.length == 6,
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
                        "Verifying...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        "Verify",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Handle OTP verification when isLoading changes to true
            LaunchedEffect(isLoading, otpValue.text) {
                if (isLoading && otpValue.text.length == 6) {
                    when (val result = onVerify(otpValue.text)) {
                        is OtpVerificationResult.Success -> {
                            onVerificationSuccess()
                        }
                        is OtpVerificationResult.Error -> {
                            isLoading = false
                            error = result.message
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Resend Button
            Button(
                onClick = {
                    countdown = RESEND_DELAY_SECONDS
                    canResend = false
                    onResend()
                },
                enabled = canResend && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PRIMARY_COLOR
                )
            ) {
                Text(
                    if (canResend) "Resend Code" else "Resend in ${countdown}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (canResend) PRIMARY_COLOR else Color(0xFF94A3B8)
                )
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
