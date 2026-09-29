package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Specifications", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("privacy_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Emerald100)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Emerald600),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Zero-Cloud Guarantee",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Every file task (PDF merge, image scaling, contact sheets, compression, watermarking) runs entirely inside the local device sandbox. No documents, photos, or personal data are ever uploaded, sent to third-party clouds, or used to train artificial intelligence models.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Supported Formats & Limits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text("📄 PDF Documents (*.pdf)", fontWeight = FontWeight.SemiBold, color = BluePrimary)
                        Text("Supports multi-page documents, standard page sizes (A4, US Letter), rotation, compression, annotations, and rendering up to 300 DPI.", style = MaterialTheme.typography.bodySmall, color = Slate600)

                        HorizontalDivider(color = Slate200)

                        Text("🖼️ Image Files (*.jpg, *.jpeg, *.png, *.webp)", fontWeight = FontWeight.SemiBold, color = BluePrimary)
                        Text("Supports 24-bit sRGB color, alpha transparency in PNG/WebP, EXIF orientation correction, scaling, and contact sheet generation up to 4 columns.", style = MaterialTheme.typography.bodySmall, color = Slate600)

                        HorizontalDivider(color = Slate200)

                        Text("📦 Archives (*.zip)", fontWeight = FontWeight.SemiBold, color = BluePrimary)
                        Text("Standard DEFLATE compression and extraction for batch packaging.", style = MaterialTheme.typography.bodySmall, color = Slate600)

                        HorizontalDivider(color = Slate200)

                        Text("⚖️ Recommended File Size Limits", fontWeight = FontWeight.SemiBold, color = BluePrimary)
                        Text("For optimal device performance, files under 150 MB are recommended. Contact sheets comfortably arrange up to 50 photos per batch.", style = MaterialTheme.typography.bodySmall, color = Slate600)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "External Services & AI Disclosure",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Certain advanced computer-vision tasks (such as automated background cutout / subject extraction) rely on high-parameter cloud neural networks. In accordance with OmniDoc's privacy charter, no external calls are made without explicit user consent. The Background Cutout tool displays this status clearly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Temporary File Retention",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Generated files are stored only in the application's private cache and output folder. You can save any file directly to your device Downloads, or clear all temporary exports at any time from the Recents tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
