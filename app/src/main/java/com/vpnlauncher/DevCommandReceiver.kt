package com.vpnlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DevCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_RESET_BANNER) {
            AppConfigStore(context).isBannerDismissed = false
        }
    }

    companion object {
        const val ACTION_RESET_BANNER = "com.vpnlauncher.RESET_BANNER"
    }
}
