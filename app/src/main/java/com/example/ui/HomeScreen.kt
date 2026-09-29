package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ToolCategory
import com.example.model.ToolDefinition
import com.example.model.ToolRegistry
import com.example.ui.components.PrivacyBadgeBanner
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val filteredTools = remember(searchQuery, selectedCategory) {
        ToolRegistry.tools.filter { tool ->
            val matchesCategory = selectedCategory == ToolCategory.ALL || tool.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                    tool.title.contains(searchQuery, ignoreCase = true) ||
                    tool.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val popularTools = remember {
        ToolRegistry.tools.filter { it.isPopular }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "OmniDoc",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.selectTool(ToolRegistry.getTool(com.example.model.ToolId.PHOTO_SHEET))
                            viewModel.loadSampleFilesForCurrentTool()
                        },
                        modifier = Modifier.testTag("action_quick_demo")
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Quick Demo",
                            tint = Amber600
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("home_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Column {
                    Text(
                        text = "Everyday file tools, all in one place.",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Merge, split, compress, organize PDFs, create photo contact sheets, and package archives with complete local privacy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Privacy Banner
            item {
                PrivacyBadgeBanner()
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_input"),
                    placeholder = { Text("Search tools (e.g. merge, photo sheet, watermark...)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToolCategory.values().forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCategory(category) },
                            label = { Text(category.label) },
                            leadingIcon = {
                                when (category) {
                                    ToolCategory.ALL -> Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(16.dp))
                                    ToolCategory.PDF -> Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    ToolCategory.IMAGE -> Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    ToolCategory.UTILITY -> Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BluePrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("chip_category_${category.name.lowercase()}")
                        )
                    }
                }
            }

            // Quick Access / Popular Tools (when not searching)
            if (searchQuery.isBlank() && selectedCategory == ToolCategory.ALL) {
                item {
                    Text(
                        text = "Frequently Used Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        popularTools.take(5).forEach { tool ->
                            PopularToolCard(
                                tool = tool,
                                onClick = { viewModel.selectTool(tool) }
                            )
                        }
                    }
                }
            }

            // All Tools Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedCategory.label} (${filteredTools.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tools List
            items(filteredTools) { tool ->
                ToolCard(
                    tool = tool,
                    onClick = { viewModel.selectTool(tool) }
                )
            }

            // Footer specifications & limits card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("specs_footer_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Supported Formats & Limits",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Formats: PDF, JPG, PNG, WebP, ZIP\n• Recommended file sizes: up to 100MB per file\n• Processing: Native Android Graphics & PdfRenderer\n• Security: Zero cloud dependencies, 100% on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate600
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PopularToolCard(
    tool: ToolDefinition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(140.dp)
            .height(115.dp)
            .clickable { onClick() }
            .testTag("popular_tool_${tool.id.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (tool.category) {
                                ToolCategory.PDF -> Rose100
                                ToolCategory.IMAGE -> BlueLight
                                else -> Emerald100
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        tint = when (tool.category) {
                            ToolCategory.PDF -> Rose600
                            ToolCategory.IMAGE -> BluePrimary
                            else -> Emerald600
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (tool.badge != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Amber100
                    ) {
                        Text(
                            text = tool.badge,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Amber600,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Text(
                text = tool.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ToolCard(
    tool: ToolDefinition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tool_card_${tool.id.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when (tool.category) {
                            ToolCategory.PDF -> Rose100
                            ToolCategory.IMAGE -> BlueLight
                            else -> Indigo100
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = when (tool.category) {
                        ToolCategory.PDF -> Rose600
                        ToolCategory.IMAGE -> BluePrimary
                        else -> Indigo600
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tool.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (tool.badge != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (tool.badge == "Notice") Rose100 else Amber100
                        ) {
                            Text(
                                text = tool.badge,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tool.badge == "Notice") Rose600 else Amber600,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open tool",
                tint = Slate400,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
