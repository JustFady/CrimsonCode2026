package org.crimsoncode2026.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Help
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import maplibre.compose.CircleLayer
import maplibre.compose.SymbolLayer
import maplibre.compose.expressions.dsl.const
import maplibre.compose.expressions.dsl.iconOffset
import maplibre.compose.expressions.dsl.iconSize
import maplibre.compose.expressions.dsl.image
import maplibre.compose.sources.GeoJsonData
import maplibre.compose.sources.rememberGeoJsonSource
import org.crimsoncode2026.data.Category
import org.crimsoncode2026.data.Event

/**
 * Severity colors for event markers
 */
object SeverityColors {
    /**
     * Orange for ALERT (Warning) severity
     */
    val Alert = Color(0xFFFF6600)

    /**
     * Red for CRISIS severity
     */
    val Crisis = Color(0xFFFF0000)

    /**
     * Gets the color for a severity
     */
    fun colorForSeverity(severity: String): Color {
        return when (severity) {
            "CRISIS" -> Crisis
            "ALERT" -> Alert
            else -> Alert
        }
    }
}

/**
 * Gets the material icon vector for a category
 */
fun getCategoryIconVector(category: Category) = when (category) {
    Category.MEDICAL -> Icons.Default.LocalHospital
    Category.FIRE -> Icons.Default.LocalFireDepartment
    Category.WEATHER -> Icons.Default.Thunderstorm
    Category.CRIME -> Icons.Default.Shield
    Category.NATURAL_DISASTER -> Icons.Default.Terrain
    Category.INFRASTRUCTURE -> Icons.Default.Build
    Category.SEARCH_RESCUE -> Icons.Default.Search
    Category.TRAFFIC -> Icons.Default.DirectionsCar
    Category.OTHER -> Icons.Default.Help
}

/**
 * Event marker with severity-based styling and category icon overlay
 *
 * Features:
 * - Red pulsing circle for CRISIS severity
 * - Orange static circle for ALERT severity
 * - Category icon overlay
 * - Configurable marker size
 *
 * Spec requirements:
 * - Crisis: Red circle with pulsing animation
 * - Alert: Orange circle with static styling
 * - Category icons: Small overlay on marker indicating category
 *
 * This composable should be used inside MapLibreMap content block.
 *
 * @param event The event to display
 * @param sourceId Unique ID for the GeoJSON source
 * @param layerPrefix Prefix for layer IDs (allows multiple marker groups)
 * @param markerSize Size of the marker circle in dp (default 24dp)
 * @param iconSize Size of the category icon in dp (default 16dp)
 * @param onClick Optional callback when marker is clicked
 */
@Composable
fun EventMarker(
    event: Event,
    sourceId: String = "event-marker-${event.id}",
    layerPrefix: String = "event-${event.id}",
    markerSize: Dp = 24.dp,
    iconSize: Dp = 16.dp,
    onClick: () -> Unit = {}
) {
    // Create GeoJSON source with event location
    val markerSource = rememberGeoJsonSource(
        id = sourceId,
        data = GeoJsonData.Features(
            org.maplibre.spatialk.geojson.Point(
                org.maplibre.spatialk.geojson.Position(
                    latitude = event.lat,
                    longitude = event.lon
                )
            )
        )
    )

    val isCrisis = event.severity == "CRISIS"
    val severityColor = SeverityColors.colorForSeverity(event.severity)

    // Pulsing animation for crisis markers
    if (isCrisis) {
        val infiniteTransition = rememberInfiniteTransition()

        val pulseRadius by infiniteTransition.animateDp(
            initialValue = markerSize,
            targetValue = markerSize * 1.8,
            animationSpec = pulseAnimationSpec()
        )

        // Pulsing outer circle (crisis only)
        CircleLayer(
            id = "${layerPrefix}-pulse",
            source = markerSource,
            color = const(severityColor),
            opacity = const(0.3f),
            radius = const(pulseRadius),
        )

        // Inner pulsing circle for crisis
        CircleLayer(
            id = "${layerPrefix}-pulse-inner",
            source = markerSource,
            color = const(severityColor),
            opacity = const(0.5f),
            radius = const(markerSize * 1.3),
        )
    }

    // Main marker circle
    CircleLayer(
        id = "${layerPrefix}-marker",
        source = markerSource,
        color = const(severityColor),
        radius = const(markerSize),
        onClick = { onClick(); maplibre.compose.util.ClickResult.Consume }
    )

    // Category icon overlay
    val categoryEnum = event.categoryEnum
    if (categoryEnum != null) {
        val iconVector = getCategoryIconVector(categoryEnum)

        SymbolLayer(
            id = "${layerPrefix}-icon",
            source = markerSource,
            iconImage = image(rememberVectorPainter(iconVector)),
            iconSize = const(iconSize),
            iconOffset = iconOffset(0.0, 0.0),
            onClick = { onClick(); maplibre.compose.util.ClickResult.Consume }
        )
    }
}

/**
 * Animation spec for pulsing effect
 *
 * 1 second duration with fast-out-slow-in easing for smooth pulse
 */
private fun pulseAnimationSpec(): InfiniteRepeatableSpec<Dp> {
    return InfiniteRepeatableSpec(
        animation = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        repeatMode = RepeatMode.Reverse
    )
}

/**
 * Gets the severity color for an event
 *
 * @param severity Severity string from Event
 * @return Color for the severity
 */
fun getSeverityColor(severity: String): Color {
    return SeverityColors.colorForSeverity(severity)
}
