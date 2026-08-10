package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.entities.AppSettingsEntity
import com.example.data.db.entities.ProtectedAppEntity
import com.example.data.manager.DevicePolicyController
import com.example.data.manager.DevicePolicyManagerWrapper
import com.example.data.manager.GamingBlockOverlayManager
import com.example.data.manager.GamingEnforcementManager
import com.example.data.manager.InstalledAppDetector
import com.example.data.manager.InstalledAppInfo
import com.example.data.manager.OverlayManager
import com.example.data.manager.UsageTrackingManager
import com.example.data.repository.FocusGuardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HealthStatusState(
    val hasUsageAccess: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val isAdminActive: Boolean = false,
    val hasExactAlarmPermission: Boolean = false
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

    val repository: FocusGuardRepository
    val usageTrackingManager: UsageTrackingManager
    val overlayManager: OverlayManager
    val gamingBlockOverlayManager: GamingBlockOverlayManager
    val devicePolicyController: DevicePolicyController
    val devicePolicyWrapper: DevicePolicyManagerWrapper
    val gamingEnforcementManager: GamingEnforcementManager
    val installedAppDetector: InstalledAppDetector

    init {
        val db = AppDatabase.getInstance(application)
        repository = FocusGuardRepository(db.focusGuardDao())
        usageTrackingManager = UsageTrackingManager(application, repository)
        overlayManager = OverlayManager(application)
        gamingBlockOverlayManager = GamingBlockOverlayManager(application)
        devicePolicyController = DevicePolicyController(application)
        devicePolicyWrapper = DevicePolicyManagerWrapper(application)
        gamingEnforcementManager = GamingEnforcementManager(
            repository,
            usageTrackingManager,
            devicePolicyController,
            gamingBlockOverlayManager,
            application
        )
        installedAppDetector = InstalledAppDetector(application)
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
            isDeviceOwner = devicePolicyController.isDeviceOwner(),
            isAdminActive = devicePolicyController.isAdminActive(),
            hasExactAlarmPermission = com.example.data.manager.EnforcementAlarmScheduler.canScheduleExactAlarms(getApplication())
        )
    }

    fun getInstalledNonSystemApps(): List<InstalledAppInfo> {
        val protectedPkgs = dashboardUiState.value.protectedApps.map { it.packageName }.toSet()
        return installedAppDetector.getInstalledNonSystemApps(protectedPkgs)
    }

    fun setAppInstallationBlocked(blocked: Boolean) {
        viewModelScope.launch {
            val success = devicePolicyController.setAppInstallationBlocked(blocked)
            if (success || !devicePolicyController.isDeviceOwner()) {
                val settings = repository.getAppSettings()
                repository.updateAppSettings(settings.copy(
                    isInstallationBlocked = blocked,
                    policyGeneration = settings.policyGeneration + 1
                ))
            }
        }
    }

    fun setAppUninstallationBlocked(blocked: Boolean) {
        viewModelScope.launch {
            val success = devicePolicyController.setAppUninstallationBlocked(blocked)
            val settings = repository.getAppSettings()
            repository.updateAppSettings(settings.copy(
                isUninstallationBlocked = blocked,
                policyGeneration = settings.policyGeneration + 1
            ))
            val protectedAppsList = dashboardUiState.value.protectedApps
            protectedAppsList.forEach { app ->
                devicePolicyController.setUninstallBlockedForPackage(app.packageName, blocked)
            }
        }
    }

    fun saveProtectedApp(app: ProtectedAppEntity) {
        viewModelScope.launch {
            repository.saveProtectedApp(app)
            if (dashboardUiState.value.appSettings.isUninstallationBlocked) {
                devicePolicyController.setUninstallBlockedForPackage(app.packageName, true)
            }
            val settings = repository.getAppSettings()
            repository.updateAppSettings(settings.copy(policyGeneration = settings.policyGeneration + 1))
            gamingEnforcementManager.evaluateAndEnforceAll()
        }
    }

    fun deleteProtectedApp(packageName: String) {
        viewModelScope.launch {
            repository.deleteProtectedApp(packageName)
            devicePolicyController.setUninstallBlockedForPackage(packageName, false)
            val settings = repository.getAppSettings()
            repository.updateAppSettings(settings.copy(policyGeneration = settings.policyGeneration + 1))
            gamingEnforcementManager.evaluateAndEnforceAll()
        }
    }

    fun updateSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            val currentGen = repository.getAppSettings().policyGeneration
            repository.updateAppSettings(settings.copy(policyGeneration = currentGen + 1))
            gamingEnforcementManager.evaluateAndEnforceAll()
        }
    }

    fun setGuardianPin(pin: String) {
        viewModelScope.launch {
            repository.setGuardianPin(pin)
            refreshHealthStatus()
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        return repository.verifyPin(pin)
    }

    fun removeDeviceOwner(): Boolean {
        val success = devicePolicyController.removeDeviceOwner()
        refreshHealthStatus()
        return success
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
            gamingEnforcementManager.evaluateAndEnforceAll()
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
            gamingEnforcementManager.evaluateAndEnforceAll()
        }
    }

}
