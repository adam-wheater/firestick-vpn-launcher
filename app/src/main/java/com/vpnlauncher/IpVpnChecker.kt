package com.vpnlauncher

import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

/**
 * Checks if the device's public IP belongs to a VPN provider by querying
 * an external API. Used for router-level VPN detection where no VPN
 * interface is visible to Android.
 */
class IpVpnChecker {

    private val mainHandler = Handler(Looper.getMainLooper())

    // Known datacenter/VPN org keywords (case-insensitive match against ISP org name)
    private val vpnOrgKeywords = listOf(
        "nordvpn", "nord", "datacamp", "expressvpn", "surfshark",
        "cyberghost", "private internet access", "pia", "mullvad",
        "protonvpn", "proton", "ipvanish", "windscribe", "torguard",
        "hosting", "datacenter", "data center", "server", "cloud",
        "digital ocean", "digitalocean", "amazon", "aws", "linode",
        "vultr", "hetzner", "ovh", "m247"
    )

    /**
     * Asynchronously checks if the public IP appears to be behind a VPN.
     * Uses ipinfo.io (HTTPS, no key needed) and checks the org field
     * against known VPN/datacenter provider names.
     * Calls back on the main thread.
     */
    fun checkVpnByIp(callback: (Boolean) -> Unit) {
        Thread {
            val result = try {
                val url = URL("https://ipinfo.io/json")
                val conn = url.openConnection() as HttpsURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")

                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(response)
                val org = json.optString("org", "").lowercase()

                // Check if the org name matches known VPN/datacenter providers
                vpnOrgKeywords.any { keyword -> org.contains(keyword) }
            } catch (e: Exception) {
                false
            }

            mainHandler.post { callback(result) }
        }.start()
    }
}
