# Task
- Create an Android App using Kotlin.

# What should the App do
- The App should provide the functionality to track the time a WiFi Network with a specific SSID is connected.
- Access the nearby WiFi Devices with permission NEARBY_WIFI_DEVICES and neverForLocation flag.
- Default-View
    - show the currently connected SSID
    - click on a button to create a Tracker for that SSID
- Tracker-View
    - this view contains all tracked SSIDs and their timers
    - it needs to be possible to remove the tracking for an SSID (request confirmation before deletion)
    - reset of a timer needs to be possible (request confirmation before deletion)
    - it needs to be setable for which interval the tracked time should be shown (e.g. only this month/last month/date x to date y), on default show the overall time
    - also show the 2 most recent recorded sessions (with connect and disconnect event time)
    - on click onto a timer go into a view for it -> show all the Timeranges recorded for it (lazy loading, load the latest 20 and more on scroll down), Use Jetpack Paging 3 for the event log list.
        - on this view the records should be editable (the time for the connect/disconnect events)
        - Only completed sessions (CONNECT + DISCONNECT pair) are editable. Open/ongoing sessions are read-only until they close.
- General
    - The App should be able to run permently in the background and track connect and disconnect events for the networks and from those calculate the connected time for the specific SSID. 
    - Trackers need to be persistent on the device

# Stack
- Kotlin

# Important Notes
- This app needs to be optimized for low battery usage.
- The UI needs to be pretty and UX optimized

# Additional Notes
- For Background-working use ForegroundService, use foregroundServiceType connectedDevice
- Min Android API 33
- Use Jetpack Compose as UI toolkit
- Use Room for persistence
- The Data Model chosen for the TimeTracking needs to support storing when the device was connected to the SSID. You should track the Network Connect and Disconnect events and calculate the connected time from them.
- Timer Display should include Days, hours, minutes, seconds
- Date filter should be fully customizable (from day, hour, minute, second till day hour minute second with presets like "this month", "last month", "this week", "last week", "today", "yesterday")
- App Language should be English with German support
- both dark and light theme
- If the user clicks on an SSID that is already being tracked redirect him to the tracking page
- Two identical SSIDs but different networks should be differentiated by BSSID -> if there are two trackers with same SSID, the one with the correct BSSID gets the events.
- On Tracker Deletion the data should be deleted too
- The App should request IGNORE_BATTERY_OPTIMIZATION
- The reset timer action should remove all entries for that timetracker
- The ForegroundServiceNotification should only show "Currently tracking network: [SSID]" or "Currently not tracking"
- If a tracker currently has an open session (connected right now), the displayed time should update every second in real-time using the formula: stored_time + (now - last_connect_timestamp).
- Read SSID and BSSID via ConnectivityManager.registerNetworkCallback() using NetworkCallback(FLAG_INCLUDE_LOCATION_INFO), extracting WifiInfo from NetworkCapabilities.transportInfo inside onCapabilitiesChanged()
- If no WiFi is currently connected, the Default-View should show a placeholder state (e.g. 'Not connected to any WiFi network') and disable the 'Create Tracker' button.
- Use MVVM architecture with a ViewModel per screen. Use Hilt for dependency injection.
- Register a BOOT_COMPLETED BroadcastReceiver to restart the ForegroundService automatically after device reboot.
- Use a bottom navigation bar with two tabs: 'Home' (Default-View) and 'Trackers' (Tracker-View). The detail view (event log) is pushed onto the navigation back stack from the Tracker-View."

# Crash-behavior
- On App Startup check for already connected network and in case we are tracking that network insert a connect for that at the moment of app startup. Only run this if the ForegroundService is not already running (i.e. first launch or after a crash where the service died). The service itself handles all events during normal operation.
- On App Startup check all timers for open connect statements where the disconnect wasnt captured. Send a warning to the user and show a button, that redirects him to the page where all entries can be seen. Insert a DISCONNECT statement with the current time. Leave the user the option to specify a custom time (edit the disconnect time) so it might get more accurate based on his memory.
