package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ModManageApplication
import com.example.data.provider.ModrinthProvider
import com.example.domain.ModRepository
import com.example.model.Instance
import com.example.model.Loader
import com.example.model.Mod
import com.example.model.Source
import com.example.ui.components.ModCard
import com.example.ui.theme.CurseForgeOrange
import com.example.ui.theme.ModrinthGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BrowseScreen(
    activeInstance: Instance?,
    onModClick: (Mod) -> Unit,
    onInstallClick: (Mod) -> Unit,
    modifier: Modifier = Modifier
) {
    val app = remember { ModManageApplication.getInstance() }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMcVersion by remember { mutableStateOf(activeInstance?.minecraftVersion ?: "1.21.1") }
    var selectedLoader by remember { mutableStateOf(activeInstance?.loader ?: Loader.FABRIC) }

    var modrinthEnabled by remember { mutableStateOf(app.modRepository.isModrinthEnabled) }
    var curseForgeEnabled by remember { mutableStateOf(app.modRepository.isCurseForgeEnabled) }

    var mods by remember { mutableStateOf<List<Mod>>(emptyList()) }
    var warnings by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var noSourcesActive by remember { mutableStateOf(false) }

    var showVersionMenu by remember { mutableStateOf(false) }
    var showLoaderMenu by remember { mutableStateOf(false) }
    var availableMcVersions by remember { mutableStateOf(ModrinthProvider.getDefaultGameVersions()) }

    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Fetch dynamic versions once
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = app.modrinthProvider.supportedGameVersions
            if (list.isNotEmpty()) availableMcVersions = list
        }
    }

    // Trigger search when query or filters change (debounced 300ms)
    fun executeSearch() {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(300)
            isLoading = true
            withContext(Dispatchers.IO) {
                app.modRepository.isModrinthEnabled = modrinthEnabled
                app.modRepository.isCurseForgeEnabled = curseForgeEnabled
                val result = app.modRepository.searchMods(
                    searchQuery,
                    if (selectedMcVersion == "All") "" else selectedMcVersion,
                    selectedLoader,
                    0,
                    40
                )
                mods = result.mods
                warnings = result.warnings
                noSourcesActive = result.noSourcesActive
                isLoading = false
            }
        }
    }

    LaunchedEffect(searchQuery, selectedMcVersion, selectedLoader, modrinthEnabled, curseForgeEnabled) {
        executeSearch()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Search Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                placeholder = {
                    Text(
                        text = "Search mods (e.g. Sodium, Iris, Gobber)...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter row: Source toggles & Version & Loader
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modrinth Filter
                FilterChip(
                    selected = modrinthEnabled,
                    onClick = { modrinthEnabled = !modrinthEnabled },
                    label = {
                        Text(
                            text = "Modrinth",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (modrinthEnabled) FontWeight.Bold else FontWeight.Normal,
                                color = if (modrinthEnabled) ModrinthGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ModrinthGreen.copy(alpha = 0.15f),
                        selectedLabelColor = ModrinthGreen
                    )
                )

                // CurseForge Filter
                FilterChip(
                    selected = curseForgeEnabled,
                    onClick = { curseForgeEnabled = !curseForgeEnabled },
                    label = {
                        Text(
                            text = "CurseForge",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (curseForgeEnabled) FontWeight.Bold else FontWeight.Normal,
                                color = if (curseForgeEnabled) CurseForgeOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CurseForgeOrange.copy(alpha = 0.15f),
                        selectedLabelColor = CurseForgeOrange
                    )
                )

                // MC Version dropdown button
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showVersionMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "MC: $selectedMcVersion",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showVersionMenu,
                        onDismissRequest = { showVersionMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Versions") },
                            onClick = {
                                selectedMcVersion = "All"
                                showVersionMenu = false
                            }
                        )
                        availableMcVersions.take(12).forEach { ver ->
                            DropdownMenuItem(
                                text = { Text(ver) },
                                onClick = {
                                    selectedMcVersion = ver
                                    showVersionMenu = false
                                }
                            )
                        }
                    }
                }

                // Loader dropdown button
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showLoaderMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Loader: ${if (selectedLoader == Loader.UNKNOWN) "All" else selectedLoader.displayName}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showLoaderMenu,
                        onDismissRequest = { showLoaderMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Loaders") },
                            onClick = {
                                selectedLoader = Loader.UNKNOWN
                                showLoaderMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fabric") },
                            onClick = {
                                selectedLoader = Loader.FABRIC
                                showLoaderMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Forge") },
                            onClick = {
                                selectedLoader = Loader.FORGE
                                showLoaderMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("NeoForge") },
                            onClick = {
                                selectedLoader = Loader.NEOFORGE
                                showLoaderMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Quilt") },
                            onClick = {
                                selectedLoader = Loader.QUILT
                                showLoaderMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Warnings / notes banner (e.g. CurseForge key note, or source down)
        if (warnings.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                warnings.forEach { warning ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (isLoading && mods.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Browsing Minecraft mods...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (noSourcesActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Sources Selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Both Modrinth and CurseForge sources are disabled in filters. Enable at least one source at the top to browse mods.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (mods.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Mods Found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No mods match '${searchQuery}' with the selected filters (${if (modrinthEnabled) "Modrinth" else ""} ${if (curseForgeEnabled) "CurseForge" else ""}, MC $selectedMcVersion, ${selectedLoader.displayName}). Try broadening your filters.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mods, key = { "${it.source}_${it.id}" }) { mod ->
                        ModCard(
                            mod = mod,
                            onModClick = onModClick,
                            onInstallClick = onInstallClick
                        )
                    }
                }
            }
        }
    }
}
