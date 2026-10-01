package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.InstallationManager
import com.example.model.DownloadTask
import com.example.model.InstalledMod
import com.example.model.Instance
import com.example.model.Loader
import com.example.model.Mod
import com.example.model.ModVersion
import com.example.ui.components.InstallDialog
import com.example.ui.components.ModDetailSheet
import com.example.ui.components.OnboardingDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.BrowseScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.InstalledModsScreen
import com.example.ui.screens.InstancesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var app: ModManageApplication

    // Storage Access Framework Directory Picker
    private val pickSafDirectoryLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                app.storageManager.saveGrantedUri(uri)
                Toast.makeText(this, "Launcher storage access granted!", Toast.LENGTH_SHORT).show()
                refreshInstancesTrigger.value++
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to persist folder access: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val refreshInstancesTrigger = mutableStateOf(0)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = ModManageApplication.getInstance()

        setContent {
            MyApplicationTheme {
                MainAppContent(
                    app = app,
                    onPickSafFolder = { pickSafDirectoryLauncher.launch(null) },
                    refreshTrigger = refreshInstancesTrigger.value
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    app: ModManageApplication,
    onPickSafFolder: () -> Unit,
    refreshTrigger: Int
) {
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(Screen.BROWSE) }

    // Instances state
    var instances by remember { mutableStateOf<List<Instance>>(emptyList()) }
    var activeInstance by remember { mutableStateOf<Instance?>(null) }
    var installedMods by remember { mutableStateOf<List<InstalledMod>>(emptyList()) }

    var isPojavDetected by remember { mutableStateOf(false) }
    var isAmethystDetected by remember { mutableStateOf(false) }

    // Download Tasks state
    val downloadTasks = remember { mutableStateListOf<DownloadTask>() }

    // Mod details & install modal states
    var selectedModForDetails by remember { mutableStateOf<Mod?>(null) }
    var selectedModForInstall by remember { mutableStateOf<Mod?>(null) }
    var selectedVersionForInstall by remember { mutableStateOf<ModVersion?>(null) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Onboarding state
    var showOnboarding by remember { mutableStateOf(!app.storageManager.isOnboardingCompleted) }

    // Quick switch menu state
    var showInstanceQuickMenu by remember { mutableStateOf(false) }

    fun refreshInstancesAndMods() {
        scope.launch {
            withContext(Dispatchers.IO) {
                isPojavDetected = app.instanceRepository.isPojavInstalled
                isAmethystDetected = app.instanceRepository.isAmethystInstalled

                val insts = app.instanceRepository.refreshInstances()
                instances = insts
                val active = app.instanceRepository.activeInstance
                activeInstance = active

                if (active != null) {
                    installedMods = app.storageManager.scanInstalledMods(active)
                } else {
                    installedMods = emptyList()
                }
            }
        }
    }

    LaunchedEffect(refreshTrigger) {
        refreshInstancesAndMods()
    }

    // Connect Download Manager listener
    DisposableEffect(Unit) {
        val listener = object : InstallationManager.DownloadListener {
            override fun onTaskUpdated(task: DownloadTask) {
                val idx = downloadTasks.indexOfFirst { it.taskId == task.taskId }
                if (idx >= 0) {
                    downloadTasks[idx] = task
                } else {
                    downloadTasks.add(0, task)
                }
            }

            override fun onAllCompleted() {
                refreshInstancesAndMods()
            }
        }

        app.installationManager.addListener(listener)
        downloadTasks.clear()
        downloadTasks.addAll(app.installationManager.tasks)

        onDispose {
            app.installationManager.removeListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showInstanceQuickMenu = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Mod Manage",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeInstance?.name ?: "No instance selected",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 180.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Instance",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showInstanceQuickMenu,
                            onDismissRequest = { showInstanceQuickMenu = false }
                        ) {
                            instances.forEach { inst ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = inst.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (inst.id == activeInstance?.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                            )
                                            Text(
                                                text = "MC ${inst.minecraftVersion} · ${inst.loader.displayName}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        app.instanceRepository.activeInstance = inst
                                        activeInstance = inst
                                        installedMods = app.storageManager.scanInstalledMods(inst)
                                        showInstanceQuickMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            val activeDownloadCount = downloadTasks.count {
                it.status == DownloadTask.Status.DOWNLOADING ||
                it.status == DownloadTask.Status.INSTALLING ||
                it.status == DownloadTask.Status.VERIFYING
            }

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Screen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            if (screen == Screen.DOWNLOADS && activeDownloadCount > 0) {
                                BadgedBox(badge = { Badge { Text("$activeDownloadCount") } }) {
                                    Icon(imageVector = screen.icon, contentDescription = screen.title)
                                }
                            } else {
                                Icon(imageVector = screen.icon, contentDescription = screen.title)
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.BROWSE -> {
                    BrowseScreen(
                        activeInstance = activeInstance,
                        onModClick = { mod -> selectedModForDetails = mod },
                        onInstallClick = { mod ->
                            selectedModForInstall = mod
                            selectedVersionForInstall = null
                        }
                    )
                }
                Screen.INSTANCES -> {
                    InstancesScreen(
                        instances = instances,
                        activeInstance = activeInstance,
                        isPojavDetected = isPojavDetected,
                        isAmethystDetected = isAmethystDetected,
                        onSelectActive = { inst ->
                            app.instanceRepository.activeInstance = inst
                            activeInstance = inst
                            installedMods = app.storageManager.scanInstalledMods(inst)
                        },
                        onPickSafFolder = onPickSafFolder,
                        onRefreshInstances = { refreshInstancesAndMods() },
                        onAddCustomInstance = { name, mcVer, loader, loaderVer ->
                            app.instanceRepository.addCustomInstance(name, mcVer, loader, loaderVer)
                            refreshInstancesAndMods()
                        }
                    )
                }
                Screen.INSTALLED -> {
                    InstalledModsScreen(
                        activeInstance = activeInstance,
                        installedMods = installedMods,
                        onToggleModEnabled = { mod ->
                            if (activeInstance != null) {
                                val success = app.storageManager.toggleModEnabled(activeInstance!!, mod)
                                if (success) {
                                    installedMods = app.storageManager.scanInstalledMods(activeInstance!!)
                                }
                            }
                        },
                        onDeleteMod = { mod ->
                            if (activeInstance != null) {
                                val success = app.storageManager.deleteMod(activeInstance!!, mod)
                                if (success) {
                                    installedMods = app.storageManager.scanInstalledMods(activeInstance!!)
                                }
                            }
                        },
                        onUpdateMod = { mod, newVer ->
                            if (activeInstance != null) {
                                app.updateManager.updateMod(mod, newVer, activeInstance!!)
                                currentScreen = Screen.DOWNLOADS
                            }
                        },
                        onRefreshMods = {
                            if (activeInstance != null) {
                                installedMods = app.storageManager.scanInstalledMods(activeInstance!!)
                            }
                        },
                        onGoToBrowse = { currentScreen = Screen.BROWSE }
                    )
                }
                Screen.DOWNLOADS -> {
                    DownloadsScreen(
                        tasks = downloadTasks,
                        onClearCompleted = {
                            app.installationManager.clearCompletedTasks()
                            downloadTasks.clear()
                            downloadTasks.addAll(app.installationManager.tasks)
                        },
                        onGoToBrowse = { currentScreen = Screen.BROWSE }
                    )
                }
                Screen.SETTINGS -> {
                    SettingsScreen(onPickSafFolder = onPickSafFolder)
                }
            }
        }
    }

    // Mod Detail Modal Sheet
    if (selectedModForDetails != null) {
        ModDetailSheet(
            mod = selectedModForDetails!!,
            activeInstance = activeInstance,
            sheetState = detailSheetState,
            onDismiss = { selectedModForDetails = null },
            onInstall = { mod, version ->
                selectedModForDetails = null
                selectedModForInstall = mod
                selectedVersionForInstall = version
            }
        )
    }

    // Install & Dependency Resolution Confirmation Dialog
    if (selectedModForInstall != null) {
        InstallDialog(
            mod = selectedModForInstall!!,
            version = selectedVersionForInstall,
            instance = activeInstance,
            installedMods = installedMods,
            onDismiss = {
                selectedModForInstall = null
                selectedVersionForInstall = null
            },
            onConfirmInstall = { plan, instance ->
                app.installationManager.installPlan(plan, instance)
                selectedModForInstall = null
                selectedVersionForInstall = null
                currentScreen = Screen.DOWNLOADS
            }
        )
    }

    // First Launch Onboarding
    if (showOnboarding) {
        OnboardingDialog(
            isPojavDetected = isPojavDetected,
            isAmethystDetected = isAmethystDetected,
            instances = instances,
            selectedInstance = activeInstance,
            onPickDirectory = onPickSafFolder,
            onSelectInstance = { inst ->
                app.instanceRepository.activeInstance = inst
                activeInstance = inst
            },
            onFinishOnboarding = {
                app.storageManager.isOnboardingCompleted = true
                showOnboarding = false
            }
        )
    }
}
