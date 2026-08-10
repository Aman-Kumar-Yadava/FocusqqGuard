package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.entities.AppSettingsEntity
import com.example.data.db.entities.DailyUsageEntity
import com.example.data.db.entities.ProtectedAppEntity
import com.example.data.manager.DevicePolicyManagerWrapper
import com.example.data.manager.OverlayManager
import com.example.data.manager.UsageTrackingManager
import com.example.data.repository.FocusGuardRepository
import com.example.service.GamingAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HealthStatusState(
    val hasUsageAccess: Boolean = false,
    val hasAccessibility: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val isAdminActive: Boolean = false
)

data class DashboardUiState(
    val todayDateString: String = "",
    val protectedApps: List<ProtectedAppEntity> = emptyList(),
    val totalUsedSeconds: Long = 0L,
    val totalLimitSeconds: Long = 120 * 60L,
    val remainingSeconds: Long = 0L,
    val sessionCount: Int = 0,
    val isBlocked: Boolean = false,
    val healthStatus: HealthStatusState = HealthStatusState(),
    val appSettings: AppSettingsEntity = AppSettingsEntity()
)

class FocusGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FocusGuardRepository
    val usageTrackingManager: UsageTrackingManager
    val overlayManager: OverlayManager
    val devicePolicyWrapper: DevicePolicyManagerWrapper

    init {
        val db = AppDatabase.getInstance(application)
        repository = FocusGuardRepository(db.focusGuardDao())
        usageTrackingManager = UsageTrackingManager(application, repository)
        overlayManager = OverlayManager(application)
        devicePolicyWrapper = DevicePolicyManagerWrapper(application)
    }

    private val _healthStatus = MutableStateFlow(getHealthStatus())
    val healthStatus: StateFlow<HealthStatusState> = _healthStatus

    val todayDate = repository.getTodayDateString()
    val protectedApps = repository.protectedApps
    val appSettings = repository.appSettingsFlow

    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        protectedApps,
        repository.getDailyUsageForDate(todayDate),
        appSettings,
        _healthStatus
    ) { apps, usageList, settingsNullable, health ->
        val settings = settingsNullable ?: AppSettingsEntity()

        val primaryApp = apps.firstOrNull { it.packageName == "com.dts.freefireth" } ?: apps.firstOrNull()
        val baseLimitMins = primaryApp?.dailyLimitMinutes ?: 120
        val totalLimitSeconds = baseLimitMins * 60L

        val totalUsed = if (settings.isDebugSimulationEnabled) {
            settings.simulatedUsageSeconds
        } else {
            usageList.sumOf { it.totalUsedSeconds }
        }

        val sessionCount = usageList.sumOf { it.sessionCount }
        val remainingSeconds = (totalLimitSeconds - totalUsed).coerceAtLeast(0L)
        val isBlocked = totalUsed >= totalLimitSeconds

        DashboardUiState(
            todayDateString = todayDate,
            protectedApps = apps,
            totalUsedSeconds = totalUsed,
            totalLimitSeconds = totalLimitSeconds,
            remainingSeconds = remainingSeconds,
            sessionCount = sessionCount,
            isBlocked = isBlocked,
            healthStatus = health,
            appSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun refreshHealthStatus() {
        _healthStatus.value = getHealthStatus()
    }

    private fun getHealthStatus(): HealthStatusState {
        return HealthStatusState(
            hasUsageAccess = usageTrackingManager.hasUsageAccessPermission(),
            hasAccessibility = GamingAccessibilityService.isServiceRunning,
            hasOverlayPermission = overlayManager.hasOverlayPermission(),
            isDeviceOwner = devicePolicyWrapper.isDeviceOwner(),
            isAdminActive = devicePolicyWrapper.isAdminActive()
        )
    }

    fun saveProtectedApp(app: ProtectedAppEntity) {
        viewModelScope.launch {
            repository.saveProtectedApp(app)
        }
    }

    fun deleteProtectedApp(packageName: String) {
        viewModelScope.launch {
            repository.deleteProtectedApp(packageName)
        }
    }

    fun updateSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateAppSettings(settings)
        }
    }

    fun setGuardianPin(pin: String) {
        viewModelScope.launch {
            repository.setGuardianPin(pin)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        return repository.verifyPin(pin)
    }

    fun incrementSimulatedUsage(additionalMinutes: Long) {
        viewModelScope.launch {
            val settings = repository.getAppSettings()
            val current = settings.simulatedUsageSeconds
            val updated = settings.copy(
                isDebugSimulationEnabled = true,
                simulatedUsageSeconds = current + (additionalMinutes * 60L)
            )
            repository.updateAppSettings(updated)
        }
    }

    fun resetSimulatedUsage() {
        viewModelScope.launch {
            val settings = repository.getAppSettings()
            val updated = settings.copy(
                isDebugSimulationEnabled = false,
                simulatedUsageSeconds = 0L
            )
            repository.updateAppSettings(updated)
        }
    }
}
