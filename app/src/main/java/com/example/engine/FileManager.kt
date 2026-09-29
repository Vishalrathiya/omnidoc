package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.model.PickedFile
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

object FileManager {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val df = DecimalFormat("#,##0.#")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    fun copyUriToLocalCache(context: Context, uri: Uri): PickedFile? {
        return try {
            val contentResolver = context.contentResolver
            var displayName = "file_${System.currentTimeMillis()}"
            var size = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }

            val mimeType = contentResolver.getType(uri) ?: when {
                displayName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                displayName.endsWith(".jpg", ignoreCase = true) || displayName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                displayName.endsWith(".png", ignoreCase = true) -> "image/png"
                displayName.endsWith(".webp", ignoreCase = true) -> "image/webp"
                displayName.endsWith(".zip", ignoreCase = true) -> "application/zip"
                else -> "application/octet-stream"
            }

            val cacheDir = File(context.cacheDir, "picked_inputs")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val tempFile = File(cacheDir, "${System.currentTimeMillis()}_$displayName")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (size <= 0) size = tempFile.length()

            var thumbnail: Bitmap? = null
            var pageCount: Int? = null
            var width: Int? = null
            var height: Int? = null

            if (mimeType == "application/pdf") {
                try {
                    val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    pageCount = renderer.pageCount
                    if (renderer.pageCount > 0) {
                        val page = renderer.openPage(0)
                        val w = (page.width / 2).coerceAtLeast(120)
                        val h = (page.height / 2).coerceAtLeast(160)
                        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bm.eraseColor(android.graphics.Color.WHITE)
                        page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        thumbnail = bm
                        width = page.width
                        height = page.height
                        page.close()
                    }
                    renderer.close()
                    pfd.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (mimeType.startsWith("image/")) {
                try {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(tempFile.absolutePath, options)
                    width = options.outWidth
                    height = options.outHeight

                    val sampleOptions = BitmapFactory.Options().apply {
                        inSampleSize = calculateInSampleSize(options, 200, 200)
                    }
                    thumbnail = BitmapFactory.decodeFile(tempFile.absolutePath, sampleOptions)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            PickedFile(
                uriString = uri.toString(),
                name = displayName,
                sizeBytes = size,
                mimeType = mimeType,
                localFile = tempFile,
                thumbnail = thumbnail,
                pageCount = pageCount,
                width = width,
                height = height
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun wrapLocalFile(file: File, mimeType: String): PickedFile {
        var thumbnail: Bitmap? = null
        var pageCount: Int? = null
        var width: Int? = null
        var height: Int? = null

        if (mimeType == "application/pdf") {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                pageCount = renderer.pageCount
                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(0)
                    val w = (page.width / 2).coerceAtLeast(120)
                    val h = (page.height / 2).coerceAtLeast(160)
                    val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bm.eraseColor(android.graphics.Color.WHITE)
                    page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    thumbnail = bm
                    width = page.width
                    height = page.height
                    page.close()
                }
                renderer.close()
                pfd.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (mimeType.startsWith("image/")) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                width = options.outWidth
                height = options.outHeight
                val sampleOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(options, 200, 200)
                }
                thumbnail = BitmapFactory.decodeFile(file.absolutePath, sampleOptions)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return PickedFile(
            name = file.name,
            sizeBytes = file.length(),
            mimeType = mimeType,
            localFile = file,
            thumbnail = thumbnail,
            pageCount = pageCount,
            width = width,
            height = height
        )
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun getOutputDirectory(context: Context): File {
        val dir = File(context.filesDir, "OmniDoc_Outputs")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String = "Share File") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveToPublicDownloads(context: Context, file: File, mimeType: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values) ?: return false

                resolver.openOutputStream(itemUri)?.use { out ->
                    file.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                true
            } else {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val dest = File(downloads, file.name)
                file.copyTo(dest, overwrite = true)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
