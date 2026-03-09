package app.swilk.wifitracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.swilk.wifitracker.R
import app.swilk.wifitracker.service.WifiTrackingService
import app.swilk.wifitracker.ui.navigation.AppNavHost
import app.swilk.wifitracker.ui.navigation.BottomNavigationBar
import app.swilk.wifitracker.ui.navigation.Screen
import app.swilk.wifitracker.ui.theme.WifiTrackerTheme
import app.swilk.wifitracker.util.LocaleManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var localeManager: LocaleManager

    // Tracks whether the "stop tracking" confirmation dialog should be shown.
    // Updated from both onCreate and onNewIntent so it works whether the app
    // is freshly launched or already running when the notification action fires.
    private val showStopConfirmation = mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        // Create temporary LocaleManager to apply locale before dependency injection
        val tempLocaleManager = LocaleManager(newBase.applicationContext)
        super.attachBaseContext(tempLocaleManager.attachBaseContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showStopConfirmation.value =
            intent?.getBooleanExtra(EXTRA_SHOW_STOP_DIALOG, false) ?: false

        setContent {
            WifiTrackerTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Settings.route
                )

                val stopVisible by showStopConfirmation

                androidx.compose.material3.Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(navController = navController)
                        }
                    }
                ) { paddingValues ->
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                if (stopVisible) {
                    AlertDialog(
                        onDismissRequest = { showStopConfirmation.value = false },
                        title = { Text(stringResource(R.string.confirm_stop_tracking_title)) },
                        text = { Text(stringResource(R.string.confirm_stop_tracking_message)) },
                        confirmButton = {
                            Button(onClick = {
                                showStopConfirmation.value = false
                                val serviceIntent =
                                    Intent(this@MainActivity, WifiTrackingService::class.java)
                                stopService(serviceIntent)
                            }) {
                                Text(stringResource(R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showStopConfirmation.value = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Update the stop-dialog visibility when the activity is already running
        // and the user taps the notification's "Stop" action.
        showStopConfirmation.value =
            intent.getBooleanExtra(EXTRA_SHOW_STOP_DIALOG, false)
    }

    companion object {
        const val EXTRA_SHOW_STOP_DIALOG = "extra_show_stop_dialog"
    }
}
