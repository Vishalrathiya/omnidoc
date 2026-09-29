package com.example.engine

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.model.PickedFile
import java.io.File
import java.io.FileOutputStream

object ImageEngine {

    suspend fun convertImageFormat(
        context: Context,
        file: PickedFile,
        targetFormat: String, // "JPEG", "PNG", "WEBP"
        quality: Int = 90,
        onProgress: (Float, String) -> Unit
    ): File {
        onProgress(0.2f, "Loading source image...")
        val bitmap = BitmapFactory.decodeFile(file.localFile.absolutePath)
            ?: throw IllegalStateException("Could not decode image file")

        onProgress(0.6f, "Encoding as $targetFormat...")
        val outputDir = FileManager.getOutputDirectory(context)
        val ext = when (targetFormat) {
            "PNG" -> "png"
            "WEBP" -> "webp"
            else -> "jpg"
        }
        val outputFile = File(outputDir, "Converted_${System.currentTimeMillis()}.$ext")

        val format = when (targetFormat) {
            "PNG" -> Bitmap.CompressFormat.PNG
            "WEBP" -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            else -> Bitmap.CompressFormat.JPEG
        }

        FileOutputStream(outputFile).use { out ->
            bitmap.compress(format, quality, out)
        }
        bitmap.recycle()
        onProgress(1f, "Conversion complete!")
        return outputFile
    }

    suspend fun resizeAndCropImage(
        context: Context,
        file: PickedFile,
        targetWidth: Int,
        targetHeight: Int,
        cropRatio: String = "Free", // "Free", "1:1", "4:3", "16:9"
        onProgress: (Float, String) -> Unit
    ): File {
        onProgress(0.2f, "Decoding image...")
        val original = BitmapFactory.decodeFile(file.localFile.absolutePath)
            ?: throw IllegalStateException("Could not load image")

        onProgress(0.5f, "Applying crop & scaling...")
        var working = original

        // Apply crop if requested
        if (cropRatio != "Free") {
            val (aspectW, aspectH) = when (cropRatio) {
                "1:1" -> 1f to 1f
                "4:3" -> 4f to 3f
                "16:9" -> 16f to 9f
                else -> 1f to 1f
            }
            val targetAspect = aspectW / aspectH
            val currentAspect = original.width.toFloat() / original.height.toFloat()

            var cropW = original.width
            var cropH = original.height

            if (currentAspect > targetAspect) {
                cropW = (original.height * targetAspect).toInt()
            } else {
                cropH = (original.width / targetAspect).toInt()
            }

            val cropX = (original.width - cropW) / 2
            val cropY = (original.height - cropH) / 2
            working = Bitmap.createBitmap(original, cropX, cropY, cropW, cropH)
        }

        val finalBm = Bitmap.createScaledBitmap(working, targetWidth.coerceAtLeast(10), targetHeight.coerceAtLeast(10), true)
        if (working != original && working != finalBm) working.recycle()
        if (original != finalBm) original.recycle()

        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Resized_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            finalBm.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        finalBm.recycle()
        onProgress(1f, "Resize complete!")
        return outputFile
    }

    suspend fun compressImage(
        context: Context,
        file: PickedFile,
        qualityPercent: Int, // e.g. 60
        maxDimension: Int = 1920,
        onProgress: (Float, String) -> Unit
    ): File {
        onProgress(0.2f, "Analyzing image dimensions...")
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.localFile.absolutePath, options)

        val origW = options.outWidth
        val origH = options.outHeight

