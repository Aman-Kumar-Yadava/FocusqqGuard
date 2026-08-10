package com.example.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.manager.BlockReason
import com.example.data.manager.EnforcementStatus
import com.example.data.manager.GamingEnforcementManager
import com.example.data.manager.OverlayManager
import com.example.data.manager.UsageTrackingManager
import com.example.data.repository.FocusGuardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GamingAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var repository: FocusGuardRepository
    private lateinit var usageTrackingManager: UsageTrackingManager
    private lateinit var enforcementManager: GamingEnforcementManager
    private lateinit var overlayManager: OverlayManager

    private var activeProtectedPackage: String? = null
    private var sessionStartTimeMs: Long = 0L
    private var monitoringJob: Job? = null

    companion object {
        var isServiceRunning = false
            private set
        const val CHANNEL_ID_WARNINGS = "focusguard_warnings_channel"
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        val database = AppDatabase.getInstance(this)
        repository = FocusGuardRepository(database.focusGuardDao())
        usageTrackingManager = UsageTrackingManager(this, repository)
        enforcementManager = GamingEnforcementManager(repository, usageTrackingManager)
        overlayManager = OverlayManager(this)
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        serviceScope.launch {
            val protectedApps = repository.protectedApps.first()
            val isProtected = protectedApps.any { it.packageName == packageName && it.isEnabled }

            if (isProtected) {
                if (activeProtectedPackage != packageName) {
                    activeProtectedPackage = packageName
                    sessionStartTimeMs = System.currentTimeMillis()
                    startMonitoringSession(packageName)
                }
            } else if (activeProtectedPackage != null && packageName != packageName) {
                // Left the protected game
                stopMonitoringSession()
            }
        }
    }

    private fun startMonitoringSession(packageName: String) {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (activeProtectedPackage == packageName) {
                val currentContinuousSeconds = (System.currentTimeMillis() - sessionStartTimeMs) / 1000L
                val status = enforcementManager.evaluateStatus(packageName, currentContinuousSeconds)

                when (status) {
                    is EnforcementStatus.Blocked -> {
                        val reasonText = when (val reason = status.reason) {
                            is BlockReason.DailyLimitExceeded ->
                                "Daily limit of ${reason.totalLimitSeconds / 60}m reached."
                            is BlockReason.ContinuousLimitExceeded ->
                                "Continuous limit of ${reason.limitSeconds / 60}m reached. Take a break!"
                            is BlockReason.NightScheduleActive ->
                                "Night gaming lock active (${reason.startHour}:${"%02d".format(reason.startMin)} - ${reason.endHour}:${"%02d".format(reason.endMin)})."
                            BlockReason.Disabled -> "App blocked by guardian."
                        }
                        overlayManager.showBlockingScreen(packageName, reasonText)
                    }
                    EnforcementStatus.Allowed -> {
                        // Check approaching limit warnings
                        val app = repository.getProtectedApp(packageName)
                        if (app != null) {
                            val totalAllowance = app.dailyLimitMinutes * 60L
                            val usedSeconds = usageTrackingManager.getTodayUsageSeconds(packageName)
                            val remainingSeconds = totalAllowance - usedSeconds

                            if (remainingSeconds in 1..300) { // Under 5 mins
                                sendWarningNotification("Gaming time remaining: ${remainingSeconds / 60 + 1} minutes!")
                            }
                        }
                    }
                }

                delay(3000L) // Poll every 3 seconds while game is active
            }
        }
    }

    private fun stopMonitoringSession() {
        monitoringJob?.cancel()
        activeProtectedPackage = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_WARNINGS,
                "Gaming Limit Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when daily gaming allowance is running low"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendWarningNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_WARNINGS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FocusGuard Warning")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1001, notification)
    }

    override fun onInterrupt() {
        stopMonitoringSession()
    }
}
