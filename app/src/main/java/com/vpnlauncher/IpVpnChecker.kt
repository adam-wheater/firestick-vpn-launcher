package com.vpnlauncher

import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Checks if the device's public IP belongs to a VPN provider by querying
 * an external API. Used for router-level VPN detection where no VPN
 * interface is visible to Android.
 */
class IpVpnChecker {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Asynchronously checks if the public IP appears to be behind a VPN.
     * Uses ip-api.com which returns hosting/proxy detection fields.
     * Calls back on the main thread.
     */
    fun checkVpnByIp(callback: (Boolean) -> Unit) {
        Thread {
            val result = try {
                val url = URL("http://ip-api.com/json/?fields=hosting,proxy")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "GET"

                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val json = JSONObject(response)
                // hosting=true indicates datacenter IP (VPN/proxy)
                // proxy=true indicates known proxy/VPN
                json.optBoolean("hosting", false) || json.optBoolean("proxy", false)
            } catch (e: Exception) {
                // On failure (no internet, timeout), assume not VPN
                // The user's router VPN toggle is the primary signal
                false
            }

            mainHandler.post { callback(result) }
        }.start()
    }
}
