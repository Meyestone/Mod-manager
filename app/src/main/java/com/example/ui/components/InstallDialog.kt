package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ModManageApplication
import com.example.domain.DependencyResolver
import com.example.model.InstalledMod
import com.example.model.Instance
import com.example.model.Mod
import com.example.model.ModVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InstallDialog(
    mod: Mod,
    version: ModVersion?,
    instance: Instance?,
    installedMods: List<InstalledMod>,
    onDismiss: () -> Unit,
    onConfirmInstall: (DependencyResolver.ResolutionPlan, Instance) -> Unit
) {
    var isChecking by remember { mutableStateOf(true) }
    var resolutionPlan by remember { mutableStateOf<DependencyResolver.ResolutionPlan?>(null) }
    var resolvedVersion by remember { mutableStateOf<ModVersion?>(version) }
    var isAlreadyInstalled by remember { mutableStateOf(false) }
    var alreadyInstalledMod by remember { mutableStateOf<InstalledMod?>(null) }

    LaunchedEffect(mod, instance) {
        if (instance == null) {
            isChecking = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val app = ModManageApplication.getInstance()

            // 1. Fetch version if not already provided
            var targetVer = version
            if (targetVer == null) {
                try {
                    val versions = app.modRepository.getVersions(
                        mod.id,
                        mod.source,
                        instance.minecraftVersion,
                        instance.loader
                    )
                    if (versions.isNotEmpty()) {
                        targetVer = versions[0]
                        resolvedVersion = targetVer
                    }
                } catch (ignored: Exception) {
                }
            }

            // 2. Check if already installed
            for (inst in installedMods) {
                if (inst.modId.equals(mod.id, ignoreCase = true) ||
                    inst.name.equals(mod.name, ignoreCase = true) ||
                    (targetVer?.primaryFile != null && inst.fileName.equals(targetVer.primaryFile.fileName, ignoreCase = true))) {
                    isAlreadyInstalled = true
                    alreadyInstalledMod = inst
                    break
                }
            }

            // 3. Resolve dependencies
            if (targetVer != null) {
                val plan = app.dependencyResolver.resolveDependencies(mod, targetVer, instance, installedMods)
                resolutionPlan = plan
            }
            isChecking = false
        }
    }

    if (instance == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select an Instance First") },
            text = { Text("You must select or create a Minecraft instance before installing '${mod.name}'.") },
            confirmButton = {
                Button(onClick = onDismiss) { Text("OK") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Install ${mod.name}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            if (isChecking) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Analyzing compatibility & dependencies...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (resolutionPlan == null || resolvedVersion == null) {
                Column {
                    Text(
                        text = "No compatible version found for ${instance.name} (Minecraft ${instance.minecraftVersion} · ${instance.loader.displayName}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                val plan = resolutionPlan!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Instance target
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Target Instance: ${instance.name}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Minecraft ${instance.minecraftVersion} · ${instance.loader.displayName}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Already installed warning
                    if (isAlreadyInstalled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Mod Already Installed",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Current: v${alreadyInstalledMod?.version ?: "1.0"}. Installing will update the existing .jar file.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // Incompatible reasons
                    if (plan.incompatibleReasons.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Compatibility Issues:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                                plan.incompatibleReasons.forEach { reason ->
                                    Text(
                                        text = "• $reason",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // Summary of files to install
                    Column {
                        Text(
                            text = "Installation Summary (${plan.totalModsToInstallCount} file(s)):",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. ${mod.name} (v${plan.targetVersion.versionNumber}) - ${plan.targetVersion.primaryFile?.formattedSize ?: "JAR"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (plan.requiredDependenciesToInstall.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Auto-installing missing required dependencies:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            plan.requiredDependenciesToInstall.forEachIndexed { idx, depVer ->
                                Text(
                                    text = "   ${idx + 2}. ${depVer.name} (v${depVer.versionNumber})",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (resolutionPlan != null && resolutionPlan!!.isCompatible) {
                Button(
                    onClick = {
                        onConfirmInstall(resolutionPlan!!, instance)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isAlreadyInstalled) "Update Mod" else "Install Now")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
