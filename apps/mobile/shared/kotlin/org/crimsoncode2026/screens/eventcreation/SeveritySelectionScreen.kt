package org.crimsoncode2026.screens.eventcreation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.data.Severity

/**
 * Severity selection screen for event creation wizard step 2.
 *
 * Features:
 * - Two severity options: Alert (Warning) and Crisis
 * - Visual distinction: Orange for Warning, Red for Crisis
 * - Default selection: Warning (Alert)
 * - Emits selected severity callback
 *
 * @param onSeveritySelected Callback when user selects a severity
 * @param onCancel Callback when user cancels selection
 * @param initialSeverity Initial severity selection (defaults to ALERT)
 */
@Composable
fun SeveritySelectionScreen(
    onSeveritySelected: (Severity) -> Unit,
    onCancel: () -> Unit = {},
    initialSeverity: Severity = Severity.ALERT
) {
    var selectedSeverity by remember { mutableStateOf(initialSeverity) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            SeveritySelectionHeader(onCancel = onCancel)

            Spacer(modifier = Modifier.height(24.dp))

            // Severity options
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SeverityOptionCard(
                    severity = Severity.ALERT,
                    isSelected = selectedSeverity == Severity.ALERT,
                    onClick = { selectedSeverity = Severity.ALERT }
                )

                SeverityOptionCard(
                    severity = Severity.CRISIS,
                    isSelected = selectedSeverity == Severity.CRISIS,
                    onClick = { selectedSeverity = Severity.CRISIS }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continue button
            Button(
                onClick = { onSeveritySelected(selectedSeverity) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue with ${severityName(selectedSeverity)}")
            }
        }
    }
}

/**
 * Header for severity selection screen
 */
@Composable
private fun SeveritySelectionHeader(onCancel: () -> Unit) {
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
                text = "Select Severity",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "How urgent is this emergency?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Severity option card displaying one severity choice
 */
@Composable
private fun SeverityOptionCard(
    severity: Severity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected) {
                severityColor(severity)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                severityColor(severity).copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Severity icon
            Surface(
                color = severityColor(severity),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = severityName(severity),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Severity info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = severityName(severity),
                    style = MaterialTheme.typography.titleLarge,
                    color = severityColor(severity)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = severityDescription(severity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Selection indicator
            if (isSelected) {
                Surface(
                    color = severityColor(severity),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    androidx.compose.material.icons.Icons.Default.CheckCircle.let {
                        Icon(
                            imageVector = it,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get display name for severity
 */
private fun severityName(severity: Severity): String {
    return when (severity) {
        Severity.ALERT -> "Warning"
        Severity.CRISIS -> "Crisis"
    }
}

/**
 * Get color for severity (Orange for Warning, Red for Crisis per spec)
 */
private fun severityColor(severity: Severity): Color {
    return when (severity) {
        Severity.ALERT -> Color(0xFFF97316) // Orange
        Severity.CRISIS -> Color(0xFFDC2626) // Red
    }
}

/**
 * Get description for each severity level
 */
private fun severityDescription(severity: Severity): String {
    return when (severity) {
        Severity.ALERT -> "Important situation that requires attention"
        Severity.CRISIS -> "Life-threatening emergency requiring immediate response"
    }
}
