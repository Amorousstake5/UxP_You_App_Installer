package com.uxp.uxpyouappinstaller.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.uxp.uxpyouappinstaller.databinding.ActivityCreditsBinding

class CreditsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreditsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreditsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Credits"

        setupCredits()
    }

    private fun setupCredits() {
        binding.creditsContent.text = """
            UxP You! App Installer
            Version 1.0
            
            Developed with ❤️ by Soumyajit Roy
            and Claude Sonnet 4.5
            
            Features:
            • Install APK, XAPK, APKS, APKM files
            • Export installed apps in multiple formats
            • Material Design 3 UI
            • Split APK support
            • System & user apps management
            
            Libraries & Technologies:
            • Kotlin Coroutines
            • AndroidX Material Components
            • Gson for JSON parsing
            • FileProvider for secure file sharing
            
            Permissions:
            • Storage access for reading/writing APK files
            • Package installation for app installation
            • Query all packages for listing installed apps
            
            © 2024 APK Installer Pro
            All rights reserved.
        """.trimIndent()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}