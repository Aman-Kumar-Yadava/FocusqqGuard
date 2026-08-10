package com.example.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val guardianPinHash: String = "",
    val guardianPinSalt: String = "",
    val isPinSet: Boolean = false,
    val globalNightLockEnabled: Boolean = true,
    val nightLockStartHour: Int = 22,
    val nightLockStartMinute: Int = 30,
    val nightLockEndHour: Int = 7,
    val nightLockEndMinute: Int = 0,
    val warning30mSent: Boolean = false,
    val warning15mSent: Boolean = false,
    val warning5mSent: Boolean = false,
    val warning1mSent: Boolean = false,
    val isDebugSimulationEnabled: Boolean = false,
    val simulatedUsageSeconds: Long = 0L,
    val isInstallationBlocked: Boolean = false,
    val isUninstallationBlocked: Boolean = false
)
