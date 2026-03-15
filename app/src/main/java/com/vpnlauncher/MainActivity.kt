package com.vpnlauncher

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var vpnChecker: VpnChecker
    private lateinit var configStore: AppConfigStore
    private lateinit var adapter: AppListAdapter
    private lateinit var tvVpnStatus: TextView
    private lateinit var ivVpnStatus: ImageView
    private lateinit var rvAppList: RecyclerView

    private var pendingLaunchPackage: String? = null

    private val excludedPrefixes = listOf(
        "com.amazon.tv.",
        "com.amazon.device.",
        "com.android.",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vpnChecker = VpnChecker(this)
        configStore = AppConfigStore(this)

        tvVpnStatus = findViewById(R.id.tvVpnStatus)
        ivVpnStatus = findViewById(R.id.ivVpnStatus)
        rvAppList = findViewById(R.id.rvAppList)

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
    }

    override fun onResume() {
        super.onResume()

        val vpnActive = vpnChecker.isVpnActive()
        updateVpnStatus(vpnActive)
        loadApps()

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

        if (vpnChecker.isVpnActive()) {
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
        if (isConnected) {
            tvVpnStatus.text = getString(R.string.vpn_connected)
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
