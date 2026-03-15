package com.vpnlauncher

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper

class VpnChecker(context: Context) {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var listener: ((Boolean) -> Unit)? = null
    private var vpnCallback: ConnectivityManager.NetworkCallback? = null

    fun isVpnActive(): Boolean {
        val activeNetwork = cm.activeNetwork
        if (activeNetwork != null) {
            val caps = cm.getNetworkCapabilities(activeNetwork)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) return true
        }

        for (network in cm.allNetworks) {
            val netCaps = cm.getNetworkCapabilities(network)
            if (netCaps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                return true
            }
        }
        return false
    }

    fun startMonitoring(onStatusChanged: (Boolean) -> Unit) {
        this.listener = onStatusChanged

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                mainHandler.post { listener?.invoke(true) }
            }

            override fun onLost(network: Network) {
                mainHandler.post { listener?.invoke(isVpnActive()) }
            }
        }

        vpnCallback = callback
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .build()
        cm.registerNetworkCallback(request, callback)
    }

    fun stopMonitoring() {
        vpnCallback?.let { cm.unregisterNetworkCallback(it) }
        vpnCallback = null
        listener = null
    }
}
