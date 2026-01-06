package com.uxp.uxpyouappinstaller.ui


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.uxp.uxpyouappinstaller.R
import com.uxp.uxpyouappinstaller.databinding.ActivityMainBinding
import com.uxp.uxpyouappinstaller.utils.PackageManagerUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppListAdapter

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            loadInstalledApps()
        } else {
            showStoragePermissionDialog()
        }
    }

    private val allFilesAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadInstalledApps()
            } else {
                showStoragePermissionDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
        setupFab()

        checkPermissions()
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter { appInfo, action ->
            when (action) {
                AppListAdapter.Action.EXPORT_APK -> exportApp(appInfo, ExportFormat.APK)
                AppListAdapter.Action.EXPORT_XAPK -> exportApp(appInfo, ExportFormat.XAPK)
                AppListAdapter.Action.EXPORT_APKS -> exportApp(appInfo, ExportFormat.APKS)
                AppListAdapter.Action.EXPORT_APKM -> exportApp(appInfo, ExportFormat.APKM)
            }
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/vnd.android.package-archive",
                    "application/octet-stream",
                    "application/zip"
                ))
            }
            startActivityForResult(intent, REQUEST_PICK_FILE)
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showStoragePermissionDialog()
            } else {
                loadInstalledApps()
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    loadInstalledApps()
                }
                else -> {
                    storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun showStoragePermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Storage Permission Required")
            .setMessage("This app needs storage access to read and export apps. Please grant all files access.")
            .setPositiveButton("Grant") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    allFilesAccessLauncher.launch(intent)
                } else {
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadInstalledApps() {
        binding.progressBar.show()
        lifecycleScope.launch {
            try {
                val apps = PackageManagerUtil.getInstalledApps(this@MainActivity)
                adapter.submitList(apps)
                binding.progressBar.hide()
            } catch (e: Exception) {
                binding.progressBar.hide()
                Toast.makeText(this@MainActivity, "Error loading apps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportApp(appInfo: com.uxp.uxpyouappinstaller.model.AppInfo, format: ExportFormat) {
        binding.progressBar.show()
        lifecycleScope.launch {
            try {
                val result = format.export(this@MainActivity, appInfo)
                binding.progressBar.hide()
                if (result != null) {
                    Toast.makeText(
                        this@MainActivity,
                        "Exported to: ${result.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this@MainActivity, "Export failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.hide()
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_credits -> {
                startActivity(Intent(this, CreditsActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                loadInstalledApps()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val intent = Intent(this, InstallActivity::class.java).apply {
                    setData(uri)
                }
                startActivity(intent)
            }
        }
    }

    private enum class ExportFormat {
        APK {
            override suspend fun export(ctx: android.content.Context, app: com.uxp.uxpyouappinstaller.model.AppInfo) =
                com.uxp.uxpyouappinstaller.utils.ExportUtil.exportAsApk(ctx, app)
        },
        XAPK {
            override suspend fun export(ctx: android.content.Context, app: com.uxp.uxpyouappinstaller.model.AppInfo) =
                com.uxp.uxpyouappinstaller.utils.ExportUtil.exportAsXapk(ctx, app)
        },
        APKS {
            override suspend fun export(ctx: android.content.Context, app: com.uxp.uxpyouappinstaller.model.AppInfo) =
                com.uxp.uxpyouappinstaller.utils.ExportUtil.exportAsSplitApks(ctx, app)
        },
        APKM {
            override suspend fun export(ctx: android.content.Context, app: com.uxp.uxpyouappinstaller.model.AppInfo) =
                com.uxp.uxpyouappinstaller.utils.ExportUtil.exportAsApkm(ctx, app)
        };

        abstract suspend fun export(ctx: android.content.Context, app: com.uxp.uxpyouappinstaller.model.AppInfo): java.io.File?
    }

    companion object {
        private const val REQUEST_PICK_FILE = 1001
    }
}