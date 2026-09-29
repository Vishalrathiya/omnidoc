package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FileManager
import com.example.model.PickedFile
import com.example.model.ToolResult
import com.example.ui.theme.*

@Composable
fun AppBottomBar(
    currentScreen: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("app_bottom_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == "HOME",
            onClick = { onNavigate("HOME") },
            icon = { Icon(Icons.Default.HomeRepairService, contentDescription = "Tools") },
            label = { Text("Tools") },
            modifier = Modifier.testTag("nav_tools")
        )
        NavigationBarItem(
            selected = currentScreen == "RECENTS",
            onClick = { onNavigate("RECENTS") },
            icon = { Icon(Icons.Default.History, contentDescription = "Recent Exports") },
            label = { Text("Recents") },
            modifier = Modifier.testTag("nav_recents")
        )
        NavigationBarItem(
            selected = currentScreen == "PRIVACY",
            onClick = { onNavigate("PRIVACY") },
            icon = { Icon(Icons.Default.Shield, contentDescription = "Privacy & Formats") },
            label = { Text("Privacy") },
            modifier = Modifier.testTag("nav_privacy")
        )
    }
}

@Composable
fun PrivacyBadgeBanner(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("privacy_badge_banner"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Emerald100
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Emerald600),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security lock",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "100% On-Device Processing",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF065F46)
                )
                Text(
                    text = "Your files never leave this device. Zero server uploads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF047857)
                )
            }
        }
    }
}

@Composable
fun FilePickerZone(
    acceptedFormats: String,
    onPickFiles: () -> Unit,
    onLoadSample: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("file_picker_zone"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(BlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Upload files",
                    tint = BluePrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Select or Drop Files",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Supported: $acceptedFormats",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPickFiles,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_choose_files"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose Files")
                }

                OutlinedButton(
                    onClick = onLoadSample,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_load_sample_files"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = Amber600)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Try Demo", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun SelectedFileItemRow(
    file: PickedFile,
    index: Int,
    totalCount: Int,
    canReorder: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("selected_file_row_$index"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                if (file.thumbnail != null) {
                    Image(
                        bitmap = file.thumbnail.asImageBitmap(),
                        contentDescription = file.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (file.mimeType == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = if (file.mimeType == "application/pdf") Rose600 else Slate600
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FileManager.formatFileSize(file.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (file.pageCount != null) {
                        Text(
                            text = " • ${file.pageCount} pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (file.width != null && file.height != null) {
                        Text(
                            text = " • ${file.width}×${file.height}px",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (canReorder && totalCount > 1) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier.size(36.dp).testTag("move_up_$index")
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier.size(36.dp).testTag("move_down_$index")
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp).testTag("remove_file_$index")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Remove file", tint = Rose600, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ProgressDisplayCard(
    progress: Float,
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("progress_display_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Processing on device...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = BluePrimary,
                trackColor = Slate200
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OutputSuccessCard(
    result: ToolResult,
    onSaveToDownloads: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("output_success_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Emerald100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Emerald600,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Processing Ready!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = result.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Preview Thumbnail if present
            if (result.previewBitmap != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate100)
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = result.previewBitmap.asImageBitmap(),
                        contentDescription = "Result Preview",
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate50,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("File Name:", style = MaterialTheme.typography.bodySmall, color = Slate600)
                        Text(result.file.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Output Size:", style = MaterialTheme.typography.bodySmall, color = Slate600)
                        Text(FileManager.formatFileSize(result.sizeBytes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BluePrimary)
                    }

                    if (result.originalSizeBytes != null && result.originalSizeBytes > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val diff = result.originalSizeBytes - result.sizeBytes
                        val percent = if (result.originalSizeBytes > 0) (diff.toDouble() / result.originalSizeBytes * 100).toInt() else 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Size Change:", style = MaterialTheme.typography.bodySmall, color = Slate600)
                            Text(
                                text = if (diff > 0) "Saved $percent% (${FileManager.formatFileSize(diff)})" else "Formatted",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (diff > 0) Emerald600 else Slate700
                            )
                        }
                    }

                    if (result.pageCount != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Page Count:", style = MaterialTheme.typography.bodySmall, color = Slate600)
                            Text("${result.pageCount} pages", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSaveToDownloads,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_save_downloads"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }

                Button(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_share_result"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_open_result"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View File")
                }

                TextButton(
                    onClick = onStartOver,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_start_over")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Over")
                }
            }
        }
    }
}
