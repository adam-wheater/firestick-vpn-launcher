package com.vpnlauncher

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private var apps: List<AppInfo>,
    private val configStore: AppConfigStore,
    private val onAppClicked: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appLaunchArea: View = view.findViewById(R.id.appLaunchArea)
        val ivAppIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val ivShield: ImageView = view.findViewById(R.id.ivShield)
        val switchVpn: Switch = view.findViewById(R.id.switchVpn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        val isVpnRequired = configStore.isVpnRequired(app.packageName)

        holder.ivAppIcon.setImageDrawable(app.icon)
        holder.tvAppName.text = app.label
        holder.ivShield.visibility = if (isVpnRequired) View.VISIBLE else View.GONE

        holder.switchVpn.setOnCheckedChangeListener(null)
        holder.switchVpn.isChecked = isVpnRequired
        holder.switchVpn.setOnCheckedChangeListener { _, checked ->
            configStore.setVpnRequired(app.packageName, checked)
            holder.ivShield.visibility = if (checked) View.VISIBLE else View.GONE
        }

        // Click listener for touch and standard click events
        holder.appLaunchArea.setOnClickListener {
            onAppClicked(app)
        }

        // Explicit D-pad center/enter key handling for Fire TV remote
        holder.appLaunchArea.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP &&
                (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                onAppClicked(app)
                true
            } else {
                false
            }
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
