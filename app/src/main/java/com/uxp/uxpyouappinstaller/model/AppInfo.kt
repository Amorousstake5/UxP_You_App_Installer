package com.uxp.uxpyouappinstaller.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable?,
    val sourceDir: String,
    val isSystemApp: Boolean,
    val installTime: Long,
    val updateTime: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val splitApks: List<String> = emptyList()
)