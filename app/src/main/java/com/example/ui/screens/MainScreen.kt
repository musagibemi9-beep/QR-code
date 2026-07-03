package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.QrViewModel
import com.example.ui.components.AiStudioSection
import com.example.ui.components.HistorySection
import com.example.ui.components.InputFormsSection
import com.example.ui.components.LivePreviewSection
import com.example.ui.components.StyleControlsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: QrViewModel) {
    val config by viewModel.currentConfig.collectAsState()
    val bitmap by viewModel.generatedBitmap.collectAsState()
    val svgString by viewModel.generatedSvg.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val savedItems by viewModel.savedItems.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    
    val isSearchingLink by viewModel.isSearchingLink.collectAsState()
    val isSearchingLocation by viewModel.isSearchingLocation.collectAsState()
    val isGeneratingImage by viewModel.isGeneratingImage.collectAsState()
    val aiMessage by viewModel.aiMessage.collectAsState()

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("QR Studio Pro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Production-Ready Vector & AI Studio", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    if (user != null) {
                        Surface(
                            color = Color(0xFFD1FAE5),
                            shape = CircleShape,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("☁️ ${user?.displayName?.split(" ")?.firstOrNull() ?: "User"}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                            }
                        }
                    } else {
                        Surface(
                            onClick = { viewModel.loginDemoUser("Alex Rivera", "alex@qrstudio.dev") },
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text("Connect Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (!isWideScreen) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.setTab(0) },
                        icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        label = { Text("Create") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { viewModel.setTab(1) },
                        icon = { Icon(Icons.Default.Palette, contentDescription = null) },
                        label = { Text("Style") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { viewModel.setTab(2) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        label = { Text("AI Studio") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { viewModel.setTab(3) },
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                        label = { Text("Library (${savedItems.size})") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isWideScreen) {
                // 🖥️ Two-Column Layout for Tablet / Foldable / Expanded Screens
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Panel: Configuration & AI Suite (Weight 1.3f)
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxSize()
                    ) {
                        // Wide Screen Top Tabs
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            listOf(
                                0 to ("Create Form" to Icons.Default.Edit),
                                1 to ("Design & Style" to Icons.Default.Palette),
                                2 to ("AI Studio" to Icons.Default.AutoAwesome),
                                3 to ("Cloud Library (${savedItems.size})" to Icons.Default.Bookmark)
                            ).forEach { (idx, pair) ->
                                val (title, icon) = pair
                                val isSel = selectedTab == idx
                                Surface(
                                    onClick = { viewModel.setTab(idx) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    ) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(title, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Left Pane Scrollable Content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(end = 6.dp)
                        ) {
                            when (selectedTab) {
                                0 -> InputFormsSection(
                                    config = config,
                                    onConfigChanged = { viewModel.updateConfig(it) },
                                    onOpenAiLinkSearch = { viewModel.setTab(2) },
                                    onOpenAiLocationSearch = { viewModel.setTab(2) }
                                )
                                1 -> StyleControlsSection(
                                    config = config,
                                    presets = viewModel.presets,
                                    onConfigChanged = { viewModel.updateConfig(it) },
                                    onApplyPreset = { viewModel.applyPreset(it) },
                                    onOpenAiLogoStudio = { viewModel.setTab(2) }
                                )
                                2 -> AiStudioSection(
                                    isSearchingLink = isSearchingLink,
                                    isSearchingLocation = isSearchingLocation,
                                    isGeneratingImage = isGeneratingImage,
                                    onSearchLink = { viewModel.searchSmartLink(it) },
                                    onSearchLocation = { viewModel.searchSmartLocation(it) },
                                    onGenerateLogo = { prompt, ratio -> viewModel.generateAiLogo(prompt, ratio) }
                                )
                                3 -> HistorySection(
                                    user = user,
                                    items = savedItems,
                                    onLoginDemo = { viewModel.loginDemoUser("Alex Rivera", "alex@qrstudio.dev") },
                                    onLogout = { viewModel.logout() },
                                    onSyncCloud = { viewModel.syncCloudNow() },
                                    onLoadItem = { viewModel.loadSavedItem(it) },
                                    onDeleteItem = { viewModel.deleteSavedItem(it) }
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }

                    // Right Panel: Sticky Live Preview & Download (Weight 1f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        LivePreviewSection(
                            config = config,
                            bitmap = bitmap,
                            svgString = svgString,
                            onSaveToLibrary = { viewModel.saveCurrentQrToLibrary() }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            } else {
                // 📱 Mobile Portrait Layout (Compact)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    // In mobile, show the Live Preview card right at top for immediate real-time feedback!
                    LivePreviewSection(
                        config = config,
                        bitmap = bitmap,
                        svgString = svgString,
                        onSaveToLibrary = { viewModel.saveCurrentQrToLibrary() }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mode Specific Editor below
                    when (selectedTab) {
                        0 -> InputFormsSection(
                            config = config,
                            onConfigChanged = { viewModel.updateConfig(it) },
                            onOpenAiLinkSearch = { viewModel.setTab(2) },
                            onOpenAiLocationSearch = { viewModel.setTab(2) }
                        )
                        1 -> StyleControlsSection(
                            config = config,
                            presets = viewModel.presets,
                            onConfigChanged = { viewModel.updateConfig(it) },
                            onApplyPreset = { viewModel.applyPreset(it) },
                            onOpenAiLogoStudio = { viewModel.setTab(2) }
                        )
                        2 -> AiStudioSection(
                            isSearchingLink = isSearchingLink,
                            isSearchingLocation = isSearchingLocation,
                            isGeneratingImage = isGeneratingImage,
                            onSearchLink = { viewModel.searchSmartLink(it) },
                            onSearchLocation = { viewModel.searchSmartLocation(it) },
                            onGenerateLogo = { prompt, ratio -> viewModel.generateAiLogo(prompt, ratio) }
                        )
                        3 -> HistorySection(
                            user = user,
                            items = savedItems,
                            onLoginDemo = { viewModel.loginDemoUser("Alex Rivera", "alex@qrstudio.dev") },
                            onLogout = { viewModel.logout() },
                            onSyncCloud = { viewModel.syncCloudNow() },
                            onLoadItem = { viewModel.loadSavedItem(it) },
                            onDeleteItem = { viewModel.deleteSavedItem(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            // Floating AI / Notification Banner
            AnimatedVisibility(
                visible = aiMessage != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                aiMessage?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.inverseSurface,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth(if (isWideScreen) 0.6f else 0.95f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.clearAiMessage() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
