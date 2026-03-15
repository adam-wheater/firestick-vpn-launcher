package com.vpnlauncher

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
    private lateinit var tvServiceStatus: TextView

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
        tvServiceStatus = findViewById(R.id.tvServiceStatus)

        // Service status banner — click to enable
        tvServiceStatus.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                showEnableServiceDialog()
            }
        }

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

        if (configStore.isRouterVpnEnabled) {
            verifyRouterVpn()
        }

        // Prompt to enable accessibility service on first launch
        if (!isAccessibilityServiceEnabled()) {
            showEnableServiceDialog()
        }
    }

    override fun onResume() {
        super.onResume()

        val vpnActive = isVpnEffectivelyActive()
        updateVpnStatus(vpnActive)
        updateServiceStatus()
        loadApps()

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

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        )
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }
    }

    private fun updateServiceStatus() {
        if (isAccessibilityServiceEnabled()) {
            tvServiceStatus.text = getString(R.string.service_enabled)
            tvServiceStatus.setTextColor(getColor(R.color.vpn_connected))
        } else {
            tvServiceStatus.text = getString(R.string.service_disabled)
            tvServiceStatus.setTextColor(getColor(R.color.vpn_disconnected))
        }
    }

    private fun showEnableServiceDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.enable_service_title)
            .setMessage(R.string.enable_service_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

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
                if (configStore.isRouterVpnEnabled) {
                    updateVpnStatus(true)
                }
            }
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
