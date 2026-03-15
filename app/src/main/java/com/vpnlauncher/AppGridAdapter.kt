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
    private val onEditModeDrop: () -> Unit,
    private val onToggleHidden: () -> Unit
) : RecyclerView.Adapter<AppGridAdapter.ViewHolder>() {

    companion object {
        const val MOVE_LEFT = 0
        const val MOVE_RIGHT = 1
        const val MOVE_UP = 2
        const val MOVE_DOWN = 3
        private const val TYPE_APP = 0
        private const val TYPE_TOGGLE_HIDDEN = 1
    }

    var editModePosition: Int = -1
        set(value) {
            val old = field
            field = value
            if (old >= 0 && old < itemCount) notifyItemChanged(old)
            if (value >= 0 && value < itemCount) notifyItemChanged(value)
        }

    val isInEditMode: Boolean get() = editModePosition >= 0
    var showingHidden: Boolean = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appCell: View = view.findViewById(R.id.appCell)
        val ivAppIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val ivShield: ImageView = view.findViewById(R.id.ivShield)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == apps.size) TYPE_TOGGLE_HIDDEN else TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Last tile: toggle hidden apps
        if (position == apps.size) {
            bindToggleHiddenTile(holder)
            return
        }

        val app = apps[position]
        val isVpnRequired = configStore.isVpnRequired(app.packageName)
        val isHidden = configStore.isHidden(app.packageName)

        holder.ivAppIcon.setImageDrawable(app.icon)
        holder.tvAppName.text = app.label
        holder.ivShield.visibility = if (isVpnRequired) View.VISIBLE else View.GONE

        // Dim hidden apps when showing them
        holder.appCell.alpha = if (isHidden && showingHidden) 0.4f else 1.0f

        // Edit mode visual indicator
        if (position == editModePosition) {
            holder.appCell.alpha = 0.6f
            holder.appCell.scaleX = 1.1f
            holder.appCell.scaleY = 1.1f
        } else {
            holder.appCell.scaleX = 1.0f
            holder.appCell.scaleY = 1.0f
        }

        holder.appCell.setOnClickListener {
            if (isInEditMode) {
                onEditModeDrop()
            } else {
                onAppClicked(app)
            }
        }

        holder.appCell.setOnLongClickListener {
            onAppLongClicked(holder.adapterPosition)
            true
        }

        holder.appCell.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_MENU -> {
                    // Menu button: cycle through states
                    // No VPN + visible -> VPN required
                    // VPN required -> Hidden
                    // Hidden -> No VPN + visible
                    if (isHidden) {
                        configStore.setHidden(app.packageName, false)
                    } else if (isVpnRequired) {
                        configStore.setVpnRequired(app.packageName, false)
                        configStore.setHidden(app.packageName, true)
                    } else {
                        configStore.setVpnRequired(app.packageName, true)
                    }
                    notifyItemChanged(holder.adapterPosition)
                    true
                }
                else -> {
                    if (isInEditMode && holder.adapterPosition == editModePosition) {
                        handleEditModeKey(keyCode)
                    } else {
                        false
                    }
                }
            }
        }
    }

    private fun bindToggleHiddenTile(holder: ViewHolder) {
        holder.ivAppIcon.setImageResource(
            if (showingHidden) R.drawable.ic_vpn_connected else R.drawable.ic_vpn_disconnected
        )
        holder.tvAppName.text = if (showingHidden) "Hide Hidden" else "Show Hidden"
        holder.ivShield.visibility = View.GONE
        holder.appCell.alpha = 0.7f
        holder.appCell.scaleX = 1.0f
        holder.appCell.scaleY = 1.0f

        holder.appCell.setOnClickListener { onToggleHidden() }
        holder.appCell.setOnLongClickListener { false }
        holder.appCell.setOnKeyListener { _, _, _ -> false }
    }

    private fun handleEditModeKey(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> onEditModeMove(editModePosition, MOVE_LEFT)
            KeyEvent.KEYCODE_DPAD_RIGHT -> onEditModeMove(editModePosition, MOVE_RIGHT)
            KeyEvent.KEYCODE_DPAD_UP -> onEditModeMove(editModePosition, MOVE_UP)
            KeyEvent.KEYCODE_DPAD_DOWN -> onEditModeMove(editModePosition, MOVE_DOWN)
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                onEditModeDrop()
            }
            else -> return false
        }
        return true
    }

    override fun getItemCount(): Int = apps.size + 1  // +1 for toggle tile

    fun getAppCount(): Int = apps.size

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
