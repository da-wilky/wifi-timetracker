package app.swilk.wifitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.swilk.wifitracker.R
import app.swilk.wifitracker.domain.model.DateFilter
import app.swilk.wifitracker.util.MondayFirstLocale
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

private enum class CustomPickerStep { DATE_RANGE, START_TIME, END_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterDialog(
    currentFilter: DateFilter,
    onFilterSelected: (DateFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf<DateFilter?>(currentFilter) }
    var showCustomPicker by remember { mutableStateOf(false) }
    var customPickerStep by remember { mutableStateOf(CustomPickerStep.DATE_RANGE) }

    val dateRangePickerState = remember { DateRangePickerState(locale = MondayFirstLocale) }
    val startTimeState = rememberTimePickerState(initialHour = 0, initialMinute = 0, is24Hour = true)
    val endTimeState = rememberTimePickerState(initialHour = 23, initialMinute = 59, is24Hour = true)

    // Temporary storage for the date portion while stepping through the time pickers
    var tempStartDateMs by remember { mutableLongStateOf(0L) }
    var tempEndDateMs by remember { mutableLongStateOf(0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_time_range)) },
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
                    onClick = {
                        customPickerStep = CustomPickerStep.DATE_RANGE
                        showCustomPicker = true
                    },
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
        when (customPickerStep) {
            CustomPickerStep.DATE_RANGE -> {
                DatePickerDialog(
                    onDismissRequest = { showCustomPicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val startMs = dateRangePickerState.selectedStartDateMillis
                                val endMs = dateRangePickerState.selectedEndDateMillis
                                if (startMs != null && endMs != null) {
                                    tempStartDateMs = startMs
                                    tempEndDateMs = endMs
                                    customPickerStep = CustomPickerStep.START_TIME
                                }
                            }
                        ) {
                            Text(stringResource(R.string.next))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomPicker = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                ) {
                    DateRangePicker(
                        state = dateRangePickerState,
                        title = {
                            Text(
                                text = stringResource(R.string.filter_select_date_range),
                                modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                            )
                        }
                    )
                }
            }

            CustomPickerStep.START_TIME -> {
                AlertDialog(
                    onDismissRequest = { showCustomPicker = false },
                    title = { Text(stringResource(R.string.filter_select_start_time)) },
                    text = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TimePicker(state = startTimeState)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { customPickerStep = CustomPickerStep.END_TIME }) {
                            Text(stringResource(R.string.next))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { customPickerStep = CustomPickerStep.DATE_RANGE }) {
                            Text(stringResource(R.string.back))
                        }
                    }
                )
            }

            CustomPickerStep.END_TIME -> {
                AlertDialog(
                    onDismissRequest = { showCustomPicker = false },
                    title = { Text(stringResource(R.string.filter_select_end_time)) },
                    text = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            TimePicker(state = endTimeState)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val startMs = combineDateAndTime(
                                    utcDateMs = tempStartDateMs,
                                    hour = startTimeState.hour,
                                    minute = startTimeState.minute
                                )
                                val endMs = combineDateAndTime(
                                    utcDateMs = tempEndDateMs,
                                    hour = endTimeState.hour,
                                    minute = endTimeState.minute
                                )
                                selectedPreset = DateFilter.Custom(startMs = startMs, endMs = endMs)
                                showCustomPicker = false
                            }
                        ) {
                            Text(stringResource(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { customPickerStep = CustomPickerStep.START_TIME }) {
                            Text(stringResource(R.string.back))
                        }
                    }
                )
            }
        }
    }
}

/**
 * Combines a UTC-midnight date value (as returned by [DateRangePickerState]) with a
 * local-time offset so that the resulting timestamp represents the given [hour]:[minute]
 * on that calendar date in the device's default timezone.
 *
 * [utcDateMs] is interpreted as UTC midnight (the convention used by Material3's date
 * pickers). Using [ZoneOffset.UTC] for the initial conversion avoids a date shift that
 * would otherwise occur in timezones with a negative UTC offset.
 */
private fun combineDateAndTime(utcDateMs: Long, hour: Int, minute: Int): Long {
    val localDate = Instant.ofEpochMilli(utcDateMs)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
    return localDate
        .atTime(hour, minute)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}
