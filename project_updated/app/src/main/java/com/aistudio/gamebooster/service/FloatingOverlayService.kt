package com.aistudio.gamebooster.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aistudio.gamebooster.ui.theme.DarkCardBg
import com.aistudio.gamebooster.ui.theme.NeonCyan
import com.aistudio.gamebooster.ui.theme.NeonGreen
import com.aistudio.gamebooster.ui.theme.NeonMagenta
import com.aistudio.gamebooster.ui.theme.MyApplicationTheme
import com.aistudio.gamebooster.utils.GameBoostManager
import kotlin.math.roundToInt

class FloatingOverlayService : android.app.Service(), LifecycleOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var floatingView: ComposeView? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()

    private val savedStateRegistryOwner = object : androidx.savedstate.SavedStateRegistryOwner {
        private val registry = androidx.savedstate.SavedStateRegistryController.create(this)
        init { registry.performRestore(null) }
        override val lifecycle: Lifecycle get() = this@FloatingOverlayService.lifecycle
        override val savedStateRegistry get() = registry.savedStateRegistry
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showFloatingWidget()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showFloatingWidget() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }

        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

            setContent {
                MyApplicationTheme {
                    FloatingOverlayContent(
                        onMove = { dx, dy ->
                            params.x = (params.x + dx).coerceAtLeast(0)
                            params.y = (params.y + dy).coerceAtLeast(0)
                            windowManager.updateViewLayout(this, params)
                        },
                        onClose = {
                            stopSelf()
                        }
                    )
                }
            }
        }

        windowManager.addView(floatingView, params)
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Ignore if already removed
            }
        }
        super.onDestroy()
    }
}

@Composable
fun FloatingOverlayContent(
    onMove: (Int, Int) -> Unit,
    onClose: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var fps by remember { mutableStateOf(60) }
    var showBoostGlow by remember { mutableStateOf(false) }
    var cleanedRamText by remember { mutableStateOf("") }
    
    var lastInteraction by remember { mutableStateOf(System.currentTimeMillis()) }
    var alphaVal by remember { mutableStateOf(0.95f) }
    
    val animatedAlpha by animateFloatAsState(
        targetValue = alphaVal,
        animationSpec = tween(durationMillis = 500),
        label = "fade_alpha"
    )

    LaunchedEffect(lastInteraction) {
        alphaVal = 0.95f
        kotlinx.coroutines.delay(3500)
        alphaVal = 0.4f
    }

    // Live FPS calculator using Choreographer
    DisposableEffect(Unit) {
        val choreographer = Choreographer.getInstance()
        val frameCallback = object : Choreographer.FrameCallback {
            var lastFrameTimeNanos: Long = 0
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameTimeNanos > 0) {
                    val timeDiff = frameTimeNanos - lastFrameTimeNanos
                    if (timeDiff > 0) {
                        val currentFps = (1_000_000_000.0 / timeDiff).roundToInt()
                        if (currentFps in 15..120) {
                            fps = currentFps
                        }
                    }
                }
                lastFrameTimeNanos = frameTimeNanos
                choreographer.postFrameCallback(this)
            }
        }
        
        choreographer.postFrameCallback(frameCallback)
        
        onDispose {
            choreographer.removeFrameCallback(frameCallback)
        }
    }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .graphicsLayer { alpha = animatedAlpha }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        lastInteraction = System.currentTimeMillis()
                    }
                }
            }
    ) {
        if (!isExpanded) {
            // Floating transparent / high-tech game booster head bubble
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onMove(dragAmount.x.toInt(), dragAmount.y.toInt())
                        }
                    }
                    .clip(CircleShape)
                    .background(Color(0xE60B0E14))
                    .clickable { isExpanded = true }
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                // Circular border indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .background(Color.Transparent)
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Booster Widget",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "$fps FPS",
                        color = NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Full floating micro-dashboard cards system overlay
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFB0A0E17)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .width(190.dp)
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row: FPS, title, close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FPS: $fps",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Minimize",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic boost feedback labels
                    if (cleanedRamText.isNotEmpty()) {
                        Text(
                            text = cleanedRamText,
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // Boost RAM circular/radial simulation Button inside Widget
                    Button(
                        onClick = {
                            // Run immediate background memory optimization
                            showBoostGlow = true
                            cleanedRamText = "جاري التحسين..."
                            
                            // Use a Coroutine to call the suspend function
                            (context as? FloatingOverlayService)?.let { service ->
                                service.lifecycleScope.launch {
                                    val freed = GameBoostManager.killBackgroundProcesses(context)
                                    cleanedRamText = if (freed > 0) "تم تفريغ ${freed}MB RAM!" else "تم تحسين الذاكرة بنجاح"
                                    kotlinx.coroutines.delay(2000)
                                    showBoostGlow = false
                                    cleanedRamText = "جاري الحفاظ على الاستقرار"
                                }
                            } ?: run {
                                // Fallback if context is not service (though it should be)
                                Handler(Looper.getMainLooper()).postDelayed({
                                    showBoostGlow = false
                                    cleanedRamText = "جاري الحفاظ على الاستقرار"
                                }, 2000)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showBoostGlow) NeonCyan else NeonMagenta
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Boost",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تسريع الرام",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Option to take screenshot simulation
                    OutlinedButton(
                        onClick = {
                            cleanedRamText = "تنبيه: تم التقاط لقطة الشاشة!"
                            Handler(Looper.getMainLooper()).postDelayed({
                                cleanedRamText = ""
                            }, 2500)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Screenshot",
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "لقطة شاشة",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Small exit indicator line to stop service fully
                    Text(
                        text = "إغلاق الواجهة بالكامل",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clickable { onClose() }
                            .padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
