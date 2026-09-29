package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

object ToolRegistry {

    val tools: List<ToolDefinition> = listOf(
        // PDF Tools
        ToolDefinition(
            id = ToolId.PDF_MERGE,
            title = "Merge PDFs",
            category = ToolCategory.PDF,
            description = "Combine multiple PDF documents into a single organized file.",
            oneLiner = "Combine two or more PDF files into a single document in any order.",
            icon = Icons.Default.CallMerge,
            acceptedMimeTypes = listOf("application/pdf"),
            minFiles = 2,
            maxFiles = 30,
            isPopular = true,
            badge = "Popular"
        ),
        ToolDefinition(
            id = ToolId.PDF_SPLIT,
            title = "Split PDF",
            category = ToolCategory.PDF,
            description = "Split a PDF by page ranges or extract specific selected pages.",
            oneLiner = "Extract specific pages or page ranges from any PDF file.",
            icon = Icons.Default.CallSplit,
            acceptedMimeTypes = listOf("application/pdf"),
            minFiles = 1,
            maxFiles = 1,
            isPopular = true
        ),
        ToolDefinition(
            id = ToolId.PDF_REORDER_ROTATE,
            title = "Reorder & Rotate Pages",
            category = ToolCategory.PDF,
            description = "Organize PDF pages: reorder sequence, rotate orientation, or delete unwanted pages.",
            oneLiner = "Rearrange, rotate 90°/180°, or delete individual PDF pages with instant previews.",
            icon = Icons.Default.RotateRight,
            acceptedMimeTypes = listOf("application/pdf"),
            minFiles = 1,
            maxFiles = 1,
            isPopular = false
        ),
        ToolDefinition(
            id = ToolId.PDF_COMPRESS,
            title = "Compress PDF",
            category = ToolCategory.PDF,
            description = "Reduce PDF file size for easy emailing and storage while maintaining readability.",
            oneLiner = "Shrink PDF document size with smart resolution scaling and compression.",
            icon = Icons.Default.Compress,
            acceptedMimeTypes = listOf("application/pdf"),
            minFiles = 1,
            maxFiles = 1,
            isPopular = true,
            badge = "Essential"
        ),
        ToolDefinition(
            id = ToolId.IMG_TO_PDF,
            title = "Images to PDF",
            category = ToolCategory.PDF,
            description = "Convert photos and scanned images into a clean, printable PDF document.",
            oneLiner = "Turn one or multiple JPG, PNG, and WebP images into a multi-page PDF.",
            icon = Icons.Default.PictureAsPdf,
            acceptedMimeTypes = listOf("image/*"),
            minFiles = 1,
            maxFiles = 50,
            isPopular = true,
            badge = "Top Tool"
        ),
        ToolDefinition(
            id = ToolId.PDF_TO_IMG,
            title = "PDF to Images",
            category = ToolCategory.PDF,
            description = "Extract pages of a PDF document as high-resolution JPG or PNG images.",
            oneLiner = "Convert each page of your PDF into crisp image files or a packaged ZIP.",
            icon = Icons.Default.Image,
            acceptedMimeTypes = listOf("application/pdf"),
            minFiles = 1,
            maxFiles = 1,
            isPopular = false
        ),
        ToolDefinition(
            id = ToolId.PDF_PAGE_NUMBERS,
            title = "Add Page Numbers",
            category = ToolCategory.PDF,
            description = "Insert clear page numbering at the top or bottom of your PDF pages.",
            oneLiner = "Stamp page numbers (e.g. 'Page 1 of 10') in customizable positions and fonts.",
            icon = Icons.Default.FormatListNumbered,
            acceptedMimeTypes = listOf("application/pdf"),
            minFiles = 1,
            maxFiles = 1,
            isPopular = false
        ),
        ToolDefinition(
            id = ToolId.PDF_WATERMARK,
            title = "Watermark PDF",
            category = ToolCategory.PDF,
            description = "Protect your documents with customized text or logo watermarks.",
            oneLiner = "Add a translucent diagonal or horizontal text watermark across all pages.",
            icon = Icons.Default.BrandingWatermark,
            acceptedMimeTypes = listOf("application/pdf"),
            minFiles = 1,
            maxFiles = 1,
            isPopular = false
        ),
        ToolDefinition(
            id = ToolId.PDF_TEXT_NOTES,
            title = "Add Header & Annotations",
            category = ToolCategory.PDF,
            description = "Add headers, footers, stamps, or annotation banners to PDF pages.",
            oneLiner = "Stamp custom labels, headers, or confidential notices onto your document.",
            icon = Icons.Default.EditNote,
            acceptedMimeTypes = listOf("application/pdf"),
            minFiles = 1,
            maxFiles = 1,
            isPopular = false
        ),
        ToolDefinition(
            id = ToolId.PDF_DOC_CREATOR,
            title = "Printable Doc Creator",
            category = ToolCategory.PDF,
            description = "Quickly assemble text notes, titles, and pictures into a formatted printable document.",
            oneLiner = "Generate clean formatted PDF memos and printable sheets from text and photos.",
            icon = Icons.Default.PostAdd,
            acceptedMimeTypes = listOf("image/*"),
            minFiles = 0,
            maxFiles = 5,
            isPopular = false
        ),

        // Image Tools
        ToolDefinition(
            id = ToolId.PHOTO_SHEET,
            title = "Photo Sheet Maker",
            category = ToolCategory.IMAGE,
            description = "Arrange photos on printable A4 or Letter sheets with columns, margins, and labels.",
            oneLiner = "Create contact sheets and multi-photo grids ready for printing or PDF export.",
            icon = Icons.Default.GridView,
            acceptedMimeTypes = listOf("image/*"),
            minFiles = 1,
            maxFiles = 30,
            isPopular = true,
            badge = "Featured"
        ),
        ToolDefinition(
            id = ToolId.IMG_CONVERT,
            title = "Convert Image Format",
            category = ToolCategory.IMAGE,
            description = "Convert photos smoothly between JPG, PNG, and modern WebP formats.",
            oneLiner = "Switch images between JPG, PNG, and WebP with custom quality control.",
            icon = Icons.Default.Transform,
            acceptedMimeTypes = listOf("image/*"),
            minFiles = 1,
            maxFiles = 10,
            isPopular = true
        ),
        ToolDefinition(
            id = ToolId.IMG_COMPRESS,
            title = "Compress Image",
            category = ToolCategory.IMAGE,
            description = "Reduce image file size while keeping visual quality high.",
            oneLiner = "Shrink image file weight with instant size preview and quality adjustment.",
            icon = Icons.Default.PhotoSizeSelectLarge,
            acceptedMimeTypes = listOf("image/*"),
            minFiles = 1,
            maxFiles = 10,
            isPopular = true
        ),
        ToolDefinition(
            id = ToolId.IMG_RESIZE_CROP,
            title = "Resize & Crop Image",
            category = ToolCategory.IMAGE,
            description = "Scale dimensions by percentage or pixels, and crop to common aspect ratios.",
            oneLiner = "Adjust image dimensions, scale resolution, and apply aspect ratio crops.",
            icon = Icons.Default.Crop,
            acceptedMimeTypes = listOf("image/*"),
            minFiles = 1,
            maxFiles = 1,
            isPopular = false
        ),
        ToolDefinition(
            id = ToolId.IMG_ROTATE_FLIP,
            title = "Rotate & Flip Image",
            category = ToolCategory.IMAGE,
            description = "Rotate photo orientation by 90°, 180°, or mirror horizontally and vertically.",
            oneLiner = "Quickly correct photo orientation and mirror images horizontally or vertically.",
            icon = Icons.Default.Flip,
            acceptedMimeTypes = listOf("image/*"),
            minFiles = 1,
            maxFiles = 5,
            isPopular = false
        ),
        ToolDefinition(
            id = ToolId.BG_REMOVER_INFO,
            title = "Background Remover",
            category = ToolCategory.IMAGE,
            description = "Status and disclosure regarding external AI background removal services.",
            oneLiner = "Privacy disclosure & local status check for AI background cutout services.",
            icon = Icons.Default.AutoFixHigh,
            acceptedMimeTypes = listOf("image/*"),
            minFiles = 0,
            maxFiles = 1,
            isPopular = false,
            badge = "Notice",
            isSupportedOffline = false
        ),

        // Utilities
        ToolDefinition(
            id = ToolId.ZIP_CREATE,
            title = "Create ZIP Archive",
            category = ToolCategory.UTILITY,
            description = "Bundle multiple documents, images, and files into a compressed .zip package.",
            oneLiner = "Package multiple files into a single downloadable and shareable ZIP archive.",
            icon = Icons.Default.FolderZip,
            acceptedMimeTypes = listOf("*/*"),
            minFiles = 1,
            maxFiles = 50,
            isPopular = true
        ),
        ToolDefinition(
            id = ToolId.ZIP_EXTRACT,
            title = "Extract ZIP Archive",
            category = ToolCategory.UTILITY,
            description = "Inspect and extract files safely from a ZIP archive on your device.",
            oneLiner = "Open and unpack files from any standard ZIP archive with zero cloud uploads.",
            icon = Icons.Default.Unarchive,
            acceptedMimeTypes = listOf("application/zip", "application/x-zip-compressed"),
            minFiles = 1,
            maxFiles = 1,
            isPopular = false
        ),
        ToolDefinition(
            id = ToolId.BATCH_RENAME,
            title = "Batch File Renamer",
            category = ToolCategory.UTILITY,
            description = "Apply prefixes, suffixes, sequential numbers, or replace text across files.",
            oneLiner = "Standardize filenames with sequential numbering and pattern replacement.",
            icon = Icons.Default.DriveFileRenameOutline,
            acceptedMimeTypes = listOf("*/*"),
            minFiles = 1,
            maxFiles = 30,
            isPopular = false
        ),
        ToolDefinition(
            id = ToolId.FILE_INSPECTOR,
            title = "File & Meta Inspector",
            category = ToolCategory.UTILITY,
            description = "View detailed page count, pixel dimensions, color depth, and size breakdown.",
            oneLiner = "Examine document and image specifications, page metrics, and exact byte sizes.",
            icon = Icons.Default.Info,
            acceptedMimeTypes = listOf("*/*"),
            minFiles = 1,
            maxFiles = 1,
            isPopular = false
        )
    )

    fun getTool(id: ToolId): ToolDefinition {
        return tools.first { it.id == id }
    }
}
