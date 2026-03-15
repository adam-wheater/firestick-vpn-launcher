package com.vpnlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

/**
 * Detects installed VPN apps by looking for apps that declare
 * the android.net.VpnService intent filter.
 */
class VpnAppDetector(private val context: Context) {

    data class VpnApp(
        val packageName: String,
        val label: String
    )

    fun getInstalledVpnApps(): List<VpnApp> {
        val pm = context.packageManager
        val vpnIntent = Intent("android.net.VpnService")

        @Suppress("DEPRECATION")
        val resolvedServices: List<ResolveInfo> = pm.queryIntentServices(vpnIntent, 0)

        return resolvedServices
            .mapNotNull { resolveInfo ->
                val serviceInfo = resolveInfo.serviceInfo ?: return@mapNotNull null
                val pkg = serviceInfo.packageName
                // Exclude system VPN services
                if (pkg.startsWith("com.android.") || pkg == "android") return@mapNotNull null
                // Exclude ourselves
                if (pkg == context.packageName) return@mapNotNull null

                try {
                    @Suppress("DEPRECATION")
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    VpnApp(
                        packageName = pkg,
                        label = pm.getApplicationLabel(appInfo).toString()
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
