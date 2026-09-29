package com.example.model

import android.graphics.Bitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File

enum class ToolCategory(val label: String) {
    ALL("All Tools"),
    PDF("PDF Tools"),
    IMAGE("Image Tools"),
    UTILITY("Utilities")
}

enum class ToolId {
    PDF_MERGE,
    PDF_SPLIT,
    PDF_REORDER_ROTATE,
    PDF_COMPRESS,
    IMG_TO_PDF,
    PDF_TO_IMG,
    PDF_PAGE_NUMBERS,
    PDF_WATERMARK,
    PDF_TEXT_NOTES,
    PDF_DOC_CREATOR,
    IMG_CONVERT,
    IMG_RESIZE_CROP,
    IMG_COMPRESS,
    IMG_ROTATE_FLIP,
    PHOTO_SHEET,
    ZIP_CREATE,
    ZIP_EXTRACT,
    BATCH_RENAME,
    FILE_INSPECTOR,
    BG_REMOVER_INFO
}

data class ToolDefinition(
    val id: ToolId,
    val title: String,
    val category: ToolCategory,
    val description: String,
    val oneLiner: String,
    val icon: ImageVector,
    val acceptedMimeTypes: List<String>,
    val minFiles: Int = 1,
    val maxFiles: Int = 50,
    val isPopular: Boolean = false,
    val badge: String? = null,
    val isSupportedOffline: Boolean = true
)

data class PickedFile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uriString: String? = null,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val localFile: File,
    val thumbnail: Bitmap? = null,
    val pageCount: Int? = null,
    val width: Int? = null,
    val height: Int? = null
)

sealed class ToolExecutionState {
    data object Idle : ToolExecutionState()
    data class Processing(val progress: Float, val statusMessage: String) : ToolExecutionState()
    data class Success(val result: ToolResult) : ToolExecutionState()
    data class Error(val message: String) : ToolExecutionState()
}

data class ToolResult(
    val title: String,
    val file: File,
    val mimeType: String,
    val sizeBytes: Long,
    val originalSizeBytes: Long? = null,
    val pageCount: Int? = null,
    val previewBitmap: Bitmap? = null,
    val message: String = "Processing completed successfully!"
)

data class RecentItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolTitle: String,
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val mimeType: String
)

data class PageItem(
    val pageIndex: Int,
    val originalPageIndex: Int,
    val rotationDegrees: Int = 0,
    val thumbnail: Bitmap? = null
)
