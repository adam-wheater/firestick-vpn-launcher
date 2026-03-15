package com.vpnlauncher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

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

        if (packageName == "com.vpnlauncher") return
        if (packageName.startsWith("com.android.")) return
        if (packageName == "android") return

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
