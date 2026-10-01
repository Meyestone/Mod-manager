package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Instance
import com.example.model.Loader
import com.example.ui.components.InstanceCard

@Composable
fun InstancesScreen(
    instances: List<Instance>,
    activeInstance: Instance?,
    isPojavDetected: Boolean,
    isAmethystDetected: Boolean,
    onSelectActive: (Instance) -> Unit,
    onPickSafFolder: () -> Unit,
    onRefreshInstances: () -> Unit,
    onAddCustomInstance: (String, String, Loader, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
                    text = "Minecraft Instances",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${instances.size} instance(s) detected",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onRefreshInstances) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Instances",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Launcher Status Banner
        if (!isPojavDetected && !isAmethystDetected) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "No Launcher Automatically Detected",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Mod Manage requires PojavLauncher or Amethyst Launcher. If your launcher is installed, grant folder access using Android Storage Access Framework (SAF).",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onPickSafFolder,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Select Launcher Folder (SAF)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        } else {
            // Launcher status info bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LauncherBadge(name = "PojavLauncher", detected = isPojavDetected)
                LauncherBadge(name = "Amethyst Launcher", detected = isAmethystDetected)
            }
        }

        // Instances list
        if (instances.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Instances Found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Scan for PojavLauncher or Amethyst Launcher instances or add a custom instance directory.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onPickSafFolder) {
                        Text(text = "Grant Folder Access (SAF)")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(instances, key = { it.id }) { instance ->
                    InstanceCard(
                        instance = instance,
                        isActive = activeInstance?.id == instance.id,
                        onSelectActive = onSelectActive,
                        onGrantFolderAccess = { onPickSafFolder() }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddInstanceDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, mcVer, loader, loaderVer ->
                onAddCustomInstance(name, mcVer, loader, loaderVer)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun LauncherBadge(name: String, detected: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (detected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (detected) MaterialTheme.colorScheme.primary else Color.Gray)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$name: ${if (detected) "Detected" else "Missing"}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (detected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddInstanceDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, mcVer: String, loader: Loader, loaderVer: String) -> Unit
) {
    var name by remember { mutableStateOf("My Instance") }
    var mcVersion by remember { mutableStateOf("1.21.1") }
    var selectedLoader by remember { mutableStateOf(Loader.FABRIC) }
    var loaderVersion by remember { mutableStateOf("0.16.9") }
    var showLoaderDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Minecraft Instance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Instance Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mcVersion,
                    onValueChange = { mcVersion = it },
                    label = { Text("Minecraft Version (e.g. 1.21.1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    OutlinedButton(
                        onClick = { showLoaderDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Loader: ${selectedLoader.displayName}")
                    }
                    DropdownMenu(
                        expanded = showLoaderDropdown,
                        onDismissRequest = { showLoaderDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Fabric") },
                            onClick = {
                                selectedLoader = Loader.FABRIC
                                loaderVersion = "0.16.9"
                                showLoaderDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Forge") },
                            onClick = {
                                selectedLoader = Loader.FORGE
                                loaderVersion = "47.2.0"
                                showLoaderDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("NeoForge") },
                            onClick = {
                                selectedLoader = Loader.NEOFORGE
                                loaderVersion = "21.1.72"
                                showLoaderDropdown = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Quilt") },
                            onClick = {
                                selectedLoader = Loader.QUILT
                                loaderVersion = "0.26.0"
                                showLoaderDropdown = false
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = loaderVersion,
                    onValueChange = { loaderVersion = it },
                    label = { Text("Loader Version") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, mcVersion, selectedLoader, loaderVersion) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Add Instance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
