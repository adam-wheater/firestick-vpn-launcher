package com.vpnlauncher

import android.content.Context
import android.content.SharedPreferences

class AppConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vpn_launcher_prefs", Context.MODE_PRIVATE)

    private val key = "vpn_required_packages"
    private val routerVpnKey = "router_vpn_enabled"
    private val appOrderKey = "app_order"
    private val hiddenAppsKey = "hidden_packages"

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

    /**
     * Returns the saved app order as a list of package names.
     * Apps not in this list appear at the end (alphabetically).
     */
    fun getAppOrder(): List<String> {
        val orderString = prefs.getString(appOrderKey, null) ?: return emptyList()
        return orderString.split(",").filter { it.isNotEmpty() }
    }

    fun saveAppOrder(order: List<String>) {
        prefs.edit().putString(appOrderKey, order.joinToString(",")).apply()
    }

    fun isHidden(packageName: String): Boolean {
        return getHiddenPackages().contains(packageName)
    }

    fun setHidden(packageName: String, hidden: Boolean) {
        val packages = getHiddenPackages().toMutableSet()
        if (hidden) packages.add(packageName) else packages.remove(packageName)
        prefs.edit().putStringSet(hiddenAppsKey, packages).apply()
    }

    fun getHiddenPackages(): Set<String> {
        return prefs.getStringSet(hiddenAppsKey, emptySet()) ?: emptySet()
    }
}
