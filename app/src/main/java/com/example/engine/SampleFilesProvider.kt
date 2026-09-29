package com.example.engine

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SampleFilesProvider {

    fun getSamplePdf(context: Context, filename: String = "Sample_Report_MultiPage.pdf"): File {
        val sampleDir = File(context.cacheDir, "sample_files")
        if (!sampleDir.exists()) sampleDir.mkdirs()
        val file = File(sampleDir, filename)
        if (file.exists() && file.length() > 0) return file

        val pdfDoc = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 3-page document
        val pages = listOf(
            "Executive Summary & Project Overview" to "OmniDoc Project Proposal: This document outlines the technical architecture and specifications for privacy-first, on-device document and image manipulation tools. All processing takes place locally within the device sandbox without cloud transmission.",
            "Clause 2: Privacy, Security & Data Handling" to "Under Section 4.2 of the data protection guidelines, all temporary files generated during transformation operations are isolated within local application storage. No user metadata, biometric tokens, or document content are stored remotely.",
            "Annexure A: Certified Specifications & Metrics" to "Benchmark Results:\n- PDF Merge Latency: < 450ms\n- Image Resizing Throughput: 60 FPS\n- Color Depth: 24-bit sRGB\n- Vector Page Resolution: 300 DPI Native Rendering."
        )

        for (i in pages.indices) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, i + 1).create() // A4 at 72dpi
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            // Background
            paint.color = Color.WHITE
            canvas.drawRect(0f, 0f, 595f, 842f, paint)

            // Header Banner
            paint.color = Color.rgb(37, 99, 235) // #2563EB
            canvas.drawRect(0f, 0f, 595f, 60f, paint)

            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("OMNIDOC VERIFIED DOCUMENT", 40f, 38f, paint)

            // Page Title
            paint.color = Color.rgb(15, 23, 42) // #0F172A
            paint.textSize = 18f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(pages[i].first, 40f, 110f, paint)

            // Divider
            paint.color = Color.rgb(226, 232, 240)
            paint.strokeWidth = 2f
            canvas.drawLine(40f, 125f, 555f, 125f, paint)

            // Body content
            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            val text = pages[i].second
            var y = 160f
            val words = text.split(" ")
            var line = ""
            for (w in words) {
                if (w.contains("\n")) {
                    val parts = w.split("\n")
                    line += parts[0]
                    canvas.drawText(line, 40f, y, paint)
                    y += 20f
                    line = parts[1] + " "
                    continue
                }
                if (paint.measureText(line + w) < 510f) {
                    line += "$w "
                } else {
                    canvas.drawText(line, 40f, y, paint)
                    y += 20f
                    line = "$w "
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, 40f, y, paint)
            }

            // Decorative sample diagram/box
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(40f, 320f, 555f, 520f, 12f, 12f, paint)
            paint.color = Color.rgb(37, 99, 235)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(40f, 320f, 555f, 520f, 12f, 12f, paint)
            paint.style = Paint.Style.FILL

            paint.textSize = 14f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Sample Document Graphic / Table [Page ${i + 1}]", 60f, 355f, paint)

            // Bar chart illustration
            val chartColors = listOf(Color.rgb(37, 99, 235), Color.rgb(13, 148, 136), Color.rgb(217, 119, 6), Color.rgb(79, 70, 229))
            val heights = listOf(80f, 120f, 60f, 100f)
            for (b in heights.indices) {
                paint.color = chartColors[b]
                canvas.drawRect(80f + b * 110f, 490f - heights[b], 150f + b * 110f, 490f, paint)
            }

            // Footer
            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Generated for Demonstration • Confidential Record", 40f, 800f, paint)
            canvas.drawText("Page ${i + 1} of 3", 500f, 800f, paint)

            pdfDoc.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return file
    }

    fun getSampleImages(context: Context): List<File> {
        val sampleDir = File(context.cacheDir, "sample_files")
        if (!sampleDir.exists()) sampleDir.mkdirs()

        val files = mutableListOf<File>()

        // 1. Landscape Sunset
        val f1 = File(sampleDir, "Sample_Landscape_Sunset.jpg")
        if (!f1.exists() || f1.length() == 0L) {
            val bm = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
            val c = Canvas(bm)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)

            // Sky gradient
            p.shader = LinearGradient(0f, 0f, 0f, 400f, Color.rgb(249, 115, 22), Color.rgb(234, 179, 8), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, 800f, 400f, p)
            p.shader = null

            // Sun
            p.color = Color.WHITE
            c.drawCircle(400f, 220f, 50f, p)

            // Mountains
            p.color = Color.rgb(30, 41, 59)
            val path = Path().apply {
                moveTo(0f, 400f)
                lineTo(180f, 250f)
                lineTo(340f, 380f)
                lineTo(520f, 220f)
                lineTo(800f, 400f)
                close()
            }
            c.drawPath(path, p)

            // Water reflection
            p.shader = LinearGradient(0f, 400f, 0f, 600f, Color.rgb(15, 23, 42), Color.rgb(30, 58, 138), Shader.TileMode.CLAMP)
            c.drawRect(0f, 400f, 800f, 600f, p)
            p.shader = null

            FileOutputStream(f1).use { out ->
                bm.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bm.recycle()
        }
        files.add(f1)

        // 2. Modern Architecture
        val f2 = File(sampleDir, "Sample_Modern_Architecture.png")
        if (!f2.exists() || f2.length() == 0L) {
            val bm = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
            val c = Canvas(bm)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)

            p.color = Color.rgb(241, 245, 249)
            c.drawRect(0f, 0f, 800f, 600f, p)

            // Geometric architectural shapes
            p.color = Color.rgb(13, 148, 136)
            c.drawRect(120f, 150f, 350f, 550f, p)

            p.color = Color.rgb(37, 99, 235)
            c.drawRect(380f, 80f, 680f, 550f, p)

            p.color = Color.rgb(217, 119, 6)
            c.drawCircle(530f, 220f, 70f, p)

            FileOutputStream(f2).use { out ->
                bm.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bm.recycle()
        }
        files.add(f2)

        // 3. Document Scan / Certificate
        val f3 = File(sampleDir, "Sample_Certificate_Scan.jpg")
        if (!f3.exists() || f3.length() == 0L) {
            val bm = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
            val c = Canvas(bm)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)

            p.color = Color.WHITE
            c.drawRect(0f, 0f, 800f, 600f, p)

            // Elegant border
            p.color = Color.rgb(217, 119, 6)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 6f
            c.drawRect(30f, 30f, 770f, 570f, p)
            p.style = Paint.Style.FILL

            p.color = Color.rgb(15, 23, 42)
            p.textSize = 28f
            p.typeface = Typeface.DEFAULT_BOLD
            c.drawText("CERTIFICATE OF COMPLETION", 180f, 140f, p)

            p.color = Color.rgb(71, 85, 105)
            p.textSize = 16f
            p.typeface = Typeface.DEFAULT
            c.drawText("This certificate verifies successful test file synthesis.", 200f, 200f, p)

            FileOutputStream(f3).use { out ->
                bm.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            bm.recycle()
        }
        files.add(f3)

        return files
    }

    fun getSampleZip(context: Context): File {
        val sampleDir = File(context.cacheDir, "sample_files")
        if (!sampleDir.exists()) sampleDir.mkdirs()
        val file = File(sampleDir, "Sample_Archive.zip")
        if (file.exists() && file.length() > 0) return file

        val pdf = getSamplePdf(context)
        val images = getSampleImages(context)

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // Add a readme file
            val readme = "OmniDoc Sample Archive\nContains verified sample PDF and image files for testing extraction and compression."
            val readmeEntry = ZipEntry("README.txt")
            zos.putNextEntry(readmeEntry)
            zos.write(readme.toByteArray())
            zos.closeEntry()

            // Add sample pdf
            val pdfEntry = ZipEntry(pdf.name)
            zos.putNextEntry(pdfEntry)
            pdf.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()

            // Add one image
            if (images.isNotEmpty()) {
                val img = images[0]
                val imgEntry = ZipEntry(img.name)
                zos.putNextEntry(imgEntry)
                img.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return file
    }
}
