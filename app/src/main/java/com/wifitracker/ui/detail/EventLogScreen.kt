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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.wifitracker.R
import com.wifitracker.domain.model.BssidRecord
import com.wifitracker.domain.model.WifiEvent
import com.wifitracker.util.MondayFirstLocale
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
    val bssidRecords by viewModel.bssidRecords.collectAsState()
    var editingEvent by remember { mutableStateOf<WifiEvent?>(null) }
    var isEditMode by remember { mutableStateOf(false) }

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
                },
                actions = {
                    TextButton(onClick = { isEditMode = !isEditMode }) {
                        Text(
                            if (isEditMode)
                                stringResource(R.string.view_only_mode)
                            else
                                stringResource(R.string.enable_edit_mode)
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
            if (bssidRecords.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.known_bssids),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(bssidRecords) { record ->
                    BssidItem(record = record)
                }
                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                }
            }

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
                        isEditMode = isEditMode,
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
                        isEditMode = isEditMode,
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
fun BssidItem(record: BssidRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = record.bssid,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.bssid_first_seen, formatTimestamp(record.firstSeenAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EventItem(
    event: WifiEvent,
    isEditMode: Boolean,
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

            if (event.isEditable && isEditMode) {
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
    val initialZoned = Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault())
    var showTimePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val afterMinLabel = event.minTimestamp?.let { formatTimestamp(it) }
    val beforeMaxLabel = event.maxTimestamp?.let { formatTimestamp(it) }
    val constraintLabel = when {
        afterMinLabel != null && beforeMaxLabel != null ->
            stringResource(R.string.edit_time_between, afterMinLabel, beforeMaxLabel)
        afterMinLabel != null ->
            stringResource(R.string.edit_time_after_min, afterMinLabel)
        beforeMaxLabel != null ->
            stringResource(R.string.edit_time_before_max, beforeMaxLabel)
        else -> null
    }

    // Use Monday-first locale so the calendar week always starts on Monday
    val datePickerState = remember {
        DatePickerState(
            locale = MondayFirstLocale,
            initialSelectedDateMillis = event.timestamp
        )
    }

    val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    val timePickerState = rememberTimePickerState(
        initialHour = initialZoned.hour,
        initialMinute = initialZoned.minute,
        is24Hour = is24Hour
    )

    if (!showTimePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        errorMessage = null
                        showTimePicker = true
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text(stringResource(R.string.next))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.edit_event)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMs ->
                            // DatePicker stores dates as midnight UTC; extract the UTC date
                            // then apply the user-selected local time
                            val selectedDate = Instant.ofEpochMilli(dateMs)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            val newTimestamp = selectedDate
                                .atTime(timePickerState.hour, timePickerState.minute)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()

                            val isAfterMinimum = event.minTimestamp == null || newTimestamp > event.minTimestamp
                            val isBeforeMaximum = event.maxTimestamp == null || newTimestamp < event.maxTimestamp

                            if (isAfterMinimum && isBeforeMaximum) {
                                onSave(newTimestamp)
                            } else {
                                errorMessage = constraintLabel
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    errorMessage = null
                }) {
                    Text(stringResource(R.string.back))
                }
            }
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
