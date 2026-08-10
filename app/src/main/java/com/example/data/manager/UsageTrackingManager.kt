package com.example.data.manager

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.data.db.entities.DailyUsageEntity
import com.example.data.repository.FocusGuardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class UsageTrackingManager(
    private val context: Context,
    private val repository: FocusGuardRepository
) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /**
     * Checks if Usage Access permission is granted.
     */
    fun hasUsageAccessPermission(): Boolean {
        if (usageStatsManager == null) return false
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 1000 * 60,
            now
        )
        return !stats.isNullOrEmpty()
    }

    /**
     * Reconciles usage for today using UsageStatsManager and Room database.
     */
    suspend fun getTodayUsageSeconds(packageName: String): Long = withContext(Dispatchers.IO) {
        val settings = repository.getAppSettings()
        if (settings.isDebugSimulationEnabled) {
            return@withContext settings.simulatedUsageSeconds
        }

        val todayDate = repository.getTodayDateString()
        var roomUsage = repository.getDailyUsage(todayDate, packageName)?.totalUsedSeconds ?: 0L

        if (hasUsageAccessPermission() && usageStatsManager != null) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            var systemUsageMs = 0L
            statsList?.forEach { stats ->
                if (stats.packageName == packageName) {
                    systemUsageMs += stats.totalTimeInForeground
                }
            }

            val systemUsageSeconds = systemUsageMs / 1000L
            if (systemUsageSeconds > roomUsage) {
                roomUsage = systemUsageSeconds
                // Persist updated reconciled usage
                repository.updateDailyUsage(
                    DailyUsageEntity(
                        dateString = todayDate,
                        packageName = packageName,
                        totalUsedSeconds = roomUsage
                    )
                )
            }
        }

        return@withContext roomUsage
    }
}
