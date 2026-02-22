package org.crimsoncode2026.location.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import org.crimsoncode2026.location.AccuracyLevel

/**
 * Accuracy visualization component that displays a color-coded circle
 *
 * Color coding per specification:
 * - Green: High accuracy (< 10 meters)
 * - Yellow: Good accuracy (10-50 meters)
 * - Orange: Fair accuracy (50-100 meters)
 * - Red: Low accuracy (> 100 meters)
 * - Gray: Unknown accuracy
 *
 * @param accuracyLevel The current accuracy level
 * @param modifier Modifier for the composable
 * @param circleSize Size of the accuracy circle in dp
 * @param strokeWidth Stroke width for the circle in dp
 * @param showText Whether to show accuracy text inside the circle
 * @param textStyle Text style for the accuracy label
 */
@Composable
fun LocationAccuracyIndicator(
    accuracyLevel: AccuracyLevel,
    modifier: Modifier = Modifier,
    circleSize: Dp = 120.dp,
    strokeWidth: Dp = 4.dp,
    showText: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall
) {
    val color = accuracyLevel.toColor()
    val label = accuracyLevel.toLabel()

    Box(
        modifier = modifier
            .size(circleSize)
            .drawBehind {
                val strokeWidthPx = strokeWidth.toPx()
                val radius = size.minDimension / 2 - strokeWidthPx / 2

                // Draw outer circle with stroke
                drawCircle(
                    color = color,
                    radius = radius,
                    style = Stroke(width = strokeWidthPx)
                )

                // Draw semi-transparent fill
                drawCircle(
                    color = color.copy(alpha = 0.15f),
                    radius = radius
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (showText) {
            Text(
                text = label,
                style = textStyle,
                color = color
            )
        }
    }
}

/**
 * Accuracy legend component showing all accuracy levels with their colors
 *
 * @param modifier Modifier for the composable
 */
@Composable
fun AccuracyLegend(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Text(
            text = buildString {
                appendLine("Accuracy Levels:")
                appendLine("• Green: High (< 10m)")
                appendLine("• Yellow: Good (10-50m)")
                appendLine("• Orange: Fair (50-100m)")
                appendLine("• Red: Low (> 100m)")
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Accuracy badge component showing a small colored indicator
 *
 * @param accuracyLevel The current accuracy level
 * @param modifier Modifier for the composable
 */
@Composable
fun AccuracyBadge(
    accuracyLevel: AccuracyLevel,
    modifier: Modifier = Modifier
) {
    val color = accuracyLevel.toColor()
    val label = accuracyLevel.toShortLabel()

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier
        )
    }
}

/**
 * Extension function to map accuracy level to color
 */
private fun AccuracyLevel.toColor(): Color = when (this) {
    AccuracyLevel.HIGH -> Color(0xFF4CAF50)     // Green
    AccuracyLevel.GOOD -> Color(0xFFFFC107)     // Yellow/Amber
    AccuracyLevel.FAIR -> Color(0xFFFF9800)     // Orange
    AccuracyLevel.LOW -> Color(0xFFF44336)      // Red
    AccuracyLevel.UNKNOWN -> Color(0xFF9E9E9E)  // Gray
}

/**
 * Extension function to get accuracy level label
 */
private fun AccuracyLevel.toLabel(): String = when (this) {
    AccuracyLevel.HIGH -> "High"
    AccuracyLevel.GOOD -> "Good"
    AccuracyLevel.FAIR -> "Fair"
    AccuracyLevel.LOW -> "Low"
    AccuracyLevel.UNKNOWN -> "Unknown"
}

/**
 * Extension function to get short accuracy level label for badge
 */
private fun AccuracyLevel.toShortLabel(): String = when (this) {
    AccuracyLevel.HIGH -> "H"
    AccuracyLevel.GOOD -> "G"
    AccuracyLevel.FAIR -> "F"
    AccuracyLevel.LOW -> "L"
    AccuracyLevel.UNKNOWN -> "?"
}
