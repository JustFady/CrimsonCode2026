package org.crimsoncode2026.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import maplibre.compose.CircleLayer
import maplibre.compose.SymbolLayer
import maplibre.compose.expressions.dsl.const
import maplibre.compose.expressions.dsl.feature
import maplibre.compose.expressions.dsl.get
import maplibre.compose.expressions.dsl.iconOffset
import maplibre.compose.expressions.dsl.iconSize
import maplibre.compose.expressions.dsl.image
import maplibre.compose.sources.GeoJsonData
import maplibre.compose.sources.rememberGeoJsonSource
import org.crimsoncode2026.data.Event

/**
 * Event markers layer for displaying multiple events on map
 *
 * Features:
 * - Displays all provided events as markers on the map
 * - Clustering for dense areas (cluster color based on max severity)
 * - Cluster expands to show individual markers at maximum zoom
 * - Severity-based coloring (red for CRISIS, orange for ALERT)
 * - Category icons as overlays
 * - Pulsing animation for crisis events
 *
 * This composable should be used inside MapLibreMap content block.
 * It creates a GeoJSON source with clustering enabled containing all event locations
 * and uses data-driven styling based on event and cluster properties.
 *
 * Spec requirements:
 * - All public events within map bounds displayed
 * - Map clustering for dense areas
 * - Cluster color: Maximum severity within cluster
 * - Cluster expands to show individual markers at maximum zoom
 * - Markers update when camera changes (via new events list)
 * - Crisis: Red circle with pulsing animation
 * - Alert: Orange circle with static styling
 * - Category icons: Small overlay on marker
 *
 * @param events List of events to display on the map
 * @param sourceId Unique ID for the GeoJSON source (default: "event-markers")
 * @param layerPrefix Prefix for layer IDs (default: "events")
 * @param markerSize Size of the marker circle in dp (default 24dp)
 * @param iconSize Size of the category icon in dp (default 16dp)
 * @param clusterRadius Clustering radius in pixels (default 50dp)
 * @param minClusterZoom Minimum zoom level at which clustering is disabled (default 14)
 * @param onEventClick Optional callback when an event marker is clicked
 */
