package com.vpnlauncher

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen blocking activity shown when a VPN-required app is launched
 * without an active VPN. Covers the blocked app and presents options.
 */
class BlockedActivity : AppCompatActivity() {

    private lateinit var vpnChecker: VpnChecker
    private var blockedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked)

        vpnChecker = VpnChecker(this)
        blockedPackage = intent.getStringExtra("blocked_package")

        showBlockedDialog()
    }

    override fun onResume() {
        super.onResume()

        // If VPN is now active, launch the blocked app
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
        val nordvpnIntent = packageManager.getLaunchIntentForPackage("com.nordvpn.android")

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
            .setNegativeButton(R.string.go_back) { _, _ ->
                finish()
            }

        if (nordvpnIntent != null) {
            builder.setPositiveButton(R.string.open_nordvpn) { _, _ ->
                startActivity(nordvpnIntent)
                // Don't finish — onResume will auto-launch the app if VPN connects
            }
        } else {
            builder.setPositiveButton(R.string.vpn_app_not_found, null)
            builder.setOnShowListener { dialog ->
                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
            }
        }

        builder.show()
    }

    override fun onBackPressed() {
        // Go back to home instead of the blocked app
        super.onBackPressed()
        finish()
    }
}
