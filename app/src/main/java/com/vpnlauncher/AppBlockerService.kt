package com.vpnlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Monitors foreground app changes system-wide. When a VPN-required app
 * comes to the foreground without an active VPN, launches BlockedActivity
 * to prevent access.
 */
class AppBlockerService : AccessibilityService() {

    private lateinit var vpnChecker: VpnChecker
    private lateinit var configStore: AppConfigStore

    override fun onServiceConnected() {
        super.onServiceConnected()
        vpnChecker = VpnChecker(this)
        configStore = AppConfigStore(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Don't block ourselves or the system
        if (packageName == BuildConfig.APPLICATION_ID) return
        if (packageName == "com.nordvpn.android") return
        if (packageName.startsWith("com.android.")) return
        if (packageName == "android") return

        // Check if this app requires VPN
        if (!configStore.isVpnRequired(packageName)) return

        // Check if VPN is active (device-level or router-level)
        if (vpnChecker.isVpnActive()) return
        if (configStore.isRouterVpnEnabled) {
            // If router VPN mode is on, allow (trust the user's setting)
            return
        }

        // VPN required but not active — launch the blocked screen
        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("blocked_package", packageName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Required override, nothing to clean up
    }
}