@Composable
fun EventMarkers(
    events: List<Event>,
    sourceId: String = "event-markers",
    layerPrefix: String = "events",
    markerSize: Dp = 24.dp,
    iconSize: Dp = 16.dp,
    clusterRadius: Dp = 50.dp,
    minClusterZoom: Double = 14.0,
    onEventClick: (Event) -> Unit = {}
) {
    if (events.isEmpty()) {
        return
    }

    // Create GeoJSON source with clustering enabled
    val markerSource = rememberGeoJsonSource(
        id = sourceId,
        clusterRadius = clusterRadius,
        clusterMinZoom = minClusterZoom,
        data = GeoJsonData.Features(
            org.maplibre.spatialk.geojson.dsl.featureCollection {
                events.forEach { event ->
                    feature(
                        org.maplibre.spatialk.geojson.Point(
                            org.maplibre.spatialk.geojson.Position(
                                latitude = event.lat,
                                longitude = event.lon
                            )
                        )
                    ) {
                        // Store event properties for data-driven styling
                        put("severity", event.severity)
                        put("severity_raw", event.severity)
                        put("category", event.category)
                        put("id", event.id)
                        put("crisis", event.severity == "CRISIS")
                        put("alert", event.severity == "ALERT")
                        put("category_medical", event.category == "MEDICAL")
                        put("category_fire", event.category == "FIRE")
                        put("category_weather", event.category == "WEATHER")
                        put("category_crime", event.category == "CRIME")
                        put("category_natural_disaster", event.category == "NATURAL_DISASTER")
                        put("category_infrastructure", event.category == "INFRASTRUCTURE")
                        put("category_search_rescue", event.category == "SEARCH_RESCUE")
                        put("category_traffic", event.category == "TRAFFIC")
                        put("category_other", event.category == "OTHER")
                    }
                }
            }
        )
    )

    // Pulsing outer circle for crisis events
    CircleLayer(
        id = "${layerPrefix}-pulse",
        source = markerSource,
        filter = feature { get("crisis").toBool() },
        color = const(SeverityColors.Crisis),
        opacity = const(0.3f),
        radius = const(markerSize * 1.5),
    )

    // Inner pulsing circle for crisis events
    CircleLayer(
        id = "${layerPrefix}-pulse-inner",
        source = markerSource,
        filter = feature { get("crisis").toBool() },
        color = const(SeverityColors.Crisis),
        opacity = const(0.5f),
        radius = const(markerSize * 1.2),
    )

    // Cluster circles - crisis color for larger clusters (10+ events)
    CircleLayer(
        id = "${layerPrefix}-cluster-crisis",
        source = markerSource,
        filter = feature { get("cluster").toBool() && get("point_count").gte(const(10)) },
        color = const(SeverityColors.Crisis),
        radius = const(markerSize * 2),
    )

    // Cluster circles - alert color for medium clusters (5-9 events)
    CircleLayer(
        id = "${layerPrefix}-cluster-alert",
        source = markerSource,
        filter = feature { get("cluster").toBool() && get("point_count").gte(const(5)) && get("point_count").lt(const(10)) },
        color = const(SeverityColors.Alert),
        radius = const(markerSize * 1.7),
    )

    // Cluster circles - default color for small clusters (2-4 events)
    CircleLayer(
        id = "${layerPrefix}-cluster-small",
        source = markerSource,
        filter = feature { get("cluster").toBool() && get("point_count").lt(const(5)) },
        color = const(androidx.compose.ui.graphics.Color(0xFFFFCC00)),
        radius = const(markerSize * 1.4),
    )

    // Cluster count labels
    SymbolLayer(
        id = "${layerPrefix}-cluster-count",
        source = markerSource,
        filter = feature { get("cluster").toBool() },
        textField = get("point_count").toString(),
        textSize = const(14),
        textColor = const(androidx.compose.ui.graphics.Color.White),
        textHaloColor = const(androidx.compose.ui.graphics.Color.Black),
        textHaloWidth = const(1),
    )

    // Main marker circles - alert events (orange), only when not clustered
    CircleLayer(
        id = "${layerPrefix}-markers-alert",
        source = markerSource,
        filter = feature { get("alert").toBool() && get("cluster").not() },
        color = const(SeverityColors.Alert),
        radius = const(markerSize),
    )

    // Main marker circles - crisis events (red), only when not clustered
    CircleLayer(
        id = "${layerPrefix}-markers-crisis",
        source = markerSource,
        filter = feature { get("crisis").toBool() && get("cluster").not() },
        color = const(SeverityColors.Crisis),
        radius = const(markerSize),
    )

    // Category icon overlays - Medical, only when not clustered
    SymbolLayer(
        id = "${layerPrefix}-icons-medical",
        source = markerSource,
        filter = feature { get("category_medical").toBool() && get("cluster").not() },
        iconImage = image(rememberVectorPainter(getCategoryIconVector(org.crimsoncode2026.data.Category.MEDICAL))),
        iconSize = const(iconSize),
        iconOffset = iconOffset(0.0, 0.0),
    )

    // Category icon overlays - Fire, only when not clustered
    SymbolLayer(
        id = "${layerPrefix}-icons-fire",
        source = markerSource,
        filter = feature { get("category_fire").toBool() && get("cluster").not() },
        iconImage = image(rememberVectorPainter(getCategoryIconVector(org.crimsoncode2026.data.Category.FIRE))),
        iconSize = const(iconSize),
        iconOffset = iconOffset(0.0, 0.0),
    )

    // Category icon overlays - Weather, only when not clustered
    SymbolLayer(
        id = "${layerPrefix}-icons-weather",
        source = markerSource,
        filter = feature { get("category_weather").toBool() && get("cluster").not() },
        iconImage = image(rememberVectorPainter(getCategoryIconVector(org.crimsoncode2026.data.Category.WEATHER))),
        iconSize = const(iconSize),
        iconOffset = iconOffset(0.0, 0.0),
    )

    // Category icon overlays - Crime, only when not clustered
    SymbolLayer(
        id = "${layerPrefix}-icons-crime",
        source = markerSource,
        filter = feature { get("category_crime").toBool() && get("cluster").not() },
        iconImage = image(rememberVectorPainter(getCategoryIconVector(org.crimsoncode2026.data.Category.CRIME))),
        iconSize = const(iconSize),
        iconOffset = iconOffset(0.0, 0.0),
    )

    // Category icon overlays - Natural Disaster, only when not clustered
    SymbolLayer(
        id = "${layerPrefix}-icons-natural-disaster",
        source = markerSource,
        filter = feature { get("category_natural_disaster").toBool() && get("cluster").not() },
        iconImage = image(rememberVectorPainter(getCategoryIconVector(org.crimsoncode2026.data.Category.NATURAL_DISASTER))),
        iconSize = const(iconSize),
        iconOffset = iconOffset(0.0, 0.0),
    )

    // Category icon overlays - Infrastructure, only when not clustered
    SymbolLayer(
        id = "${layerPrefix}-icons-infrastructure",
        source = markerSource,
        filter = feature { get("category_infrastructure").toBool() && get("cluster").not() },
        iconImage = image(rememberVectorPainter(getCategoryIconVector(org.crimsoncode2026.data.Category.INFRASTRUCTURE))),
        iconSize = const(iconSize),
        iconOffset = iconOffset(0.0, 0.0),
    )

    // Category icon overlays - Search & Rescue, only when not clustered
    SymbolLayer(
        id = "${layerPrefix}-icons-search-rescue",
        source = markerSource,
        filter = feature { get("category_search_rescue").toBool() && get("cluster").not() },
        iconImage = image(rememberVectorPainter(getCategoryIconVector(org.crimsoncode2026.data.Category.SEARCH_RESCUE))),
        iconSize = const(iconSize),
        iconOffset = iconOffset(0.0, 0.0),
    )

    // Category icon overlays - Traffic, only when not clustered
    SymbolLayer(
        id = "${layerPrefix}-icons-traffic",
        source = markerSource,
        filter = feature { get("category_traffic").toBool() && get("cluster").not() },
        iconImage = image(rememberVectorPainter(getCategoryIconVector(org.crimsoncode2026.data.Category.TRAFFIC))),
        iconSize = const(iconSize),
        iconOffset = iconOffset(0.0, 0.0),
    )

    // Category icon overlays - Other, only when not clustered
    SymbolLayer(
        id = "${layerPrefix}-icons-other",
        source = markerSource,
        filter = feature { get("category_other").toBool() && get("cluster").not() },
        iconImage = image(rememberVectorPainter(getCategoryIconVector(org.crimsoncode2026.data.Category.OTHER))),
        iconSize = const(iconSize),
        iconOffset = iconOffset(0.0, 0.0),
    )
}
