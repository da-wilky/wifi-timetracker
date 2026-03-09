package app.swilk.wifitracker.ui.trackers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.swilk.wifitracker.R
import app.swilk.wifitracker.domain.model.Tracker
import app.swilk.wifitracker.ui.components.ConfirmationDialog
import app.swilk.wifitracker.ui.components.TimerDisplay

@Composable
fun TrackerCard(
    tracker: Tracker,
    displayTime: Long,
    onDelete: () -> Unit,
    onReset: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentlyConnected: Boolean = false,
    showDays: Boolean = false
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = if (isCurrentlyConnected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isCurrentlyConnected) {
                Text(
                    text = stringResource(R.string.currently_connected_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = tracker.ssid,
                style = MaterialTheme.typography.titleLarge
            )

            TimerDisplay(durationMs = displayTime, showDays = showDays)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
