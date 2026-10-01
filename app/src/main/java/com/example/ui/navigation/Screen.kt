package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val title: String, val icon: ImageVector) {
    BROWSE("Browse", Icons.Default.Search),
    INSTANCES("Instances", Icons.Default.Layers),
    INSTALLED("Installed", Icons.Default.Extension),
    DOWNLOADS("Downloads", Icons.Default.Download),
    SETTINGS("Settings", Icons.Default.Settings)
}
