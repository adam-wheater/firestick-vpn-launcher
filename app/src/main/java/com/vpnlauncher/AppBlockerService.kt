package com.vpnlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service that:
 * 1. Redirects from Amazon home screen to VPN Launcher (home takeover)
 * 2. Blocks VPN-required apps when VPN is off
 */
class AppBlockerService : AccessibilityService() {

    private lateinit var vpnChecker: VpnChecker
    private lateinit var configStore: AppConfigStore

    // Amazon launcher package names
    private val amazonLaunchers = setOf(
        "com.amazon.tv.launcher",
        "com.amazon.tv.leanbacklauncher",
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        vpnChecker = VpnChecker(this)
        configStore = AppConfigStore(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Skip our own app and system internals
        if (packageName == "com.vpnlauncher") return
        if (packageName.startsWith("com.android.")) return
        if (packageName == "android") return

        // Redirect Amazon launcher to VPN Launcher (home takeover)
        if (packageName in amazonLaunchers) {
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
