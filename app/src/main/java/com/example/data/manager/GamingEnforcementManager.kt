package com.example.data.manager

import com.example.data.db.entities.ProtectedAppEntity
import com.example.data.repository.FocusGuardRepository
import kotlinx.coroutines.flow.first
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
    private val usageTrackingManager: UsageTrackingManager
) {

    suspend fun evaluateStatus(
        packageName: String,
        currentContinuousSessionSeconds: Long = 0L
    ): EnforcementStatus {
        val app = repository.getProtectedApp(packageName) ?: return EnforcementStatus.Allowed
        if (!app.isEnabled) return EnforcementStatus.Allowed

        val settings = repository.getAppSettings()
        val todayDate = repository.getTodayDateString()

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

            // Night mode is usually late evening to early morning (e.g. 22:30 to 07:00)
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

        // 2. Check Daily Usage Allowance + Earned Focus Time
        val baseLimitSeconds = app.dailyLimitMinutes * 60L
        val earnedMinutes = if (settings.allowEarnedTime) {
            repository.getTodayEarnedGamingMinutes(todayDate).first() ?: 0
        } else {
            0
        }
        val totalAllowanceSeconds = baseLimitSeconds + (earnedMinutes * 60L)

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
}
