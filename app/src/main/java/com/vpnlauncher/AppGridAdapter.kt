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
    private val onAppLongClicked: (Int) -> Unit,
    private val onEditModeMove: (from: Int, direction: Int) -> Unit,
    private val onEditModeDrop: () -> Unit
) : RecyclerView.Adapter<AppGridAdapter.ViewHolder>() {

    companion object {
        const val MOVE_LEFT = 0
        const val MOVE_RIGHT = 1
        const val MOVE_UP = 2
        const val MOVE_DOWN = 3
    }

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

        // Normal click = launch app (or drop in edit mode)
        holder.appCell.setOnClickListener {
            if (isInEditMode) {
                onEditModeDrop()
            } else {
                onAppClicked(app)
            }
        }

        // Long click = enter edit mode
        holder.appCell.setOnLongClickListener {
            onAppLongClicked(holder.adapterPosition)
            true
        }

        // Key listener: only intercept D-pad in edit mode + menu button always
        holder.appCell.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            // Menu button always toggles VPN requirement
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                val newState = !configStore.isVpnRequired(app.packageName)
                configStore.setVpnRequired(app.packageName, newState)
                notifyItemChanged(holder.adapterPosition)
                return@setOnKeyListener true
            }

            // In edit mode, intercept D-pad to move the app
            if (isInEditMode && holder.adapterPosition == editModePosition) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        onEditModeMove(editModePosition, MOVE_LEFT)
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        onEditModeMove(editModePosition, MOVE_RIGHT)
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        onEditModeMove(editModePosition, MOVE_UP)
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        onEditModeMove(editModePosition, MOVE_DOWN)
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        onEditModeDrop()
                        return@setOnKeyListener true
                    }
                }
            }

            false
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
