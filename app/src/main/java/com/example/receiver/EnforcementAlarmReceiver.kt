package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.worker.EnforcementWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.data.db.AppDatabase
import com.example.data.repository.FocusGuardRepository

class EnforcementAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val alarmGeneration = intent.getIntExtra("EXTRA_POLICY_GENERATION", -1)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val repo = FocusGuardRepository(db.focusGuardDao())
                val currentGen = repo.getAppSettings().policyGeneration
                
                if (alarmGeneration != -1 && alarmGeneration != currentGen) {
                    // Stale alarm detected.
                    return@launch
                }
                
                EnforcementWorker.runImmediate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
