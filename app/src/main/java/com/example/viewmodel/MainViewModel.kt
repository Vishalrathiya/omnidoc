package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.*
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class AppScreen {
    HOME,
    TOOL_DETAIL,
    RECENTS,
    PRIVACY
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedTool = MutableStateFlow<ToolDefinition?>(null)
    val selectedTool: StateFlow<ToolDefinition?> = _selectedTool.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(ToolCategory.ALL)
    val selectedCategory: StateFlow<ToolCategory> = _selectedCategory.asStateFlow()

    // Selected files for active tool
    private val _selectedFiles = MutableStateFlow<List<PickedFile>>(emptyList())
    val selectedFiles: StateFlow<List<PickedFile>> = _selectedFiles.asStateFlow()

    // Interactive page items for reordering/rotating PDF
    private val _pageItems = MutableStateFlow<List<PageItem>>(emptyList())
    val pageItems: StateFlow<List<PageItem>> = _pageItems.asStateFlow()

    // Tool Execution State
    private val _executionState = MutableStateFlow<ToolExecutionState>(ToolExecutionState.Idle)
    val executionState: StateFlow<ToolExecutionState> = _executionState.asStateFlow()

    // Recent items
    private val _recentItems = MutableStateFlow<List<RecentItem>>(emptyList())
    val recentItems: StateFlow<List<RecentItem>> = _recentItems.asStateFlow()

    // Tool Specific Settings States
    val splitPageRange = MutableStateFlow("1-2")
    val pdfCompressLevel = MutableStateFlow("Medium")
    val targetImageFormat = MutableStateFlow("PNG")
    val imageQuality = MutableStateFlow(85)
    val watermarkText = MutableStateFlow("CONFIDENTIAL")
    val watermarkOpacity = MutableStateFlow(30)
    val watermarkAngle = MutableStateFlow(45f)
    val pageNumberPosition = MutableStateFlow("Bottom Center")
    val pageNumberFormat = MutableStateFlow("Page {n} of {total}")
    val annotationText = MutableStateFlow("REVIEWED & APPROVED")
    val annotationPosition = MutableStateFlow("Top Header Banner")

    // Photo Sheet Maker Settings
    val photoSheetPaperSize = MutableStateFlow("A4")
    val photoSheetOrientation = MutableStateFlow("Portrait")
    val photoSheetColumns = MutableStateFlow(2)
    val photoSheetMargin = MutableStateFlow(36f)
    val photoSheetSpacing = MutableStateFlow(16f)
    val photoSheetCaptions = MutableStateFlow(true)
    val photoSheetCaptionType = MutableStateFlow("Numbered")
    val photoSheetCustomLabel = MutableStateFlow("Item")
    val photoSheetExportFormat = MutableStateFlow("PDF") // "PDF" or "IMAGE"

    // Image Resize / Crop
    val resizeWidth = MutableStateFlow(1080)
    val resizeHeight = MutableStateFlow(1080)
    val cropRatio = MutableStateFlow("Free")
    val compressQuality = MutableStateFlow(65)
    val rotateAngle = MutableStateFlow(90f)
    val flipH = MutableStateFlow(false)
    val flipV = MutableStateFlow(false)

    // Document Creator
    val docTitle = MutableStateFlow("Executive Meeting Summary")
    val docSubtitle = MutableStateFlow("Quarterly Strategy Review & Milestones")
    val docBody = MutableStateFlow("1. Project Goals\nAll critical deliverables were completed within schedule. Processing performance metrics exceeded expectations with zero network latency.\n\n2. Security & Compliance\nAll document operations take place on-device in a sandboxed runtime. No user data leaves the client device.\n\n3. Action Items\nContinue optimizing bitmap buffers and memory overhead for large document batches.")

    // ZIP & Renamer
    val zipArchiveName = MutableStateFlow("OmniDoc_Archive")
    val renamePrefix = MutableStateFlow("Doc_")
    val renameSuffix = MutableStateFlow("")
    val renameNumbering = MutableStateFlow(true)
    val renameFindText = MutableStateFlow("")
    val renameReplaceText = MutableStateFlow("")

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectTool(tool: ToolDefinition) {
        _selectedTool.value = tool
        _selectedFiles.value = emptyList()
        _pageItems.value = emptyList()
        _executionState.value = ToolExecutionState.Idle
        _currentScreen.value = AppScreen.TOOL_DETAIL
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: ToolCategory) {
        _selectedCategory.value = category
    }

    fun addPickedUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = _selectedFiles.value.toMutableList()
            for (uri in uris) {
                val picked = FileManager.copyUriToLocalCache(context, uri)
                if (picked != null) {
                    list.add(picked)
                }
            }
            _selectedFiles.value = list

            // If active tool is PDF Reorder/Rotate and we have 1 PDF, load page items
            if (_selectedTool.value?.id == ToolId.PDF_REORDER_ROTATE && list.isNotEmpty()) {
                loadPagesForPdf(list.first().localFile)
            }

            // If active tool is Resize, prepopulate dimensions
            if (_selectedTool.value?.id == ToolId.IMG_RESIZE_CROP && list.isNotEmpty()) {
                val first = list.first()
                if (first.width != null && first.height != null) {
                    resizeWidth.value = first.width
                    resizeHeight.value = first.height
                }
            }
        }
    }

    fun loadSampleFilesForCurrentTool() {
        viewModelScope.launch(Dispatchers.IO) {
            val tool = _selectedTool.value ?: return@launch
            val list = mutableListOf<PickedFile>()

            when (tool.category) {
                ToolCategory.PDF -> {
                    if (tool.id == ToolId.PDF_MERGE) {
                        // Needs 2+ PDFs
                        val p1 = SampleFilesProvider.getSamplePdf(context, "Sample_Report_Part1.pdf")
                        val p2 = SampleFilesProvider.getSamplePdf(context, "Sample_Report_Part2.pdf")
                        list.add(FileManager.wrapLocalFile(p1, "application/pdf"))
                        list.add(FileManager.wrapLocalFile(p2, "application/pdf"))
                    } else if (tool.id == ToolId.IMG_TO_PDF) {
                        val images = SampleFilesProvider.getSampleImages(context)
                        images.forEach { list.add(FileManager.wrapLocalFile(it, "image/jpeg")) }
                    } else {
                        val samplePdf = SampleFilesProvider.getSamplePdf(context)
                        list.add(FileManager.wrapLocalFile(samplePdf, "application/pdf"))
                    }
                }
                ToolCategory.IMAGE -> {
                    val images = SampleFilesProvider.getSampleImages(context)
                    images.forEach { list.add(FileManager.wrapLocalFile(it, "image/jpeg")) }
                }
                ToolCategory.UTILITY -> {
                    if (tool.id == ToolId.ZIP_EXTRACT) {
                        val zip = SampleFilesProvider.getSampleZip(context)
                        list.add(FileManager.wrapLocalFile(zip, "application/zip"))
                    } else {
                        val pdf = SampleFilesProvider.getSamplePdf(context)
                        val images = SampleFilesProvider.getSampleImages(context)
                        list.add(FileManager.wrapLocalFile(pdf, "application/pdf"))
                        images.forEach { list.add(FileManager.wrapLocalFile(it, "image/jpeg")) }
                    }
                }
                else -> {
                    val pdf = SampleFilesProvider.getSamplePdf(context)
                    list.add(FileManager.wrapLocalFile(pdf, "application/pdf"))
                }
            }

            _selectedFiles.value = list

            if (tool.id == ToolId.PDF_REORDER_ROTATE && list.isNotEmpty()) {
                loadPagesForPdf(list.first().localFile)
            }
            if (tool.id == ToolId.IMG_RESIZE_CROP && list.isNotEmpty()) {
                val first = list.first()
                if (first.width != null && first.height != null) {
                    resizeWidth.value = first.width
                    resizeHeight.value = first.height
                }
            }
        }
    }

    private suspend fun loadPagesForPdf(file: File) {
        val items = PdfEngine.getPageItemsForPdf(file)
        _pageItems.value = items
    }

    fun removeFile(index: Int) {
        val current = _selectedFiles.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _selectedFiles.value = current
            if (current.isEmpty()) {
                _pageItems.value = emptyList()
            }
        }
    }

    fun moveFileUp(index: Int) {
        if (index > 0) {
            val list = _selectedFiles.value.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _selectedFiles.value = list
        }
    }

    fun moveFileDown(index: Int) {
        val list = _selectedFiles.value.toMutableList()
        if (index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _selectedFiles.value = list
        }
    }

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
        _pageItems.value = emptyList()
        _executionState.value = ToolExecutionState.Idle
    }

    // Page items manipulation
    fun rotatePage(index: Int) {
        val list = _pageItems.value.toMutableList()
        if (index in list.indices) {
            val item = list[index]
            list[index] = item.copy(rotationDegrees = (item.rotationDegrees + 90) % 360)
            _pageItems.value = list
        }
    }

    fun movePageLeft(index: Int) {
        if (index > 0) {
            val list = _pageItems.value.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _pageItems.value = list
        }
    }

    fun movePageRight(index: Int) {
        val list = _pageItems.value.toMutableList()
        if (index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _pageItems.value = list
        }
    }

    fun deletePage(index: Int) {
        val list = _pageItems.value.toMutableList()
        if (list.size > 1 && index in list.indices) {
            list.removeAt(index)
            _pageItems.value = list
        }
    }

    // Execution dispatcher
    fun executeCurrentTool() {
        val tool = _selectedTool.value ?: return
        val files = _selectedFiles.value

        // Validation
        if (files.size < tool.minFiles) {
            _executionState.value = ToolExecutionState.Error("Please select at least ${tool.minFiles} file(s) to continue.")
            return
        }

        _executionState.value = ToolExecutionState.Processing(0f, "Preparing processing engine...")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val totalOriginalSize = files.sumOf { it.sizeBytes }

                val outputFile: File
                var mimeType = "application/pdf"
                var resultPageCount: Int? = null
                var previewBm: Bitmap? = null

                when (tool.id) {
                    ToolId.PDF_MERGE -> {
                        outputFile = PdfEngine.mergePdfs(context, files) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        resultPageCount = getPdfPageCount(outputFile)
                        previewBm = getPdfFirstPagePreview(outputFile)
                    }
                    ToolId.PDF_SPLIT -> {
                        outputFile = PdfEngine.splitPdf(context, files.first(), splitPageRange.value) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        resultPageCount = getPdfPageCount(outputFile)
                        previewBm = getPdfFirstPagePreview(outputFile)
                    }
                    ToolId.PDF_REORDER_ROTATE -> {
                        outputFile = PdfEngine.reorderRotateAndSavePdf(context, files.first(), _pageItems.value) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        resultPageCount = _pageItems.value.size
                        previewBm = getPdfFirstPagePreview(outputFile)
                    }
                    ToolId.PDF_COMPRESS -> {
                        outputFile = PdfEngine.compressPdf(context, files.first(), pdfCompressLevel.value) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        resultPageCount = getPdfPageCount(outputFile)
                        previewBm = getPdfFirstPagePreview(outputFile)
                    }
                    ToolId.IMG_TO_PDF -> {
                        outputFile = PdfEngine.imagesToPdf(context, files) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        resultPageCount = files.size
                        previewBm = getPdfFirstPagePreview(outputFile)
                    }
                    ToolId.PDF_TO_IMG -> {
                        outputFile = PdfEngine.pdfToImages(context, files.first(), targetImageFormat.value) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        mimeType = if (outputFile.name.endsWith(".zip")) "application/zip" else "image/png"
                        if (mimeType != "application/zip") {
                            previewBm = BitmapFactory.decodeFile(outputFile.absolutePath)
                        }
                    }
                    ToolId.PDF_PAGE_NUMBERS -> {
                        outputFile = PdfEngine.addPageNumbers(
                            context,
                            files.first(),
                            position = pageNumberPosition.value,
                            formatPattern = pageNumberFormat.value
                        ) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        resultPageCount = getPdfPageCount(outputFile)
                        previewBm = getPdfFirstPagePreview(outputFile)
                    }
                    ToolId.PDF_WATERMARK -> {
                        outputFile = PdfEngine.addWatermark(
                            context,
                            files.first(),
                            watermarkText = watermarkText.value,
                            opacityPercent = watermarkOpacity.value,
                            rotationDeg = watermarkAngle.value
                        ) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        resultPageCount = getPdfPageCount(outputFile)
                        previewBm = getPdfFirstPagePreview(outputFile)
                    }
                    ToolId.PDF_TEXT_NOTES -> {
                        outputFile = PdfEngine.addHeaderAnnotation(
                            context,
                            files.first(),
                            annotationText = annotationText.value,
                            position = annotationPosition.value
                        ) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        resultPageCount = getPdfPageCount(outputFile)
                        previewBm = getPdfFirstPagePreview(outputFile)
                    }
                    ToolId.PDF_DOC_CREATOR -> {
                        val photo = files.firstOrNull()?.localFile
                        outputFile = PdfEngine.createPrintableDoc(
                            context,
                            title = docTitle.value,
                            subtitle = docSubtitle.value,
                            bodyText = docBody.value,
                            optionalPhoto = photo
                        ) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        resultPageCount = 1
                        previewBm = getPdfFirstPagePreview(outputFile)
                    }
                    ToolId.PHOTO_SHEET -> {
                        outputFile = ImageEngine.createPhotoSheet(
                            context,
                            images = files,
                            paperSize = photoSheetPaperSize.value,
                            orientation = photoSheetOrientation.value,
                            columns = photoSheetColumns.value,
                            marginPt = photoSheetMargin.value,
                            spacingPt = photoSheetSpacing.value,
                            showCaptions = photoSheetCaptions.value,
                            captionType = photoSheetCaptionType.value,
                            customLabel = photoSheetCustomLabel.value,
                            exportFormat = photoSheetExportFormat.value
                        ) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        mimeType = if (photoSheetExportFormat.value == "PDF") "application/pdf" else "image/jpeg"
                        if (mimeType == "application/pdf") {
                            previewBm = getPdfFirstPagePreview(outputFile)
                        } else {
                            previewBm = BitmapFactory.decodeFile(outputFile.absolutePath)
                        }
                    }
                    ToolId.IMG_CONVERT -> {
                        outputFile = ImageEngine.convertImageFormat(
                            context,
                            files.first(),
                            targetImageFormat.value,
                            imageQuality.value
                        ) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        mimeType = when (targetImageFormat.value) {
                            "PNG" -> "image/png"
                            "WEBP" -> "image/webp"
                            else -> "image/jpeg"
                        }
                        previewBm = BitmapFactory.decodeFile(outputFile.absolutePath)
                    }
                    ToolId.IMG_RESIZE_CROP -> {
                        outputFile = ImageEngine.resizeAndCropImage(
                            context,
                            files.first(),
                            resizeWidth.value,
                            resizeHeight.value,
                            cropRatio.value
                        ) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        mimeType = "image/jpeg"
                        previewBm = BitmapFactory.decodeFile(outputFile.absolutePath)
                    }
                    ToolId.IMG_COMPRESS -> {
                        outputFile = ImageEngine.compressImage(
                            context,
                            files.first(),
                            compressQuality.value
                        ) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        mimeType = "image/jpeg"
                        previewBm = BitmapFactory.decodeFile(outputFile.absolutePath)
                    }
                    ToolId.IMG_ROTATE_FLIP -> {
                        outputFile = ImageEngine.rotateAndFlipImage(
                            context,
                            files.first(),
                            rotateAngle.value,
                            flipH.value,
                            flipV.value
                        ) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        mimeType = "image/jpeg"
                        previewBm = BitmapFactory.decodeFile(outputFile.absolutePath)
                    }
                    ToolId.ZIP_CREATE -> {
                        outputFile = ZipAndUtilityEngine.createZip(
                            context,
                            files,
                            zipArchiveName.value
                        ) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        mimeType = "application/zip"
                    }
                    ToolId.ZIP_EXTRACT -> {
                        val extracted = ZipAndUtilityEngine.extractZip(context, files.first()) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        // Package or point to first extracted file
                        outputFile = extracted.firstOrNull() ?: files.first().localFile
                        mimeType = "application/octet-stream"
                    }
                    ToolId.BATCH_RENAME -> {
                        outputFile = ZipAndUtilityEngine.executeBatchRename(
                            context,
                            files,
                            renamePrefix.value,
                            renameSuffix.value,
                            renameNumbering.value,
                            renameFindText.value,
                            renameReplaceText.value
                        ) { p, msg ->
                            _executionState.value = ToolExecutionState.Processing(p, msg)
                        }
                        mimeType = "application/zip"
                    }
                    ToolId.FILE_INSPECTOR -> {
                        outputFile = files.first().localFile
                        mimeType = files.first().mimeType
                        previewBm = files.first().thumbnail
                    }
                    ToolId.BG_REMOVER_INFO -> {
                        throw IllegalStateException("Background AI cutout is an external service feature. See privacy disclosure below.")
                    }
                }

                val toolResult = ToolResult(
                    title = tool.title,
                    file = outputFile,
                    mimeType = mimeType,
                    sizeBytes = outputFile.length(),
                    originalSizeBytes = totalOriginalSize,
                    pageCount = resultPageCount,
                    previewBitmap = previewBm,
                    message = "${tool.title} succeeded!"
                )

                // Add to recent items
                val recents = _recentItems.value.toMutableList()
                recents.add(0, RecentItem(
                    toolTitle = tool.title,
                    fileName = outputFile.name,
                    filePath = outputFile.absolutePath,
                    sizeBytes = outputFile.length(),
                    mimeType = mimeType
                ))
                _recentItems.value = recents

                _executionState.value = ToolExecutionState.Success(toolResult)

            } catch (e: Exception) {
                e.printStackTrace()
                _executionState.value = ToolExecutionState.Error(e.message ?: "An unexpected error occurred during processing.")
            }
        }
    }

    private fun getPdfPageCount(file: File): Int {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            1
        }
    }

    private fun getPdfFirstPagePreview(file: File): Bitmap? {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val w = (page.width / 2).coerceAtLeast(160)
                val h = (page.height / 2).coerceAtLeast(220)
                val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bm.eraseColor(android.graphics.Color.WHITE)
                page.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                bm
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun resetExecutionState() {
        _executionState.value = ToolExecutionState.Idle
    }

    fun deleteRecentItem(item: RecentItem) {
        val list = _recentItems.value.toMutableList()
        list.remove(item)
        _recentItems.value = list
        try {
            File(item.filePath).delete()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun clearAllRecents() {
        _recentItems.value.forEach {
            try { File(it.filePath).delete() } catch (e: Exception) {}
        }
        _recentItems.value = emptyList()
    }
}
