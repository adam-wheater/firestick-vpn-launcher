package com.vpnlauncher

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
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

    private val excludedPrefixes = listOf(
        "com.amazon.tv.",
        "com.amazon.device.",
        "com.android.",
    )

    private val excludedPackages = setOf(
        "android",
        "com.nordvpn.android",
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

        // Quick access buttons
        findViewById<TextView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }

        findViewById<TextView>(R.id.btnAppStore).setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage("com.amazon.venezia")
                ?: packageManager.getLaunchIntentForPackage("com.amazon.apps.store")
            intent?.let { startActivity(it) }
        }

        findViewById<TextView>(R.id.btnNordVpn).setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage("com.nordvpn.android")
            intent?.let { startActivity(it) }
        }

        // Router VPN toggle
        switchRouterVpn.isChecked = configStore.isRouterVpnEnabled
        switchRouterVpn.setOnCheckedChangeListener { _, checked ->
            configStore.isRouterVpnEnabled = checked
            if (checked) {
                verifyRouterVpn()
            } else {
                tvRouterVpnHint.text = getString(R.string.router_vpn_hint)
                refreshVpnHeader()
            }
        }

        adapter = AppListAdapter(emptyList(), configStore) { app ->
            onAppClicked(app)
        }

        rvAppList.layoutManager = LinearLayoutManager(this)
        rvAppList.adapter = adapter

        vpnChecker.startMonitoring { _ ->
            refreshVpnHeader()
        }

        refreshVpnHeader()
        loadApps()

        if (configStore.isRouterVpnEnabled) {
            verifyRouterVpn()
        }
    }

    override fun onResume() {
        super.onResume()

        refreshVpnHeader()
        loadApps()

        // Handle pending launch from returning from NordVPN
        val pending = pendingLaunchPackage
        if (pending != null) {
            pendingLaunchPackage = null
            if (vpnChecker.isVpnActive()) {
                // Device VPN is on — launch immediately
                launchApp(pending)
            } else if (configStore.isRouterVpnEnabled) {
                // Router mode — verify IP then launch if confirmed
                val savedPending = pending
                ipVpnChecker.checkVpnByIp { isVpn ->
                    if (isVpn) {
                        launchApp(savedPending)
                    }
                    tvRouterVpnHint.text = getString(
                        if (isVpn) R.string.router_vpn_verified
                        else R.string.router_vpn_not_verified
                    )
                    refreshVpnHeader()
                }
                return
            }
        }

        if (configStore.isRouterVpnEnabled) {
            verifyRouterVpn()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vpnChecker.stopMonitoring()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Do nothing — this is the home screen
    }

    /**
     * Returns true if VPN is active at device level, or if router VPN
     * mode is enabled (trusts the user's toggle).
     */
    private fun isVpnEffectivelyActive(): Boolean {
        if (vpnChecker.isVpnActive()) return true
        if (configStore.isRouterVpnEnabled) return true
        return false
    }

    private fun verifyRouterVpn() {
        tvRouterVpnHint.text = getString(R.string.vpn_checking)
        ipVpnChecker.checkVpnByIp { isVpn ->
            tvRouterVpnHint.text = getString(
                if (isVpn) R.string.router_vpn_verified
                else R.string.router_vpn_not_verified
            )
            refreshVpnHeader()
        }
    }

    /**
     * Updates the VPN status header based on current state.
     * Single source of truth — no parameters, just reads current state.
     */
    private fun refreshVpnHeader() {
        val deviceVpn = vpnChecker.isVpnActive()
        val routerVpn = configStore.isRouterVpnEnabled

        if (deviceVpn) {
            tvVpnStatus.text = getString(R.string.vpn_connected)
            tvVpnStatus.setTextColor(getColor(R.color.vpn_connected))
            ivVpnStatus.setImageResource(R.drawable.ic_vpn_connected)
            ivVpnStatus.contentDescription = getString(R.string.vpn_connected)
        } else if (routerVpn) {
            tvVpnStatus.text = getString(R.string.vpn_connected_router)
            tvVpnStatus.setTextColor(getColor(R.color.vpn_connected))
            ivVpnStatus.setImageResource(R.drawable.ic_vpn_connected)
            ivVpnStatus.contentDescription = getString(R.string.vpn_connected_router)
        } else {
            tvVpnStatus.text = getString(R.string.vpn_disconnected)
            tvVpnStatus.setTextColor(getColor(R.color.vpn_disconnected))
            ivVpnStatus.setImageResource(R.drawable.ic_vpn_disconnected)
            ivVpnStatus.contentDescription = getString(R.string.vpn_disconnected)
        }
    }

    private fun loadApps() {
        val pm = packageManager
        @Suppress("DEPRECATION")
        val apps = pm.getInstalledApplications(0)
            .filter { appInfo ->
                pm.getLaunchIntentForPackage(appInfo.packageName) != null
            }
            .filter { appInfo ->
                appInfo.packageName != packageName
            }
            .filter { appInfo ->
                appInfo.packageName !in excludedPackages
            }
            .filter { appInfo ->
                excludedPrefixes.none { prefix ->
                    appInfo.packageName.startsWith(prefix)
                }
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
}
