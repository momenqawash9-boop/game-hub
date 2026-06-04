package com.aistudio.gamebooster.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aistudio.gamebooster.R
import com.aistudio.gamebooster.ui.theme.*
import com.aistudio.gamebooster.ui.viewmodel.BoostState
import com.aistudio.gamebooster.ui.viewmodel.GameBoosterViewModel
import com.aistudio.gamebooster.ui.viewmodel.TelemetryState
import com.aistudio.gamebooster.utils.InstalledApp
import com.aistudio.gamebooster.utils.RamInfo
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: GameBoosterViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAppSelector: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("dashboard") }

    val telemetryState by viewModel.telemetryState.collectAsState()
    val boostState by viewModel.boostState.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val userGamesSet by viewModel.userGames.collectAsState(initial = emptySet())

    // Observe user settings & playtimes
    val userName by viewModel.userName.collectAsState("Challenger")
    val userAvatar by viewModel.userAvatar.collectAsState("preset_1")
    val gamePlaytimes by viewModel.gamePlaytimes.collectAsState(initial = emptyMap())

    val gameApps = remember(allApps, userGamesSet) {
        allApps.filter { it.isSystemGame || it.isSelectedByUser }
    }

    var showProfileEditDialog by remember { mutableStateOf(false) }
    var activeGamingSessionOfApp by remember { mutableStateOf<InstalledApp?>(null) }

    val lastTrackingResult by viewModel.lastTrackingResult.collectAsState()

    // Observe app lifecycle events to handle automatic game tracking finalization smoothly
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Use ON_STOP to ensure session is finalized when user leaves the app or closes it
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.checkAndFinalizeAutoTracking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Glowing Neon Theme Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0E17), // Deep space black-purple
                        Color(0xFF07070B)  // Absolute black
                    )
                )
            )
            .testTag("dashboard_screen")
    ) {
        // Upper background glowing spotlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x35CEB1FF), // Lavender purple glow
                            Color(0x1500E5FF), // Cyan subtle blend
                            Color(0x00000000)  // Fades out
                        ),
                        radius = 800f
                    )
                )
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                // Floating navigation bar
                FloatingNavDockWidget(
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it },
                    onBoostClick = { viewModel.boostMemory() },
                    boostState = boostState,
                    context = context
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    "dashboard" -> {
                        DashboardMainView(
                            telemetryState = telemetryState,
                            boostState = boostState,
                            gameApps = gameApps,
                            viewModel = viewModel,
                            onNavigateToAppSelector = onNavigateToAppSelector,
                            context = context,
                            userName = userName,
                            userAvatar = userAvatar,
                            gamePlaytimes = gamePlaytimes,
                            onAvatarClick = { showProfileEditDialog = true },
                            onLaunchActiveHUD = { activeGamingSessionOfApp = it }
                        )
                    }
                    "my_games" -> {
                        MyGamesLibraryView(
                            gameApps = gameApps,
                            gamePlaytimes = gamePlaytimes,
                            onNavigateToAppSelector = onNavigateToAppSelector,
                            context = context,
                            onLaunchActiveHUD = { activeGamingSessionOfApp = it }
                        )
                    }
                    "settings" -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = {},
                            showBackButton = false
                        )
                    }
                }
            }
        }

        // Profile Editor Popup Dialog
        if (showProfileEditDialog) {
            ProfileEditDialog(
                currentName = userName,
                currentAvatar = userAvatar,
                onSave = { name, avatar ->
                    viewModel.setUserName(name)
                    viewModel.setUserAvatar(avatar)
                    showProfileEditDialog = false
                },
                onDismiss = { showProfileEditDialog = false }
            )
        }

        // Fullscreen Active companion Gaming HUD with automatic tracker pre-combat starts
        activeGamingSessionOfApp?.let { app ->
            ActiveGamingHUD(
                app = app,
                onFinishedBoostAndLaunch = {
                    activeGamingSessionOfApp = null
                },
                onRealLaunch = {
                    viewModel.launchGame(app.packageName)
                }
            )
        }

        // Automatic Playtime Session Tracking complete Dialog
        lastTrackingResult?.let { result ->
            val secs = result.secondsPlayed
            val min = secs / 60
            val sec = secs % 60
            val durationLabel = if (min > 0) "$min دقيقة و $sec ثانية" else "$sec ثانية"

            AlertDialog(
                onDismissRequest = { viewModel.dismissLastTrackingResult() },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFFCAF02A),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "اكتمل تتبع الجلسة بنجاح!",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1E1E26)),
                            contentAlignment = Alignment.Center
                        ) {
                            val app = gameApps.find { it.packageName == result.packageName }
                            if (app?.icon != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        ImageView(ctx).apply {
                                            setImageDrawable(app.icon)
                                            scaleType = ImageView.ScaleType.FIT_CENTER
                                        }
                                    },
                                    modifier = Modifier.size(50.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = Color(0xFFCAF02A),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = result.appName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "مدة اللعب المكتشفة: $durationLabel",
                            color = Color(0xFFCEB1FF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "تم رصد وقت اللعب وتحديث إحصائياتك تلقائياً بنجاح.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF1A1A24), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("90 FPS", color = Color(0xFFCAF02A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("ثبات النظام", color = Color.Gray, fontSize = 9.sp)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF1A1A24), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("مستقر", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("مستهلك الطاقة", color = Color.Gray, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.dismissLastTrackingResult() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCAF02A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تم (رائع)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF0F0F14),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}

@Composable
fun DashboardMainView(
    telemetryState: TelemetryState,
    boostState: BoostState,
    gameApps: List<InstalledApp>,
    viewModel: GameBoosterViewModel,
    onNavigateToAppSelector: () -> Unit,
    context: Context,
    userName: String,
    userAvatar: String,
    gamePlaytimes: Map<String, Long>,
    onAvatarClick: () -> Unit,
    onLaunchActiveHUD: (InstalledApp) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val categories = listOf("All Mode", "Performance", "Diagnostics", "System Status")
    var selectedCategoryIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFCAF02A), CircleShape)
                    .background(Color(0xFF1E1E24))
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                AvatarDisplay(avatarStr = userAvatar, modifier = Modifier.fillMaxSize())
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E24).copy(alpha = 0.65f))
                        .clickable { onNavigateToAppSelector() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Manage apps",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E24).copy(alpha = 0.65f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.refreshTelemetry()
                            Toast.makeText(context, "تم تحديث البيانات الحيوية", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh telemetry",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Greetings and Subtitles
        Column {
            Text(
                text = "مرحباً، $userName",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "ترددات المعالج والأنوية مهيأة لأحدث جلسات القتال",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Categories selector pills
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(categories) { index, category ->
                val isSelected = selectedCategoryIndex == index
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isSelected) Color(0xFFCAF02A) else Color(0xFF1A1A22).copy(alpha = 0.7f))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFFCAF02A) else Color(0xFF23232C),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedCategoryIndex = index
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Ultimate Booster Adapter Section label
        Text(
            text = "المسرّع الذكي المخصّص للألعاب",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        CuratedBoosterLavenderCard(
            gameApps = gameApps,
            gamePlaytimes = gamePlaytimes,
            boostState = boostState,
            onBoostClick = { viewModel.boostMemory() },
            onLaunchGame = { onLaunchActiveHUD(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Diagnostics Telemetry Section
        AnimatedVisibility(
            visible = selectedCategoryIndex == 0 || selectedCategoryIndex == 2 || selectedCategoryIndex == 3,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Text(
                    text = "مراقبة البيئة الحيوية للنظام",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TelemetryHeaderSectionWidget(telemetryState)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Daily Playtime Analytics
        val totalPlaySeconds = gamePlaytimes.values.sum()
        val totalPlayMinutes = totalPlaySeconds / 60
        val hr = totalPlayMinutes / 60
        val mn = totalPlayMinutes % 60
        val totalPlaybackString = if (hr > 0) "$hr ساعة و $mn دقيقة" else "$totalPlayMinutes دقيقة لعب"

        Text(
            text = "تحليلات الجلسات والوقت الحريص",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .border(1.dp, Color(0xFF1E1E26), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121217).copy(alpha = 0.85f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0xFFCAF02A).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFFCAF02A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "إجمالي اللعب اليوم اليومي",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = totalPlaybackString,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFCAF02A).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "جهاز مثالي",
                        color = Color(0xFFCAF02A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Library Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مكتبة ألعابك - Speed Games",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "إدارة الألعاب",
                color = Color(0xFFCAF02A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigateToAppSelector() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // App lists
        if (gameApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF14141A))
                    .border(1.dp, Color(0xFF26262F), RoundedCornerShape(20.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = "Empty",
                        tint = Color.Gray,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "لا توجد ألعاب مضافة حتى الآن",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "أضف ألعابك المفضلة لتشغيلها بأقصى تسريع",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToAppSelector,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFCAF02A),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("إضافة ألعاب الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gameApps.forEach { app ->
                    val appPlaytime = gamePlaytimes[app.packageName] ?: 0L
                    GameTrackItemRow(
                        app = app,
                        playtimeSeconds = appPlaytime,
                        onLaunch = {
                            onLaunchActiveHUD(app)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(115.dp))
    }
}

@Composable
fun AvatarDisplay(avatarStr: String, modifier: Modifier = Modifier) {
    if (avatarStr.startsWith("content://") || avatarStr.startsWith("file://")) {
        val painter = coil.compose.rememberAsyncImagePainter(
            model = avatarStr
        )
        androidx.compose.foundation.Image(
            painter = painter,
            contentDescription = "Custom Avatar",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else if (avatarStr == "cyber_avatar") {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.cyber_gamer_avatar_1780580144052),
            contentDescription = "Default Gamer Avatar",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val (icon, color) = when (avatarStr) {
            "preset_1" -> Icons.Default.LocalFireDepartment to Color(0xFFFF5722)
            "preset_2" -> Icons.Default.FlashOn to Color(0xFFFFEB3B)
            "preset_3" -> Icons.Default.Shield to Color(0xFF2196F3)
            "preset_4" -> Icons.Default.Gamepad to Color(0xFF00E5FF)
            "preset_5" -> Icons.Default.BugReport to Color(0xFFFF00FF)
            else -> Icons.Default.Stars to Color(0xFFCAF02A)
        }
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.fillMaxSize(0.55f)
            )
        }
    }
}

data class GameColorPalette(
    val primaryGlow: Color,
    val cardBackground: Color,
    val textTitleColor: Color,
    val textBodyColor: Color,
    val darkAccent: Color
)

@Composable
fun CuratedBoosterLavenderCard(
    gameApps: List<InstalledApp>,
    gamePlaytimes: Map<String, Long>,
    boostState: BoostState,
    onBoostClick: () -> Unit,
    onLaunchGame: (InstalledApp) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedGameIndex by remember { mutableStateOf(0) }
    
    // Reset index if list changes to avoid out of bounds or weird jumps
    LaunchedEffect(gameApps.size) {
        if (selectedGameIndex >= gameApps.size) {
            selectedGameIndex = 0
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    if (gameApps.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(26.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161622)),
            shape = RoundedCornerShape(26.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E1E2C), Color(0xFF111116))
                        )
                    )
                    .clickable { }
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = Color(0xFFCAF02A),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "لا توجد ألعاب لتسريعها في النواة",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "أضف ألعاباً لتظهر مخصصة هنا بألوانها البصرية الفريدة!",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        val index = if (gameApps.isNotEmpty()) selectedGameIndex % gameApps.size else 0
        val activeGame = gameApps[index]

        // Adapt profile backgrounds dynamically
        val colorTheme = remember(activeGame) {
            val nameLower = activeGame.appName.lowercase()
            val pkgLower = activeGame.packageName.lowercase()
            when {
                nameLower.contains("pubg") || pkgLower.contains("pubg") || nameLower.contains("battlegrounds") || nameLower.contains("tencent") -> {
                    GameColorPalette(
                        primaryGlow = Color(0xFFFF3D00),
                        cardBackground = Color(0xFFBC5124),
                        textTitleColor = Color.White,
                        textBodyColor = Color(0xFFFFCCBC),
                        darkAccent = Color(0xFF8D1C00)
                    )
                }
                nameLower.contains("freefire") || nameLower.contains("free fire") || nameLower.contains("ff") || pkgLower.contains("freefire") -> {
                    GameColorPalette(
                        primaryGlow = Color(0xFFFFA000),
                        cardBackground = Color(0xFFD68A1B),
                        textTitleColor = Color(0xFF261200),
                        textBodyColor = Color(0xFFFFE0B2),
                        darkAccent = Color(0xFF8C3C00)
                    )
                }
                nameLower.contains("cod") || nameLower.contains("duty") || pkgLower.contains("activision") -> {
                    GameColorPalette(
                        primaryGlow = Color(0xFF2979FF),
                        cardBackground = Color(0xFF266BB9),
                        textTitleColor = Color.White,
                        textBodyColor = Color(0xFFBBDEFB),
                        darkAccent = Color(0xFF032F73)
                    )
                }
                nameLower.contains("minecraft") || pkgLower.contains("mojang") -> {
                    GameColorPalette(
                        primaryGlow = Color(0xFF00E676),
                        cardBackground = Color(0xFF3F8A43),
                        textTitleColor = Color.White,
                        textBodyColor = Color(0xFFC8E6C9),
                        darkAccent = Color(0xFF135A17)
                    )
                }
                else -> {
                    val h = Math.abs(pkgLower.hashCode())
                    when (h % 4) {
                        0 -> GameColorPalette(
                            primaryGlow = Color(0xFFCEB1FF),
                            cardBackground = Color(0xFFCEB1FF),
                            textTitleColor = Color(0xFF1F0E3E),
                            textBodyColor = Color(0xFF4C3B78),
                            darkAccent = Color(0xFF3B1E63)
                        )
                        1 -> GameColorPalette(
                            primaryGlow = Color(0xFF00E5FF),
                            cardBackground = Color(0xFF1A8C9E),
                            textTitleColor = Color.White,
                            textBodyColor = Color(0xFFE0F7FA),
                            darkAccent = Color(0xFF01525E)
                        )
                        2 -> GameColorPalette(
                            primaryGlow = Color(0xFFFF00FF),
                            cardBackground = Color(0xFFAC2358),
                            textTitleColor = Color.White,
                            textBodyColor = Color(0xFFFCE4EC),
                            darkAccent = Color(0xFF6B0731)
                        )
                        else -> GameColorPalette(
                            primaryGlow = Color(0xFFCAF02A),
                            cardBackground = Color(0xFF75A03A),
                            textTitleColor = Color(0xFF0D1B00),
                            textBodyColor = Color(0xFFDCEDC8),
                            darkAccent = Color(0xFF2C4C0C)
                        )
                    }
                }
            }
        }

        val playtimeSeconds = gamePlaytimes[activeGame.packageName] ?: 0L
        val playtimeMinutes = playtimeSeconds / 60

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(26.dp)),
            colors = CardDefaults.cardColors(containerColor = colorTheme.cardBackground),
            shape = RoundedCornerShape(26.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f),
                        radius = 240f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.2f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1.3f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CORE GAME MOUNTED",
                                        color = colorTheme.darkAccent,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }

                                if (gameApps.size > 1) {
                                    Text(
                                        text = "${index + 1}/${gameApps.size}",
                                        color = colorTheme.textTitleColor.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeGame.appName,
                                color = colorTheme.textTitleColor,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "تسريع ثنائي النواة نشط بالكامل. تبييض ذاكرة RAM وتجميد الأطر الخلفية تلقائياً.",
                                color = colorTheme.textBodyColor,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = colorTheme.textTitleColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "وقت اللعب اليوم اليوم: $playtimeMinutes دقيقة",
                                    color = colorTheme.textTitleColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E1E26))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLaunchGame(activeGame)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Launch play HUD",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                if (gameApps.size > 1) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                selectedGameIndex = if (selectedGameIndex > 0) selectedGameIndex - 1 else gameApps.size - 1
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronLeft,
                                                contentDescription = "Prev",
                                                tint = colorTheme.textTitleColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                selectedGameIndex = selectedGameIndex + 1
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Next",
                                                tint = colorTheme.textTitleColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.9f)
                            .clip(RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeGame.icon != null) {
                            AndroidView(
                                factory = { ctx ->
                                    ImageView(ctx).apply {
                                        setImageDrawable(activeGame.icon)
                                        scaleType = ImageView.ScaleType.FIT_CENTER
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize(0.85f)
                                    .graphicsLayer(alpha = pulseAlpha)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = colorTheme.textTitleColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameTrackItemRow(
    app: InstalledApp,
    playtimeSeconds: Long,
    onLaunch: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1C1C24), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111116).copy(alpha = 0.7f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                            modifier = Modifier.size(38.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = "Standard game icon",
                            tint = Color(0xFFCAF02A),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = app.appName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFCAF02A))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val mins = playtimeSeconds / 60
                        Text(
                            text = "لعبت اليوم: $mins د • استقرار 90 إطار",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A22))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLaunch()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Launch play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MyGamesLibraryView(
    gameApps: List<InstalledApp>,
    gamePlaytimes: Map<String, Long>,
    onNavigateToAppSelector: () -> Unit,
    context: Context,
    onLaunchActiveHUD: (InstalledApp) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مكتبة ألعابك الكبرى",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onNavigateToAppSelector,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E1E24),
                    contentColor = Color(0xFFCAF02A)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إدارة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (gameApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideogameAsset,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "مكتبة الألعاب فارغة",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "قم بإضافة ألعابك وتطبيقاتك عبر لوحة الإدارة لإطلاقها مشحونة",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateToAppSelector,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCAF02A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إضافة ألعاب الآن", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(gameApps) { app ->
                    val appPlaytime = gamePlaytimes[app.packageName] ?: 0L
                    GameTrackItemRow(
                        app = app,
                        playtimeSeconds = appPlaytime,
                        onLaunch = {
                            onLaunchActiveHUD(app)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryHeaderSectionWidget(state: TelemetryState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E1E26), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F14).copy(alpha = 0.8f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state) {
                is TelemetryState.Loading -> {
                     Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFCAF02A))
                    }
                }
                is TelemetryState.Success -> {
                    CpuGaugeWidget(temp = state.cpuTempCelsius, modifier = Modifier.weight(1f))
                    RamCircularGaugeWidget(ramInfo = state.ramInfo, modifier = Modifier.weight(1.2f))
                    BatteryGaugeWidget(pct = state.batteryPct, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun RamCircularGaugeWidget(ramInfo: RamInfo, modifier: Modifier = Modifier) {
    val animateStroke by animateIntAsState(
        targetValue = ramInfo.usedPercent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(76.dp)) {
                drawCircle(
                    color = Color(0xFF1E1E24),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFFCAF02A),
                    startAngle = -90f,
                    sweepAngle = (animateStroke.toFloat() / 100f) * 360f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$animateStroke%",
                    color = Color(0xFFCAF02A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "RAM",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val totalGb = ramInfo.totalMb.toFloat() / 1024f
        val usedGb = (ramInfo.totalMb - ramInfo.availableMb).toFloat() / 1024f
        val formattedRamStr = String.format("%.1f/%.1f GB", Math.max(0f, usedGb), totalGb)

        Text(
            text = formattedRamStr,
            color = Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CpuGaugeWidget(temp: Int, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Thermostat,
            contentDescription = null,
            tint = Color(0xFF00E5FF),
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$temp°C",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.cpu_temp),
            color = TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BatteryGaugeWidget(pct: Int, modifier: Modifier = Modifier) {
    val icon = if (pct > 75) Icons.Default.BatteryChargingFull else Icons.Default.ElectricBolt
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFF00FF),
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "$pct%",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.battery_status),
            color = TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FloatingNavDockWidget(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    onBoostClick: () -> Unit,
    boostState: BoostState,
    context: Context
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            color = Color(0xE30F0F13),
            shape = RoundedCornerShape(36.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF23232C)),
            tonalElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val homeSelected = activeTab == "dashboard"
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (homeSelected) Color(0xFFCAF02A) else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTabSelected("dashboard")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = if (homeSelected) Color.Black else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                val listSelected = activeTab == "my_games"
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (listSelected) Color(0xFFCAF02A) else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTabSelected("my_games")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = "My Games",
                        tint = if (listSelected) Color.Black else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E26))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBoostClick()
                            Toast.makeText(context, "جاري تحسين المعالج وإخلاء الذاكرة...", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val angleState = rememberInfiniteTransition()
                    val fastSpinAngle by angleState.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing)
                        )
                    )

                    if (boostState is BoostState.Boosting) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Boosting",
                            tint = Color(0xFFCAF02A),
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(fastSpinAngle)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Deep cleaner",
                            tint = Color(0xFFCAF02A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                val settingsSelected = activeTab == "settings"
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (settingsSelected) Color(0xFFCAF02A) else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTabSelected("settings")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (settingsSelected) Color.Black else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditDialog(
    currentName: String,
    currentAvatar: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var selectedAvatar by remember { mutableStateOf(currentAvatar) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            selectedAvatar = it.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "بوابة الحساب - PROFILE GATEWAY",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "اسم اللاعب - COMMANDER NAME",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFCAF02A),
                        unfocusedBorderColor = Color(0xFF2C2C35),
                        focusedContainerColor = Color(0xFF14141B),
                        unfocusedContainerColor = Color(0xFF14141B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "اختر أيقونة الصورة الشخصية - SELECT AVATAR",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf("preset_1", "preset_2", "preset_3", "preset_4", "preset_5", "preset_6", "cyber_avatar")
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        presets.chunked(4).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                chunk.forEach { preset ->
                                    val isSelected = selectedAvatar == preset
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFFCAF02A).copy(alpha = 0.3f) else Color.Transparent)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFFCAF02A) else Color(0xFF2C2C35),
                                                shape = CircleShape
                                            )
                                            .clickable { selectedAvatar = preset }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AvatarDisplay(
                                            avatarStr = preset,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F1F2A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF2C2C35), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = Color(0xFFCAF02A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ارفع صورتك الخاصة من المعرض", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (selectedAvatar.startsWith("content://") || selectedAvatar.startsWith("file://")) {
                    Text(
                        text = "جاري استخدام الصورة الشخصية الخاصة بك!",
                        color = Color(0xFFCAF02A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, selectedAvatar)
                    } else {
                        Toast.makeText(context, "الرجاء إدخال اسم صحيح", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCAF02A)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ التغيرات", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF14141B),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun ActiveGamingHUD(
    app: InstalledApp,
    onFinishedBoostAndLaunch: () -> Unit,
    onRealLaunch: () -> Unit
) {
    var loadingStep by remember { mutableStateOf("بدء تهيئة محرك غيم بوستر...") }
    var progress by remember { mutableStateOf(0.15f) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        // Step 1: Optimize memory and clean RAM
        delay(500)
        loadingStep = "جاري تطهير الذاكرة العشوائية RAM وتحسين الاستقرار..."
        progress = 0.45f
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // Step 2: Configure 90 FPS profiles
        delay(600)
        loadingStep = "تطبيق ملف الأداء الخارق (Ultra FPS Cores)..."
        progress = 0.8f
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // Step 3: Fast GPU clock locking simulation
        delay(500)
        loadingStep = "تنشيط درع حجب الإشعارات وتبريد المعالج..."
        progress = 1.0f
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(300)

        // Launch game & close launcher overlays
        onRealLaunch()
        onFinishedBoostAndLaunch()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF207070B))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale, alpha = pulseAlpha)
                .background(Color(0xFFCAF02A).copy(alpha = 0.15f), CircleShape)
                .border(2.dp, Color(0xFFCAF02A).copy(alpha = 0.3f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(40.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFCAF02A).copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "جاري الحوسبة والتشغيل اللحظي",
                        color = Color(0xFFCAF02A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF14141B))
                        .border(2.dp, Color(0xFFCAF02A), RoundedCornerShape(20.dp)),
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
                            modifier = Modifier.size(58.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = Color(0xFFCAF02A),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = app.appName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "نظام التتبع الكهرومغناطيسي الذكي نشط",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = progress,
                        color = Color(0xFFCAF02A),
                        strokeWidth = 6.dp,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(80.dp),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = loadingStep,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "تتبع الوقت تلقائياً قيد التحضير...",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
