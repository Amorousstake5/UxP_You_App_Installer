package com.uxp.uxpyouappinstaller.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.uxp.uxpyouappinstaller.databinding.ActivityInstallBinding
import com.uxp.uxpyouappinstaller.utils.FileUtil
import com.uxp.uxpyouappinstaller.utils.InstallUtil
import kotlinx.coroutines.launch
import java.io.File

class InstallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val uri = intent.data
        if (uri == null) {
            Toast.makeText(this, "No file provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        processFile(uri)
    }

    private fun processFile(uri: android.net.Uri) {
        binding.progressBar.show()
        binding.statusText.text = "Analyzing file..."

        lifecycleScope.launch {
            try {
                val tempFile = File(cacheDir, "temp_install_${System.currentTimeMillis()}")
                if (!FileUtil.copyUriToFile(this@InstallActivity, uri, tempFile)) {
                    showError("Failed to read file")
                    return@launch
                }

                val fileType = InstallUtil.getFileType(tempFile)
                binding.statusText.text = "Installing ${fileType.name}..."

                val success = when (fileType) {
                    InstallUtil.FileType.APK -> InstallUtil.installApk(this@InstallActivity, tempFile)
                    InstallUtil.FileType.XAPK -> InstallUtil.installXapk(this@InstallActivity, tempFile)
                    InstallUtil.FileType.SPLIT_APKS -> InstallUtil.installSplitApks(this@InstallActivity, tempFile)
                    InstallUtil.FileType.AAB -> {
                        showError("AAB files require conversion to APK/APKS format")
                        false
                    }
                    InstallUtil.FileType.UNKNOWN -> {
                        showError("Unsupported file type")
                        false
                    }
                }

                tempFile.delete()
                binding.progressBar.hide()

                if (success) {
                    binding.statusText.text = "Installation initiated"
                    finish()
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        binding.progressBar.hide()
        binding.statusText.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}