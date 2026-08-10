package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.manager.DevicePolicyController
import com.example.data.manager.GamingBlockOverlayManager
import com.example.data.manager.GamingEnforcementManager
import com.example.data.manager.UsageTrackingManager
import com.example.data.repository.FocusGuardRepository
import java.util.concurrent.TimeUnit

class EnforcementWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(context)
            val repository = FocusGuardRepository(db.focusGuardDao())
            val usageTrackingManager = UsageTrackingManager(context, repository)
            val devicePolicyController = DevicePolicyController(context)
            val overlayManager = GamingBlockOverlayManager(context)
            val enforcementManager = GamingEnforcementManager(
                repository,
                usageTrackingManager,
                devicePolicyController,
                overlayManager,
                context
            )

            enforcementManager.evaluateAndEnforceAll(context)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME_PERIODIC = "focus_guard_enforcement_periodic"
        private const val WORK_NAME_IMMEDIATE = "focus_guard_enforcement_immediate"

        fun schedulePeriodic(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<EnforcementWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun runImmediate(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<EnforcementWorker>().build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
