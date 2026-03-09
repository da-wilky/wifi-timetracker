package app.swilk.wifitracker.ui.trackers

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
import app.swilk.wifitracker.R
import app.swilk.wifitracker.ui.components.DateFilterDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackersScreen(
    onNavigateToEventLog: (Long) -> Unit,
    viewModel: TrackersViewModel = hiltViewModel()
) {
    val trackers by viewModel.trackers.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val showDays by viewModel.showDays.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

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
                title = { Text(stringResource(R.string.tab_trackers)) },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.filter_time_range)
                        )
                    }
                }
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trackers, key = { it.id }) { tracker ->
                    val displayTime by viewModel.getTrackerTime(tracker.id).collectAsState()

                    TrackerCard(
                        tracker = tracker,
                        displayTime = displayTime,
                        onDelete = { viewModel.deleteTracker(tracker) },
                        onReset = { viewModel.resetTimer(tracker.id) },
                        onClick = { onNavigateToEventLog(tracker.id) },
                        showDays = showDays
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
