package app.swilk.wifitracker.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.swilk.wifitracker.util.TimeFormatter

@Composable
fun TimerDisplay(
    durationMs: Long,
    showDays: Boolean = false,
    modifier: Modifier = Modifier
) {
    Text(
        text = TimeFormatter.formatDuration(durationMs, showDays),
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier
    )
}
