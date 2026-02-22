package org.crimsoncode2026.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventNote

/**
 * Main app screen with event map, settings, FAB, and event list toggle button
 *
 * Features:
 * - Event map view (to be implemented)
 * - Settings button in top app bar
 * - FAB button to launch event creation wizard
 * - Event list toggle button showing event count
 *
 * @param onNavigateToSettings Callback when user navigates to settings
 * @param onCreateEvent Callback when user taps FAB to create event
 * @param eventCount Number of events to display on list button
 * @param onShowEventList Callback when user taps event list button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    onCreateEvent: () -> Unit = {},
    eventCount: Int = 0,
    onShowEventList: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CrimsonCode") },
                actions = {
                    Row {
                        // Event list toggle button with badge
                        Box {
                            androidx.compose.material3.IconButton(onClick = onShowEventList) {
                                Icon(Icons.Default.EventNote, contentDescription = "Event List")
                            }
                            // Event count badge
                            if (eventCount > 0) {
                                androidx.compose.material3.Badge(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = eventCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = androidx.compose.foundation.layout.width(8.dp))

                        // Settings button
                        androidx.compose.material3.IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEvent
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Event")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Main App - Event Map Coming Soon")
        }
    }
}
