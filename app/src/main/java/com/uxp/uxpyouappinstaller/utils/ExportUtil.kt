package com.uxp.uxpyouappinstaller.utils

import android.content.Context
import com.uxp.uxpyouappinstaller.model.AppInfo
import com.uxp.uxpyouappinstaller.model.SplitInfo
import com.uxp.uxpyouappinstaller.model.XapkInfo
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ExportUtil {

    suspend fun exportAsApk(context: Context, appInfo: AppInfo): File? = withContext(Dispatchers.IO) {
        try {
            val exportDir = FileUtil.getExportDirectory()
            val fileName = "${FileUtil.getSafeFileName(appInfo.appName)}_${appInfo.versionName}.apk"
            val destFile = File(exportDir, fileName)

            val sourceFile = File(appInfo.sourceDir)
            if (FileUtil.copyFile(sourceFile, destFile)) destFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportAsXapk(context: Context, appInfo: AppInfo): File? = withContext(Dispatchers.IO) {
        try {
            val exportDir = FileUtil.getExportDirectory()
            val fileName = "${FileUtil.getSafeFileName(appInfo.appName)}_${appInfo.versionName}.xapk"
            val xapkFile = File(exportDir, fileName)

            val tempDir = File(context.cacheDir, "export_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val baseApk = File(tempDir, "base.apk")
            FileUtil.copyFile(File(appInfo.sourceDir), baseApk)

            val filesToZip = mutableListOf<File>(baseApk)
            val splits = mutableListOf<SplitInfo>()

            appInfo.splitApks.forEachIndexed { index, splitPath ->
                val splitFile = File(tempDir, "split_$index.apk")
                if (FileUtil.copyFile(File(splitPath), splitFile)) {
                    filesToZip.add(splitFile)
                    splits.add(SplitInfo("split_$index", splitFile.name))
                }
            }

            val xapkInfo = XapkInfo(
                packageName = appInfo.packageName,
                versionCode = appInfo.versionCode,
                versionName = appInfo.versionName,
                minSdkVersion = appInfo.minSdk,
                targetSdkVersion = appInfo.targetSdk,
                splits = splits
            )

            val manifestFile = File(tempDir, "manifest.json")
            manifestFile.writeText(GsonBuilder().setPrettyPrinting().create().toJson(xapkInfo))
            filesToZip.add(manifestFile)

            val success = FileUtil.createZip(filesToZip, xapkFile)
            FileUtil.deleteRecursive(tempDir)

            if (success) xapkFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportAsSplitApks(context: Context, appInfo: AppInfo): File? = withContext(Dispatchers.IO) {
        try {
            val exportDir = FileUtil.getExportDirectory()
            val fileName = "${FileUtil.getSafeFileName(appInfo.appName)}_${appInfo.versionName}.apks"
            val apksFile = File(exportDir, fileName)

            val tempDir = File(context.cacheDir, "export_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val baseApk = File(tempDir, "base.apk")
            FileUtil.copyFile(File(appInfo.sourceDir), baseApk)

            val filesToZip = mutableListOf<File>(baseApk)

            appInfo.splitApks.forEachIndexed { index, splitPath ->
                val splitFile = File(tempDir, "split_config_$index.apk")
                if (FileUtil.copyFile(File(splitPath), splitFile)) {
                    filesToZip.add(splitFile)
                }
            }

            val success = FileUtil.createZip(filesToZip, apksFile)
            FileUtil.deleteRecursive(tempDir)

            if (success) apksFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportAsApkm(context: Context, appInfo: AppInfo): File? = withContext(Dispatchers.IO) {
        exportAsSplitApks(context, appInfo)?.let { apksFile ->
            val apkmFile = File(apksFile.parentFile, apksFile.nameWithoutExtension + ".apkm")
            if (apksFile.renameTo(apkmFile)) apkmFile else apksFile
        }
    }
}