package com.vpnlauncher

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppGridAdapter(
    private var apps: MutableList<AppInfo>,
    private val configStore: AppConfigStore,
    private val onAppClicked: (AppInfo) -> Unit,
    private val onAppLongClicked: (Int) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.ViewHolder>() {

    var editModePosition: Int = -1
        set(value) {
            val old = field
            field = value
            if (old >= 0 && old < apps.size) notifyItemChanged(old)
            if (value >= 0 && value < apps.size) notifyItemChanged(value)
        }

    val isInEditMode: Boolean get() = editModePosition >= 0

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appCell: View = view.findViewById(R.id.appCell)
        val ivAppIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val ivShield: ImageView = view.findViewById(R.id.ivShield)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        val isVpnRequired = configStore.isVpnRequired(app.packageName)

        holder.ivAppIcon.setImageDrawable(app.icon)
        holder.tvAppName.text = app.label
        holder.ivShield.visibility = if (isVpnRequired) View.VISIBLE else View.GONE

        // Edit mode visual indicator
        if (position == editModePosition) {
            holder.appCell.alpha = 0.6f
            holder.appCell.scaleX = 1.1f
            holder.appCell.scaleY = 1.1f
        } else {
            holder.appCell.alpha = 1.0f
            holder.appCell.scaleX = 1.0f
            holder.appCell.scaleY = 1.0f
        }

        holder.appCell.setOnClickListener {
            if (isInEditMode) {
                // Drop the app in edit mode
                editModePosition = -1
            } else {
                onAppClicked(app)
            }
        }

        holder.appCell.setOnLongClickListener {
            onAppLongClicked(holder.adapterPosition)
            true
        }

        holder.appCell.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_UP) return@setOnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (isInEditMode) {
                        editModePosition = -1
                    } else {
                        onAppClicked(app)
                    }
                    true
                }
                KeyEvent.KEYCODE_MENU -> {
                    // Long-press alternative: menu button toggles VPN requirement
                    val newState = !configStore.isVpnRequired(app.packageName)
                    configStore.setVpnRequired(app.packageName, newState)
                    notifyItemChanged(holder.adapterPosition)
                    true
                }
                else -> false
            }
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps.toMutableList()
        notifyDataSetChanged()
    }

    fun moveApp(from: Int, to: Int) {
        if (from < 0 || from >= apps.size || to < 0 || to >= apps.size) return
        val app = apps.removeAt(from)
        apps.add(to, app)
        notifyItemMoved(from, to)
        editModePosition = to
    }

    fun getAppOrder(): List<String> = apps.map { it.packageName }
}
