package com.example.data.manager

import com.example.data.db.entities.ProtectedAppEntity
import com.example.data.repository.FocusGuardRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

sealed class BlockReason {
    data class DailyLimitExceeded(val usedSeconds: Long, val totalLimitSeconds: Long) : BlockReason()
    data class ContinuousLimitExceeded(val continuousSeconds: Long, val limitSeconds: Long) : BlockReason()
    data class NightScheduleActive(val startHour: Int, val startMin: Int, val endHour: Int, val endMin: Int) : BlockReason()
    object Disabled : BlockReason()
}

sealed class EnforcementStatus {
    object Allowed : EnforcementStatus()
    data class Blocked(val reason: BlockReason) : EnforcementStatus()
}

class GamingEnforcementManager(
    private val repository: FocusGuardRepository,
    private val usageTrackingManager: UsageTrackingManager,
    private val devicePolicyController: DevicePolicyController,
    private val overlayManager: GamingBlockOverlayManager,
    private val context: android.content.Context? = null
) {

    private suspend fun calculateNextPolicyBoundaryDelays(app: ProtectedAppEntity): List<Long> {
        if (!app.isEnabled) return emptyList()
        val settings = repository.getAppSettings()

        val delays = mutableListOf<Long>()
        val now = Calendar.getInstance()

        // 1. Remaining Daily Allowance
        val totalAllowanceSeconds = app.dailyLimitMinutes * 60L
        val usedSeconds = usageTrackingManager.getTodayUsageSeconds(app.packageName)
        val remainingDailySeconds = totalAllowanceSeconds - usedSeconds
        if (remainingDailySeconds > 0) {
            delays.add(remainingDailySeconds * 1000L)
        }

        // 2. Schedule / Night Lock start and end time
        if (settings.globalNightLockEnabled || app.isScheduleEnabled) {
            val startHour = if (app.isScheduleEnabled) app.allowedEndHour else settings.nightLockStartHour
            val startMin = if (app.isScheduleEnabled) app.allowedEndMinute else settings.nightLockStartMinute
            val endHour = if (app.isScheduleEnabled) app.allowedStartHour else settings.nightLockEndHour
            val endMin = if (app.isScheduleEnabled) app.allowedStartMinute else settings.nightLockEndMinute

            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMin)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (startCal.timeInMillis <= now.timeInMillis) {
                startCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            delays.add(startCal.timeInMillis - now.timeInMillis)

            val endCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMin)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (endCal.timeInMillis <= now.timeInMillis) {
                endCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            delays.add(endCal.timeInMillis - now.timeInMillis)
        }

        return delays.filter { it > 0 }
    }

    suspend fun evaluateStatus(
        packageName: String,
        currentContinuousSessionSeconds: Long = 0L
    ): EnforcementStatus {
        val app = repository.getProtectedApp(packageName) ?: return EnforcementStatus.Allowed
        if (!app.isEnabled) return EnforcementStatus.Allowed

        val settings = repository.getAppSettings()

        // 1. Check Night Lock Schedule
        if (settings.globalNightLockEnabled || app.isScheduleEnabled) {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMin = calendar.get(Calendar.MINUTE)
            val currentMinutesOfDay = currentHour * 60 + currentMin

            val startMinutes = (if (app.isScheduleEnabled) app.allowedEndHour else settings.nightLockStartHour) * 60 +
                    (if (app.isScheduleEnabled) app.allowedEndMinute else settings.nightLockStartMinute)
            val endMinutes = (if (app.isScheduleEnabled) app.allowedStartHour else settings.nightLockEndHour) * 60 +
                    (if (app.isScheduleEnabled) app.allowedStartMinute else settings.nightLockEndMinute)

            val isNightBlocked = if (startMinutes > endMinutes) {
                currentMinutesOfDay >= startMinutes || currentMinutesOfDay < endMinutes
            } else {
                currentMinutesOfDay in startMinutes..<endMinutes
            }

            if (isNightBlocked) {
                return EnforcementStatus.Blocked(
                    BlockReason.NightScheduleActive(
                        startHour = if (app.isScheduleEnabled) app.allowedEndHour else settings.nightLockStartHour,
                        startMin = if (app.isScheduleEnabled) app.allowedEndMinute else settings.nightLockStartMinute,
                        endHour = if (app.isScheduleEnabled) app.allowedStartHour else settings.nightLockEndHour,
                        endMin = if (app.isScheduleEnabled) app.allowedStartMinute else settings.nightLockEndMinute
                    )
                )
            }
        }

        // 2. Check Daily Usage Allowance
        val totalAllowanceSeconds = app.dailyLimitMinutes * 60L
        val usedSeconds = usageTrackingManager.getTodayUsageSeconds(packageName)

        if (usedSeconds >= totalAllowanceSeconds) {
            return EnforcementStatus.Blocked(
                BlockReason.DailyLimitExceeded(
                    usedSeconds = usedSeconds,
                    totalLimitSeconds = totalAllowanceSeconds
                )
            )
        }

        // 3. Check Continuous Session Limit
        val continuousLimitSeconds = app.continuousLimitMinutes * 60L
        if (app.continuousLimitMinutes > 0 && currentContinuousSessionSeconds >= continuousLimitSeconds) {
            return EnforcementStatus.Blocked(
                BlockReason.ContinuousLimitExceeded(
                    continuousSeconds = currentContinuousSessionSeconds,
                    limitSeconds = continuousLimitSeconds
                )
            )
        }

        return EnforcementStatus.Allowed
    }

    suspend fun checkAndEnforceApp(packageName: String): EnforcementStatus {
        val status = evaluateStatus(packageName)
        when (status) {
            is EnforcementStatus.Blocked -> {
                if (devicePolicyController.isDeviceOwner()) {
                    devicePolicyController.setPackageSuspended(packageName, true)
                }
                val reasonText = when (val reason = status.reason) {
                    is BlockReason.DailyLimitExceeded -> "Daily limit of ${reason.totalLimitSeconds / 60}m reached."
                    is BlockReason.ContinuousLimitExceeded -> "Continuous limit reached."
                    is BlockReason.NightScheduleActive -> "Night lock is active."
                    is BlockReason.Disabled -> "App blocked by guardian."
                }
                overlayManager.showBlockingScreen(packageName, reasonText)
            }
            is EnforcementStatus.Allowed -> {
                if (devicePolicyController.isDeviceOwner()) {
                    devicePolicyController.setPackageSuspended(packageName, false)
                }
            }
        }
        return status
    }

    suspend fun evaluateAndEnforceAll(targetContext: android.content.Context? = null): Map<String, EnforcementStatus> {
        val settings = repository.getAppSettings()
        val results = mutableMapOf<String, EnforcementStatus>()
        val activeContext = targetContext ?: context
        
        // Enforce uninstall protection on FocusGuard itself if configured
        if (devicePolicyController.isDeviceOwner() && settings.isUninstallationBlocked) {
            devicePolicyController.setAppUninstallationBlocked(true)
        }

        // Fetch protected apps
        val protectedAppsList = repository.protectedApps.firstOrNull() ?: emptyList()
        val upcomingDelays = mutableListOf<Long>()
        
        // Always add next midnight as a boundary to reset usage stats
        val now = Calendar.getInstance()
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        upcomingDelays.add(nextMidnight.timeInMillis - now.timeInMillis)

        for (app in protectedAppsList) {
            val status = evaluateStatus(app.packageName)
            results[app.packageName] = status

            if (devicePolicyController.isDeviceOwner()) {
                val shouldSuspend = status is EnforcementStatus.Blocked
                devicePolicyController.setPackageSuspended(app.packageName, shouldSuspend)
                
                if (settings.isUninstallationBlocked) {
                    devicePolicyController.setUninstallBlockedForPackage(app.packageName, true)
                }
            }

            upcomingDelays.addAll(calculateNextPolicyBoundaryDelays(app))
        }

        activeContext?.let { ctx ->
            val shortestDelayMs = upcomingDelays.minOrNull()
            if (shortestDelayMs != null && shortestDelayMs > 0) {
                val triggerDelay = shortestDelayMs.coerceAtLeast(5000L)
                EnforcementAlarmScheduler.scheduleExactAlarm(ctx, triggerDelay, settings.policyGeneration)
            } else {
                EnforcementAlarmScheduler.cancelAlarm(ctx)
            }
        }

        return results
    }
}

