package com.uxp.uxpyouappinstaller.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.uxp.uxpyouappinstaller.model.XapkInfo
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

object InstallUtil {

    suspend fun installApk(context: Context, apkFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            installPackage(context, uri)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun installXapk(context: Context, xapkFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.cacheDir, "xapk_temp_${System.currentTimeMillis()}")
            val extractedFiles = FileUtil.extractZip(xapkFile, tempDir)

            val manifestFile = extractedFiles.find { it.name == "manifest.json" }
            if (manifestFile == null) {
                FileUtil.deleteRecursive(tempDir)
                return@withContext false
            }

            val xapkInfo = Gson().fromJson(manifestFile.readText(), XapkInfo::class.java)
            val apkFiles = extractedFiles.filter { it.extension == "apk" }.sortedBy {
                if (it.name.contains("base")) 0 else 1
            }

            if (apkFiles.isEmpty()) {
                FileUtil.deleteRecursive(tempDir)
                return@withContext false
            }

            installMultipleApks(context, apkFiles)
            FileUtil.deleteRecursive(tempDir)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun installSplitApks(context: Context, apksFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.cacheDir, "split_temp_${System.currentTimeMillis()}")
            val apkFiles = FileUtil.extractZip(apksFile, tempDir).filter { it.extension == "apk" }

            if (apkFiles.isEmpty()) {
                FileUtil.deleteRecursive(tempDir)
                return@withContext false
            }

            installMultipleApks(context, apkFiles)
            FileUtil.deleteRecursive(tempDir)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun installPackage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    private fun installMultipleApks(context: Context, apkFiles: List<File>) {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        try {
            apkFiles.forEach { apkFile ->
                session.openWrite(apkFile.name, 0, apkFile.length()).use { output ->
                    FileInputStream(apkFile).use { input ->
                        input.copyTo(output)
                    }
                    session.fsync(output)
                }
            }

            val intent = Intent(context, InstallResultReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

            session.commit(pendingIntent.intentSender)
        } catch (e: Exception) {
            session.abandon()
            throw e
        } finally {
            session.close()
        }
    }

    fun getFileType(file: File): FileType {
        return when (file.extension.lowercase()) {
            "apk" -> FileType.APK
            "xapk" -> FileType.XAPK
            "apks", "apkm" -> FileType.SPLIT_APKS
            "aab" -> FileType.AAB
            else -> FileType.UNKNOWN
        }
    }

    enum class FileType {
        APK, XAPK, SPLIT_APKS, AAB, UNKNOWN
    }
}