package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FileManager
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tool by viewModel.selectedTool.collectAsState()
    val files by viewModel.selectedFiles.collectAsState()
    val pageItems by viewModel.pageItems.collectAsState()
    val executionState by viewModel.executionState.collectAsState()

    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    if (tool == null) {
        viewModel.navigateTo(AppScreen.HOME)
        return
    }

    val currentTool = tool!!

    // Generic file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addPickedUris(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentTool.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.testTag("button_back_to_home")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (files.isNotEmpty() && executionState !is ToolExecutionState.Processing) {
                        IconButton(
                            onClick = { viewModel.clearSelectedFiles() },
                            modifier = Modifier.testTag("button_clear_files")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all files", tint = Rose600)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("tool_detail_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step 1: One-sentence explanation
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueLight.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = currentTool.icon,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = currentTool.oneLiner,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Slate800
                        )
                    }
                }
            }

            // Step 2 & 3: File Picker & Selected Files
            if (currentTool.id != ToolId.BG_REMOVER_INFO) {
                item {
                    val mimeDisplay = when (currentTool.category) {
                        ToolCategory.PDF -> if (currentTool.id == ToolId.IMG_TO_PDF) "JPG, PNG, WebP" else "PDF (*.pdf)"
                        ToolCategory.IMAGE -> "JPG, PNG, WebP"
                        else -> if (currentTool.id == ToolId.ZIP_EXTRACT) "ZIP (*.zip)" else "Any files"
                    }

                    FilePickerZone(
                        acceptedFormats = mimeDisplay,
                        onPickFiles = {
                            val mimeTypes = when (currentTool.category) {
                                ToolCategory.PDF -> if (currentTool.id == ToolId.IMG_TO_PDF) arrayOf("image/*") else arrayOf("application/pdf")
                                ToolCategory.IMAGE -> arrayOf("image/*")
                                else -> if (currentTool.id == ToolId.ZIP_EXTRACT) arrayOf("application/zip", "application/x-zip-compressed", "*/*") else arrayOf("*/*")
                            }
                            filePickerLauncher.launch(mimeTypes)
                        },
                        onLoadSample = {
                            viewModel.loadSampleFilesForCurrentTool()
                            Toast.makeText(context, "Loaded verified sample files for demo!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Selected Files Count / Reorder Notice
                if (files.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected Files (${files.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (currentTool.maxFiles > 1) {
                                Text(
                                    text = "Use arrows to reorder",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate600
                                )
                            }
                        }
                    }

                    itemsIndexed(files) { index, file ->
                        SelectedFileItemRow(
                            file = file,
                            index = index,
                            totalCount = files.size,
                            canReorder = currentTool.maxFiles > 1,
                            onMoveUp = { viewModel.moveFileUp(index) },
                            onMoveDown = { viewModel.moveFileDown(index) },
                            onRemove = { viewModel.removeFile(index) }
                        )
                    }
                }
            }

            // Step 4 & 5: Tool-Specific Configuration & Previews
            item {
                ToolSpecificControls(
                    tool = currentTool,
                    viewModel = viewModel,
                    files = files,
                    pageItems = pageItems
                )
            }

            // Step 6: Process Action Button
            if (currentTool.id != ToolId.BG_REMOVER_INFO && executionState !is ToolExecutionState.Processing && executionState !is ToolExecutionState.Success) {
                item {
                    val isEnabled = files.size >= currentTool.minFiles
                    val actionLabel = when (currentTool.id) {
                        ToolId.PDF_MERGE -> "Merge ${files.size} PDFs"
                        ToolId.PDF_SPLIT -> "Extract Selected Pages"
                        ToolId.PDF_REORDER_ROTATE -> "Save Organized PDF"
                        ToolId.PDF_COMPRESS -> "Compress PDF"
                        ToolId.IMG_TO_PDF -> "Create PDF from ${files.size} Image(s)"
                        ToolId.PDF_TO_IMG -> "Convert PDF to Images"
                        ToolId.PDF_PAGE_NUMBERS -> "Stamp Page Numbers"
                        ToolId.PDF_WATERMARK -> "Apply Watermark"
                        ToolId.PDF_TEXT_NOTES -> "Apply Annotations"
                        ToolId.PDF_DOC_CREATOR -> "Generate Printable Document"
                        ToolId.PHOTO_SHEET -> "Generate Photo Sheet"
                        ToolId.IMG_CONVERT -> "Convert Format"
                        ToolId.IMG_RESIZE_CROP -> "Apply Resize & Crop"
                        ToolId.IMG_COMPRESS -> "Compress Image"
                        ToolId.IMG_ROTATE_FLIP -> "Apply Rotation & Flip"
                        ToolId.ZIP_CREATE -> "Create ZIP Package"
                        ToolId.ZIP_EXTRACT -> "Extract All Files"
                        ToolId.BATCH_RENAME -> "Batch Rename Files"
                        ToolId.FILE_INSPECTOR -> "Inspect File"
                        else -> "Process Files"
                    }

                    Button(
                        onClick = { viewModel.executeCurrentTool() },
                        enabled = isEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("button_execute_tool"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BluePrimary,
                            disabledContainerColor = Slate200
                        )
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnabled) actionLabel else "Select at least ${currentTool.minFiles} file(s)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Step 7: Progress State
            if (executionState is ToolExecutionState.Processing) {
                val p = executionState as ToolExecutionState.Processing
                item {
                    ProgressDisplayCard(progress = p.progress, statusMessage = p.statusMessage)
                }
            }

            // Step 8: Success Result
            if (executionState is ToolExecutionState.Success) {
                val s = executionState as ToolExecutionState.Success
                item {
                    OutputSuccessCard(
                        result = s.result,
                        onSaveToDownloads = {
                            val saved = FileManager.saveToPublicDownloads(context, s.result.file, s.result.mimeType)
                            if (saved) {
                                Toast.makeText(context, "Saved to your device Downloads folder!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Saved in OmniDoc workspace: ${s.result.file.name}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShare = {
                            FileManager.shareFile(context, s.result.file, s.result.mimeType)
                        },
                        onOpen = {
                            FileManager.openFile(context, s.result.file, s.result.mimeType)
                        },
                        onStartOver = {
                            viewModel.resetExecutionState()
                        }
                    )
                }
            }

            // Error State
            if (executionState is ToolExecutionState.Error) {
                val e = executionState as ToolExecutionState.Error
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("error_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Rose100)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = Rose600)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Processing Issue",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9F1239)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = e.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF881337)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.resetExecutionState() },
                                colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ToolSpecificControls(
    tool: ToolDefinition,
    viewModel: MainViewModel,
    files: List<PickedFile>,
    pageItems: List<PageItem>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tool_settings_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Tool Settings & Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when (tool.id) {
                // PHOTO SHEET MAKER (FEATURE HIGHLIGHTED IN PROMPT)
                ToolId.PHOTO_SHEET -> {
                    val paperSize by viewModel.photoSheetPaperSize.collectAsState()
                    val orientation by viewModel.photoSheetOrientation.collectAsState()
                    val columns by viewModel.photoSheetColumns.collectAsState()
                    val showCaptions by viewModel.photoSheetCaptions.collectAsState()
                    val captionType by viewModel.photoSheetCaptionType.collectAsState()
                    val customLabel by viewModel.photoSheetCustomLabel.collectAsState()
                    val exportFormat by viewModel.photoSheetExportFormat.collectAsState()

                    Text("Paper Standard & Orientation:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("A4", "US Letter").forEach { size ->
                            FilterChip(
                                selected = paperSize == size,
                                onClick = { viewModel.photoSheetPaperSize.value = size },
                                label = { Text(size) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        listOf("Portrait", "Landscape").forEach { ori ->
                            FilterChip(
                                selected = orientation == ori,
                                onClick = { viewModel.photoSheetOrientation.value = ori },
                                label = { Text(ori) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("Grid Columns: $columns", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = columns.toFloat(),
                        onValueChange = { viewModel.photoSheetColumns.value = it.toInt() },
                        valueRange = 1f..4f,
                        steps = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Include Text Captions", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = showCaptions,
                            onCheckedChange = { viewModel.photoSheetCaptions.value = it }
                        )
                    }

                    if (showCaptions) {
                        Text("Caption Style:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Numbered", "Filename", "Custom").forEach { type ->
                                FilterChip(
                                    selected = captionType == type,
                                    onClick = { viewModel.photoSheetCaptionType.value = type },
                                    label = { Text(type) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (captionType == "Custom") {
                            OutlinedTextField(
                                value = customLabel,
                                onValueChange = { viewModel.photoSheetCustomLabel.value = it },
                                label = { Text("Custom prefix (e.g. Photo, Item, Sample)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Text("Export Format:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = exportFormat == "PDF",
                            onClick = { viewModel.photoSheetExportFormat.value = "PDF" },
                            label = { Text("Print-Ready PDF") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = exportFormat == "IMAGE",
                            onClick = { viewModel.photoSheetExportFormat.value = "IMAGE" },
                            label = { Text("High-Res Image (JPG)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // PDF SPLIT
                ToolId.PDF_SPLIT -> {
                    val range by viewModel.splitPageRange.collectAsState()
                    val totalPages = files.firstOrNull()?.pageCount ?: 1
                    Text(
                        text = "Page Selection (PDF has $totalPages pages):",
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = range,
                        onValueChange = { viewModel.splitPageRange.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Page range e.g. '1-3' or '1, 3, 5' or 'all'") },
                        placeholder = { Text("1-2") },
                        singleLine = true
                    )
                }

                // PDF REORDER & ROTATE (Interactive thumbnails)
                ToolId.PDF_REORDER_ROTATE -> {
                    if (pageItems.isEmpty()) {
                        Text(
                            text = "Add a PDF to view and reorder its pages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    } else {
                        Text(
                            text = "Tap arrows to rearrange or rotate individual pages:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(pageItems) { idx, item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Slate50),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Page ${idx + 1} (Orig #${item.originalPageIndex + 1})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (item.thumbnail != null) {
                                            Image(
                                                bitmap = item.thumbnail.asImageBitmap(),
                                                contentDescription = "Page thumbnail",
                                                modifier = Modifier
                                                    .height(110.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.rotatePage(idx) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.movePageLeft(idx) },
                                                enabled = idx > 0,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.movePageRight(idx) },
                                                enabled = idx < pageItems.size - 1,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowForward, contentDescription = "Move Right", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.deletePage(idx) },
                                                enabled = pageItems.size > 1,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose600, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // PDF COMPRESS
                ToolId.PDF_COMPRESS -> {
                    val level by viewModel.pdfCompressLevel.collectAsState()
                    Text("Compression Target:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("High", "Medium", "Low").forEach { l ->
                            FilterChip(
                                selected = level == l,
                                onClick = { viewModel.pdfCompressLevel.value = l },
                                label = {
                                    Text(
                                        when (l) {
                                            "High" -> "High (Max Shrink)"
                                            "Medium" -> "Medium (Balanced)"
                                            else -> "Light (Crisp)"
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // PDF WATERMARK
                ToolId.PDF_WATERMARK -> {
                    val text by viewModel.watermarkText.collectAsState()
                    val opacity by viewModel.watermarkOpacity.collectAsState()
                    val angle by viewModel.watermarkAngle.collectAsState()

                    OutlinedTextField(
                        value = text,
                        onValueChange = { viewModel.watermarkText.value = it },
                        label = { Text("Watermark Text") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Opacity: $opacity%", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = opacity.toFloat(),
                        onValueChange = { viewModel.watermarkOpacity.value = it.toInt() },
                        valueRange = 10f..80f
                    )

                    Text("Orientation Angle:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0f to "Horizontal (0°)", 45f to "Diagonal (45°)", -45f to "Counter (-45°)").forEach { (deg, name) ->
                            FilterChip(
                                selected = angle == deg,
                                onClick = { viewModel.watermarkAngle.value = deg },
                                label = { Text(name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // PDF PAGE NUMBERS
                ToolId.PDF_PAGE_NUMBERS -> {
                    val pos by viewModel.pageNumberPosition.collectAsState()
                    val fmt by viewModel.pageNumberFormat.collectAsState()

                    Text("Number Placement:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Bottom Center", "Bottom Right", "Top Center").forEach { p ->
                            FilterChip(
                                selected = pos == p,
                                onClick = { viewModel.pageNumberPosition.value = p },
                                label = { Text(p) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = fmt,
                        onValueChange = { viewModel.pageNumberFormat.value = it },
                        label = { Text("Format Pattern (use {n} and {total})") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // PDF TEXT NOTES / ANNOTATIONS
                ToolId.PDF_TEXT_NOTES -> {
                    val text by viewModel.annotationText.collectAsState()
                    val pos by viewModel.annotationPosition.collectAsState()

                    OutlinedTextField(
                        value = text,
                        onValueChange = { viewModel.annotationText.value = it },
                        label = { Text("Banner Label / Notice") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Banner Position:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Top Header Banner", "Bottom Footer Banner").forEach { p ->
                            FilterChip(
                                selected = pos == p,
                                onClick = { viewModel.annotationPosition.value = p },
                                label = { Text(p) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // PRINTABLE DOC CREATOR
                ToolId.PDF_DOC_CREATOR -> {
                    val title by viewModel.docTitle.collectAsState()
                    val subtitle by viewModel.docSubtitle.collectAsState()
                    val body by viewModel.docBody.collectAsState()

                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.docTitle.value = it },
                        label = { Text("Document Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = { viewModel.docSubtitle.value = it },
                        label = { Text("Subtitle / Author / Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { viewModel.docBody.value = it },
                        label = { Text("Body Text / Paragraphs") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }

                // IMAGE CONVERT
                ToolId.IMG_CONVERT -> {
                    val targetFmt by viewModel.targetImageFormat.collectAsState()
                    val quality by viewModel.imageQuality.collectAsState()

                    Text("Convert to Format:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PNG", "JPEG", "WEBP").forEach { fmt ->
                            FilterChip(
                                selected = targetFmt == fmt,
                                onClick = { viewModel.targetImageFormat.value = fmt },
                                label = { Text(fmt) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (targetFmt != "PNG") {
                        Text("Output Quality: $quality%", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = quality.toFloat(),
                            onValueChange = { viewModel.imageQuality.value = it.toInt() },
                            valueRange = 20f..100f
                        )
                    }
                }

                // IMAGE RESIZE & CROP
                ToolId.IMG_RESIZE_CROP -> {
                    val width by viewModel.resizeWidth.collectAsState()
                    val height by viewModel.resizeHeight.collectAsState()
                    val crop by viewModel.cropRatio.collectAsState()

                    Text("Preset Scale:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.25f to "25%", 0.50f to "50%", 0.75f to "75%", 1.0f to "100%").forEach { (factor, name) ->
                            OutlinedButton(
                                onClick = {
                                    val first = files.firstOrNull()
                                    if (first?.width != null && first.height != null) {
                                        viewModel.resizeWidth.value = (first.width * factor).toInt()
                                        viewModel.resizeHeight.value = (first.height * factor).toInt()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(name)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = width.toString(),
                            onValueChange = { viewModel.resizeWidth.value = it.toIntOrNull() ?: width },
                            label = { Text("Width (px)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = height.toString(),
                            onValueChange = { viewModel.resizeHeight.value = it.toIntOrNull() ?: height },
                            label = { Text("Height (px)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Text("Aspect Ratio Crop:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Free", "1:1", "4:3", "16:9").forEach { ratio ->
                            FilterChip(
                                selected = crop == ratio,
                                onClick = { viewModel.cropRatio.value = ratio },
                                label = { Text(ratio) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // IMAGE COMPRESS
                ToolId.IMG_COMPRESS -> {
                    val q by viewModel.compressQuality.collectAsState()
                    Text("Compression Quality: $q%", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = q.toFloat(),
                        onValueChange = { viewModel.compressQuality.value = it.toInt() },
                        valueRange = 10f..95f
                    )
                    Text(
                        text = "Lower quality produces smaller file sizes. 60-75% is ideal for web & email.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }

                // IMAGE ROTATE & FLIP
                ToolId.IMG_ROTATE_FLIP -> {
                    val angle by viewModel.rotateAngle.collectAsState()
                    val fh by viewModel.flipH.collectAsState()
                    val fv by viewModel.flipV.collectAsState()

                    Text("Rotation:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(90f to "90° CW", 180f to "180°", 270f to "90° CCW").forEach { (deg, name) ->
                            FilterChip(
                                selected = angle == deg,
                                onClick = { viewModel.rotateAngle.value = deg },
                                label = { Text(name) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("Mirroring:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = fh,
                            onClick = { viewModel.flipH.value = !fh },
                            label = { Text("Flip Horizontal") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = fv,
                            onClick = { viewModel.flipV.value = !fv },
                            label = { Text("Flip Vertical") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ZIP CREATE
                ToolId.ZIP_CREATE -> {
                    val name by viewModel.zipArchiveName.collectAsState()
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.zipArchiveName.value = it },
                        label = { Text("Archive Name (without .zip)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // BATCH RENAME
                ToolId.BATCH_RENAME -> {
                    val prefix by viewModel.renamePrefix.collectAsState()
                    val suffix by viewModel.renameSuffix.collectAsState()
                    val numbering by viewModel.renameNumbering.collectAsState()
                    val findText by viewModel.renameFindText.collectAsState()
                    val replaceText by viewModel.renameReplaceText.collectAsState()

                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { viewModel.renamePrefix.value = it },
                        label = { Text("Prefix (e.g. Doc_)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = suffix,
                        onValueChange = { viewModel.renameSuffix.value = it },
                        label = { Text("Suffix (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add Sequential Numbers (_001, _002...)", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = numbering, onCheckedChange = { viewModel.renameNumbering.value = it })
                    }

                    if (files.isNotEmpty()) {
                        Text("Preview of Renamed Files:", style = MaterialTheme.typography.labelMedium)
                        val previews = com.example.engine.ZipAndUtilityEngine.computeRenamePreview(
                            files = files,
                            prefix = prefix,
                            suffix = suffix,
                            useNumbering = numbering,
                            findText = findText,
                            replaceText = replaceText
                        )
                        previews.take(4).forEach { (orig, ren) ->
                            Text(
                                text = "$orig  ➔  $ren",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // FILE INSPECTOR
                ToolId.FILE_INSPECTOR -> {
                    val file = files.firstOrNull()
                    if (file == null) {
                        Text("Pick any file to inspect metadata.", style = MaterialTheme.typography.bodySmall, color = Slate600)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Name: ${file.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("Size: ${FileManager.formatFileSize(file.sizeBytes)} (${file.sizeBytes} bytes)", style = MaterialTheme.typography.bodySmall)
                            Text("MIME: ${file.mimeType}", style = MaterialTheme.typography.bodySmall)
                            if (file.pageCount != null) {
                                Text("Page Count: ${file.pageCount} pages", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                            if (file.width != null && file.height != null) {
                                Text("Dimensions: ${file.width} × ${file.height} pixels", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Emerald600)
                            }
                        }
                    }
                }

                // BG REMOVER INFO & DISCLOSURE
                ToolId.BG_REMOVER_INFO -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Amber100),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Amber600)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "External Processing Disclosure",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78350F)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "To uphold strict privacy standards: OmniDoc runs 100% locally on your device without cloud transmission. AI Background Cutout requires server-side machine learning APIs which would send your photo to an external server. Therefore, this feature is kept disconnected to protect your privacy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = "Standard on-device processing will be applied.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                }
            }
        }
    }
}
