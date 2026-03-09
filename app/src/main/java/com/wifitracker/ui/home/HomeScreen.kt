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
import com.wifitracker.domain.model.DateFilter
import com.wifitracker.ui.components.DateFilterDialog
import com.wifitracker.ui.trackers.TrackerCard
import com.wifitracker.util.DateFilterCalculator
import com.wifitracker.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNavigateToEventLog: (trackerId: Long, filterStart: Long, filterEnd: Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val currentSsid by viewModel.currentSsid.collectAsState()
    val isTracked by viewModel.isTracked.collectAsState()
    val trackers by viewModel.trackers.collectAsState()
    val showWarning by viewModel.showOrphanedWarning.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    // Compute filter nav-args once per filter change and reuse for all tracker clicks.
    val (filterNavStart, filterNavEnd) = remember(selectedFilter) {
        if (selectedFilter is DateFilter.All) {
            Pair(-1L, -1L)
        } else {
            DateFilterCalculator.calculateRange(selectedFilter)
        }
    }

    var showFilterDialog by remember { mutableStateOf(false) }

    val foregroundPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    )

    val backgroundLocationState = rememberMultiplePermissionsState(
        permissions = listOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    )

    // First, request foreground permissions
    LaunchedEffect(Unit) {
        if (!foregroundPermissionsState.allPermissionsGranted) {
            foregroundPermissionsState.launchMultiplePermissionRequest()
        }
    }

    // Once foreground permissions are granted, request background location
    LaunchedEffect(foregroundPermissionsState.allPermissionsGranted) {
        if (foregroundPermissionsState.allPermissionsGranted && !backgroundLocationState.allPermissionsGranted) {
            // Small delay to avoid overwhelming the user with back-to-back permission dialogs
            kotlinx.coroutines.delay(500)
            backgroundLocationState.launchMultiplePermissionRequest()
        }
    }

    val allPermissionsGranted = foregroundPermissionsState.allPermissionsGranted && backgroundLocationState.allPermissionsGranted

    // Track permission state transitions to trigger refresh only when changing from denied to granted
    var previousPermissionsGranted by remember { mutableStateOf(allPermissionsGranted) }
    LaunchedEffect(allPermissionsGranted) {
        if (!previousPermissionsGranted && allPermissionsGranted) {
            // Permissions just granted - wait briefly for Android to propagate the permission
            // before querying the WiFi state, otherwise getCurrentState() may still see <unknown ssid>
            kotlinx.coroutines.delay(200)
            viewModel.refresh()
        }
        previousPermissionsGranted = allPermissionsGranted
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
                                        viewModel.getFirstTrackerId()?.let { onNavigateToEventLog(it, -1L, -1L) }
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
                if (!allPermissionsGranted) {
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
                                Button(onClick = {
                                    if (!foregroundPermissionsState.allPermissionsGranted) {
                                        foregroundPermissionsState.launchMultiplePermissionRequest()
                                    } else if (!backgroundLocationState.allPermissionsGranted) {
                                        backgroundLocationState.launchMultiplePermissionRequest()
                                    }
                                }) {
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
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                        if (selectedFilter !is DateFilter.All) {
                            Text(
                                text = TimeFormatter.formatDateRange(filterNavStart, filterNavEnd),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
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
                            onClick = { onNavigateToEventLog(tracker.id, filterNavStart, filterNavEnd) }
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
                        onClick = { onNavigateToEventLog(tracker.id, filterNavStart, filterNavEnd) }
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
