package com.uxp.uxpyouappinstaller.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.uxp.uxpyouappinstaller.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PackageManagerUtil {

    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }

        packages.mapNotNull { packageInfo ->
            try {
                val appInfo = packageInfo.applicationInfo
                val isSystemApp = (appInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0

                appInfo?.let {
                    AppInfo(
                        packageName = packageInfo.packageName,
                        appName = appInfo.let { pm.getApplicationLabel(it) }.toString(),
                        versionName = packageInfo.versionName ?: "Unknown",
                        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            packageInfo.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            packageInfo.versionCode.toLong()
                        },
                        icon = appInfo.let { pm.getApplicationIcon(it) },
                        sourceDir = it.sourceDir,
                        isSystemApp = isSystemApp,
                        installTime = packageInfo.firstInstallTime,
                        updateTime = packageInfo.lastUpdateTime,
                        targetSdk = appInfo.targetSdkVersion,
                        minSdk =
                            appInfo.minSdkVersion,
                        splitApks = appInfo.splitSourceDirs?.toList() ?: emptyList()
                    )
                }
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.appName.lowercase() }
    }

    fun getPackageInfo(context: Context, packageName: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
        } catch (_: Exception) {
            null
        }
    }
}