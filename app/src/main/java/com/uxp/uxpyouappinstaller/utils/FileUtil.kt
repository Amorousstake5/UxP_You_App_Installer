package com.uxp.uxpyouappinstaller.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtil {

    fun getExportDirectory(): File {
        val dir = File(Environment.getExternalStorageDirectory(), "ApkInstaller/Exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun copyFile(source: File, dest: File): Boolean {
        return try {
            source.inputStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun copyUriToFile(context: Context, uri: Uri, dest: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun extractZip(zipFile: File, destDir: File): List<File> {
        val extractedFiles = mutableListOf<File>()
        if (!destDir.exists()) destDir.mkdirs()

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                    extractedFiles.add(file)
                }
                entry = zis.nextEntry
            }
        }
        return extractedFiles
    }

    fun createZip(files: List<File>, zipFile: File, basePath: String = ""): Boolean {
        return try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                files.forEach { file ->
                    if (file.exists()) {
                        val entryName = if (basePath.isNotEmpty()) {
                            file.absolutePath.removePrefix(basePath).removePrefix("/")
                        } else {
                            file.name
                        }

                        FileInputStream(file).use { fis ->
                            BufferedInputStream(fis).use { bis ->
                                val entry = ZipEntry(entryName)
                                zos.putNextEntry(entry)
                                bis.copyTo(zos)
                                zos.closeEntry()
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }

    fun getSafeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9.-]"), "_")
    }
}