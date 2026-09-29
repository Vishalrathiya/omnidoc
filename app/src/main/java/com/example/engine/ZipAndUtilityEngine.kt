package com.example.engine

import android.content.Context
import com.example.model.PickedFile
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipAndUtilityEngine {

    suspend fun createZip(
        context: Context,
        files: List<PickedFile>,
        archiveName: String = "Archive",
        onProgress: (Float, String) -> Unit
    ): File {
        val outputDir = FileManager.getOutputDirectory(context)
        val safeName = if (archiveName.endsWith(".zip", ignoreCase = true)) archiveName else "$archiveName.zip"
        val outputFile = File(outputDir, "${System.currentTimeMillis()}_$safeName")

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            for ((idx, file) in files.withIndex()) {
                onProgress(idx.toFloat() / files.size, "Compressing ${file.name} (${idx + 1}/${files.size})")

                val entry = ZipEntry(file.name)
                zos.putNextEntry(entry)
                file.localFile.inputStream().use { input ->
                    input.copyTo(zos)
                }
                zos.closeEntry()
            }
        }

        onProgress(1f, "ZIP archive created successfully!")
        return outputFile
    }

    suspend fun extractZip(
        context: Context,
        zipFile: PickedFile,
        onProgress: (Float, String) -> Unit
    ): List<File> {
        val outputDir = File(FileManager.getOutputDirectory(context), "Extracted_${System.currentTimeMillis()}")
        if (!outputDir.exists()) outputDir.mkdirs()

        val extractedFiles = mutableListOf<File>()

        ZipInputStream(zipFile.localFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            var count = 0
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = File(entry.name).name // Flatten to prevent path traversal
                    val outFile = File(outputDir, entryName)
                    onProgress(0.5f, "Extracting $entryName...")
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    extractedFiles.add(outFile)
                    count++
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        onProgress(1f, "Extracted ${extractedFiles.size} files!")
        return extractedFiles
    }

    fun computeRenamePreview(
        files: List<PickedFile>,
        prefix: String,
        suffix: String,
        useNumbering: Boolean,
        findText: String,
        replaceText: String
    ): List<Pair<String, String>> {
        return files.mapIndexed { idx, file ->
            val origName = file.name
            val dotIdx = origName.lastIndexOf('.')
            val baseName = if (dotIdx > 0) origName.substring(0, dotIdx) else origName
            val ext = if (dotIdx > 0) origName.substring(dotIdx) else ""

            var newBase = baseName
            if (findText.isNotEmpty()) {
                newBase = newBase.replace(findText, replaceText)
            }
            if (useNumbering) {
                val numStr = String.format("%03d", idx + 1)
                newBase = "${newBase}_$numStr"
            }
            if (prefix.isNotEmpty()) {
                newBase = "$prefix$newBase"
            }
            if (suffix.isNotEmpty()) {
                newBase = "$newBase$suffix"
            }

            origName to "$newBase$ext"
        }
    }

    suspend fun executeBatchRename(
        context: Context,
        files: List<PickedFile>,
        prefix: String,
        suffix: String,
        useNumbering: Boolean,
        findText: String,
        replaceText: String,
        onProgress: (Float, String) -> Unit
    ): File {
        val pairs = computeRenamePreview(files, prefix, suffix, useNumbering, findText, replaceText)
        val outputDir = File(FileManager.getOutputDirectory(context), "Renamed_${System.currentTimeMillis()}")
        if (!outputDir.exists()) outputDir.mkdirs()

        val renamedFiles = mutableListOf<File>()
        for ((idx, file) in files.withIndex()) {
            onProgress(idx.toFloat() / files.size, "Renaming ${file.name}...")
            val newName = pairs[idx].second
            val target = File(outputDir, newName)
            file.localFile.copyTo(target, overwrite = true)
            renamedFiles.add(target)
        }

        // Package renamed files into a convenient single ZIP for user download/share
        val zipFile = File(FileManager.getOutputDirectory(context), "BatchRenamed_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for (f in renamedFiles) {
                val entry = ZipEntry(f.name)
                zos.putNextEntry(entry)
                f.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        onProgress(1f, "Batch rename complete! Packaged into ZIP.")
        return zipFile
    }
}
