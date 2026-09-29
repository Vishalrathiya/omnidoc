package com.example.engine

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.model.PageItem
import com.example.model.PickedFile
import java.io.File
import java.io.FileOutputStream

object PdfEngine {

    suspend fun mergePdfs(
        context: Context,
        files: List<PickedFile>,
        onProgress: (Float, String) -> Unit
    ): File {
        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Merged_${System.currentTimeMillis()}.pdf")
        val pdfDoc = PdfDocument()

        var totalPages = 0
        // Pre-count total pages for progress
        for (f in files) {
            val pfd = ParcelFileDescriptor.open(f.localFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            totalPages += renderer.pageCount
            renderer.close()
            pfd.close()
        }

        var processedCount = 0
        var currentOutPageIndex = 1

        for ((fileIdx, file) in files.withIndex()) {
            onProgress(
                processedCount.toFloat() / totalPages.coerceAtLeast(1),
                "Processing file ${fileIdx + 1} of ${files.size}: ${file.name}"
            )

            val pfd = ParcelFileDescriptor.open(file.localFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            for (p in 0 until renderer.pageCount) {
                val page = renderer.openPage(p)
                val width = page.width
                val height = page.height

                // Render at 2x density for crisp quality
                val scale = 2f
                val renderWidth = (width * scale).toInt()
                val renderHeight = (height * scale).toInt()
                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                val pageInfo = PdfDocument.PageInfo.Builder(width, height, currentOutPageIndex++).create()
                val newPage = pdfDoc.startPage(pageInfo)
                val canvas = newPage.canvas

                val destRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                pdfDoc.finishPage(newPage)
                bitmap.recycle()
                page.close()

                processedCount++
                onProgress(
                    processedCount.toFloat() / totalPages.coerceAtLeast(1),
                    "Merged page $processedCount of $totalPages"
                )
            }
            renderer.close()
            pfd.close()
        }

        onProgress(0.95f, "Finalizing merged PDF document...")
        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        onProgress(1f, "Merge complete!")
        return outputFile
    }

    suspend fun splitPdf(
        context: Context,
        file: PickedFile,
        pageRangeInput: String, // e.g., "1-3, 5" or "1,2"
        onProgress: (Float, String) -> Unit
    ): File {
        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Split_${System.currentTimeMillis()}.pdf")
        val pdfDoc = PdfDocument()

        val pfd = ParcelFileDescriptor.open(file.localFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val targetPages = parsePageRange(pageRangeInput, pageCount)

        if (targetPages.isEmpty()) {
            renderer.close()
            pfd.close()
            throw IllegalArgumentException("No valid pages selected from range: '$pageRangeInput'. Total pages: $pageCount.")
        }

        var currentOutIndex = 1
        for ((idx, pageIndexZero) in targetPages.withIndex()) {
            onProgress(
                idx.toFloat() / targetPages.size,
                "Extracting page ${pageIndexZero + 1} (${idx + 1}/${targetPages.size})"
            )

            val page = renderer.openPage(pageIndexZero)
            val w = page.width
            val h = page.height

            val scale = 2f
            val bitmap = Bitmap.createBitmap((w * scale).toInt(), (h * scale).toInt(), Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

            val pageInfo = PdfDocument.PageInfo.Builder(w, h, currentOutIndex++).create()
            val newPage = pdfDoc.startPage(pageInfo)
            newPage.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
            pdfDoc.finishPage(newPage)

            bitmap.recycle()
            page.close()
        }

        renderer.close()
        pfd.close()

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        onProgress(1f, "Split finished! Extracted ${targetPages.size} pages.")
        return outputFile
    }

    fun parsePageRange(rangeStr: String, maxPages: Int): List<Int> {
        val result = mutableSetOf<Int>()
        val trimmed = rangeStr.trim()
        if (trimmed.isEmpty() || trimmed.equals("all", ignoreCase = true)) {
            return (0 until maxPages).toList()
        }
        val parts = trimmed.split(",")
        for (part in parts) {
            val p = part.trim()
            if (p.contains("-")) {
                val sub = p.split("-")
                val start = sub[0].trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
                val end = sub[1].trim().toIntOrNull()?.coerceAtMost(maxPages) ?: maxPages
                for (i in start..end) {
                    if (i in 1..maxPages) result.add(i - 1)
                }
            } else {
                val single = p.toIntOrNull()
                if (single != null && single in 1..maxPages) {
                    result.add(single - 1)
                }
            }
        }
        return result.toList().sorted()
    }

    suspend fun getPageItemsForPdf(file: File): List<PageItem> {
        val list = mutableListOf<PageItem>()
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val thumbWidth = (page.width / 4).coerceIn(100, 240)
            val thumbHeight = (page.height / 4).coerceIn(140, 320)
            val bm = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
            bm.eraseColor(Color.WHITE)
            page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            list.add(PageItem(pageIndex = i, originalPageIndex = i, rotationDegrees = 0, thumbnail = bm))
        }
        renderer.close()
        pfd.close()
        return list
    }

    suspend fun reorderRotateAndSavePdf(
        context: Context,
        file: PickedFile,
        pageItems: List<PageItem>,
        onProgress: (Float, String) -> Unit
    ): File {
        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Organized_${System.currentTimeMillis()}.pdf")
        val pdfDoc = PdfDocument()

        val pfd = ParcelFileDescriptor.open(file.localFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)

        for ((newIdx, item) in pageItems.withIndex()) {
            onProgress(newIdx.toFloat() / pageItems.size, "Processing page ${newIdx + 1} of ${pageItems.size}")

            val page = renderer.openPage(item.originalPageIndex)
            val origW = page.width
            val origH = page.height

            val isRotated90 = (item.rotationDegrees % 180 != 0)
            val outW = if (isRotated90) origH else origW
            val outH = if (isRotated90) origW else origH

            val scale = 2f
            val bm = Bitmap.createBitmap((origW * scale).toInt(), (origH * scale).toInt(), Bitmap.Config.ARGB_8888)
            bm.eraseColor(Color.WHITE)
            page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

            val pageInfo = PdfDocument.PageInfo.Builder(outW, outH, newIdx + 1).create()
            val newPage = pdfDoc.startPage(pageInfo)
            val canvas = newPage.canvas

            if (item.rotationDegrees != 0) {
                canvas.save()
                canvas.rotate(item.rotationDegrees.toFloat(), outW / 2f, outH / 2f)
                val left = (outW - origW) / 2f
                val top = (outH - origH) / 2f
                canvas.drawBitmap(bm, null, RectF(left, top, left + origW, top + origH), Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            } else {
                canvas.drawBitmap(bm, null, RectF(0f, 0f, outW.toFloat(), outH.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
            }

            pdfDoc.finishPage(newPage)
            bm.recycle()
            page.close()
        }

        renderer.close()
        pfd.close()

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        onProgress(1f, "Organization saved!")
        return outputFile
    }

    suspend fun compressPdf(
        context: Context,
        file: PickedFile,
        compressionLevel: String, // "High" (Shrink most), "Medium" (Balanced), "Low" (Light)
        onProgress: (Float, String) -> Unit
    ): File {
        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Compressed_${System.currentTimeMillis()}.pdf")
        val pdfDoc = PdfDocument()

        val pfd = ParcelFileDescriptor.open(file.localFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val (scale, jpegQuality) = when (compressionLevel) {
            "High" -> 1.0f to 50
            "Medium" -> 1.3f to 72
            else -> 1.6f to 85
        }

        for (i in 0 until pageCount) {
            onProgress(i.toFloat() / pageCount, "Optimizing page ${i + 1} of $pageCount")
            val page = renderer.openPage(i)
            val w = page.width
            val h = page.height

            val renderW = (w * scale).toInt().coerceAtLeast(100)
            val renderH = (h * scale).toInt().coerceAtLeast(100)
            val bm = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
            bm.eraseColor(Color.WHITE)
            page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            // Compress to JPEG stream to apply actual lossy compression
            val byteOut = java.io.ByteArrayOutputStream()
            bm.compress(Bitmap.CompressFormat.JPEG, jpegQuality, byteOut)
            val compressedBytes = byteOut.toByteArray()
            val compressedBm = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

            val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
            val newPage = pdfDoc.startPage(pageInfo)
            newPage.canvas.drawBitmap(compressedBm ?: bm, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
            pdfDoc.finishPage(newPage)

            bm.recycle()
            compressedBm?.recycle()
            page.close()
        }

        renderer.close()
        pfd.close()

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        onProgress(1f, "PDF compression finished!")
        return outputFile
    }

    suspend fun imagesToPdf(
        context: Context,
        images: List<PickedFile>,
        pageSizeName: String = "A4", // "A4", "Letter", "Fit"
        marginPoints: Int = 20,
        onProgress: (Float, String) -> Unit
    ): File {
        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Images_${System.currentTimeMillis()}.pdf")
        val pdfDoc = PdfDocument()

        val (stdW, stdH) = when (pageSizeName) {
            "Letter" -> 612 to 792
            "Fit" -> 0 to 0
            else -> 595 to 842 // A4
        }

        for ((idx, imgFile) in images.withIndex()) {
            onProgress(idx.toFloat() / images.size, "Adding image ${idx + 1} of ${images.size}")

            val bitmap = BitmapFactory.decodeFile(imgFile.localFile.absolutePath)
                ?: continue

            val pageW = if (pageSizeName == "Fit") bitmap.width else stdW
            val pageH = if (pageSizeName == "Fit") bitmap.height else stdH

            val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, idx + 1).create()
            val newPage = pdfDoc.startPage(pageInfo)
            val canvas = newPage.canvas

            // Background
            canvas.drawColor(Color.WHITE)

            val availW = (pageW - marginPoints * 2).toFloat().coerceAtLeast(10f)
            val availH = (pageH - marginPoints * 2).toFloat().coerceAtLeast(10f)

            // Calculate scaled rect preserving aspect ratio
            val imgAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            val availAspect = availW / availH

            val drawW: Float
            val drawH: Float
            if (imgAspect > availAspect) {
                drawW = availW
                drawH = availW / imgAspect
            } else {
                drawH = availH
                drawW = availH * imgAspect
            }

            val left = marginPoints + (availW - drawW) / 2f
            val top = marginPoints + (availH - drawH) / 2f

            canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            pdfDoc.finishPage(newPage)
            bitmap.recycle()
        }

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        onProgress(1f, "PDF generated from images!")
        return outputFile
    }

    suspend fun pdfToImages(
        context: Context,
        file: PickedFile,
        format: String = "PNG", // "PNG" or "JPG"
        dpi: Int = 150,
        onProgress: (Float, String) -> Unit
    ): File {
        val outputDir = FileManager.getOutputDirectory(context)
        val pfd = ParcelFileDescriptor.open(file.localFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val scale = dpi / 72f
        val compressFormat = if (format == "JPG") Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
        val ext = if (format == "JPG") "jpg" else "png"

        if (pageCount == 1) {
            onProgress(0.5f, "Rendering single page...")
            val page = renderer.openPage(0)
            val bm = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
            bm.eraseColor(Color.WHITE)
            page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

            val outputFile = File(outputDir, "Page_1_${System.currentTimeMillis()}.$ext")
            FileOutputStream(outputFile).use { out ->
                bm.compress(compressFormat, 92, out)
            }
            bm.recycle()
            page.close()
            renderer.close()
            pfd.close()
            onProgress(1f, "Image saved!")
            return outputFile
        } else {
            // Multiple pages: package into a ZIP
            val zipFile = File(outputDir, "PDF_Pages_${System.currentTimeMillis()}.zip")
            java.util.zip.ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for (i in 0 until pageCount) {
                    onProgress(i.toFloat() / pageCount, "Rendering page ${i + 1} of $pageCount")
                    val page = renderer.openPage(i)
                    val bm = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                    bm.eraseColor(Color.WHITE)
                    page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                    val entry = java.util.zip.ZipEntry("Page_${i + 1}.$ext")
                    zos.putNextEntry(entry)
                    bm.compress(compressFormat, 90, zos)
                    zos.closeEntry()

                    bm.recycle()
                    page.close()
                }
            }
            renderer.close()
            pfd.close()
            onProgress(1f, "All $pageCount pages converted to ZIP archive!")
            return zipFile
        }
    }

    suspend fun addPageNumbers(
        context: Context,
        file: PickedFile,
        position: String = "Bottom Center", // "Bottom Center", "Bottom Right", "Top Center"
        formatPattern: String = "Page {n} of {total}",
        startNumber: Int = 1,
        onProgress: (Float, String) -> Unit
    ): File {
        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Numbered_${System.currentTimeMillis()}.pdf")
        val pdfDoc = PdfDocument()

        val pfd = ParcelFileDescriptor.open(file.localFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 11f
            typeface = Typeface.DEFAULT
        }

        for (i in 0 until pageCount) {
            onProgress(i.toFloat() / pageCount, "Numbering page ${i + 1} of $pageCount")
            val page = renderer.openPage(i)
            val w = page.width
            val h = page.height

            val scale = 2f
            val bm = Bitmap.createBitmap((w * scale).toInt(), (h * scale).toInt(), Bitmap.Config.ARGB_8888)
            bm.eraseColor(Color.WHITE)
            page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

            val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
            val newPage = pdfDoc.startPage(pageInfo)
            val canvas = newPage.canvas

            canvas.drawBitmap(bm, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))

            val currentNum = startNumber + i
            val numberText = formatPattern
                .replace("{n}", currentNum.toString())
                .replace("{total}", (startNumber + pageCount - 1).toString())

            val textW = textPaint.measureText(numberText)
            val (x, y) = when (position) {
                "Bottom Right" -> (w - textW - 36f) to (h - 24f)
                "Top Center" -> ((w - textW) / 2f) to 30f
                "Top Right" -> (w - textW - 36f) to 30f
                else -> ((w - textW) / 2f) to (h - 24f) // Bottom Center
            }

            canvas.drawText(numberText, x, y, textPaint)
            pdfDoc.finishPage(newPage)
            bm.recycle()
            page.close()
        }

        renderer.close()
        pfd.close()

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        onProgress(1f, "Page numbers successfully added!")
        return outputFile
    }

    suspend fun addWatermark(
        context: Context,
        file: PickedFile,
        watermarkText: String,
        opacityPercent: Int = 30,
        rotationDeg: Float = 45f,
        textSizeSp: Float = 44f,
        onProgress: (Float, String) -> Unit
    ): File {
        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Watermarked_${System.currentTimeMillis()}.pdf")
        val pdfDoc = PdfDocument()

        val pfd = ParcelFileDescriptor.open(file.localFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val alphaVal = (opacityPercent * 255 / 100).coerceIn(10, 255)
        val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(alphaVal, 220, 38, 38) // Translucent Red/Crimson
            textSize = textSizeSp
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        for (i in 0 until pageCount) {
            onProgress(i.toFloat() / pageCount, "Applying watermark to page ${i + 1} of $pageCount")
            val page = renderer.openPage(i)
            val w = page.width
            val h = page.height

            val scale = 2f
            val bm = Bitmap.createBitmap((w * scale).toInt(), (h * scale).toInt(), Bitmap.Config.ARGB_8888)
            bm.eraseColor(Color.WHITE)
            page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

            val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
            val newPage = pdfDoc.startPage(pageInfo)
            val canvas = newPage.canvas

            canvas.drawBitmap(bm, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))

            canvas.save()
            canvas.rotate(rotationDeg, w / 2f, h / 2f)
            canvas.drawText(watermarkText, w / 2f, h / 2f, wmPaint)
            canvas.restore()

            pdfDoc.finishPage(newPage)
            bm.recycle()
            page.close()
        }

        renderer.close()
        pfd.close()

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        onProgress(1f, "Watermark added!")
        return outputFile
    }

    suspend fun addHeaderAnnotation(
        context: Context,
        file: PickedFile,
        annotationText: String,
        position: String = "Top Header Banner", // "Top Header Banner", "Bottom Footer Banner"
        onProgress: (Float, String) -> Unit
    ): File {
        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Annotated_${System.currentTimeMillis()}.pdf")
        val pdfDoc = PdfDocument()

        val pfd = ParcelFileDescriptor.open(file.localFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(254, 243, 199) // Warm alert yellow #FEF3C7
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(146, 64, 14)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        for (i in 0 until pageCount) {
            onProgress(i.toFloat() / pageCount, "Annotating page ${i + 1} of $pageCount")
            val page = renderer.openPage(i)
            val w = page.width
            val h = page.height

            val scale = 2f
            val bm = Bitmap.createBitmap((w * scale).toInt(), (h * scale).toInt(), Bitmap.Config.ARGB_8888)
            bm.eraseColor(Color.WHITE)
            page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

            val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
            val newPage = pdfDoc.startPage(pageInfo)
            val canvas = newPage.canvas

            canvas.drawBitmap(bm, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))

            val bannerH = 34f
            val bannerRect = if (position.contains("Top")) {
                RectF(0f, 0f, w.toFloat(), bannerH)
            } else {
                RectF(0f, h - bannerH, w.toFloat(), h.toFloat())
            }

            canvas.drawRect(bannerRect, bannerPaint)
            canvas.drawRect(bannerRect, borderPaint)
            val textY = bannerRect.centerY() + 4f
            canvas.drawText(annotationText, w / 2f, textY, textPaint)

            pdfDoc.finishPage(newPage)
            bm.recycle()
            page.close()
        }

        renderer.close()
        pfd.close()

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        onProgress(1f, "Annotation banner stamped!")
        return outputFile
    }

    suspend fun createPrintableDoc(
        context: Context,
        title: String,
        subtitle: String,
        bodyText: String,
        optionalPhoto: File?,
        onProgress: (Float, String) -> Unit
    ): File {
        val outputDir = FileManager.getOutputDirectory(context)
        val outputFile = File(outputDir, "Document_${System.currentTimeMillis()}.pdf")
        val pdfDoc = PdfDocument()

        onProgress(0.2f, "Formatting printable page...")

        val pageW = 595
        val pageH = 842 // A4 standard
        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Accent top bar
        paint.color = Color.rgb(37, 99, 235)
        canvas.drawRect(0f, 0f, pageW.toFloat(), 12f, paint)

        var currentY = 56f

        // Optional photo banner
        if (optionalPhoto != null && optionalPhoto.exists()) {
            val bm = BitmapFactory.decodeFile(optionalPhoto.absolutePath)
            if (bm != null) {
                val photoH = 140f
                val photoW = pageW - 72f
                val destRect = RectF(36f, currentY, 36f + photoW, currentY + photoH)
                canvas.drawBitmap(bm, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                bm.recycle()
                currentY += photoH + 24f
            }
        }

        // Title
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 24f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(title, 36f, currentY, paint)
        currentY += 24f

        // Subtitle
        if (subtitle.isNotEmpty()) {
            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 14f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(subtitle, 36f, currentY, paint)
            currentY += 20f
        }

        // Divider
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1.5f
        canvas.drawLine(36f, currentY, pageW - 36f, currentY, paint)
        currentY += 24f

        // Body Text wrapping
        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        val maxTextWidth = pageW - 72f

        val paragraphs = bodyText.split("\n")
        for (para in paragraphs) {
            val words = para.split(" ")
            var line = ""
            for (w in words) {
                if (paint.measureText(line + w) < maxTextWidth) {
                    line += "$w "
                } else {
                    canvas.drawText(line, 36f, currentY, paint)
                    currentY += 18f
                    line = "$w "
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, 36f, currentY, paint)
                currentY += 18f
            }
            currentY += 8f
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 10f
        canvas.drawText("Generated with OmniDoc • Printable Document", 36f, pageH - 24f, paint)

        pdfDoc.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        onProgress(1f, "Document generated!")
        return outputFile
    }
}
