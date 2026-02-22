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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.People
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

/**
 * Broadcast scope selection screen for event creation wizard step 3.
 *
 * Features:
 * - Toggle between Public and Private broadcast scope
 * - Default: Public (50-mile radius)
 * - Public shows "All nearby users within 50 miles"
 * - Private shows "Your selected emergency contacts"
 * - Emits selected broadcast scope callback
 *
 * @param onBroadcastScopeSelected Callback when user selects broadcast scope
 * @param onCancel Callback when user cancels selection
 * @param initialScope Initial broadcast scope selection (defaults to PUBLIC)
 */
@Composable
fun BroadcastScopeScreen(
    onBroadcastScopeSelected: (org.crimsoncode2026.data.BroadcastType) -> Unit,
    onCancel: () -> Unit = {},
    initialScope: org.crimsoncode2026.data.BroadcastType = org.crimsoncode2026.data.BroadcastType.PUBLIC
) {
    var selectedScope by remember { mutableStateOf(initialScope) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            BroadcastScopeHeader(onCancel = onCancel)

            Spacer(modifier = Modifier.height(24.dp))

            // Broadcast scope options
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BroadcastScopeOptionCard(
                    scope = org.crimsoncode2026.data.BroadcastType.PUBLIC,
                    isSelected = selectedScope == org.crimsoncode2026.data.BroadcastType.PUBLIC,
                    onClick = { selectedScope = org.crimsoncode2026.data.BroadcastType.PUBLIC }
                )

                BroadcastScopeOptionCard(
                    scope = org.crimsoncode2026.data.BroadcastType.PRIVATE,
                    isSelected = selectedScope == org.crimsoncode2026.data.BroadcastType.PRIVATE,
                    onClick = { selectedScope = org.crimsoncode2026.data.BroadcastType.PRIVATE }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Continue button
            Button(
                onClick = { onBroadcastScopeSelected(selectedScope) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue with ${scopeName(selectedScope)}")
            }
        }
    }
}

/**
 * Header for broadcast scope selection screen
 */
@Composable
private fun BroadcastScopeHeader(onCancel: () -> Unit) {
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
                text = "Broadcast Scope",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Who should receive this emergency alert?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Broadcast scope option card displaying one scope choice
 */
@Composable
private fun BroadcastScopeOptionCard(
    scope: org.crimsoncode2026.data.BroadcastType,
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
                scopeColor(scope)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                scopeColor(scope).copy(alpha = 0.1f)
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
            // Scope icon
            Surface(
                color = scopeColor(scope),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = scopeIcon(scope),
                        contentDescription = scopeName(scope),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Scope info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scopeName(scope),
                        style = MaterialTheme.typography.titleLarge,
                        color = scopeColor(scope)
                    )
                    if (scope == org.crimsoncode2026.data.BroadcastType.PUBLIC) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Default",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scopeDescription(scope),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Selection indicator
            if (isSelected) {
                Surface(
                    color = scopeColor(scope),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.filled.CheckCircle,
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

/**
 * Get display name for broadcast scope
 */
private fun scopeName(scope: org.crimsoncode2026.data.BroadcastType): String {
    return when (scope) {
        org.crimsoncode2026.data.BroadcastType.PUBLIC -> "Public"
        org.crimsoncode2026.data.BroadcastType.PRIVATE -> "Private"
    }
}

/**
 * Get icon for each broadcast scope
 */
private fun scopeIcon(scope: org.crimsoncode2026.data.BroadcastType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (scope) {
        org.crimsoncode2026.data.BroadcastType.PUBLIC -> Icons.Default.Public
        org.crimsoncode2026.data.BroadcastType.PRIVATE -> Icons.Default.People
    }
}

/**
 * Get color for each broadcast scope
 */
private fun scopeColor(scope: org.crimsoncode2026.data.BroadcastType): Color {
    return when (scope) {
        org.crimsoncode2026.data.BroadcastType.PUBLIC -> Color(0xFF3B82F6) // Blue
        org.crimsoncode2026.data.BroadcastType.PRIVATE -> Color(0xFF8B5CF6) // Purple
    }
}

/**
 * Get description for each broadcast scope (per spec)
 */
private fun scopeDescription(scope: org.crimsoncode2026.data.BroadcastType): String {
    return when (scope) {
        org.crimsoncode2026.data.BroadcastType.PUBLIC -> "All nearby users within 50 miles"
        org.crimsoncode2026.data.BroadcastType.PRIVATE -> "Your selected emergency contacts"
    }
}