        var sampleSize = 1
        val maxOriginal = maxOf(origW, origH)
        if (maxOriginal > maxDimension && maxDimension > 0) {
            sampleSize = (maxOriginal / maxDimension).coerceAtLeast(1)
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(file.localFile.absolutePath, decodeOptions)
            ?: throw IllegalStateException("Could not decode image")

        onProgress(0.6f, "Optimizing image data...")
        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Compressed_${System.currentTimeMillis()}.jpg")

        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, qualityPercent.coerceIn(10, 100), out)
        }
        bitmap.recycle()
        onProgress(1f, "Image compression finished!")
        return outputFile
    }

    suspend fun rotateAndFlipImage(
        context: Context,
        file: PickedFile,
        rotateDegrees: Float,
        flipHorizontal: Boolean,
        flipVertical: Boolean,
        onProgress: (Float, String) -> Unit
    ): File {
        onProgress(0.3f, "Loading image...")
        val original = BitmapFactory.decodeFile(file.localFile.absolutePath)
            ?: throw IllegalStateException("Could not decode image")

        val matrix = Matrix().apply {
            if (rotateDegrees != 0f) postRotate(rotateDegrees)
            val sx = if (flipHorizontal) -1f else 1f
            val sy = if (flipVertical) -1f else 1f
            if (sx != 1f || sy != 1f) postScale(sx, sy)
        }

        onProgress(0.7f, "Transforming image matrix...")
        val result = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        original.recycle()

        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Transformed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            result.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        result.recycle()
        onProgress(1f, "Transformation saved!")
        return outputFile
    }

    // Photo Sheet / Contact Sheet Maker: Arranges photos on printable page with margins, columns, spacing, captions, export to PDF or high-res image
    suspend fun createPhotoSheet(
        context: Context,
        images: List<PickedFile>,
        paperSize: String = "A4", // "A4", "US Letter"
        orientation: String = "Portrait", // "Portrait", "Landscape"
        columns: Int = 2,
        marginPt: Float = 36f,
        spacingPt: Float = 16f,
        showCaptions: Boolean = true,
        captionType: String = "Filename", // "Filename", "Numbered", "Custom"
        customLabel: String = "",
        exportFormat: String = "PDF", // "PDF" or "IMAGE"
        onProgress: (Float, String) -> Unit
    ): File {
        onProgress(0.1f, "Setting up photo sheet layout...")

        var (paperW, paperH) = when (paperSize) {
            "US Letter" -> 612f to 792f
            else -> 595f to 842f // A4
        }
        if (orientation == "Landscape") {
            val tmp = paperW
            paperW = paperH
            paperH = tmp
        }

        val availableW = paperW - marginPt * 2
        val cols = columns.coerceIn(1, 4)
        val cellW = (availableW - spacingPt * (cols - 1)) / cols

        val captionH = if (showCaptions) 20f else 0f
        val photoCellH = cellW * 0.75f // 4:3 cell aspect ratio
        val totalCellH = photoCellH + captionH

        val rowsPerPage = ((paperH - marginPt * 2 + spacingPt) / (totalCellH + spacingPt)).toInt().coerceAtLeast(1)
        val cellsPerPage = cols * rowsPerPage

        val outputDir = FileManager.getOutputDirectory(context)

        if (exportFormat == "PDF") {
            val outputFile = File(outputDir, "PhotoSheet_${System.currentTimeMillis()}.pdf")
            val pdfDoc = PdfDocument()

            val totalSheets = ((images.size + cellsPerPage - 1) / cellsPerPage).coerceAtLeast(1)

            for (sheetIndex in 0 until totalSheets) {
                onProgress(sheetIndex.toFloat() / totalSheets, "Rendering sheet ${sheetIndex + 1} of $totalSheets")

                val pageInfo = PdfDocument.PageInfo.Builder(paperW.toInt(), paperH.toInt(), sheetIndex + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                canvas.drawColor(Color.WHITE)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(71, 85, 105)
                    textSize = 9f
                    textAlign = Paint.Align.CENTER
                }

                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(226, 232, 240)
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }

                val startIndex = sheetIndex * cellsPerPage
                val endIndex = (startIndex + cellsPerPage).coerceAtMost(images.size)

                for (idx in startIndex until endIndex) {
                    val cellIndex = idx - startIndex
                    val col = cellIndex % cols
                    val row = cellIndex / cols

                    val cellLeft = marginPt + col * (cellW + spacingPt)
                    val cellTop = marginPt + row * (totalCellH + spacingPt)

                    val imgFile = images[idx]
                    val bitmap = BitmapFactory.decodeFile(imgFile.localFile.absolutePath)
                    if (bitmap != null) {
                        // Draw photo inside photoCellH
                        val photoRect = RectF(cellLeft, cellTop, cellLeft + cellW, cellTop + photoCellH)

                        // Center crop fit inside cell
                        val srcAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val dstAspect = photoRect.width() / photoRect.height()

                        val srcRect = if (srcAspect > dstAspect) {
                            val newW = (bitmap.height * dstAspect).toInt()
                            val left = (bitmap.width - newW) / 2
                            Rect(left, 0, left + newW, bitmap.height)
                        } else {
                            val newH = (bitmap.width / dstAspect).toInt()
                            val top = (bitmap.height - newH) / 2
                            Rect(0, top, bitmap.width, top + newH)
                        }

                        canvas.drawBitmap(bitmap, srcRect, photoRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                        canvas.drawRect(photoRect, borderPaint)
                        bitmap.recycle()
                    }

                    // Caption
                    if (showCaptions) {
                        val captionText = when (captionType) {
                            "Numbered" -> "Photo #${idx + 1}"
                            "Custom" -> if (customLabel.isNotEmpty()) "$customLabel #${idx + 1}" else imgFile.name
                            else -> imgFile.name
                        }
                        val capX = cellLeft + cellW / 2f
                        val capY = cellTop + photoCellH + 13f
                        canvas.drawText(captionText, capX, capY, textPaint)
                    }
                }

                pdfDoc.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            onProgress(1f, "Print-ready PDF photo sheet generated!")
            return outputFile
        } else {
            // High-res Image output (Sheet 1)
            val scale = 2f
            val bmW = (paperW * scale).toInt()
            val bmH = (paperH * scale).toInt()
            val sheetBitmap = Bitmap.createBitmap(bmW, bmH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(sheetBitmap)
            canvas.drawColor(Color.WHITE)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 9f * scale
                textAlign = Paint.Align.CENTER
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f * scale
                style = Paint.Style.STROKE
            }

            val endIndex = cellsPerPage.coerceAtMost(images.size)
            for (idx in 0 until endIndex) {
                val col = idx % cols
                val row = idx / cols

                val cellLeft = (marginPt + col * (cellW + spacingPt)) * scale
                val cellTop = (marginPt + row * (totalCellH + spacingPt)) * scale
                val scaledCellW = cellW * scale
                val scaledPhotoCellH = photoCellH * scale

                val imgFile = images[idx]
                val bitmap = BitmapFactory.decodeFile(imgFile.localFile.absolutePath)
                if (bitmap != null) {
                    val photoRect = RectF(cellLeft, cellTop, cellLeft + scaledCellW, cellTop + scaledPhotoCellH)
                    val srcAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val dstAspect = photoRect.width() / photoRect.height()

                    val srcRect = if (srcAspect > dstAspect) {
                        val newW = (bitmap.height * dstAspect).toInt()
                        val left = (bitmap.width - newW) / 2
                        Rect(left, 0, left + newW, bitmap.height)
                    } else {
                        val newH = (bitmap.width / dstAspect).toInt()
                        val top = (bitmap.height - newH) / 2
                        Rect(0, top, bitmap.width, top + newH)
                    }

                    canvas.drawBitmap(bitmap, srcRect, photoRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                    canvas.drawRect(photoRect, borderPaint)
                    bitmap.recycle()
                }

                if (showCaptions) {
                    val captionText = when (captionType) {
                        "Numbered" -> "Photo #${idx + 1}"
                        "Custom" -> if (customLabel.isNotEmpty()) "$customLabel #${idx + 1}" else imgFile.name
                        else -> imgFile.name
                    }
                    val capX = cellLeft + scaledCellW / 2f
                    val capY = cellTop + scaledPhotoCellH + 14f * scale
                    canvas.drawText(captionText, capX, capY, textPaint)
                }
            }

            val outputFile = File(outputDir, "PhotoSheet_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outputFile).use { out ->
                sheetBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            sheetBitmap.recycle()
            onProgress(1f, "High-resolution photo sheet image ready!")
            return outputFile
        }
    }
}
