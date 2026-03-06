package com.wifitracker.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.wifitracker.R
import com.wifitracker.domain.model.WifiEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLogScreen(
    trackerId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EventLogViewModel = hiltViewModel()
) {
    val eventsPager = viewModel.eventsPager.collectAsLazyPagingItems()
    val recentSessions by viewModel.recentSessions.collectAsState()
    var editingEvent by remember { mutableStateOf<WifiEvent?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.event_log)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (recentSessions.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.recent_sessions),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(recentSessions.take(4)) { event ->
                    EventItem(
                        event = event,
                        onEdit = { editingEvent = event }
                    )
                }

                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "All Events",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(eventsPager.itemCount) { index ->
                eventsPager[index]?.let { event ->
                    EventItem(
                        event = event,
                        onEdit = { editingEvent = event }
                    )
                }
            }
        }
    }

    editingEvent?.let { event ->
        EventEditDialog(
            event = event,
            onSave = { newTimestamp ->
                viewModel.updateEvent(event.id, newTimestamp)
                editingEvent = null
            },
            onDismiss = { editingEvent = null }
        )
    }
}

@Composable
fun EventItem(
    event: WifiEvent,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (event.eventType.name) {
                        "CONNECT" -> stringResource(R.string.connect_event)
                        "DISCONNECT" -> stringResource(R.string.disconnect_event)
                        else -> event.eventType.name
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatTimestamp(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (event.isEditable) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_event)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditDialog(
    event: WifiEvent,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = event.timestamp
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_event)) },
        text = {
            DatePicker(state = datePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onSave(it) }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
