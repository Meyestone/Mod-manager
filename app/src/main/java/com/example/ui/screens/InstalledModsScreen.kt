package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ModManageApplication
import com.example.model.InstalledMod
import com.example.model.Instance
import com.example.model.ModVersion
import com.example.ui.components.InstalledModItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InstalledModsScreen(
    activeInstance: Instance?,
    installedMods: List<InstalledMod>,
    onToggleModEnabled: (InstalledMod) -> Unit,
    onDeleteMod: (InstalledMod) -> Unit,
    onUpdateMod: (InstalledMod, ModVersion) -> Unit,
    onRefreshMods: () -> Unit,
    onGoToBrowse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = remember { ModManageApplication.getInstance() }
    val scope = rememberCoroutineScope()

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updatesFoundCount by remember { mutableStateOf(0) }
    var updateMap by remember { mutableStateOf<Map<String, ModVersion>>(emptyMap()) }

    var modToDelete by remember { mutableStateOf<InstalledMod?>(null) }
    var dependentModsWarning by remember { mutableStateOf<List<String>>(emptyList()) }

    fun checkForUpdates() {
        if (activeInstance == null || installedMods.isEmpty()) return
        scope.launch {
            isCheckingUpdates = true
            withContext(Dispatchers.IO) {
                val results = app.updateManager.checkForUpdates(activeInstance, installedMods)
                val map = mutableMapOf<String, ModVersion>()
                for (r in results) {
                    map[r.originalMod.id] = r.latestVersion
                }
                updateMap = map
                updatesFoundCount = results.size
                isCheckingUpdates = false
            }
        }
    }

    LaunchedEffect(activeInstance, installedMods.size) {
        checkForUpdates()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Installed Mods",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (activeInstance != null) "${activeInstance.name} · ${installedMods.size} mod(s)" else "No active instance",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCheckingUpdates) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    OutlinedButton(
                        onClick = { checkForUpdates() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Check Updates", style = MaterialTheme.typography.labelSmall)
                    }
                }

                IconButton(onClick = onRefreshMods) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Active Instance Target Info Bar
        if (activeInstance != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MC ${activeInstance.minecraftVersion} · ${activeInstance.loader.displayName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (updatesFoundCount > 0) {
                        Text(
                            text = "$updatesFoundCount update(s) available",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }

        // Mod List
        if (installedMods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Mods Installed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (activeInstance != null) "The 'mods' folder of ${activeInstance.name} is currently empty." else "Select an instance to view installed mods.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onGoToBrowse,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "Browse & Install Mods")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(installedMods, key = { it.fileName }) { mod ->
                    val latestVer = updateMap[mod.id]
                    val modWithUpdate = if (latestVer != null) {
                        mod.withUpdate(true, latestVer.versionNumber, latestVer.versionId)
                    } else {
                        mod
                    }

                    InstalledModItem(
                        mod = modWithUpdate,
                        onToggleEnabled = onToggleModEnabled,
                        onDeleteMod = {
                            // Check if other installed mods require this mod!
                            val dependents = mutableListOf<String>()
                            for (other in installedMods) {
                                if (other.fileName != mod.fileName && other.dependencies.contains(mod.modId)) {
                                    dependents.add(other.name)
                                }
                            }
                            dependentModsWarning = dependents
                            modToDelete = mod
                        },
                        onUpdateMod = {
                            if (latestVer != null) {
                                onUpdateMod(mod, latestVer)
                            }
                        }
                    )
                }
            }
        }
    }

    // Deletion confirmation dialog with Dependency warning (Section 13)
    if (modToDelete != null) {
        val targetMod = modToDelete!!
        AlertDialog(
            onDismissRequest = { modToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dependentModsWarning.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = "Delete ${targetMod.name}?")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to remove '${targetMod.fileName}' from ${activeInstance?.name ?: "instance"}?",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (dependentModsWarning.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Dependency Warning:",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                                Text(
                                    text = "The following installed mod(s) require '${targetMod.name}':",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                dependentModsWarning.forEach { dep ->
                                    Text(
                                        text = " • $dep",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = "Removing it may prevent these mods from working.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMod(targetMod)
                        modToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { modToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
