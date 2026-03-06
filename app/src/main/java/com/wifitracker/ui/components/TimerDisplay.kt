package com.wifitracker.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wifitracker.util.TimeFormatter

@Composable
fun TimerDisplay(
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    Text(
        text = TimeFormatter.formatDuration(durationMs),
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier
    )
}
