package com.wifitracker.ui.trackers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wifitracker.R
import com.wifitracker.ui.components.DateFilterDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackersScreen(
    onNavigateToEventLog: (Long) -> Unit,
    viewModel: TrackersViewModel = hiltViewModel()
) {
    val trackers by viewModel.trackers.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedTrackerId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_trackers)) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                    onFilterClick = {
                        selectedTrackerId = tracker.id
                        showFilterDialog = true
                    },
                    onClick = { onNavigateToEventLog(tracker.id) }
                )
            }
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
