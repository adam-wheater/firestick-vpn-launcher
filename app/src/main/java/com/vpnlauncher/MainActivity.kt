package com.vpnlauncher

import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var vpnChecker: VpnChecker
    private lateinit var ipVpnChecker: IpVpnChecker
    private lateinit var vpnAppDetector: VpnAppDetector
    private lateinit var configStore: AppConfigStore
    private lateinit var adapter: AppGridAdapter
    private lateinit var tvVpnStatus: TextView
    private lateinit var ivVpnStatus: ImageView
    private lateinit var rvAppGrid: RecyclerView
    private lateinit var tvEditHint: TextView
    private lateinit var btnRouterVpn: TextView

    private var pendingLaunchPackage: String? = null

    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // After the user responds to the "set as home" dialog, refresh UI
        updateSetAsHomeVisibility()
    }

    private val excludedPrefixes = listOf(
        "com.amazon.tv.",
        "com.amazon.device.",
        "com.android.",
    )

    private val excludedPackages = setOf(
        "android",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vpnChecker = VpnChecker(this)
        ipVpnChecker = IpVpnChecker()
        vpnAppDetector = VpnAppDetector(this)
        configStore = AppConfigStore(this)

        tvVpnStatus = findViewById(R.id.tvVpnStatus)
        ivVpnStatus = findViewById(R.id.ivVpnStatus)
        rvAppGrid = findViewById(R.id.rvAppGrid)
        tvEditHint = findViewById(R.id.tvEditHint)
        btnRouterVpn = findViewById(R.id.btnRouterVpn)

        // Quick access: Settings
        findViewById<TextView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }

        // Quick access: Appstore
        findViewById<TextView>(R.id.btnAppStore).setOnClickListener {
            val intent = packageManager.getLaunchIntentForPackage("com.amazon.venezia")
                ?: packageManager.getLaunchIntentForPackage("com.amazon.apps.store")
            intent?.let { startActivity(it) }
        }

        // Quick access: VPN (auto-detect)
        findViewById<TextView>(R.id.btnVpn).setOnClickListener {
            openVpnApp()
        }

        // Quick access: Router VPN toggle
        updateRouterVpnButton()
        btnRouterVpn.setOnClickListener {
            configStore.isRouterVpnEnabled = !configStore.isRouterVpnEnabled
            updateRouterVpnButton()
            if (configStore.isRouterVpnEnabled) {
                verifyRouterVpn()
            }
            refreshVpnHeader()
        }

        // Set as Home button
        findViewById<TextView>(R.id.btnSetHome).setOnClickListener {
            requestHomeRole()
        }
        updateSetAsHomeVisibility()

        // Grid adapter with reorder support
        adapter = AppGridAdapter(
            apps = mutableListOf(),
            configStore = configStore,
            onAppClicked = { app -> onAppClicked(app) },
            onAppLongClicked = { position -> enterEditMode(position) },
            onEditModeMove = { from, direction -> handleEditModeMove(from, direction) },
            onEditModeDrop = { exitEditMode() }
        )

        rvAppGrid.layoutManager = GridLayoutManager(this, 4)
        rvAppGrid.adapter = adapter

        vpnChecker.startMonitoring { _ -> refreshVpnHeader() }

        refreshVpnHeader()
        loadApps()

        if (configStore.isRouterVpnEnabled) {
            verifyRouterVpn()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshVpnHeader()
        updateRouterVpnButton()
        updateSetAsHomeVisibility()
        loadApps()

        val pending = pendingLaunchPackage
        if (pending != null) {
            pendingLaunchPackage = null
            if (vpnChecker.isVpnActive() || configStore.isRouterVpnEnabled) {
                launchApp(pending)
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
        if (adapter.isInEditMode) {
            exitEditMode()
        }
        // Otherwise do nothing — this is the home screen
    }

    // --- Default home launcher ---

    private fun isDefaultHome(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun requestHomeRole() {
        try {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                homeRoleLauncher.launch(intent)
            }
        } catch (e: Exception) {
            // Fallback: open home settings
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (e2: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun updateSetAsHomeVisibility() {
        val btnSetHome = findViewById<TextView>(R.id.btnSetHome)
        if (isDefaultHome()) {
            btnSetHome.visibility = View.GONE
        } else {
            btnSetHome.visibility = View.VISIBLE
        }
    }

    // --- Edit mode (long-press reorder) ---

    private fun enterEditMode(position: Int) {
        adapter.editModePosition = position
        tvEditHint.visibility = View.VISIBLE
    }

    private fun exitEditMode() {
        adapter.editModePosition = -1
        tvEditHint.visibility = View.GONE
        // Save the new order
        configStore.saveAppOrder(adapter.getAppOrder())
    }

    private fun handleEditModeMove(from: Int, direction: Int) {
        val columns = 4
        val newPos = when (direction) {
            AppGridAdapter.MOVE_LEFT -> if (from > 0) from - 1 else from
            AppGridAdapter.MOVE_RIGHT -> if (from < adapter.itemCount - 1) from + 1 else from
            AppGridAdapter.MOVE_UP -> if (from >= columns) from - columns else from
            AppGridAdapter.MOVE_DOWN -> if (from + columns < adapter.itemCount) from + columns else from
            else -> from
        }

        if (newPos != from) {
            adapter.moveApp(from, newPos)
            rvAppGrid.scrollToPosition(newPos)
        }
    }

    // --- VPN logic ---

    private fun isVpnEffectivelyActive(): Boolean {
        if (vpnChecker.isVpnActive()) return true
        if (configStore.isRouterVpnEnabled) return true
        return false
    }

    private fun verifyRouterVpn() {
        ipVpnChecker.checkVpnByIp { isVpn ->
            // Informational only — the toggle is trusted regardless
            refreshVpnHeader()
        }
    }

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

    private fun updateRouterVpnButton() {
        val enabled = configStore.isRouterVpnEnabled
        btnRouterVpn.text = getString(
            if (enabled) R.string.router_vpn_on else R.string.router_vpn_off
        )
        btnRouterVpn.setTextColor(getColor(
            if (enabled) R.color.vpn_connected else R.color.text_secondary
        ))
    }

    // --- App loading with custom order ---

    private fun loadApps() {
        val pm = packageManager
        @Suppress("DEPRECATION")
        val allApps = pm.getInstalledApplications(0)
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

        // Apply saved order
        val savedOrder = configStore.getAppOrder()
        val orderMap = savedOrder.withIndex().associate { (index, pkg) -> pkg to index }
        val sorted = allApps.sortedWith(compareBy(
            { orderMap[it.packageName] ?: Int.MAX_VALUE },
            { it.label.lowercase() }
        ))

        adapter.updateApps(sorted)
    }

    // --- App launch + VPN blocking ---

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
        val vpnApps = vpnAppDetector.getInstalledVpnApps()

        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.vpn_not_connected_title)
            .setMessage(R.string.vpn_not_connected_message)
            .setNegativeButton(R.string.cancel, null)

        when {
            vpnApps.size == 1 -> {
                val vpnApp = vpnApps[0]
                builder.setPositiveButton(getString(R.string.open_vpn, vpnApp.label)) { _, _ ->
                    pendingLaunchPackage = packageName
                    val intent = this.packageManager.getLaunchIntentForPackage(vpnApp.packageName)
                    intent?.let { startActivity(it) }
                }
            }
            vpnApps.size > 1 -> {
                val labels = vpnApps.map { it.label }.toTypedArray()
                builder.setTitle(R.string.choose_vpn)
                builder.setItems(labels) { _, which ->
                    pendingLaunchPackage = packageName
                    val intent = this.packageManager.getLaunchIntentForPackage(vpnApps[which].packageName)
                    intent?.let { startActivity(it) }
                }
            }
            else -> {
                builder.setPositiveButton(R.string.vpn_app_not_found, null)
            }
        }

        val dialog = builder.create()
        dialog.show()

        if (vpnApps.isEmpty()) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        }
    }

    private fun openVpnApp() {
        val vpnApps = vpnAppDetector.getInstalledVpnApps()
        when {
            vpnApps.size == 1 -> {
                val intent = packageManager.getLaunchIntentForPackage(vpnApps[0].packageName)
                intent?.let { startActivity(it) }
            }
            vpnApps.size > 1 -> {
                val labels = vpnApps.map { it.label }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle(R.string.choose_vpn)
                    .setItems(labels) { _, which ->
                        val intent = packageManager.getLaunchIntentForPackage(vpnApps[which].packageName)
                        intent?.let { startActivity(it) }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        startActivity(intent)
    }
}
