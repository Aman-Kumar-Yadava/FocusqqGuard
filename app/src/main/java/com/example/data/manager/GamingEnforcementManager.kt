package com.example.data.manager

import com.example.data.db.entities.ProtectedAppEntity
import com.example.data.repository.FocusGuardRepository
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
    private val overlayManager: GamingBlockOverlayManager
) {

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
}
