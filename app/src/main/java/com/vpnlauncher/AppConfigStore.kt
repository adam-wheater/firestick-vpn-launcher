package com.vpnlauncher

import android.content.Context
import android.content.SharedPreferences

class AppConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vpn_launcher_prefs", Context.MODE_PRIVATE)

    private val key = "vpn_required_packages"
    private val routerVpnKey = "router_vpn_enabled"

    fun isVpnRequired(packageName: String): Boolean {
        return getVpnRequiredPackages().contains(packageName)
    }

    fun setVpnRequired(packageName: String, required: Boolean) {
        val packages = getVpnRequiredPackages().toMutableSet()
        if (required) {
            packages.add(packageName)
        } else {
            packages.remove(packageName)
        }
        prefs.edit().putStringSet(key, packages).apply()
    }

    fun getVpnRequiredPackages(): Set<String> {
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
    }

    var isRouterVpnEnabled: Boolean
        get() = prefs.getBoolean(routerVpnKey, false)
        set(value) = prefs.edit().putBoolean(routerVpnKey, value).apply()
}
