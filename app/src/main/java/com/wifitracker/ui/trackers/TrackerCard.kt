package com.wifitracker.ui.trackers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wifitracker.R
import com.wifitracker.domain.model.Tracker
import com.wifitracker.ui.components.ConfirmationDialog
import com.wifitracker.ui.components.DateFilterDialog
import com.wifitracker.ui.components.TimerDisplay

@Composable
fun TrackerCard(
    tracker: Tracker,
    displayTime: Long,
    onDelete: () -> Unit,
    onReset: () -> Unit,
    onFilterClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = tracker.ssid,
                style = MaterialTheme.typography.titleLarge
            )

            TimerDisplay(durationMs = displayTime)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Filter"
                    )
                }
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.reset_timer)
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_tracker)
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showResetDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_reset_title),
            message = stringResource(R.string.confirm_reset_message),
            onConfirm = {
                onReset()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }
}
