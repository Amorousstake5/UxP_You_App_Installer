package com.uxp.uxpyouappinstaller.ui

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.uxp.uxpyouappinstaller.databinding.ItemAppBinding
import com.uxp.uxpyouappinstaller.model.AppInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class AppListAdapter(
    private val onAction: (AppInfo, Action) -> Unit
) : ListAdapter<AppInfo, AppListAdapter.ViewHolder>(DiffCallback()) {

    enum class Action {
        EXPORT_APK, EXPORT_XAPK, EXPORT_APKS, EXPORT_APKM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.apply {
                appIcon.setImageDrawable(app.icon)
                appName.text = app.appName
                packageName.text = app.packageName
                versionInfo.text = "${app.versionName} (${app.versionCode})"

                val size = File(app.sourceDir).length()
                appSize.text = Formatter.formatFileSize(root.context, size)

                systemBadge.visibility = if (app.isSystemApp) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                root.setOnClickListener {
                    showExportDialog(app)
                }
            }
        }

        private fun showExportDialog(app: AppInfo) {
            val options = if (app.splitApks.isEmpty()) {
                arrayOf("Export as APK", "Export as XAPK")
            } else {
                arrayOf("Export as APK", "Export as XAPK", "Export as APKS", "Export as APKM")
            }

            MaterialAlertDialogBuilder(binding.root.context)
                .setTitle("Export ${app.appName}")
                .setItems(options) { _, which ->
                    val action = when (which) {
                        0 -> Action.EXPORT_APK
                        1 -> Action.EXPORT_XAPK
                        2 -> Action.EXPORT_APKS
                        3 -> Action.EXPORT_APKM
                        else -> return@setItems
                    }
                    onAction(app, action)
                }
                .show()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(old: AppInfo, new: AppInfo) =
            old.packageName == new.packageName

        override fun areContentsTheSame(old: AppInfo, new: AppInfo) =
            old == new
    }
}