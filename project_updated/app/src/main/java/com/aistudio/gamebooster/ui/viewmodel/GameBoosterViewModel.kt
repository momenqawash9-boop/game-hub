package com.aistudio.gamebooster.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.gamebooster.data.SettingsRepository
import com.aistudio.gamebooster.utils.GameBoostManager
import com.aistudio.gamebooster.utils.InstalledApp
import com.aistudio.gamebooster.utils.RamInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface TelemetryState {
    object Loading : TelemetryState
    data class Success(
        val ramInfo: RamInfo,
        val batteryPct: Int,
        val cpuTempCelsius: Int
    ) : TelemetryState
}

sealed interface BoostState {
    object Idle : BoostState
    object Boosting : BoostState
    data class Success(val freedMemoryMb: Long) : BoostState
}

class GameBoosterViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val repository = SettingsRepository(context)

    // Telemetry Telecommunications
    private val _telemetryState = MutableStateFlow<TelemetryState>(TelemetryState.Loading)
    val telemetryState: StateFlow<TelemetryState> = _telemetryState.asStateFlow()

    // Boosting System State
    private val _boostState = MutableStateFlow<BoostState>(BoostState.Idle)
    val boostState: StateFlow<BoostState> = _boostState.asStateFlow()

    // Apps loading states
    private val _allApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val allApps: StateFlow<List<InstalledApp>> = _allApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    // Preferences states
    val performanceMode = repository.performanceModeFlow
    val dndMode = repository.dndModeFlow
    val brightnessLock = repository.brightnessLockFlow
    val brightnessLevel = repository.brightnessLevelFlow
    val floatingWidgetEnabled = repository.floatingWidgetFlow
    val userGames = repository.userGamesFlow
    val userName = repository.userNameFlow
    val userAvatar = repository.userAvatarFlow
    val gamePlaytimes = repository.gamePlaytimesFlow

    fun setUserName(name: String) {
        viewModelScope.launch { repository.setUserName(name) }
    }

    fun setUserAvatar(avatar: String) {
        viewModelScope.launch { repository.setUserAvatar(avatar) }
    }

    fun addPlaytime(packageName: String, seconds: Long) {
        viewModelScope.launch { repository.addPlaytime(packageName, seconds) }
    }

    init {
        refreshTelemetry()
        loadInstalledApps()
        
        // Start live monitoring loop
        viewModelScope.launch {
            while (true) {
                delay(4000)
                updateLiveTelemetryOnly()
            }
        }
    }

    fun refreshTelemetry() {
        viewModelScope.launch {
            _telemetryState.value = TelemetryState.Loading
            delay(400) // Beautiful cybernetic loader lag
            updateLiveTelemetryOnly()
        }
    }

    private fun updateLiveTelemetryOnly() {
        val ram = GameBoostManager.getRamInfo(context)
        val batt = GameBoostManager.getBatteryLevel(context)
        val temp = GameBoostManager.getCpuTemp(context)
        _telemetryState.value = TelemetryState.Success(ram, batt, temp)
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val savedGames = repository.userGamesFlow.first()
            val apps = GameBoostManager.getInstalledApps(context, savedGames)
            _allApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Toggle app status (game / regular app)
    fun toggleAppSelection(packageName: String, isSelected: Boolean) {
        viewModelScope.launch {
            if (isSelected) {
                repository.addGame(packageName)
            } else {
                repository.removeGame(packageName)
            }
            
            // Re-map localized memory listing
            val updatedGames = repository.userGamesFlow.first()
            _allApps.value = _allApps.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isSelectedByUser = isSelected)
                } else app
            }
        }
    }

    // Manual instant memory booster function
    fun boostMemory() {
        viewModelScope.launch {
            _boostState.value = BoostState.Boosting
            delay(1500) // Simulated immersive sweep animation
            val freed = GameBoostManager.killBackgroundProcesses(context)
            _boostState.value = BoostState.Success(freed)
            
            // Refresh Telemetry after optimization
            val ram = GameBoostManager.getRamInfo(context)
            val batt = GameBoostManager.getBatteryLevel(context)
            val temp = GameBoostManager.getCpuTemp(context)
            _telemetryState.value = TelemetryState.Success(ram, batt, temp)
            
            delay(2500)
            _boostState.value = BoostState.Idle
        }
    }

    // Tracking active session state
    private val _autoTrackingSession = MutableStateFlow<AutoTrackingSession?>(null)
    val autoTrackingSession: StateFlow<AutoTrackingSession?> = _autoTrackingSession.asStateFlow()

    private val _lastTrackingResult = MutableStateFlow<TrackingResult?>(null)
    val lastTrackingResult: StateFlow<TrackingResult?> = _lastTrackingResult.asStateFlow()

    fun launchGame(packageName: String) {
        viewModelScope.launch {
            val app = _allApps.value.find { it.packageName == packageName }
            val appName = app?.appName ?: packageName

            // Auto booster if Performance Mode enabled
            val autoBoost = performanceMode.first()
            val floatEnabled = floatingWidgetEnabled.first()

            if (autoBoost) {
                GameBoostManager.killBackgroundProcesses(context)
            }

            // Set auto-tracking session start parameters
            _autoTrackingSession.value = AutoTrackingSession(
                packageName = packageName,
                appName = appName,
                startTimeMillis = System.currentTimeMillis()
            )

            GameBoostManager.launchGame(
                context = context,
                packageName = packageName,
                startFloatingOverlay = floatEnabled
            )
        }
    }

    fun checkAndFinalizeAutoTracking() {
        val session = _autoTrackingSession.value ?: return
        val endTimeMillis = System.currentTimeMillis()
        val durationMillis = endTimeMillis - session.startTimeMillis
        val durationSeconds = durationMillis / 1000

        // Reset tracking session immediately to avoid double checking
        _autoTrackingSession.value = null

        // Save the session if the user opened it for at least 3 seconds
        if (durationSeconds >= 3) {
            viewModelScope.launch {
                repository.addPlaytime(session.packageName, durationSeconds)
                _lastTrackingResult.value = TrackingResult(
                    appName = session.appName,
                    packageName = session.packageName,
                    secondsPlayed = durationSeconds
                )
            }
        }
    }

    fun dismissLastTrackingResult() {
        _lastTrackingResult.value = null
    }

    fun setPerformanceMode(enabled: Boolean) {
        viewModelScope.launch { repository.setPerformanceMode(enabled) }
    }

    fun setDndMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDndMode(enabled) }
    }

    fun setBrightnessLock(enabled: Boolean) {
        viewModelScope.launch { repository.setBrightnessLock(enabled) }
    }

    fun setBrightnessLevel(level: Float) {
        viewModelScope.launch { repository.setBrightnessLevel(level) }
    }

    fun setFloatingWidgetEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setFloatingWidget(enabled) }
    }
}

data class AutoTrackingSession(
    val packageName: String,
    val appName: String,
    val startTimeMillis: Long
)

data class TrackingResult(
    val appName: String,
    val packageName: String,
    val secondsPlayed: Long
)

