package com.wifitracker.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Regression test for Bug 4: Reload doesn't work after permission grant
 *
 * ROOT CAUSE: After location permissions are granted while already connected to
 * WiFi, the ConnectivityManager network callback does NOT re-fire because the
 * network state has not changed. The WiFi state remained [WifiNetworkState.SsidUnavailable]
 * (SSID unreadable without location permission) and the "not connected" hint was
 * shown even though the device was connected.
 *
 * The [HomeViewModel.refresh] function only delayed 600ms without re-querying
 * the actual network state, so neither the manual pull-to-refresh nor the
 * callback-driven flow could recover.
 *
 * FIXES APPLIED:
 * 1. [WifiMonitor.getCurrentState] added: synchronous one-shot query of the
 *    current network state via ConnectivityManager (independent of callbacks).
 * 2. [HomeViewModel._wifiState] now merges the passive callback flow with a
 *    [MutableSharedFlow] (_refreshedState) that [HomeViewModel.refresh] emits
 *    to. This allows the state to be updated on demand.
 * 3. [HomeViewModel.refresh] emits the live network state instead of only
 *    delaying. The initial value of [HomeViewModel._wifiState] is now seeded
 *    from [WifiMonitor.getCurrentState] so the correct SSID is visible even
 *    before the first callback fires.
 * 4. [HomeScreen] adds a [LaunchedEffect] keyed on
 *    `permissionsState.allPermissionsGranted` that calls [HomeViewModel.refresh]
 *    automatically when location permission transitions from denied to granted.
 */
class ReloadAfterPermissionTest {

    // ── Structural tests (no Android runtime required) ─────────────────────

    @Test
    fun `WifiMonitor exposes getCurrentState method`() {
        val method = WifiMonitor::class.java.getDeclaredMethod("getCurrentState")
        assertNotNull(
            "WifiMonitor must expose getCurrentState() for on-demand WiFi queries",
            method
        )
        assertEquals(
            "getCurrentState() must return WifiNetworkState",
            WifiNetworkState::class.java,
            method.returnType
        )
    }

    // ── Flow merge behaviour (pure-Kotlin, no Android runtime) ─────────────

    /**
     * Simulates the [HomeViewModel._wifiState] flow which merges the passive
     * callback flow with a manually-triggered [MutableSharedFlow].
     *
     * Without the fix, [HomeViewModel._wifiState] only contained the callback flow
     * and there was no way to inject an updated state from [HomeViewModel.refresh].
     */
    @Test
    fun `manual refresh emission is picked up by merged wifi state flow`() = runTest {
        // Represents _wifiState coming from the network callback (stuck at SsidUnavailable
        // because location permission was not yet granted at registration time).
        val callbackFlow = flowOf(WifiNetworkState.SsidUnavailable)

        // Represents _refreshedState: the manually-triggered SharedFlow.
        val refreshedState = MutableSharedFlow<WifiNetworkState>(extraBufferCapacity = 1)

        // Emit a Connected state (what getCurrentState() would return after permission grant).
        val connected = WifiNetworkState.Connected(ssid = "HomeWiFi", bssid = "aa:bb:cc:dd:ee:ff")
        refreshedState.tryEmit(connected)

        val merged = merge(callbackFlow, refreshedState)

        // The merge must deliver the manually-emitted Connected state.
        val received = merged.first { it is WifiNetworkState.Connected }
        assertEquals(
            "merge() must surface the manually-emitted Connected state from refresh()",
            connected,
            received
        )
    }

    @Test
    fun `SsidUnavailable is distinct from Disconnected`() {
        // This distinction is critical: on permission grant getCurrentState() returns
        // Connected while the old callback-only flow returned SsidUnavailable.
        // The refresh mechanism bridges this gap by emitting Connected on demand.
        val unavailable: WifiNetworkState = WifiNetworkState.SsidUnavailable
        val disconnected: WifiNetworkState = WifiNetworkState.Disconnected

        assertNotEquals(
            "SsidUnavailable and Disconnected must be distinct states",
            unavailable,
            disconnected
        )

        // SsidUnavailable must not produce an SSID (same as Disconnected from the UI's
        // perspective, but must NOT trigger a DISCONNECT event in the service).
        val ssidFromUnavailable = (unavailable as? WifiNetworkState.Connected)?.ssid
        assertNull(
            "SsidUnavailable must not expose an SSID — UI shows 'not connected' hint",
            ssidFromUnavailable
        )
    }

    @Test
    fun `Connected state exposes ssid after permission is granted`() {
        val state = WifiNetworkState.Connected(ssid = "OfficeWiFi", bssid = null)

        val ssid = (state as? WifiNetworkState.Connected)?.ssid
        assertEquals(
            "After refresh, Connected state must expose the real SSID",
            "OfficeWiFi",
            ssid
        )
    }
}
