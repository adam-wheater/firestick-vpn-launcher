package com.vpnlauncher

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class BlockedActivity : AppCompatActivity() {

    private lateinit var vpnChecker: VpnChecker
    private lateinit var vpnAppDetector: VpnAppDetector
    private var blockedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked)

        vpnChecker = VpnChecker(this)
        vpnAppDetector = VpnAppDetector(this)
        blockedPackage = intent.getStringExtra("blocked_package")

        showBlockedDialog()
    }

    override fun onResume() {
        super.onResume()
        if (vpnChecker.isVpnActive()) {
            blockedPackage?.let { pkg ->
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    startActivity(intent)
                }
            }
            finish()
        }
    }

    private fun showBlockedDialog() {
        val vpnApps = vpnAppDetector.getInstalledVpnApps()

        val appLabel = blockedPackage?.let { pkg ->
            try {
                @Suppress("DEPRECATION")
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg
            }
        } ?: "This app"

        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.vpn_not_connected_title)
            .setMessage(getString(R.string.vpn_blocked_app_message, appLabel))
            .setCancelable(false)
            .setNegativeButton(R.string.go_back) { _, _ -> finish() }

        when {
            vpnApps.size == 1 -> {
                builder.setPositiveButton(getString(R.string.open_vpn, vpnApps[0].label)) { _, _ ->
                    val intent = packageManager.getLaunchIntentForPackage(vpnApps[0].packageName)
                    intent?.let { startActivity(it) }
                }
            }
            vpnApps.size > 1 -> {
                builder.setPositiveButton(R.string.choose_vpn) { _, _ ->
                    showVpnPicker(vpnApps)
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

    private fun showVpnPicker(vpnApps: List<VpnAppDetector.VpnApp>) {
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

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
