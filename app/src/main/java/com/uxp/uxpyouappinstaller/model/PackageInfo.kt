package com.uxp.uxpyouappinstaller.model

data class XapkInfo(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
    val permissions: List<String> = emptyList(),
    val splits: List<SplitInfo> = emptyList()
)

data class SplitInfo(
    val id: String,
    val file: String
)