package com.vpnlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AppBlockerService : AccessibilityService() {

    private lateinit var vpnChecker: VpnChecker
    private lateinit var configStore: AppConfigStore

    private val amazonLaunchers = setOf(
        "com.amazon.tv.launcher",
        "com.amazon.tv.leanbacklauncher",
    )

    // Never redirect or block these packages
    private val whitelistedPackages = setOf(
        "com.vpnlauncher",
        "com.amazon.tv.settings",
        "com.amazon.device.settings",
        "com.amazon.venezia",           // Amazon Appstore
        "com.amazon.apps.store",
        "com.amazon.tv.settings.v2",
    )

    companion object {
        // Grace period: after user launches an app from our launcher,
        // ignore the Amazon launcher appearing briefly during transition
        var lastLaunchTime: Long = 0
        private const val GRACE_PERIOD_MS = 3000L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        vpnChecker = VpnChecker(this)
        configStore = AppConfigStore(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip our own app, system internals, and whitelisted apps
        if (packageName in whitelistedPackages) return
        if (packageName.startsWith("com.android.")) return
        if (packageName == "android") return

        // Redirect Amazon launcher to VPN Launcher
        if (packageName in amazonLaunchers) {
            // Don't redirect during grace period (user just launched something)
            if (System.currentTimeMillis() - lastLaunchTime < GRACE_PERIOD_MS) return

            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            return
        }

        // Block VPN-required apps when VPN is off
        if (!configStore.isVpnRequired(packageName)) return
        if (vpnChecker.isVpnActive()) return
        if (configStore.isRouterVpnEnabled) return

        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra("blocked_package", packageName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
