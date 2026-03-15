package com.vpnlauncher

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var vpnChecker: VpnChecker
    private lateinit var ipVpnChecker: IpVpnChecker
    private lateinit var configStore: AppConfigStore
    private lateinit var adapter: AppListAdapter
    private lateinit var tvVpnStatus: TextView
    private lateinit var ivVpnStatus: ImageView
    private lateinit var rvAppList: RecyclerView
    private lateinit var switchRouterVpn: Switch
    private lateinit var tvRouterVpnHint: TextView

    private var pendingLaunchPackage: String? = null
    private var routerVpnVerified: Boolean = false

    private val excludedPrefixes = listOf(
        "com.amazon.tv.",
        "com.amazon.device.",
        "com.android.",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vpnChecker = VpnChecker(this)
        ipVpnChecker = IpVpnChecker()
        configStore = AppConfigStore(this)

        tvVpnStatus = findViewById(R.id.tvVpnStatus)
        ivVpnStatus = findViewById(R.id.ivVpnStatus)
        rvAppList = findViewById(R.id.rvAppList)
        switchRouterVpn = findViewById(R.id.switchRouterVpn)
        tvRouterVpnHint = findViewById(R.id.tvRouterVpnHint)

        // Router VPN toggle
        switchRouterVpn.isChecked = configStore.isRouterVpnEnabled
        switchRouterVpn.setOnCheckedChangeListener { _, checked ->
            configStore.isRouterVpnEnabled = checked
            if (checked) {
                verifyRouterVpn()
            } else {
                routerVpnVerified = false
                tvRouterVpnHint.text = getString(R.string.router_vpn_hint)
                updateVpnStatus(vpnChecker.isVpnActive())
            }
        }

        adapter = AppListAdapter(emptyList(), configStore) { app ->
            onAppClicked(app)
        }

        rvAppList.layoutManager = LinearLayoutManager(this)
        rvAppList.adapter = adapter

        vpnChecker.startMonitoring { isConnected ->
            updateVpnStatus(isConnected)
        }

        updateVpnStatus(vpnChecker.isVpnActive())
        loadApps()

        // If router VPN mode is already on, verify on startup
        if (configStore.isRouterVpnEnabled) {
            verifyRouterVpn()
        }
    }

    override fun onResume() {
        super.onResume()

        val vpnActive = isVpnEffectivelyActive()
        updateVpnStatus(vpnActive)
        loadApps()

        // Re-verify router VPN on resume (network may have changed)
        if (configStore.isRouterVpnEnabled) {
            verifyRouterVpn()
        }

        pendingLaunchPackage?.let { packageName ->
            pendingLaunchPackage = null
            if (vpnActive) {
                launchApp(packageName)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vpnChecker.stopMonitoring()
    }

    /**
     * Returns true if VPN is active either at device level or router level (verified).
     */
    private fun isVpnEffectivelyActive(): Boolean {
        if (vpnChecker.isVpnActive()) return true
        if (configStore.isRouterVpnEnabled && routerVpnVerified) return true
        return false
    }

    private fun verifyRouterVpn() {
        tvRouterVpnHint.text = getString(R.string.vpn_checking)
        ipVpnChecker.checkVpnByIp { isVpn ->
            routerVpnVerified = isVpn
            if (isVpn) {
                tvRouterVpnHint.text = getString(R.string.router_vpn_verified)
                updateVpnStatus(true)
            } else {
                tvRouterVpnHint.text = getString(R.string.router_vpn_not_verified)
                // Still allow the toggle — the IP check is best-effort
                // The user's toggle is the primary signal
                if (configStore.isRouterVpnEnabled) {
                    updateVpnStatus(true)
                }
            }
        }
    }

    private fun loadApps() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            .filter { appInfo ->
                pm.getLaunchIntentForPackage(appInfo.packageName) != null
            }
            .filter { appInfo ->
                appInfo.packageName != packageName
            }
            .filter { appInfo ->
                excludedPrefixes.none { prefix ->
                    appInfo.packageName.startsWith(prefix)
                } && appInfo.packageName != "android"
            }
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            }
            .sortedBy { it.label.lowercase() }

        adapter.updateApps(apps)
    }

    private fun onAppClicked(app: AppInfo) {
        if (!configStore.isVpnRequired(app.packageName)) {
            launchApp(app.packageName)
            return
        }

        if (isVpnEffectivelyActive()) {
            launchApp(app.packageName)
            return
        }

        // Router VPN enabled but not verified — still allow with warning
        if (configStore.isRouterVpnEnabled) {
            Toast.makeText(this, R.string.router_vpn_not_verified, Toast.LENGTH_SHORT).show()
            launchApp(app.packageName)
            return
        }

        showVpnBlockedDialog(app.packageName)
    }

    private fun showVpnBlockedDialog(packageName: String) {
        val nordvpnIntent = packageManager.getLaunchIntentForPackage("com.nordvpn.android")

        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.vpn_not_connected_title)
            .setMessage(R.string.vpn_not_connected_message)
            .setNegativeButton(R.string.cancel, null)

        if (nordvpnIntent != null) {
            builder.setPositiveButton(R.string.open_nordvpn) { _, _ ->
                pendingLaunchPackage = packageName
                startActivity(nordvpnIntent)
            }
        } else {
            builder.setPositiveButton(R.string.vpn_app_not_found, null)
        }

        val dialog = builder.create()
        dialog.show()

        if (nordvpnIntent == null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        }
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        startActivity(intent)
    }

    private fun updateVpnStatus(isConnected: Boolean) {
        val effectivelyConnected = isConnected || isVpnEffectivelyActive()

        if (effectivelyConnected) {
            val isRouter = !vpnChecker.isVpnActive() && configStore.isRouterVpnEnabled
            tvVpnStatus.text = getString(
                if (isRouter) R.string.vpn_connected_router else R.string.vpn_connected
            )
            tvVpnStatus.setTextColor(getColor(R.color.vpn_connected))
            ivVpnStatus.setImageResource(R.drawable.ic_vpn_connected)
            ivVpnStatus.contentDescription = getString(R.string.vpn_connected)
        } else {
            tvVpnStatus.text = getString(R.string.vpn_disconnected)
            tvVpnStatus.setTextColor(getColor(R.color.vpn_disconnected))
            ivVpnStatus.setImageResource(R.drawable.ic_vpn_disconnected)
            ivVpnStatus.contentDescription = getString(R.string.vpn_disconnected)
        }
    }
}
