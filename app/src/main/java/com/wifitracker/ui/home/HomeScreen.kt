package com.wifitracker.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.wifitracker.R
import com.wifitracker.ui.components.DateFilterDialog
import com.wifitracker.ui.trackers.TrackerCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNavigateToEventLog: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val currentSsid by viewModel.currentSsid.collectAsState()
    val isTracked by viewModel.isTracked.collectAsState()
    val trackers by viewModel.trackers.collectAsState()
    val showWarning by viewModel.showOrphanedWarning.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // Split trackers: connected one first (if tracked), then the rest
    val connectedTracker = if (isTracked && currentSsid != null) {
        trackers.firstOrNull { it.ssid == currentSsid }
    } else null
    val remainingTrackers = trackers.filter { it.id != connectedTracker?.id }

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            pullRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Orphaned-sessions warning
                if (showWarning) {
                    item(key = "orphaned_warning") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.orphaned_sessions_warning),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { viewModel.dismissOrphanedWarning() }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                    Button(onClick = {
                                        viewModel.getFirstTrackerId()?.let { onNavigateToEventLog(it) }
                                        viewModel.dismissOrphanedWarning()
                                    }) {
                                        Text(stringResource(R.string.view_events))
                                    }
                                }
                            }
                        }
                    }
                }

                // Permissions card
                if (!permissionsState.allPermissionsGranted) {
                    item(key = "permissions_card") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.permission_rationale),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                                    Text(stringResource(R.string.grant_permissions))
                                }
                            }
                        }
                    }
                }

                // Connection status section (above "Trackers" heading)
                when {
                    currentSsid == null -> {
                        // Not connected to any network
                        item(key = "not_connected_hint") {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.not_connected_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    !isTracked -> {
                        // Connected to an untracked network — offer "Start Tracking"
                        item(key = "untracked_network") {
                            currentSsid?.let { ssid ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.connected_to, ssid),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Button(onClick = { viewModel.createTracker() }) {
                                            Text(stringResource(R.string.start_tracking))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // "Trackers" heading with global filter settings icon
                item(key = "trackers_heading") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.tab_trackers),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.filter_time_range)
                            )
                        }
                    }
                }

                // Currently connected tracker card (highlighted, at top of list)
                connectedTracker?.let { tracker ->
                    item(key = "connected_${tracker.id}") {
                        val displayTime by viewModel.getTrackerTime(tracker.id).collectAsState()
                        TrackerCard(
                            tracker = tracker,
                            displayTime = displayTime,
                            isCurrentlyConnected = true,
                            onDelete = { viewModel.deleteTracker(tracker) },
                            onReset = { viewModel.resetTimer(tracker.id) },
                            onClick = { onNavigateToEventLog(tracker.id) }
                        )
                    }
                }

                // Remaining trackers
                items(remainingTrackers, key = { it.id }) { tracker ->
                    val displayTime by viewModel.getTrackerTime(tracker.id).collectAsState()
                    TrackerCard(
                        tracker = tracker,
                        displayTime = displayTime,
                        onDelete = { viewModel.deleteTracker(tracker) },
                        onReset = { viewModel.resetTimer(tracker.id) },
                        onClick = { onNavigateToEventLog(tracker.id) }
                    )
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showFilterDialog) {
        DateFilterDialog(
            currentFilter = selectedFilter,
            onFilterSelected = { filter ->
                viewModel.setFilter(filter)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}
