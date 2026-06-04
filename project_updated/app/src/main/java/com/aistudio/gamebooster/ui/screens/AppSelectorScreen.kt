package com.aistudio.gamebooster.ui.screens

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aistudio.gamebooster.R
import com.aistudio.gamebooster.ui.theme.*
import com.aistudio.gamebooster.ui.viewmodel.GameBoosterViewModel
import com.aistudio.gamebooster.utils.InstalledApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorScreen(
    viewModel: GameBoosterViewModel,
    onNavigateBack: () -> Unit
) {
    val allApps by viewModel.allApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoadingApps.collectAsState()

    var activeFilter by remember { mutableStateOf("Games") }

    // Filter apps in real-time
    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Gorgeous Cyberpunk Glow Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0E17), // Deep space purple
                        Color(0xFF07070B)  // Absolute black
                    )
                )
            )
            .testTag("app_selector_screen")
    ) {
        // Spotlight aurora bleed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x30CAF02A), // Neon lime flare
                            Color(0x00000000)
                        ),
                        radius = 650f
                    )
                )
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_selector_title),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_selector_desc),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // High-End Cyberpunk Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(text = stringResource(R.string.search_apps), color = Color.Gray) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFFCAF02A)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.LightGray
                                )
                            }
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFCAF02A),
                        unfocusedBorderColor = Color(0xFF1C1C24),
                        focusedContainerColor = Color(0xFF14141B),
                        unfocusedContainerColor = Color(0xFF14141B)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_search_field")
                        .padding(bottom = 16.dp)
                )

                // Filter pills: Games vs All Apps
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Games", "All Apps").forEach { filter ->
                        val isSelected = activeFilter == filter
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFFCAF02A) else Color(0xFF1A1A22).copy(alpha = 0.7f))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFFCAF02A) else Color(0xFF23232C),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { activeFilter = filter }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (filter == "Games") "Games" else "All Apps",
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Loading Spinner or Apps Selection List
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFCAF02A))
                    }
                } else {
                    val detectedGames = remember(filteredApps) { filteredApps.filter { it.isSystemGame } }
                    val userGames = remember(filteredApps) { filteredApps.filter { it.isSelectedByUser && !it.isSystemGame } }
                    val otherApps = remember(filteredApps) { filteredApps.filter { !it.isSystemGame && !it.isSelectedByUser } }
                    val remainingApps = remember(userGames, otherApps) { userGames + otherApps }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("apps_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (activeFilter == "Games") {
                            if (detectedGames.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "ألعاب تم رصدها تلقائياً",
                                        color = Color(0xFFCAF02A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(detectedGames, key = { "detected_" + it.packageName }) { app ->
                                    AppSelectorItemRowWidget(
                                        app = app,
                                        onToggleSelection = { isSelected ->
                                            viewModel.toggleAppSelection(app.packageName, isSelected)
                                        }
                                    )
                                }
                            }

                            if (userGames.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "ألعاب تمت إضافتها",
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                    )
                                }
                                items(userGames, key = { "user_" + it.packageName }) { app ->
                                    AppSelectorItemRowWidget(
                                        app = app,
                                        onToggleSelection = { isSelected ->
                                            viewModel.toggleAppSelection(app.packageName, isSelected)
                                        }
                                    )
                                }
                            }

                            if (detectedGames.isEmpty() && userGames.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 60.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "لا توجد ألعاب مضافة أو مكتشفة",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            if (detectedGames.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "الألعاب المكتشفة",
                                        color = Color(0xFFCAF02A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(detectedGames, key = { "all_detected_" + it.packageName }) { app ->
                                    AppSelectorItemRowWidget(
                                        app = app,
                                        onToggleSelection = { isSelected ->
                                            viewModel.toggleAppSelection(app.packageName, isSelected)
                                        }
                                    )
                                }
                            }

                            if (remainingApps.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "باقي التطبيقات",
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                    )
                                }
                                items(remainingApps, key = { "all_other_" + it.packageName }) { app ->
                                    AppSelectorItemRowWidget(
                                        app = app,
                                        onToggleSelection = { isSelected ->
                                            viewModel.toggleAppSelection(app.packageName, isSelected)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppSelectorItemRowWidget(
    app: InstalledApp,
    onToggleSelection: (Boolean) -> Unit
) {
    val isChecked = app.isSelectedByUser || app.isSystemGame

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111116).copy(alpha = 0.7f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isChecked) Color(0xFFCAF02A).copy(alpha = 0.6f) else Color(0xFF1C1C24),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onToggleSelection(!isChecked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // App Logo Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F1219)),
                    contentAlignment = Alignment.Center
                ) {
                    if (app.icon != null) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    setImageDrawable(app.icon)
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.VideogameAsset,
                            contentDescription = null,
                            tint = Color(0xFFCAF02A)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = app.appName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (app.isSystemGame) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFCAF02A).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = Color(0xFFCAF02A),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "لعبة رُصدت تلقائياً",
                                color = Color(0xFFCAF02A),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggleSelection(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFCAF02A),
                    checkmarkColor = Color.Black,
                    uncheckedColor = Color.Gray
                ),
                enabled = !app.isSystemGame // system recognized games are locked active
            )
        }
    }
}
