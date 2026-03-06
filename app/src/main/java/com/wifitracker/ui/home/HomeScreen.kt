package com.wifitracker.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.wifitracker.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNavigateToTrackers: () -> Unit,
    onNavigateToEventLog: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val currentSsid by viewModel.currentSsid.collectAsState()
    val isTracked by viewModel.isTracked.collectAsState()
    val showWarning by viewModel.showOrphanedWarning.collectAsState()

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showWarning) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.orphaned_sessions_warning),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = { viewModel.dismissOrphanedWarning() }) {
                                Text(stringResource(R.string.cancel))
                            }
                            Button(onClick = {
                                viewModel.getFirstTrackerId()?.let { trackerId ->
                                    onNavigateToEventLog(trackerId)
                                }
                                viewModel.dismissOrphanedWarning()
                            }) {
                                Text(stringResource(R.string.view_events))
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (currentSsid != null) {
                            stringResource(R.string.connected_to, currentSsid!!)
                        } else {
                            stringResource(R.string.not_connected)
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Button(
                        onClick = {
                            if (isTracked) {
                                onNavigateToTrackers()
                            } else {
                                viewModel.createTracker()
                                onNavigateToTrackers()
                            }
                        },
                        enabled = currentSsid != null
                    ) {
                        Text(stringResource(R.string.create_tracker))
                    }
                }
            }

            if (!permissionsState.allPermissionsGranted) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.permission_rationale),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }
        }
    }
}
