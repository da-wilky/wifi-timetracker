package com.wifitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wifitracker.R
import com.wifitracker.domain.model.DateFilter
import com.wifitracker.util.MondayFirstLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterDialog(
    currentFilter: DateFilter,
    onFilterSelected: (DateFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf<DateFilter?>(currentFilter) }
    var showCustomPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time Range") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPreset is DateFilter.All,
                    onClick = { selectedPreset = DateFilter.All },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
                FilterChip(
                    selected = selectedPreset is DateFilter.Today,
                    onClick = { selectedPreset = DateFilter.Today },
                    label = { Text(stringResource(R.string.filter_today)) }
                )
                FilterChip(
                    selected = selectedPreset is DateFilter.Yesterday,
                    onClick = { selectedPreset = DateFilter.Yesterday },
                    label = { Text(stringResource(R.string.filter_yesterday)) }
                )
                FilterChip(
                    selected = selectedPreset is DateFilter.ThisWeek,
                    onClick = { selectedPreset = DateFilter.ThisWeek },
                    label = { Text(stringResource(R.string.filter_this_week)) }
                )
                FilterChip(
                    selected = selectedPreset is DateFilter.LastWeek,
                    onClick = { selectedPreset = DateFilter.LastWeek },
                    label = { Text(stringResource(R.string.filter_last_week)) }
                )
                FilterChip(
                    selected = selectedPreset is DateFilter.ThisMonth,
                    onClick = { selectedPreset = DateFilter.ThisMonth },
                    label = { Text(stringResource(R.string.filter_this_month)) }
                )
                FilterChip(
                    selected = selectedPreset is DateFilter.LastMonth,
                    onClick = { selectedPreset = DateFilter.LastMonth },
                    label = { Text(stringResource(R.string.filter_last_month)) }
                )
                FilterChip(
                    selected = selectedPreset is DateFilter.Custom,
                    onClick = { showCustomPicker = true },
                    label = { Text(stringResource(R.string.filter_custom)) }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedPreset?.let { onFilterSelected(it) }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showCustomPicker) {
        val datePickerState = remember {
            DatePickerState(locale = MondayFirstLocale)
        }
        DatePickerDialog(
            onDismissRequest = { showCustomPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { startMs ->
                            selectedPreset = DateFilter.Custom(
                                startMs = startMs,
                                endMs = System.currentTimeMillis()
                            )
                        }
                        showCustomPicker = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
